package com.example.android_sensor_navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.android_sensor_navigation.ui.theme.Android_sensor_navigationTheme
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {

    private val viewModel: SensorViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()

        setContent {
            Android_sensor_navigationTheme(dynamicColor = false) {
                SensorApp(viewModel)
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

private enum class AppPage(val label: String) {
    MAP("地图"),
    DATA("数据"),
    SETTINGS("设置"),
    ABOUT("声明")
}

@Composable
fun SensorApp(viewModel: SensorViewModel) {
    var entered by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.background
    ) {
        if (entered) {
            MainShell(viewModel = viewModel)
        } else {
            WelcomeScreen(onEnter = { entered = true })
        }
    }
}

@Composable
private fun WelcomeScreen(onEnter: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.deepInk)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Locus_PDR",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onEnter,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.accentBlue),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .height(50.dp)
                .width(168.dp)
        ) {
            Text(text = "进入", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MainShell(viewModel: SensorViewModel) {
    var selectedPage by rememberSaveable { mutableStateOf(AppPage.MAP) }
    var horizontalDrag by remember { mutableStateOf(0f) }
    val isRecording by viewModel.isRecording.collectAsState()
    val samplingRate by viewModel.samplingRate.collectAsState()
    val locationData by viewModel.locationData.collectAsState()
    val accelerometerData by viewModel.accelerometerData.collectAsState()
    val gyroscopeData by viewModel.gyroscopeData.collectAsState()
    val magneticData by viewModel.magneticData.collectAsState()
    val accelerometerHistory by viewModel.accelerometerHistory.collectAsState()
    val gyroscopeHistory by viewModel.gyroscopeHistory.collectAsState()
    val magneticHistory by viewModel.magneticHistory.collectAsState()
    val pdrSettings by viewModel.pdrSettings.collectAsState()
    val pdrState by viewModel.pdrState.collectAsState()

    Scaffold(
        containerColor = AppColors.background,
        bottomBar = {
            BottomSwitch(selectedPage = selectedPage, onSelect = { selectedPage = it })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(selectedPage) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            selectedPage = when {
                                horizontalDrag < -80f -> selectedPage.next()
                                horizontalDrag > 80f -> selectedPage.previous()
                                else -> selectedPage
                            }
                            horizontalDrag = 0f
                        },
                        onDragCancel = { horizontalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            horizontalDrag += dragAmount
                            change.consume()
                        }
                    )
                }
        ) {
            when (selectedPage) {
                AppPage.MAP -> MapScreen(
                    pdrState = pdrState,
                    settings = pdrSettings,
                    locationData = locationData,
                    isRecording = isRecording,
                    samplingRate = samplingRate,
                    onToggleRecording = viewModel::toggleRecording,
                    onSamplingRate = viewModel::setSamplingRate,
                    onReset = viewModel::resetPdr
                )

                AppPage.DATA -> DataScreen(
                    accelerometerData = accelerometerData,
                    gyroscopeData = gyroscopeData,
                    magneticData = magneticData,
                    accelerometerHistory = accelerometerHistory,
                    gyroscopeHistory = gyroscopeHistory,
                    magneticHistory = magneticHistory,
                    pdrState = pdrState,
                    isRecording = isRecording,
                    samplingRate = samplingRate,
                    onToggleRecording = viewModel::toggleRecording,
                    onSamplingRate = viewModel::setSamplingRate
                )

                AppPage.SETTINGS -> SettingsScreen(
                    settings = pdrSettings,
                    onHeightChanged = viewModel::updateHeightCm,
                    onModelCChanged = viewModel::updateModelC,
                    onDrawTrajectoryChanged = viewModel::updateDrawTrajectory,
                    onDataProcessingModeChanged = viewModel::updateDataProcessingMode,
                    onPositioningModeChanged = viewModel::updatePositioningMode
                )

                AppPage.ABOUT -> AboutScreen()
            }
        }
    }
}

private fun AppPage.next(): AppPage {
    val pages = AppPage.entries
    return pages[(ordinal + 1).coerceAtMost(pages.lastIndex)]
}

private fun AppPage.previous(): AppPage {
    val pages = AppPage.entries
    return pages[(ordinal - 1).coerceAtLeast(0)]
}

