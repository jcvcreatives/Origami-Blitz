package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Zero-asset procedural sound synthesizer generating tactile paper crinkles,
 * scissor snips, glue swaps, cardboard smashes, and rising major chord cascades.
 */
class PaperSoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050
    var isEnabled: Boolean = true

    fun playPaperCrinkle(intensity: Int = 1) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = (0.14f + (intensity.coerceAtMost(4) * 0.025f))
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var bandpassState = 0f
            var prevNoise = 0f
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                // Crinkly burst envelope with micro-crackles
                val macroEnv = exp(-14.0 * t).toFloat()
                val crackleGate = if (sin(2 * PI * 68.0 * t + sin(2 * PI * 19.0 * t)) > -0.2) 1f else 0.25f
                val rawNoise = (Random.nextFloat() * 2f - 1f)
                // High-pass / band-pass filter to mimic stiff construction paper rustle
                val highPassed = rawNoise - prevNoise * 0.82f
                prevNoise = rawNoise
                bandpassState = bandpassState * 0.55f + highPassed * 0.45f
                val popTone = sin(2.0 * PI * (320.0 + intensity * 45.0) * t).toFloat() * exp(-35.0 * t).toFloat() * 0.35f
                val value = ((bandpassState * crackleGate * macroEnv * 0.7f) + popTone).coerceIn(-1f, 1f)
                samples[i] = (value * Short.MAX_VALUE * 0.55f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playScissorsSnip() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.18f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var prev = 0f
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                // High metallic blade harmonic + crisp paper shear noise
                val bladeFreq = 1850.0 + 1200.0 * t
                val metallic = (sin(2 * PI * bladeFreq * t) + 0.5 * sin(2 * PI * bladeFreq * 2.41 * t)).toFloat()
                val noise = (Random.nextFloat() * 2f - 1f)
                val crispNoise = noise - prev * 0.88f
                prev = noise
                val env = exp(-26.0 * t).toFloat()
                val secondSnipEnv = if (t > 0.065f) exp(-32.0 * (t - 0.065f)).toFloat() else 0f
                val totalEnv = (env + secondSnipEnv * 0.8f).coerceAtMost(1.2f)
                val sample = ((metallic * 0.45f + crispNoise * 0.65f) * totalEnv).coerceIn(-1f, 1f)
                samples[i] = (sample * Short.MAX_VALUE * 0.55f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playGlueSwap() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.22f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = 340.0 + 380.0 * sin(PI * (t / durationSec))
                val env = exp(-12.0 * t).toFloat()
                val wave = sin(2.0 * PI * freq * t).toFloat() * env
                samples[i] = (wave * Short.MAX_VALUE * 0.45f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playCardboardSmash() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.32f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lp = 0f
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                // Low punchy cardboard box thud + wide paper burst
                val thudFreq = 145.0 * exp(-14.0 * t) + 48.0
                val thud = sin(2.0 * PI * thudFreq * t).toFloat() * exp(-11.0 * t).toFloat()
                val rawNoise = Random.nextFloat() * 2f - 1f
                lp = lp * 0.72f + rawNoise * 0.28f
                val tear = (rawNoise - lp) * exp(-10.0 * t).toFloat()
                val combined = (thud * 0.65f + tear * 0.65f).coerceIn(-1f, 1f)
                samples[i] = (combined * Short.MAX_VALUE * 0.65f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playComboChord(comboStep: Int) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.36f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            // Rising major triad frequencies based on combo step
            val semitoneShift = (comboStep - 1).coerceIn(0, 10) * 2
            val pitchMultiplier = Math.pow(2.0, semitoneShift / 12.0)
            val root = 523.25 * pitchMultiplier // C5 base
            val third = 659.25 * pitchMultiplier // E5
            val fifth = 783.99 * pitchMultiplier // G5
            val octave = 1046.50 * pitchMultiplier // C6

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val env1 = exp(-10.0 * t).toFloat()
                val env2 = if (t >= 0.045f) exp(-10.0 * (t - 0.045f)).toFloat() else 0f
                val env3 = if (t >= 0.09f) exp(-9.0 * (t - 0.09f)).toFloat() else 0f
                val env4 = if (t >= 0.135f) exp(-8.0 * (t - 0.135f)).toFloat() else 0f

                val n1 = sin(2.0 * PI * root * t).toFloat() * env1
                val n2 = sin(2.0 * PI * third * t).toFloat() * env2
                val n3 = sin(2.0 * PI * fifth * t).toFloat() * env3
                val n4 = sin(2.0 * PI * octave * t).toFloat() * env4

                val mix = ((n1 + n2 + n3 + n4) * 0.27f).coerceIn(-1f, 1f)
                samples[i] = (mix * Short.MAX_VALUE * 0.5f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playInvalidShake() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.18f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val freq = if (t < 0.09f) 210.0 else 165.0
                val env = exp(-14.0 * (t % 0.09f)).toFloat()
                val wave = sin(2.0 * PI * freq * t).toFloat() * env
                samples[i] = (wave * Short.MAX_VALUE * 0.4f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playSwapWhoosh() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.17f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var prev = 0f
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val norm = t / durationSec
                // Rising-then-falling air whoosh + crisp cardstock pop at midpoint
                val whooshEnv = sin(PI * norm).toFloat()
                val raw = Random.nextFloat() * 2f - 1f
                val filtered = raw - prev * 0.78f
                prev = raw
                val popFreq = 380.0 + 260.0 * norm
                val popTone = sin(2.0 * PI * popFreq * t).toFloat() * exp(-18.0 * t).toFloat() * 0.45f
                val sample = (filtered * whooshEnv * 0.48f + popTone).coerceIn(-1f, 1f)
                samples[i] = (sample * Short.MAX_VALUE * 0.52f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playDropBounce(tileCount: Int = 3) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.24f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val hits = tileCount.coerceIn(2, 5)
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var acc = 0f
                for (h in 0 until hits) {
                    val delay = h * 0.032f
                    if (t >= delay) {
                        val lt = t - delay
                        val thudFreq = (195.0 + h * 28.0) * exp(-22.0 * lt) + 85.0
                        val env = exp(-26.0 * lt).toFloat()
                        acc += sin(2.0 * PI * thudFreq * lt).toFloat() * env * 0.45f
                    }
                }
                val value = acc.coerceIn(-1f, 1f)
                samples[i] = (value * Short.MAX_VALUE * 0.55f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    fun playFeverFanfare() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.48f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val notes = doubleArrayOf(587.33, 739.99, 880.0, 1174.66) // D5, F#5, A5, D6
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var mix = 0f
                for (idx in notes.indices) {
                    val start = idx * 0.055f
                    if (t >= start) {
                        val lt = t - start
                        val env = exp(-7.5 * lt).toFloat()
                        mix += sin(2.0 * PI * notes[idx] * lt).toFloat() * env * 0.30f
                    }
                }
                samples[i] = (mix.coerceIn(-1f, 1f) * Short.MAX_VALUE * 0.55f).toInt().toShort()
            }
            playPcmBuffer(samples)
        }
    }

    private fun playPcmBuffer(samples: ShortArray) {
        try {
            val byteSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(1024))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
        } catch (_: Throwable) {
            // Safely ignore in headless JVM/Robolectric environments
        }
    }
}
