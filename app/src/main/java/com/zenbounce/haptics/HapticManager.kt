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
 * Every wall collision fires a single EFFECT_CLICK — the same crisp haptic
 * used by on-screen keyboard keys — regardless of collision speed.
 * A cooldown guard prevents back-to-back presses from blurring together.
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator = run {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    }

    private val mutex = Mutex()
    private var lastVibrationMs = 0L

    /**
     * Trigger a single keyboard-click haptic on wall collision.
     *
     * Thread-safe — can be called from any coroutine context.
     * Silently ignored if [COOLDOWN_MS] has not elapsed since the last vibration.
     */
    suspend fun vibrate(collisionSpeed: Float) {
        val now = System.currentTimeMillis()

        mutex.withLock {
            if (now - lastVibrationMs < COOLDOWN_MS) return

            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))

            lastVibrationMs = now
        }
    }

    // ---- Constants --------------------------------------------------------

    /** Minimum time between haptic pulses to prevent buzzing on rapid bounces. */
    private val COOLDOWN_MS = 50L
}
