package com.example.android_sensor_navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: SensorViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SensorApp(viewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLocation != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorApp(viewModel: SensorViewModel) {
    val isRecording by viewModel.isRecording.collectAsState()
    val samplingRate by viewModel.samplingRate.collectAsState()

    val locationData by viewModel.locationData.collectAsState()
    val accelerometerData by viewModel.accelerometerData.collectAsState()
    val gyroscopeData by viewModel.gyroscopeData.collectAsState()
    val magneticData by viewModel.magneticData.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        // Control Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            var expanded by remember { mutableStateOf(false) }
            val options = listOf(200L to "Fast (200ms)", 500L to "Normal (500ms)", 1000L to "Slow (1000ms)")

            Box {
                Button(onClick = { expanded = true }) {
                    Text(text = "Rate: ${options.find { it.first == samplingRate }?.second ?: (samplingRate.toString() + "ms")}")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { (rate, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.setSamplingRate(rate)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(onClick = { viewModel.toggleRecording() }) {
                Text(text = if (isRecording) "Stop" else "Start")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                LocationCard(locationData)
            }
            item {
                SensorCard("Accelerometer", accelerometerData)
            }
            item {
                SensorCard("Gyroscope", gyroscopeData)
            }
            item {
                SensorCard("Magnetic Field", magneticData)
            }
        }
    }
}

@Composable
fun LocationCard(data: LocationData?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "GPS Location", style = MaterialTheme.typography.titleMedium)
            if (data != null) {
                Text(text = "Lat: ${data.latitude}")
                Text(text = "Lon: ${data.longitude}")
                Text(text = "Alt: ${data.altitude} m")
                Text(text = "Speed: ${data.speed} m/s")
            } else {
                Text(text = "Waiting for data...")
            }
        }
    }
}

@Composable
fun SensorCard(title: String, data: SensorData?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (data != null) {
                Text(text = "X: ${data.values.getOrNull(0) ?: 0f}")
                Text(text = "Y: ${data.values.getOrNull(1) ?: 0f}")
                Text(text = "Z: ${data.values.getOrNull(2) ?: 0f}")
            } else {
                Text(text = "Waiting for data...")
            }
        }
    }
}
