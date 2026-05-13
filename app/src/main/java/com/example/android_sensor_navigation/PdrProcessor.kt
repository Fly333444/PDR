package com.example.android_sensor_navigation

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PdrProcessor {
    private val accelerationWindow = ArrayDeque<Float>()
    private val maxWindowSize = 48
    private var gravityMagnitude = 9.81f
    private var lowThresholdArmed = true
    private var lastStepTimestampNs = 0L
    private var lastGyroTimestampNs = 0L
    private var headingRadians = 0f
    private var x = 0f
    private var y = 0f
    private var stepCount = 0
    private var lastStepLength = 0f
    private var state = PdrState()

    fun reset() {
        accelerationWindow.clear()
        gravityMagnitude = 9.81f
        lowThresholdArmed = true
        lastStepTimestampNs = 0L
        lastGyroTimestampNs = 0L
        headingRadians = 0f
        x = 0f
        y = 0f
        stepCount = 0
        lastStepLength = 0f
        state = PdrState()
    }

    fun process(
        accelerometer: SensorData,
        gyroscope: SensorData?,
        magnetic: SensorData?,
        settings: PdrSettings
    ): PdrState {
        updateHeading(accelerometer, gyroscope, magnetic)

        val accelerationMagnitude = magnitude(accelerometer.values)
        gravityMagnitude = if (gravityMagnitude == 0f) {
            accelerationMagnitude
        } else {
            gravityMagnitude * 0.92f + accelerationMagnitude * 0.08f
        }
        val verticalMotion = accelerationMagnitude - gravityMagnitude
        pushAcceleration(verticalMotion)

        val thresholds = currentThresholds()
        if (verticalMotion < thresholds.low) {
            lowThresholdArmed = true
        }

        val canCountStep = accelerometer.timestamp - lastStepTimestampNs > 280_000_000L
        if (lowThresholdArmed && canCountStep && verticalMotion > thresholds.high) {
            lowThresholdArmed = false
            lastStepTimestampNs = accelerometer.timestamp
            stepCount += 1
            lastStepLength = estimateStepLength(settings)
            x += (lastStepLength * sin(headingRadians))
            y += (lastStepLength * cos(headingRadians))
        }

        val trajectory = if (stepCount == state.stepCount) {
            state.trajectory
        } else {
            (state.trajectory + TrajectoryPoint(x, y, System.currentTimeMillis())).takeLast(800)
        }

        val meanX = trajectory.map { it.x }.averageOrZero().toFloat()
        val meanY = trajectory.map { it.y }.averageOrZero().toFloat()
        val varianceX = trajectory.map { (it.x - meanX).pow(2) }.averageOrZero().toFloat()
        val varianceY = trajectory.map { (it.y - meanY).pow(2) }.averageOrZero().toFloat()
        val magneticMagnitude = magnetic?.values?.let(::magnitude) ?: 0f
        val magneticWeight = magneticFusionWeight(magneticMagnitude)

        state = PdrState(
            trajectory = trajectory,
            stepCount = stepCount,
            headingDegrees = normalizeDegrees(Math.toDegrees(headingRadians.toDouble()).toFloat()),
            lastStepLength = lastStepLength,
            meanX = meanX,
            meanY = meanY,
            varianceX = varianceX,
            varianceY = varianceY,
            magneticMagnitude = magneticMagnitude,
            magneticFusionWeight = magneticWeight
        )
        return state
    }

    private fun updateHeading(
        accelerometer: SensorData,
        gyroscope: SensorData?,
        magnetic: SensorData?
    ) {
        if (gyroscope != null) {
            if (lastGyroTimestampNs != 0L) {
                val dt = (gyroscope.timestamp - lastGyroTimestampNs) / 1_000_000_000f
                if (dt in 0f..1f) {
                    headingRadians = normalizeRadians(headingRadians + gyroscope.values.getOrElse(2) { 0f } * dt)
                }
            }
            lastGyroTimestampNs = gyroscope.timestamp
        }

        val magneticHeading = if (magnetic != null) {
            tiltCompensatedMagneticHeading(accelerometer.values, magnetic.values)
        } else {
            null
        }
        if (magneticHeading != null) {
            val magneticMagnitude = magnitude(magnetic!!.values)
            val weight = magneticFusionWeight(magneticMagnitude)
            headingRadians = fuseAngles(headingRadians, magneticHeading, weight)
        }
    }

    private fun estimateStepLength(settings: PdrSettings): Float {
        val amplitude = accelerationWindow.maxOrNull().orZero() - accelerationWindow.minOrNull().orZero()
        val heightM = max(settings.heightCm, 80f) / 100f
        return (heightM * settings.modelC * max(amplitude, 0.2f).pow(0.25f)).coerceIn(0.25f, 1.2f)
    }

    private fun pushAcceleration(value: Float) {
        accelerationWindow.addLast(value)
        while (accelerationWindow.size > maxWindowSize) {
            accelerationWindow.removeFirst()
        }
    }

    private fun currentThresholds(): StepThresholds {
        if (accelerationWindow.size < 12) {
            return StepThresholds(low = -0.15f, high = 0.9f)
        }
        val mean = accelerationWindow.averageOrZero().toFloat()
        val std = sqrt(accelerationWindow.map { (it - mean).pow(2) }.averageOrZero()).toFloat()
        return StepThresholds(
            low = mean + std * 0.08f,
            high = max(0.65f, mean + std * 0.85f)
        )
    }

    private fun tiltCompensatedMagneticHeading(acc: FloatArray, mag: FloatArray): Float? {
        if (acc.size < 3 || mag.size < 3) return null
        val ax = acc[0]
        val ay = acc[1]
        val az = acc[2]
        val mx = mag[0]
        val my = mag[1]
        val mz = mag[2]

        val roll = atan2(ay, az)
        val pitch = atan2(-ax, sqrt(ay * ay + az * az))
        val compensatedX = mx * cos(pitch) + mz * sin(pitch)
        val compensatedY = mx * sin(roll) * sin(pitch) + my * cos(roll) - mz * sin(roll) * cos(pitch)
        return normalizeRadians(atan2(-compensatedY, compensatedX))
    }

    private fun magneticFusionWeight(magneticMagnitude: Float): Float {
        return when {
            magneticMagnitude in 35f..55f -> 0.08f
            magneticMagnitude in 25f..65f -> 0.03f
            else -> 0f
        }
    }

    private fun fuseAngles(primary: Float, correction: Float, weight: Float): Float {
        val delta = atan2(sin(correction - primary), cos(correction - primary))
        return normalizeRadians(primary + delta * weight)
    }

    private fun magnitude(values: FloatArray): Float {
        return sqrt(values.take(3).sumOf { (it * it).toDouble() }).toFloat()
    }

    private fun normalizeRadians(value: Float): Float {
        var normalized = value
        val twoPi = (2 * PI).toFloat()
        while (normalized <= -PI) normalized += twoPi
        while (normalized > PI) normalized -= twoPi
        return normalized
    }

    private fun normalizeDegrees(value: Float): Float {
        var normalized = value
        while (normalized < 0f) normalized += 360f
        while (normalized >= 360f) normalized -= 360f
        return normalized
    }

    private fun Iterable<Float>.averageOrZero(): Double {
        val values = toList()
        return if (values.isEmpty()) 0.0 else values.average()
    }

    private fun Float?.orZero(): Float = this ?: 0f

    private data class StepThresholds(
        val low: Float,
        val high: Float
    )
}
