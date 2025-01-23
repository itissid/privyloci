package me.itissid.privyloci.service

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioAttributes.USAGE_MEDIA
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.itissid.privyloci.MainActivity
import me.itissid.privyloci.R
import me.itissid.privyloci.SensorManager
import me.itissid.privyloci.SubscriptionManager
import me.itissid.privyloci.util.Logger
import javax.inject.Inject
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.OutOfQuotaPolicy
import me.itissid.privyloci.kvrepository.ExperimentsPreferencesManager
import me.itissid.privyloci.kvrepository.UserPreferences
import me.itissid.privyloci.kvrepository.Repository

private const val PERSISTENT_FG_NOTIFICATION = 1

private const val FG_SERVICE_NOTIFICATION_DISMISSED =
    "ACTION_FROM_FOREGROUND_SERVICE_NOTIFICATION_DISMISSAL"

@AndroidEntryPoint
class PrivyForegroundService : Service() {
    @Inject
    lateinit var sensorManager: SensorManager

    @Inject
    lateinit var subscriptionManager: SubscriptionManager

    @Inject
    lateinit var repository: Repository
    val TAG: String = "PrivyForegroundService"

    @Inject
    lateinit var experimentsManager: ExperimentsPreferencesManager

    // Media playback objects:
    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    companion object {
        const val CHANNEL_ID = "PrivyLociForegroundServiceChannel"
        const val ACTION_SERVICE_STARTED = "me.itissid.privyloci.ACTION_SERVICE_STARTED"
        const val ACTION_SERVICE_STOPPED = "me.itissid.privyloci.ACTION_SERVICE_STOPPED"
        const val ACTION_PLAY_ONBOARDING_SOUND = "me.itissid.privyloci.action.PLAY_ONBOARDING_SOUND"
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d(this::class.java.simpleName, "In onCreate Privy Loci Foreground Service")

        // Start the foreground service with notification
        // recheck permissions here
        startForegroundServiceWithNotification()
        // Initialize SensorManager
        sensorManager.initialize(this)
        initMediaSessionAndPlayer()
        // Initialize SubscriptionManager lazily
        CoroutineScope(Dispatchers.IO).launch {
            subscriptionManager.initialize(this@PrivyForegroundService)
            repository.setServiceRunning(true)
            experimentsManager.headphoneOnboardingExperimentComplete.collect {
                playedOnboardingSound = it
            }
        }
    }

    lateinit var handlerThread: HandlerThread
    lateinit var audioFocusHandler: Handler

    private fun initMediaSessionAndPlayer() {
        if (mediaSession == null && exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()

            // Create a MediaSession using AndroidX Media3
            mediaSession = MediaSession.Builder(this, exoPlayer!!)
                .setSessionActivity(pendingMainActivityIntent())
                .build()

            Logger.i(TAG, "Media session and ExoPlayer initialized")
        }
    }

