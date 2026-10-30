package com.bibletriviaquiz.playandwin.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-reliability Audio Buzzer tone synthesizer for timeout effects.
 * Synthesizes a classic quiz-show harsh discordant double-tone (150Hz + 225Hz)
 * using direct PCM buffer playback via AudioTrack.
 * Operates offline with zero network or external file dependencies.
 */
object BuzzerManager {
    private const val SAMPLE_RATE = 44100
    private const val DURATION_SECONDS = 0.65
    private var isMuted: Boolean = false

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    suspend fun playBuzzer() = withContext(Dispatchers.Default) {
        if (isMuted) return@withContext

        try {
            val numSamples = (SAMPLE_RATE * DURATION_SECONDS).toInt()
            val buffer = ShortArray(numSamples)
            val freq1 = 150.0 // Low harsh tone
            val freq2 = 225.0 // Dissonant minor fifth tone

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // Fast envelope ramp up, sustain, and ramp down
                val envelope = when {
                    t < 0.03 -> t / 0.03
                    t > (DURATION_SECONDS - 0.05) -> (DURATION_SECONDS - t) / 0.05
                    else -> 1.0
                }
                // Generate discordant buzz waveform with harmonics (quiz-show buzzer)
                val sample1 = Math.sin(2.0 * Math.PI * freq1 * t)
                val sample2 = Math.sin(2.0 * Math.PI * freq2 * t)
                val harmonic = 0.5 * Math.sin(2.0 * Math.PI * freq1 * 2 * t)
                val mixed = ((sample1 + sample2 + harmonic) / 2.5) * envelope

                buffer[i] = (mixed * Short.MAX_VALUE * 0.85).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()
            
            // Release after playback finishes
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay((DURATION_SECONDS * 1000).toLong() + 50)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("BuzzerManager", "Could not play synthesized buzzer tone", e)
        }
    }
}
