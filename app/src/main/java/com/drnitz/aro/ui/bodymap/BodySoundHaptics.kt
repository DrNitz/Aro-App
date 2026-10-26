//
// BodySoundHaptics.kt
//
// Multi-sensory feedback engine for Aro anatomical body map:
// - Micro-haptic tactile pulses (VibrationEffect.EFFECT_CLICK)
// - Native synthesized PCM click and chime audio effects (zero asset bloat)
// - Global in-app mute toggle support
//

package com.drnitz.aro.ui.bodymap

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object BodySoundHaptics {

    private const val SAMPLE_RATE = 16000

    @Volatile
    private var clickTrack: AudioTrack? = null

    @Volatile
    private var chimeTrack: AudioTrack? = null

    private val trackLock = Any()

    /**
     * Synthesizes and returns a short ~15ms wooden/tactile click PCM track.
     */
    private fun getOrCreateClickTrack(): AudioTrack? {
        if (clickTrack != null) return clickTrack
        synchronized(trackLock) {
            if (clickTrack != null) return clickTrack
            try {
                val durationMs = 16
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val decay = exp(-i.toDouble() / (numSamples / 2.8))
                    val freq = 1100.0 - (450.0 * i / numSamples)
                    val sample = (sin(2.0 * PI * freq * t) * decay * Short.MAX_VALUE * 0.35).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                clickTrack = track
                return track
            } catch (_: Throwable) {
                return null
            }
        }
    }

    /**
     * Synthesizes and returns a pleasant ~120ms two-tone chime for PR/volume triumphs.
     */
    private fun getOrCreateChimeTrack(): AudioTrack? {
        if (chimeTrack != null) return chimeTrack
        synchronized(trackLock) {
            if (chimeTrack != null) return chimeTrack
            try {
                val durationMs = 120
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val decay = exp(-i.toDouble() / (numSamples / 3.0))
                    val s1 = sin(2.0 * PI * 659.25 * t) // E5
                    val s2 = sin(2.0 * PI * 987.77 * t) // B5
                    val sample = ((s1 * 0.6 + s2 * 0.4) * decay * Short.MAX_VALUE * 0.28).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                chimeTrack = track
                return track
            } catch (_: Throwable) {
                return null
            }
        }
    }

    /**
     * Plays a crisp micro-click sound unless [isMuted] is true.
     */
    fun playClickSound(isMuted: Boolean = false) {
        if (isMuted) return
        try {
            getOrCreateClickTrack()?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Throwable) {
            // Audio policy or device limitation fallback
        }
    }

    /**
     * Plays a pleasant chime sound unless [isMuted] is true.
     */
    fun playChimeSound(isMuted: Boolean = false) {
        if (isMuted) return
        try {
            getOrCreateChimeTrack()?.let { track ->
                track.stop()
                track.reloadStaticData()
                track.play()
            }
        } catch (_: Throwable) {
            // Audio policy or device limitation fallback
        }
    }

    /**
     * Triggers a subtle tactile haptic vibration click.
     */
    fun performHapticClick(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(12)
                }
            }
        } catch (_: Throwable) {
            // Non-critical if haptics fail or lack permission on restrictive devices
        }
    }

    /**
     * Combined multi-sensory feedback for muscle tap interaction:
     * fires micro-haptic click and native audio tick.
     */
    fun onMuscleTap(context: Context, isMuted: Boolean = false) {
        performHapticClick(context)
        playClickSound(isMuted)
    }

    /**
     * Combined multi-sensory feedback for PR/volume accomplishment.
     */
    fun onAchievement(context: Context, isMuted: Boolean = false) {
        performHapticClick(context)
        playChimeSound(isMuted)
    }

    /**
     * Releases synthesized audio tracks to free device audio hardware resources.
     */
    fun release() {
        synchronized(trackLock) {
            try {
                clickTrack?.release()
            } catch (_: Throwable) {}
            clickTrack = null

            try {
                chimeTrack?.release()
            } catch (_: Throwable) {}
            chimeTrack = null
        }
    }
}
