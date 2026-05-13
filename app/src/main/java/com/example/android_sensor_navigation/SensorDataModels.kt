package com.example.android_sensor_navigation

data class SensorData(
    val type: Int,
    val name: String,
    val values: FloatArray,
    val timestamp: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SensorData
        if (type != other.type) return false
        if (name != other.name) return false
        if (!values.contentEquals(other.values)) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type
        result = 31 * result + name.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Float,
    val timestamp: Long
)

data class SensorSeriesPoint(
    val timestamp: Long,
    val value: Float
)

data class TrajectoryPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long
)

enum class DataProcessingMode(val label: String) {
    REAL_TIME("实时处理模式"),
    FILE("文件处理模式")
}

enum class PositioningMode(val label: String) {
    GPS("1.GPS定位"),
    PDR("2.PDR定位"),
    INDOOR_NETWORK_PDR("3.室内网络+PDR定位"),
    GPS_PDR("4.GPS+PDR定位")
}

data class PdrSettings(
    val heightCm: Float = 175f,
    val modelC: Float = 0.28f,
    val drawTrajectory: Boolean = true,
    val dataProcessingMode: DataProcessingMode = DataProcessingMode.REAL_TIME,
    val positioningMode: PositioningMode = PositioningMode.PDR
)

data class PdrState(
    val trajectory: List<TrajectoryPoint> = listOf(TrajectoryPoint(0f, 0f, System.currentTimeMillis())),
    val stepCount: Int = 0,
    val headingDegrees: Float = 0f,
    val lastStepLength: Float = 0f,
    val meanX: Float = 0f,
    val meanY: Float = 0f,
    val varianceX: Float = 0f,
    val varianceY: Float = 0f,
    val magneticMagnitude: Float = 0f,
    val magneticFusionWeight: Float = 0f
)

