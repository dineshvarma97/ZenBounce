package com.zenbounce.haptics

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thin, safe wrapper around the Android 13+ [VibratorManager] API.
 *
 * Vibration intensity is mapped from physics collision speed and clamped to a
 * comfortable range so that bounces feel soothing rather than alarming.
 *
 * A cooldown guard prevents multiple rapid vibrations from blurring into a buzz.
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator = run {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    }

    private val mutex = Mutex()
    private var lastVibrationMs = 0L

    /**
     * Trigger a vibration pulse scaled to [collisionSpeed] (pixels/second).
     *
     * Thread-safe — can be called from any coroutine context.
     * Silently ignored if [COOLDOWN_MS] has not elapsed since the last vibration.
     *
     * @param collisionSpeed Normal-component speed of the ball at the moment of collision.
     */
    suspend fun vibrate(collisionSpeed: Float) {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (now - lastVibrationMs < COOLDOWN_MS) return

            val amplitude = mapSpeedToAmplitude(collisionSpeed)
            val durationMs = mapSpeedToDuration(collisionSpeed)

            val effect = VibrationEffect.createOneShot(durationMs, amplitude)
            vibrator.vibrate(effect)

            lastVibrationMs = now
        }
    }

    // ---- Mapping helpers --------------------------------------------------

    /**
     * Map collision speed to VibrationEffect amplitude [1, 255].
     * Input range: [MIN_SPEED, MAX_SPEED] px/s → output [AMP_MIN, AMP_MAX].
     */
    private fun mapSpeedToAmplitude(speed: Float): Int {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        val t = (clamped - MIN_SPEED) / (MAX_SPEED - MIN_SPEED)
        return (AMP_MIN + t * (AMP_MAX - AMP_MIN)).toInt()
    }

    /**
     * Map collision speed to vibration duration in ms.
     * Heavier hits last slightly longer, reinforcing the physical feel.
     */
    private fun mapSpeedToDuration(speed: Float): Long {
        val clamped = speed.coerceIn(MIN_SPEED, MAX_SPEED)
        val t = (clamped - MIN_SPEED) / (MAX_SPEED - MIN_SPEED)
        return (DUR_MIN + t * (DUR_MAX - DUR_MIN)).toLong()
    }

    // ---- Constants --------------------------------------------------------

    /** Minimum time between vibrations to prevent overwhelming buzz. */
    private val COOLDOWN_MS = 80L

    // Speed thresholds (pixels/second)
    private val MIN_SPEED = 100f
    private val MAX_SPEED = 1800f

    // Amplitude range (VibrationEffect accepts 1–255)
    private val AMP_MIN = 30
    private val AMP_MAX = 140

    // Duration range (milliseconds)
    private val DUR_MIN = 25f
    private val DUR_MAX = 70f
}
