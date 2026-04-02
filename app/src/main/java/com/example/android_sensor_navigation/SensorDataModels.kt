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

