// SensorModule.kt
package me.itissid.privyloci

import android.content.Context
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.itissid.privyloci.sensors.GoogleFusedLocationSensor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideGoogleFusedLocationSensor(
        fusedLocationProviderClient: FusedLocationProviderClient
    ): GoogleFusedLocationSensor {
        return GoogleFusedLocationSensor(fusedLocationProviderClient)
    }

    @Provides
    @Singleton
    fun provideSensorManager(
        googleLocationSensor: GoogleFusedLocationSensor
        // Add more sensors here
    ): SensorManager {
        return SensorManager(googleLocationSensor)
    }
}
