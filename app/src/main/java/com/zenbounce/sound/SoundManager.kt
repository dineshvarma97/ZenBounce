package com.zenbounce.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Wraps [SoundPool] to play short per-object collision sound effects.
 *
 * Usage:
 *  1. Call [preload] with all R.raw.* IDs at startup.
 *  2. Call [playCollisionSound] from the game loop when a collision occurs.
 *  3. Call [release] in ViewModel.onCleared() to free native resources.
 *
 * Volume is automatically scaled by collision speed so harder hits sound louder.
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    /** Maps R.raw.* resource ID → internal SoundPool sound ID. */
    private val loadedSounds = mutableMapOf<Int, Int>()

    /**
     * Pre-load all sound resources so [playCollisionSound] has zero startup latency.
     * Safe to call before first collision occurs.
     */
    fun preload(resIds: Collection<Int>) {
        resIds.forEach { resId ->
            if (resId !in loadedSounds) {
                loadedSounds[resId] = soundPool.load(context, resId, 1)
            }
        }
    }

    /**
     * Play the collision sound for the given resource ID, with volume proportional to [speed].
     * No-op if [soundResId] is null (object has no sound) or hasn't been pre-loaded.
     *
     * @param soundResId R.raw.* resource ID, or null for a silent object.
     * @param speed      Collision speed in pixels/s, used to scale volume.
     */
    fun playCollisionSound(soundResId: Int?, speed: Float) {
        soundResId ?: return
        val soundId = loadedSounds[soundResId] ?: return
        val volume = (speed / 2000f).coerceIn(0.1f, 1.0f)
        soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
    }

    /** Must be called when the owning ViewModel is cleared to release native resources. */
    fun release() {
        soundPool.release()
    }
}
