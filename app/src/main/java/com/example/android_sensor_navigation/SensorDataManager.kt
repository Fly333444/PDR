package com.example.android_sensor_navigation

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

class SensorDataManager(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun getSensorDataFlow(sensorType: Int, delayUs: Int): Flow<SensorData> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(sensorType)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == sensorType) {
                    trySend(
                        SensorData(
                            type = event.sensor.type,
                            name = event.sensor.name,
                            values = event.values.clone(),
                            timestamp = event.timestamp
                        )
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, delayUs)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }.conflate()

    @SuppressLint("MissingPermission")
    fun getLocationFlow(minTimeMs: Long, minDistanceM: Float): Flow<LocationData> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(
                    LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        altitude = location.altitude,
                        speed = location.speed,
                        timestamp = location.time
                    )
                )
            }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTimeMs,
                    minDistanceM,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            // Ignore for now
        }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }.conflate()
}