@Composable
private fun BottomSwitch(selectedPage: AppPage, onSelect: (AppPage) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppPage.entries.forEach { page ->
                val selected = selectedPage == page
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (selected) AppColors.accentBlue else Color.Transparent)
                        .clickable { onSelect(page) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = page.label,
                        color = if (selected) Color.White else AppColors.secondaryText,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun MapScreen(
    pdrState: PdrState,
    settings: PdrSettings,
    locationData: LocationData?,
    isRecording: Boolean,
    samplingRate: Long,
    onToggleRecording: () -> Unit,
    onSamplingRate: (Long) -> Unit,
    onReset: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderControl(
                title = "Locus_PDR",
                isRecording = isRecording,
                samplingRate = samplingRate,
                onToggleRecording = onToggleRecording,
                onSamplingRate = onSamplingRate
            )
        }
        item {
            InfoCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("轨迹地图", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${settings.positioningMode.label} · ${pdrState.stepCount} 步",
                            color = AppColors.secondaryText
                        )
                    }
                    TextButton(onClick = onReset) {
                        Text("重置")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TrajectoryMap(
                    trajectory = pdrState.trajectory,
                    drawTrajectory = settings.drawTrajectory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("航向", "${pdrState.headingDegrees.format(1)}°", Modifier.weight(1f))
                MetricTile("步长", "${pdrState.lastStepLength.format(2)} m", Modifier.weight(1f))
                MetricTile("磁场", "${pdrState.magneticMagnitude.format(1)} uT", Modifier.weight(1f))
            }
        }
        item {
            InfoCard {
                Text("GPS 状态", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                if (locationData == null) {
                    Text("等待定位数据", color = AppColors.secondaryText)
                } else {
                    Text("Lat: ${locationData.latitude.format(6)}")
                    Text("Lon: ${locationData.longitude.format(6)}")
                    Text("Alt: ${locationData.altitude.format(1)} m")
                    Text("Speed: ${locationData.speed.format(2)} m/s")
                }
            }
        }
    }
}

@Composable
private fun DataScreen(
    accelerometerData: SensorData?,
    gyroscopeData: SensorData?,
    magneticData: SensorData?,
    accelerometerHistory: List<SensorSeriesPoint>,
    gyroscopeHistory: List<SensorSeriesPoint>,
    magneticHistory: List<SensorSeriesPoint>,
    pdrState: PdrState,
    isRecording: Boolean,
    samplingRate: Long,
    onToggleRecording: () -> Unit,
    onSamplingRate: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeaderControl(
                title = "传感器数据",
                isRecording = isRecording,
                samplingRate = samplingRate,
                onToggleRecording = onToggleRecording,
                onSamplingRate = onSamplingRate
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("X 期望", "${pdrState.meanX.format(2)} m", Modifier.weight(1f))
                MetricTile("Y 期望", "${pdrState.meanY.format(2)} m", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("X 方差", pdrState.varianceX.format(3), Modifier.weight(1f))
                MetricTile("Y 方差", pdrState.varianceY.format(3), Modifier.weight(1f))
            }
        }
        item {
            SensorCard("Accelerometer", accelerometerData)
        }
        item {
            SensorChartCard("加速度模值", accelerometerHistory, AppColors.accentBlue)
        }
        item {
            SensorCard("Gyroscope", gyroscopeData)
        }
        item {
            SensorChartCard("角速度模值", gyroscopeHistory, AppColors.green)
        }
        item {
            SensorCard("Magnetic Field", magneticData)
        }
        item {
            SensorChartCard("磁感应强度", magneticHistory, AppColors.red)
        }
    }
}

@Composable
private fun SettingsScreen(
    settings: PdrSettings,
    onHeightChanged: (Float) -> Unit,
    onModelCChanged: (Float) -> Unit,
    onDrawTrajectoryChanged: (Boolean) -> Unit,
    onDataProcessingModeChanged: (DataProcessingMode) -> Unit,
    onPositioningModeChanged: (PositioningMode) -> Unit
) {
    var heightText by remember(settings.heightCm) { mutableStateOf(settings.heightCm.format(0)) }
    var modelCText by remember(settings.modelC) { mutableStateOf(settings.modelC.format(2)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.deepInk),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "设置",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsGroup(title = "PDR 模型") {
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = {
                            heightText = it
                            it.toFloatOrNull()?.let(onHeightChanged)
                        },
                        label = { Text("身高 (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modelCText,
                        onValueChange = {
                            modelCText = it
                            it.toFloatOrNull()?.let(onModelCChanged)
                        },
                        label = { Text("模型 C 值") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsGroup(title = "轨迹绘制") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = settings.drawTrajectory,
                            onCheckedChange = onDrawTrajectoryChanged
                        )
                        Text("绘制轨迹")
                    }
                }

                SettingsGroup(title = "数据处理模式") {
                    SelectField(
                        selectedLabel = settings.dataProcessingMode.label,
                        options = DataProcessingMode.entries,
                        optionLabel = { it.label },
                        onSelected = onDataProcessingModeChanged
                    )
                }

                SettingsGroup(title = "定位模式") {
                    SelectField(
                        selectedLabel = settings.positioningMode.label,
                        options = PositioningMode.entries,
                        optionLabel = { it.label },
                        onSelected = onPositioningModeChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "声明",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            InfoCard {
                Text("使用说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. 进入应用后在地图或数据页面点击 Start 开始采集传感器与定位数据。")
                Text("2. 地图页面会根据 PDR 步检、航向融合和步长模型绘制相对轨迹。")
                Text("3. 数据页面展示实时传感器数值、随时间变化图像，以及 PDR 轨迹期望和方差。")
                Text("4. 设置页面可调整身高、模型 C 值、轨迹绘制、数据处理模式和定位模式。")
            }
        }
        item {
            InfoCard {
                Text("制作人", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Locus_PDR 项目组")
                Text("本应用用于 Android 传感器导航、行人航位推算与轨迹跟踪实验。", color = AppColors.secondaryText)
            }
        }
    }
}

@Composable
private fun HeaderControl(
    title: String,
    isRecording: Boolean,
    samplingRate: Long,
    onToggleRecording: () -> Unit,
    onSamplingRate: (Long) -> Unit
) {
    InfoCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    text = if (isRecording) "传感器采集中" else "等待开始",
                    color = if (isRecording) AppColors.green else AppColors.secondaryText
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SamplingRateMenu(currentRate = samplingRate, onSelected = onSamplingRate)
                Button(
                    onClick = onToggleRecording,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) AppColors.red else AppColors.accentBlue
                    )
                ) {
                    Text(if (isRecording) "Stop" else "Start")
                }
            }
        }
    }
}

