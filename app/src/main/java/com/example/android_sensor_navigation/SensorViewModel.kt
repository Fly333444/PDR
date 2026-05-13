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
    private val pdrProcessor = PdrProcessor()

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

    private val _accelerometerHistory = MutableStateFlow<List<SensorSeriesPoint>>(emptyList())
    val accelerometerHistory = _accelerometerHistory.asStateFlow()

    private val _gyroscopeHistory = MutableStateFlow<List<SensorSeriesPoint>>(emptyList())
    val gyroscopeHistory = _gyroscopeHistory.asStateFlow()

    private val _magneticHistory = MutableStateFlow<List<SensorSeriesPoint>>(emptyList())
    val magneticHistory = _magneticHistory.asStateFlow()

    private val _pdrSettings = MutableStateFlow(PdrSettings())
    val pdrSettings = _pdrSettings.asStateFlow()

    private val _pdrState = MutableStateFlow(PdrState())
    val pdrState = _pdrState.asStateFlow()

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

    fun updateHeightCm(value: Float) {
        _pdrSettings.value = _pdrSettings.value.copy(heightCm = value.coerceIn(80f, 230f))
    }

    fun updateModelC(value: Float) {
        _pdrSettings.value = _pdrSettings.value.copy(modelC = value.coerceIn(0.05f, 1.2f))
    }

    fun updateDrawTrajectory(value: Boolean) {
        _pdrSettings.value = _pdrSettings.value.copy(drawTrajectory = value)
    }

    fun updateDataProcessingMode(value: DataProcessingMode) {
        _pdrSettings.value = _pdrSettings.value.copy(dataProcessingMode = value)
    }

    fun updatePositioningMode(value: PositioningMode) {
        _pdrSettings.value = _pdrSettings.value.copy(positioningMode = value)
    }

    fun resetPdr() {
        pdrProcessor.reset()
        _pdrState.value = PdrState()
        _accelerometerHistory.value = emptyList()
        _gyroscopeHistory.value = emptyList()
        _magneticHistory.value = emptyList()
    }

    private fun startRecording() {
        if (_isRecording.value) return
        _isRecording.value = true

        val rate = _samplingRate.value
        val rateUs = (rate * 1000).toInt()

        accJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_ACCELEROMETER, rateUs).collect { data ->
                _accelerometerData.value = data
                appendSeries(_accelerometerHistory, data)
                _pdrState.value = pdrProcessor.process(
                    accelerometer = data,
                    gyroscope = _gyroscopeData.value,
                    magnetic = _magneticData.value,
                    settings = _pdrSettings.value
                )
                delay(rate)
            }
        }

        gyroJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_GYROSCOPE, rateUs).collect { data ->
                _gyroscopeData.value = data
                appendSeries(_gyroscopeHistory, data)
                delay(rate)
            }
        }

        magJob = viewModelScope.launch {
            sensorDataManager.getSensorDataFlow(Sensor.TYPE_MAGNETIC_FIELD, rateUs).collect { data ->
                _magneticData.value = data
                appendSeries(_magneticHistory, data)
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

    private fun appendSeries(
        flow: MutableStateFlow<List<SensorSeriesPoint>>,
        data: SensorData
    ) {
        val magnitude = kotlin.math.sqrt(data.values.take(3).sumOf { (it * it).toDouble() }).toFloat()
        flow.value = (flow.value + SensorSeriesPoint(System.currentTimeMillis(), magnitude)).takeLast(80)
    }
}
