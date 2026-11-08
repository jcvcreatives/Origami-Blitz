package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Addictive, musical, tactile sound synthesizer combining warm marimba/kalimba plucks,
 * celesta combo arpeggios, bubbly landing plops, and silky paper-craft ASMR transients.
 */
class PaperSoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100
    var isEnabled: Boolean = true

    // Pentatonic frequencies (C5, D5, E5, G5, A5, C6, D6, E6, G6) for musical taps & cascades
    private val pentatonicScale = doubleArrayOf(
        523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1318.51, 1567.98
    )
    private var tapCycleIndex = 0

    /**
     * Warm, musical kalimba/marimba bubble-pop with a subtle, silky paper-velvet transient.
     * Cycles through pentatonic notes so every tap and match feels melodic and rewarding.
     */
    fun playPaperCrinkle(intensity: Int = 1) {
        if (!isEnabled) return
        val noteIndex = (tapCycleIndex++ + intensity - 1) % pentatonicScale.size
        val baseFreq = pentatonicScale[noteIndex]
        scope.launch {
            val durationSec = 0.16f + (intensity.coerceIn(1, 4) * 0.02f)
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.004f).coerceIn(0f, 1f)

                // Warm pitch-drop bubble pop transient (starts 18% higher and settles rapidly)
                val pitchEnv = 1.0 + 0.18 * exp(-85.0 * t)
                val f = baseFreq * pitchEnv

                // Fundamental + warm 2nd harmonic + bell 3rd harmonic (marimba/kalimba timbre)
                val env1 = exp(-20.0 * t).toFloat()
                val env2 = exp(-36.0 * t).toFloat()
                val env3 = exp(-55.0 * t).toFloat()
                val tone = (
                    sin(2.0 * PI * f * t).toFloat() * env1 * 0.68f +
                    sin(2.0 * PI * (f * 2.0) * t).toFloat() * env2 * 0.24f +
                    sin(2.0 * PI * (f * 3.0) * t).toFloat() * env3 * 0.10f
                )

                // Soft, low-passed velvet paper brush transient (never harsh or static-like)
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.88f + raw * 0.12f
                val velvetEnv = exp(-42.0 * t).toFloat()
                val velvet = lpNoise * velvetEnv * 0.08f

                val sample = softClip((tone + velvet) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.62f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Crisp, musical twin-snip shear + sparkling high chime (E6 -> A6).
     */
    fun playScissorsSnip() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.26f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.003f).coerceIn(0f, 1f)

                // First snip at t=0, second snip at t=0.075s
                val snip1Env = exp(-28.0 * t).toFloat()
                val snip2Env = if (t >= 0.075f) exp(-24.0 * (t - 0.075f)).toFloat() else 0f

                // Musical silver-bell shear notes: E6 (1318.5 Hz) -> A6 (1760 Hz)
                val chime1 = (
                    sin(2.0 * PI * 1318.51 * t) * 0.65 +
                    sin(2.0 * PI * 2637.02 * t) * 0.25
                ).toFloat() * snip1Env

                val t2 = (t - 0.075f).coerceAtLeast(0f)
                val chime2 = (
                    sin(2.0 * PI * 1760.00 * t2) * 0.70 +
                    sin(2.0 * PI * 2637.02 * t2) * 0.25
                ).toFloat() * snip2Env

                // Silky paper snip texture
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.75f + raw * 0.25f
                val crispShear = (raw - lpNoise) * (snip1Env + snip2Env) * 0.14f

                val sample = softClip((chime1 + chime2 + crispShear) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.60f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Juicy, bubbly two-note rising pitch-pop ("Bloop-Pop!") for Glue Swap.
     */
    fun playGlueSwap() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.25f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.004f).coerceIn(0f, 1f)

                // Smooth rising bubble glide (392Hz G4 -> 784Hz G5) + second pop at 0.09s
                val glideFreq = 392.0 + 392.0 * (t / durationSec).pow(1.4f)
                val env1 = exp(-10.0 * t).toFloat()
                val bubble1 = (
                    sin(2.0 * PI * glideFreq * t) * 0.7 +
                    sin(2.0 * PI * glideFreq * 2.0 * t) * 0.25
                ).toFloat() * env1

                val pop2 = if (t >= 0.09f) {
                    val lt = t - 0.09f
                    val env2 = exp(-18.0 * lt).toFloat()
                    sin(2.0 * PI * 1046.50 * lt).toFloat() * env2 * 0.45f
                } else 0f

                val sample = softClip((bubble1 + pop2) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.62f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Deep, warm, satisfying bass-drum thump + sparkling chord burst for the Hammer Smash.
     */
    fun playCardboardSmash() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.36f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.003f).coerceIn(0f, 1f)

                // Warm punchy kick/tom sweep (160Hz -> 55Hz)
                val kickPhase = 2.0 * PI * (55.0 * t + (105.0 / 22.0) * (1.0 - exp(-22.0 * t)))
                val kickEnv = exp(-9.5 * t).toFloat()
                val kick = sin(kickPhase).toFloat() * kickEnv * 0.72f

                // Warm wooden power chord chime (C5 + G5 + C6)
                val chordEnv = exp(-11.0 * t).toFloat()
                val chord = (
                    sin(2.0 * PI * 523.25 * t) * 0.22 +
                    sin(2.0 * PI * 783.99 * t) * 0.20 +
                    sin(2.0 * PI * 1046.50 * t) * 0.16
                ).toFloat() * chordEnv

                val sample = softClip((kick + chord) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.70f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Addictive ascending Celesta + Harp + Bell arpeggio that climbs in pitch with each combo step.
     * Gives an immediate, sparkling musical dopamine rush on every match & chain cascade!
     */
    fun playComboChord(comboStep: Int) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.48f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            // Climb up a joyous scale with every combo step
            val semitoneShift = (comboStep - 1).coerceIn(0, 12) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)

            // Major 9th / Pentatonic celesta arpeggio: Root, 3rd, 5th, Octave, 9th sparkle
            val notes = doubleArrayOf(
                523.25 * pitchMult,  // C5
                659.25 * pitchMult,  // E5
                783.99 * pitchMult,  // G5
                1046.50 * pitchMult, // C6
                1174.66 * pitchMult  // D6 sparkle
            )

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var mix = 0f

                for (idx in notes.indices) {
                    val start = idx * 0.036f
                    if (t >= start) {
                        val lt = t - start
                        val noteAttack = (lt / 0.003f).coerceIn(0f, 1f)
                        val freq = notes[idx]
                        val envMain = exp(-7.2 * lt).toFloat()
                        val envHarm = exp(-16.0 * lt).toFloat()
                        val envBell = exp(-28.0 * lt).toFloat()

                        // Rich celesta/harp voice: fundamental + warm detuned chorus + 2x harmonic + 3x bell
                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.56 * envMain +
                            sin(2.0 * PI * (freq * 1.003) * lt) * 0.24 * envMain +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.22 * envHarm +
                            sin(2.0 * PI * (freq * 3.0) * lt) * 0.08 * envBell
                        ).toFloat() * noteAttack

                        mix += voice * 0.34f
                    }
                }

                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.66f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Cute, soft wooden marimba double-knock (E4 -> C4) for invalid swaps.
     */
    fun playInvalidShake() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.20f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val isSecond = t >= 0.095f
                val lt = if (isSecond) t - 0.095f else t
                val freq = if (!isSecond) 329.63 else 261.63 // E4 -> C4 warm wooden knock
                val attack = (lt / 0.003f).coerceIn(0f, 1f)
                val env = exp(-24.0 * lt).toFloat()
                val env2 = exp(-48.0 * lt).toFloat()

                val wave = (
                    sin(2.0 * PI * freq * lt).toFloat() * env * 0.75f +
                    sin(2.0 * PI * (freq * 2.0) * lt).toFloat() * env2 * 0.25f
                ) * attack

                samples[i] = (softClip(wave) * Short.MAX_VALUE * 0.48f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Silky, melodic two-note marimba/flute swoosh (G4 -> D5) when swapping blocks.
     */
    fun playSwapWhoosh() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.16f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val norm = (t / durationSec).coerceIn(0f, 1f)
                val attack = (t / 0.004f).coerceIn(0f, 1f)

                // Smooth musical pitch glide from 392Hz (G4) to 587.33Hz (D5)
                val freq = 392.0 + 195.33 * sin((PI * 0.5) * norm)
                val env = sin(PI * norm).toFloat() * exp(-4.5 * t).toFloat()
                val harmonic = (
                    sin(2.0 * PI * freq * t) * 0.76 +
                    sin(2.0 * PI * (freq * 2.0) * t) * 0.24
                ).toFloat() * env * attack

                samples[i] = (softClip(harmonic) * Short.MAX_VALUE * 0.52f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Staggered cascade of warm marimba/bubble plops pitched across a pentatonic chord
     * when blocks land and bounce into place. Escalates in pitch and harmonic punch
     * as cascade combo chains climb (x2, x3, x4, x5+).
     */
    fun playDropBounce(tileCount: Int = 3, comboStep: Int = 1) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.30f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val hits = tileCount.coerceIn(2, 6)
            // Warm low-mid pentatonic marimba notes (C4, D4, E4, G4, A4, C5)
            val dropNotes = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
            val semitoneShift = (comboStep - 1).coerceIn(0, 10) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)
            val shimmerBoost = (comboStep - 1).coerceIn(0, 5) * 0.06f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var acc = 0f
                for (h in 0 until hits) {
                    val delay = h * 0.026f
                    if (t >= delay) {
                        val lt = t - delay
                        val attack = (lt / 0.003f).coerceIn(0f, 1f)
                        val baseNote = dropNotes[h % dropNotes.size] * pitchMult
                        // Punchy downward impact thump + upward bubble inflection on landing
                        val f = baseNote * (1.0 + 0.14 * exp(-60.0 * lt))
                        val env = exp(-20.0 * lt).toFloat()
                        val overtoneEnv = exp(-38.0 * lt).toFloat()
                        val bellSparkle = if (comboStep >= 2) {
                            sin(2.0 * PI * (f * 3.0) * lt).toFloat() * exp(-48.0 * lt).toFloat() * shimmerBoost
                        } else 0f
                        val plop = (
                            sin(2.0 * PI * f * lt).toFloat() * env * 0.76f +
                            sin(2.0 * PI * (f * 2.0) * lt).toFloat() * overtoneEnv * (0.24f + shimmerBoost) +
                            bellSparkle
                        ) * attack
                        acc += plop * 0.40f
                    }
                }
                val value = softClip(acc)
                samples[i] = (value * Short.MAX_VALUE * 0.64f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Sparkling, triumphant 6-note Celesta & Bell fanfare (C5 - E5 - G5 - B5 - D6 - G6)
     * for Origami Fever activation and Level Complete celebration!
     */
    fun playFeverFanfare() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.68f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 987.77, 1174.66, 1567.98)

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var mix = 0f
                for (idx in notes.indices) {
                    val start = idx * 0.052f
                    if (t >= start) {
                        val lt = t - start
                        val attack = (lt / 0.004f).coerceIn(0f, 1f)
                        val freq = notes[idx]
                        val decayRate = if (idx == notes.lastIndex) 4.2 else 6.8
                        val env = exp(-decayRate * lt).toFloat()
                        val bellEnv = exp(-15.0 * lt).toFloat()

                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.58 * env +
                            sin(2.0 * PI * (freq * 1.0025) * lt) * 0.24 * env +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.20 * bellEnv
                        ).toFloat() * attack
                        mix += voice * 0.30f
                    }
                }
                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.66f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Smooth analog-style soft clipper so overlapping harmonies stay warm and never distort.
     */
    private fun softClip(x: Float): Float {
        val clamped = x.coerceIn(-1.5f, 1.5f)
        return (clamped - (clamped * clamped * clamped) / 6.75f).coerceIn(-1f, 1f)
    }

    private fun playPcmBuffer(samples: ShortArray, durationSec: Float) {
        var track: AudioTrack? = null
        try {
            val byteSize = samples.size * 2
            track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(byteSize.coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            scope.launch {
                delay((durationSec * 1000L).toLong() + 80L)
                try {
                    track.stop()
                } catch (_: Throwable) {}
                try {
                    track.release()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            try {
                track?.release()
            } catch (_: Throwable) {}
        }
    }
}
