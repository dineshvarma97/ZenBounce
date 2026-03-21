package com.zenbounce.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.zenbounce.game.GravityVector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Provides a [Flow] of [GravityVector] values derived from the device's
 * gravity/accelerometer sensor with a low-pass filter to remove jitter.
 *
 * Uses TYPE_GRAVITY when available (hardware-fused, more stable).
 * Falls back to TYPE_ACCELEROMETER + software low-pass filter.
 *
 * Sensor axes (device natural orientation):
 *   +X → right edge of device
 *   +Y → top edge of device
 *   +Z → out of screen
 *
 * Mapping to Compose Canvas screen coordinates (y increases downward):
 *   gravityX =  raw.x   (tilt right → ball rolls right ✓)
 *   gravityY =  raw.y   (sensor +Y = upward reaction force = ball falls down in canvas ✓)
 */
class GyroscopeManager(private val context: Context) {

    /**
     * Gravity scale factor: converts m/s² to pixels/s².
     * At ~9.8 m/s² full gravity, the ball accelerates at 9.8 * SCALE px/s².
     * Tune this to adjust responsiveness.
     */
    private val gravityScale = 960f

    /** Low-pass filter smoothing factor — closer to 1 = more smoothing, slower response. */
    private val alpha = 0.12f

    fun gravityFlow(): Flow<GravityVector> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (gravitySensor == null) {
            // No sensor available — emit zeroed-out gravity so physics still works
            trySend(GravityVector(0f, 0f))
            awaitClose()
            return@callbackFlow
        }

        var smoothX = 0f
        var smoothY = 0f
        var firstSample = true

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX = event.values[0]
                val rawY = event.values[1]

                if (firstSample) {
                    // Seed the filter with the first sample to avoid a startup slew
                    smoothX = rawX
                    smoothY = rawY
                    firstSample = false
                }

                // Exponential moving average low-pass filter
                smoothX = alpha * rawX + (1f - alpha) * smoothX
                smoothY = alpha * rawY + (1f - alpha) * smoothY

                // Map to canvas coordinates (see class KDoc) and scale
                // X is negated: sensor +X = tilt right, but canvas +X = right (they oppose each other)
                val gx = -smoothX * gravityScale
                val gy = smoothY * gravityScale

                trySend(GravityVector(gx, gy))
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            gravitySensor,
            SensorManager.SENSOR_DELAY_GAME   // ~50Hz — high enough for smooth physics
        )

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
        // Only emit when the value changes meaningfully (dead-zone ≈ floating-point equality)
        .distinctUntilChanged { old, new ->
            kotlin.math.abs(old.x - new.x) < 1f && kotlin.math.abs(old.y - new.y) < 1f
        }
}
