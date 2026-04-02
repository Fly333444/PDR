package com.example.android_sensor_navigation

import android.app.Application
import android.hardware.Sensor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorDataManager = SensorDataManager(application)

    private val _accelerometerData = MutableStateFlow<SensorData?>(null)
    val accelerometerData = _accelerometerData.asStateFlow()

    private val _gyroscopeData = MutableStateFlow<SensorData?>(null)
    val gyroscopeData = _gyroscopeData.asStateFlow()

    private val _magneticData = MutableStateFlow<SensorData?>(null)
    val magneticData = _magneticData.asStateFlow()

    private val _locationData = MutableStateFlow<LocationData?>(null)
    val locationData = _locationData.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _samplingRate = MutableStateFlow(1000L) // in milliseconds
    val samplingRate = _samplingRate.asStateFlow()

    private var accJob: Job? = null
    private var gyroJob: Job? = null
    private var magJob: Job? = null
    private var locationJob: Job? = null

    fun setSamplingRate(rateMs: Long) {
        _samplingRate.value = rateMs
        if (_isRecording.value) {
            stopRecording()
            startRecording()
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        if (_isRecording.value) return
        _isRecording.value = true

        val rate = _samplingRate.value
        val rateUs = (rate * 1000).toInt()

        accJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_ACCELEROMETER, rateUs).collect { data ->
                _accelerometerData.value = data
                delay(rate)
            }
        }

        gyroJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_GYROSCOPE, rateUs).collect { data ->
                _gyroscopeData.value = data
                delay(rate)
            }
        }

        magJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_MAGNETIC_FIELD, rateUs).collect { data ->
                _magneticData.value = data
                delay(rate)
            }
        }

        locationJob = viewModelScope.launch {
            sensorDataManager.getLocationFlow(rate, 0f).collect { data ->
                _locationData.value = data
            }
        }
    }

    private fun stopRecording() {
        _isRecording.value = false
        accJob?.cancel()
        gyroJob?.cancel()
        magJob?.cancel()
        locationJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}

