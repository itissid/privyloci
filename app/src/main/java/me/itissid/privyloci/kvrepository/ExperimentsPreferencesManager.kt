package me.itissid.privyloci.kvrepository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import me.itissid.privyloci.kvrepository.UserPreferences.Companion.readWrapper
import javax.inject.Inject
import javax.inject.Singleton

private val Context.experimentStore by preferencesDataStore("privy_loci_experiments")

@Singleton
class ExperimentsPreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val dataStore = context.experimentStore

    val headPhoneOnboardingExperimentOn: Flow<Boolean> =
        dataStore.readWrapper(
            HEADPHONE_EXPERIMENT_ON_KEY,
            false,
            "ExperimentsPreferencesManager"
        )

    suspend fun setExperimentFlag(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[HEADPHONE_EXPERIMENT_ON_KEY] = value
        }
    }

    companion object {
        private val HEADPHONE_EXPERIMENT_ON_KEY =
            booleanPreferencesKey("headphone_onboarding_experiment_on")
    }
}