@Composable
private fun SamplingRateMenu(currentRate: Long, onSelected: (Long) -> Unit) {
    val options = listOf(200L to "200ms", 500L to "500ms", 1000L to "1000ms")
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(options.firstOrNull { it.first == currentRate }?.second ?: "${currentRate}ms")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (rate, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(rate)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    InfoCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun <T> SelectField(
    selectedLabel: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true },
            color = AppColors.controlFill,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(selectedLabel)
                Text("∨", color = AppColors.secondaryText)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = AppColors.secondaryText, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SensorCard(title: String, data: SensorData?) {
    InfoCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (data == null) {
            Text("Waiting for data...", color = AppColors.secondaryText)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AxisValue("X", data.values.getOrNull(0).orZero().format(3), Modifier.weight(1f))
                AxisValue("Y", data.values.getOrNull(1).orZero().format(3), Modifier.weight(1f))
                AxisValue("Z", data.values.getOrNull(2).orZero().format(3), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AxisValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.controlFill)
            .padding(10.dp)
    ) {
        Text(label, color = AppColors.secondaryText, style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SensorChartCard(title: String, points: List<SensorSeriesPoint>, lineColor: Color) {
    InfoCard {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(10.dp))
        LineChart(
            points = points,
            lineColor = lineColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
        )
    }
}

@Composable
private fun LineChart(points: List<SensorSeriesPoint>, lineColor: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.chartFill)
            .padding(8.dp)
    ) {
        val gridColor = AppColors.grid.copy(alpha = 0.55f)
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        if (points.size < 2) return@Canvas

        val values = points.map { it.value }
        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 1f
        val range = max(0.01f, maxValue - minValue)
        val stepX = size.width / (points.size - 1)
        var previous: Offset? = null
        points.forEachIndexed { index, point ->
            val x = stepX * index
            val y = size.height - ((point.value - minValue) / range) * size.height
            val current = Offset(x, y)
            previous?.let {
                drawLine(lineColor, it, current, strokeWidth = 4f, cap = StrokeCap.Round)
            }
            previous = current
        }
    }
}

@Composable
private fun TrajectoryMap(
    trajectory: List<TrajectoryPoint>,
    drawTrajectory: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.mapFill)
    ) {
        val gridColor = AppColors.grid.copy(alpha = 0.65f)
        val gridStep = size.width / 6f
        var xLine = 0f
        while (xLine <= size.width) {
            drawLine(gridColor, Offset(xLine, 0f), Offset(xLine, size.height), strokeWidth = 1f)
            xLine += gridStep
        }
        var yLine = 0f
        while (yLine <= size.height) {
            drawLine(gridColor, Offset(0f, yLine), Offset(size.width, yLine), strokeWidth = 1f)
            yLine += gridStep
        }

        val points = if (drawTrajectory) trajectory else emptyList()
        if (points.isEmpty()) return@Canvas

        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val rangeX = max(2f, maxX - minX + 2f)
        val rangeY = max(2f, maxY - minY + 2f)
        val scale = min(size.width / rangeX, size.height / rangeY) * 0.86f

        fun toCanvas(point: TrajectoryPoint): Offset {
            return Offset(
                x = size.width / 2f + (point.x - centerX) * scale,
                y = size.height / 2f - (point.y - centerY) * scale
            )
        }

        var previous = toCanvas(points.first())
        drawCircle(AppColors.deepInk, radius = 6f, center = previous)
        points.drop(1).forEach { point ->
            val current = toCanvas(point)
            drawLine(AppColors.accentBlue, previous, current, strokeWidth = 6f, cap = StrokeCap.Round)
            previous = current
        }
        drawCircle(AppColors.red, radius = 8f, center = previous)
    }
}

private object AppColors {
    val background = Color(0xFFF4F6F8)
    val deepInk = Color(0xFF111827)
    val secondaryText = Color(0xFF687382)
    val accentBlue = Color(0xFF2F7CF6)
    val green = Color(0xFF18A058)
    val red = Color(0xFFE5484D)
    val mapFill = Color(0xFFE9F0F2)
    val chartFill = Color(0xFFF1F4F7)
    val controlFill = Color(0xFFF5F7FA)
    val grid = Color(0xFFB9C4D0)
}

private fun Float.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)

private fun Double.format(digits: Int): String = String.format(Locale.US, "%.${digits}f", this)

private fun Float?.orZero(): Float = this ?: 0f