    private fun pendingMainActivityIntent(): PendingIntent {
        val notificationIntent = Intent(this, MainActivity::class.java)
        // Possibly set flags
        return PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    var playBackDelayed = false
    var resumeOnFocusGained = false
    var playedOnboardingSound = false // TODO: make this persistent for different devices.
    val focusLock = Any()
    private fun setPlayShortOnboardingSound() {
        // So that the Looper Thread can continue doing its job.
        handlerThread = HandlerThread("AudioFocusHandlerThread").apply { start() }
        val looper = handlerThread.looper
        audioFocusHandler = Handler(looper)

        // Just hold the focus briefly for the on boarding.
        val focusResult = requestAudioFocus()
        synchronized(focusLock) {
            when (focusResult) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                    Logger.v(
                        TAG,
                        "AUDIOFOCUS_REQUEST_GRANTED. Play Onboarding sound? $playedOnboardingSound"
                    )
                    if (!playedOnboardingSound) {
                        playNow()
                    }
                    // N2S do we call experimentManager.setOnboardingComplete here?
                    playedOnboardingSound = true
                }

                AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                    Logger.v(TAG, "AUDIOFOCUS_REQUEST_DELAYED")
                    playBackDelayed = true
                }
            }
        }
        // Keep the session around for some time to test with geofencing.
    }

    private fun playNow() {
        val player = exoPlayer ?: return
        val audioUri =
            Uri.parse("android.resource://${packageName}/${R.raw.privy_loci_headphone_connect}")

        player.setMediaItem(MediaItem.fromUri(audioUri))
        player.prepare()
        player.play()
        CoroutineScope(Dispatchers.IO).launch {
            experimentsManager.setOnboadingComplete(true)
        }
        updateNotification("Monitoring your subs(onboarding is on)")
    }

    private fun pausePlayback() {
        val player = exoPlayer ?: return
        player.pause()
    }

    private fun isPlaying(): Boolean {
        val player = exoPlayer ?: return false
        return player.isPlaying
    }

    private val afChangeListener =
        AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // TODO: When I get the event from a geo fence i will play it here.
                    synchronized(focusLock) {
                        if (playBackDelayed || resumeOnFocusGained) {
                            Logger.v(
                                TAG,
                                "AUDIOFOCUS_GAIN, play onboarding sound? $playedOnboardingSound"
                            )
                            playBackDelayed = false
                            if (!playedOnboardingSound) {
                                playNow()
                            }
                            playedOnboardingSound = true

                        } else {
                            Logger.v(
                                TAG,
                                "AUDIOFOCUS_GAIN playBackDelayed? $playBackDelayed, resumeOnFocusGained? $resumeOnFocusGained"
                            )
                        }
                    }
                }

                AudioManager.AUDIOFOCUS_LOSS -> {
                    Logger.v(TAG, "AUDIOFOCUS_LOSS, we won't be able to play unless")
                    playBackDelayed = false
                    resumeOnFocusGained = false
                    pausePlayback()

                    // TODO: Create a snackbar notification instead
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    Logger.v(TAG, "AUDIOFOCUS_LOSS_TRANSIENT")
                    synchronized(focusLock) {
                        resumeOnFocusGained = isPlaying()
                        playBackDelayed = false
                    }
                    pausePlayback()
                    // continue to monitoring for audio focus.
                }

                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    Logger.v(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
                    pausePlayback()
                    // still continue to monitoring for audio focus.
                }
            }
        }

    private fun requestAudioFocus(): Int {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val audioFocusRequest =
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(USAGE_MEDIA)
                    setContentType(CONTENT_TYPE_MUSIC)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(afChangeListener, audioFocusHandler)
            }.build()
        return audioManager.requestAudioFocus(audioFocusRequest)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            Logger.i(
                this::class.java.simpleName,
                "Called onStartCommand with null intent means the service was previously killed, but was recreated by the system when resources are available."
            )
        }
        Logger.d(
            this::class.java.simpleName,
            "Privy Loci onStartCommand called with action ${intent?.action}"
        )
        when (intent?.action) {
            ACTION_PLAY_ONBOARDING_SOUND -> {
                // request creates an audio request: Like onboarding vs GeofenceEvent etc
                Logger.v(TAG, "Received request to play onboarding sound")
                setPlayShortOnboardingSound()
            }
            // Add other actions if needed
        }

        // Service is already running, so return START_STICKY to keep it alive.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d(this::class.java.simpleName, "Privy Loci Foreground Service destroyed")


        // Clean up resources
        try {
            sensorManager.shutdown()
            subscriptionManager.shutdown()
            exoPlayer?.release()
            exoPlayer = null
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            Logger.e(
                this::class.java.simpleName,
                "Error shutting down subscription or sensor manager",
                e
            )
        } finally {
            // N2S: We just know that the service was started or stopped, it could be by the system/user.
            // For explicit user stop I use the NotificationDismissedReceiver
            CoroutineScope(Dispatchers.IO).launch {
                repository.setServiceRunning(false)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // When user removes a task from the preview
        Logger.d(this::class.java.simpleName, "FG Task removed")

    }

    override fun onBind(intent: Intent?): IBinder? {
        // Only consider bindings if one wants to communicate with the service.
        Logger.w(this::class.java.simpleName, "Service was non-bindable. Ignoring")
        return null
    }

    private fun startForegroundServiceWithNotification() {
        // Create the persistent notification
        try {
            val notification = createNotification()
            // Start the service in the foreground with the notification
            startForeground(PERSISTENT_FG_NOTIFICATION, notification)
            // Close any old notification for dismissal.
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(FG_NOTIFICATION_DISMISSED_DISMISSAL_NOTIFICATION_ID)
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                // TODO: How does one set up instrumentation for tracking these errors in production?
                // N2S: This happens typically due to lack of permissions but also due to a bug in the app where we try start a FG service from background. e.g. using LaunchedEffects etc.

                Log.e(
                    "PrivyForegroundService",
                    "Foreground service start not allowed for Android SDK version `${Build.VERSION.SDK_INT}` >= ${Build.VERSION_CODES.S}"
                )
            } else {
                Log.e(
                    "PrivyForegroundService",
                    "Error starting foreground service",
                    e
                )
            }
            // TODO: Sometimes what happens is that the system tries to restart the service when its closed(I think due to the STICKY option)
            // When a user intentionally pauses the collection, perhapps it is better not to shutdown the service but instead just stop the sensor collection.
            throw e
        }
    }

    private fun updateNotification(message: String) {
        val updatedNotification = createNotification(contentText = message)
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PERSISTENT_FG_NOTIFICATION, updatedNotification)
    }

    private fun createNotification(
        title: String = "Privy Loci is running",
        contentText: String = "Monitoring your subscriptions"
    ): Notification {
        // Create an intent that will open the MainActivity when the notification is tapped
        val deleteIntent = Intent(this, NotificationDismissedReceiver::class.java).apply {
            action = FG_SERVICE_NOTIFICATION_DISMISSED
        }
        val pendingDeleteIntent =
            PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // small icon for the notification
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(pendingDeleteIntent)
            .setOngoing(true)
            .build()
    }
}

@AndroidEntryPoint
class NotificationDismissedReceiver : BroadcastReceiver() {
    @Inject
    lateinit var userPreferences: UserPreferences // Hilt will inject this

    @Inject
    lateinit var repository: Repository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (intent.action) {
            FG_SERVICE_NOTIFICATION_DISMISSED -> {
                // Have a viewmodel here set so that
                Logger.d(
                    "NotificationDismissedReceiver",
                    "Persistent Notification dismissed, stopping FG Service"
                )
                context.let {
                    // N2S: I decided to stop the foreground services en-masse but we could be more sparing.
                    // We can send an intent to stop services that have private data only and let others run.
                    stopPrivyForegroundService(it)
                    CoroutineScope(Dispatchers.IO).launch {
                        // N2S: Will this work if the application context is cached?
                        Logger.v(
                            "NotificationDismissedReceiver",
                            "Setting FG Persistent Notification Dismissed"
                        )

                        userPreferences.setUserPausedLocationCollection(true)
                        repository.setFGPersistentNotificationDismissed(true)
                    }
                }
                // TODO: Also update the user preference that the notification was dismissed.
                if (context != null) {

                    val workRequest = OneTimeWorkRequestBuilder<ServiceStoppedWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            }

        }
    }
}

