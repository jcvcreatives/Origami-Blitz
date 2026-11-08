package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bespoke "Origami Craft-Table" Acoustic & Foley Synthesizer + Interactive BGM Engine.
 *
 * Uses a SINGLE persistent streaming AudioTrack (MODE_STREAM) with a real-time software
 * voice mixer so Android's AudioFlinger never exhausts native AudioTrack handles even after
 * hours of continuous play and intense x2/x3/x4+ cascades.
 */
class PaperSoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

    private class ActiveVoice(
        val samples: ShortArray,
        val isBgm: Boolean = false,
        var cursor: Int = 0
    )

    private val pendingVoices = ConcurrentLinkedQueue<ActiveVoice>()
    private val queuedVoiceCount = AtomicInteger(0)

    @Volatile
    private var mixerRunning: Boolean = true

    private var mixerJob: Job? = null

    var isEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                pendingVoices.clear()
                queuedVoiceCount.set(0)
            } else {
                ensureMixerLoopRunning()
            }
        }

    @Volatile
    private var currentStageIndex: Int = 1

    @Volatile
    private var feverIntensity: Int = 0

    @Volatile
    private var bgmEnabled: Boolean = true

    // Japanese Yo Pentatonic Scale (D, E, G, A, B) across octaves for unmistakable Origami identity
    private val yoPentatonic = doubleArrayOf(
        293.66, // D4
        329.63, // E4
        392.00, // G4
        440.00, // A4
        493.88, // B4
        587.33, // D5
        659.25, // E5
        783.99, // G5
        880.00, // A5
        987.77, // B5
        1174.66, // D6
        1318.51, // E6
        1567.98  // G6
    )

    private var tapCycleIndex = 0

    init {
        ensureMixerLoopRunning()
    }

    /**
     * Updates the musical mood of the Origami Workshop BGM loop to match the current stage
     * and active Fever / Cascade state.
     */
    fun updateMusicalMood(stageNumber: Int, isFeverOrHighCombo: Boolean) {
        currentStageIndex = stageNumber.coerceIn(1, 10)
        feverIntensity = if (isFeverOrHighCombo) 1 else 0
        if (isEnabled) {
            bgmEnabled = true
            ensureMixerLoopRunning()
        }
    }

    fun startWorkshopMusicLoop() {
        bgmEnabled = true
        if (isEnabled) {
            ensureMixerLoopRunning()
        }
    }

    fun stopWorkshopMusicLoop() {
        bgmEnabled = false
        pendingVoices.clear()
        queuedVoiceCount.set(0)
    }

    fun shutdown() {
        mixerRunning = false
        bgmEnabled = false
        pendingVoices.clear()
        queuedVoiceCount.set(0)
        mixerJob?.cancel()
        scope.cancel()
    }

    private fun enqueueVoice(samples: ShortArray, isBgm: Boolean = false) {
        if (!isEnabled || samples.isEmpty()) return
        ensureMixerLoopRunning()
        // Prevent unbounded queue growth if many sounds fire on the same frame
        if (queuedVoiceCount.get() < 24) {
            pendingVoices.offer(ActiveVoice(samples = samples, isBgm = isBgm))
            queuedVoiceCount.incrementAndGet()
        }
    }

    private fun createStreamingTrack(chunkBytes: Int): AudioTrack? {
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(chunkBytes * 2)

            AudioTrack.Builder()
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
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.play() }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Single persistent streaming AudioTrack mixer & sample-accurate BGM sequencer.
     * Eliminates per-sound AudioTrack creation/destruction so sound never drops out over time.
     */
    @Synchronized
    private fun ensureMixerLoopRunning() {
        if (mixerJob?.isActive == true) return
        mixerRunning = true
        mixerJob = scope.launch(Dispatchers.IO) {
            val chunkSamples = 882 // 20ms at 44.1kHz
            val mixFloat = FloatArray(chunkSamples)
            val outPcm = ShortArray(chunkSamples)
            val activeVoices = ArrayList<ActiveVoice>(16)

            // 16-step enchanting papercraft workshop melody indices into yoPentatonic
            val melodyPattern = intArrayOf(
                5, 7, 8, 7,   9, 8, 7, 5,
                6, 7, 9, 10,  9, 7, 6, 5
            )
            val bassPattern = intArrayOf(
                0, -1, 2, -1,  3, -1, 2, -1,
                1, -1, 3, -1,  4, -1, 0, -1
            )
            val counterPattern = intArrayOf(
                -1, 10, -1, 9,  -1, 11, -1, 10,
                -1, 9, -1, 12,  -1, 10, -1, 8
            )

            var bgmStep = 0
            var samplesUntilNextBgmStep = 0
            var track: AudioTrack? = createStreamingTrack(chunkSamples * 2)

            try {
                while (isActive && mixerRunning) {
                    if (!isEnabled) {
                        activeVoices.clear()
                        pendingVoices.clear()
                        queuedVoiceCount.set(0)
                        delay(45L)
                        continue
                    }

                    // Drain newly triggered SFX voices into active mixer list
                    while (true) {
                        val next = pendingVoices.poll() ?: break
                        queuedVoiceCount.decrementAndGet()
                        if (activeVoices.size >= 14) {
                            // Drop oldest BGM voice or oldest nearly-finished voice to preserve headroom
                            val bgmIdx = activeVoices.indexOfFirst { it.isBgm }
                            if (bgmIdx >= 0) {
                                activeVoices.removeAt(bgmIdx)
                            } else {
                                activeVoices.removeAt(0)
                            }
                        }
                        activeVoices.add(next)
                    }

                    // Sample-accurate BGM step scheduling directly inside the mixer
                    if (bgmEnabled) {
                        samplesUntilNextBgmStep -= chunkSamples
                        if (samplesUntilNextBgmStep <= 0) {
                            val stageShiftSemitones = when (currentStageIndex % 5) {
                                1 -> 0   // D Yo Pentatonic (Warm Pine Woods)
                                2 -> 2   // E Yo Pentatonic (Bright Boardwalk)
                                3 -> -2  // C Yo Pentatonic (Golden Canyon)
                                4 -> 5   // G Yo Pentatonic (Sky Harbor)
                                else -> 3 // F Yo Pentatonic (Starlight Palace)
                            }
                            val pitchShift = 2.0.pow(stageShiftSemitones / 12.0)
                            val isExcited = feverIntensity > 0
                            val stepDurationSec = if (isExcited) 0.24f else 0.28f
                            samplesUntilNextBgmStep = (sampleRate * stepDurationSec * 0.96f).toInt()

                            val melIdx = melodyPattern[bgmStep % melodyPattern.size]
                            val bassIdx = bassPattern[bgmStep % bassPattern.size]
                            val counterIdx = counterPattern[bgmStep % counterPattern.size]

                            val melFreq = yoPentatonic[melIdx] * pitchShift
                            val bassFreq = if (bassIdx >= 0) yoPentatonic[bassIdx] * pitchShift * 0.5 else 0.0
                            val counterFreq = if (isExcited && counterIdx >= 0) yoPentatonic[counterIdx] * pitchShift else 0.0
                            val hasPaperShaker = (bgmStep % 2 == 1)

                            val bgmSamples = synthesizeBgmStep(
                                durationSec = stepDurationSec,
                                melodyFreq = melFreq,
                                bassFreq = bassFreq,
                                counterFreq = counterFreq,
                                paperBrush = hasPaperShaker,
                                accent = (bgmStep % 4 == 0)
                            )
                            activeVoices.add(ActiveVoice(samples = bgmSamples, isBgm = true))
                            bgmStep = (bgmStep + 1) % 16
                        }
                    }

                    if (activeVoices.isEmpty()) {
                        // Write silence or sleep briefly when no voices are active
                        delay(16L)
                        continue
                    }

                    mixFloat.fill(0f)
                    val invShortMax = 1f / Short.MAX_VALUE
                    var vIdx = activeVoices.size - 1
                    while (vIdx >= 0) {
                        val voice = activeVoices[vIdx]
                        val src = voice.samples
                        var cur = voice.cursor
                        val limit = minOf(chunkSamples, src.size - cur)
                        for (i in 0 until limit) {
                            mixFloat[i] += src[cur + i] * invShortMax
                        }
                        cur += limit
                        voice.cursor = cur
                        if (cur >= src.size) {
                            activeVoices.removeAt(vIdx)
                        }
                        vIdx--
                    }

                    for (i in 0 until chunkSamples) {
                        val clipped = softClip(mixFloat[i])
                        outPcm[i] = (clipped * Short.MAX_VALUE * 0.92f).toInt().toShort()
                    }

                    if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
                        try { track?.release() } catch (_: Throwable) {}
                        track = createStreamingTrack(chunkSamples * 2)
                    }

                    val activeTrack = track
                    if (activeTrack != null) {
                        if (activeTrack.playState != AudioTrack.PLAYSTATE_PLAYING) {
                            try { activeTrack.play() } catch (_: Throwable) {}
                        }
                        val written = try {
                            activeTrack.write(outPcm, 0, chunkSamples)
                        } catch (_: Throwable) {
                            -1
                        }
                        if (written < 0) {
                            // Self-heal if Android audio routing or track ever invalidates
                            try { activeTrack.stop() } catch (_: Throwable) {}
                            try { activeTrack.release() } catch (_: Throwable) {}
                            track = createStreamingTrack(chunkSamples * 2)
                            delay(20L)
                        }
                    } else {
                        delay(20L)
                    }
                }
            } finally {
                try { track?.stop() } catch (_: Throwable) {}
                try { track?.release() } catch (_: Throwable) {}
            }
        }
    }

    private fun synthesizeBgmStep(
        durationSec: Float,
        melodyFreq: Double,
        bassFreq: Double,
        counterFreq: Double,
        paperBrush: Boolean,
        accent: Boolean
    ): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        var lpNoise = 0f
        var hpPrev = 0f

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val attack = (t / 0.005f).coerceIn(0f, 1f)

            // 1. Plucked Koto + Music-Box Kalimba Melody Voice
            val kotoBend = if (accent) 1.0 + 0.018 * exp(-35.0 * t) else 1.0
            val fMel = melodyFreq * kotoBend
            val melEnv = exp(-8.5 * t).toFloat()
            val harmEnv = exp(-22.0 * t).toFloat()
            val tineEnv = exp(-45.0 * t).toFloat()

            val kotoVoice = (
                sin(2.0 * PI * fMel * t) * 0.55 * melEnv +
                sin(2.0 * PI * (fMel * 2.0) * t) * 0.25 * harmEnv +
                sin(2.0 * PI * (fMel * 2.98) * t) * 0.12 * tineEnv +
                sin(2.0 * PI * (fMel * 4.0) * t) * 0.06 * tineEnv
            ).toFloat() * attack

            // 2. Warm Hollow Bamboo Bass Pluck
            val bassVoice = if (bassFreq > 10.0) {
                val bEnv = exp(-6.5 * t).toFloat()
                val bOvertone = exp(-18.0 * t).toFloat()
                (
                    sin(2.0 * PI * bassFreq * t) * 0.65 * bEnv +
                    sin(2.0 * PI * (bassFreq * 2.0) * t) * 0.25 * bOvertone
                ).toFloat() * attack
            } else 0f

            // 3. Sparkling Furin Wind-Chime Counter-Melody during Fever / High Cascades
            val counterVoice = if (counterFreq > 10.0 && t >= durationSec * 0.45f) {
                val ct = t - durationSec * 0.45f
                val cAttack = (ct / 0.003f).coerceIn(0f, 1f)
                val cEnv = exp(-14.0 * ct).toFloat()
                (
                    sin(2.0 * PI * counterFreq * ct) * 0.50 * cEnv +
                    sin(2.0 * PI * (counterFreq * 2.0) * ct) * 0.22 * cEnv
                ).toFloat() * cAttack
            } else 0f

            // 4. Soft Washi Paper-Brush Rhythm Shaker on off-beats
            val raw = Random.nextFloat() * 2f - 1f
            lpNoise = lpNoise * 0.82f + raw * 0.18f
            val bpNoise = lpNoise - hpPrev
            hpPrev = lpNoise * 0.94f
            val brushVoice = if (paperBrush) {
                val bEnv = sin((t / (durationSec * 0.45f)).coerceIn(0f, 1f) * PI).toFloat() * exp(-26.0 * t).toFloat()
                bpNoise * bEnv * 0.09f
            } else 0f

            val mix = (kotoVoice * 0.20f) + (bassVoice * 0.16f) + (counterVoice * 0.14f) + brushVoice
            samples[i] = (softClip(mix) * Short.MAX_VALUE * 0.42f).toInt().toShort()
        }
        return samples
    }

    /**
     * Crisp Origami Crease Snap + Bamboo Kalimba Pluck for tapping/selecting paper tiles.
     */
    fun playPaperCrinkle(intensity: Int = 1) {
        if (!isEnabled) return
        val noteIndex = (tapCycleIndex++ + intensity - 1) % yoPentatonic.size
        val baseFreq = yoPentatonic[noteIndex.coerceIn(3, 10)]
        scope.launch {
            val durationSec = 0.18f + (intensity.coerceIn(1, 4) * 0.02f)
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.0025f).coerceIn(0f, 1f)

                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.72f + raw * 0.28f
                val paperBand = lpNoise - prevLp
                prevLp = lpNoise * 0.88f

                val creaseBurst1 = exp(-120.0 * t).toFloat()
                val creaseBurst2 = if (t >= 0.014f) exp(-95.0 * (t - 0.014f)).toFloat() * 0.75f else 0f
                val creaseBurst3 = if (t >= 0.032f) exp(-75.0 * (t - 0.032f)).toFloat() * 0.50f else 0f
                val paperCreaseFoley = paperBand * (creaseBurst1 + creaseBurst2 + creaseBurst3) * 0.42f

                val knockFreq = 185.0 + 125.0 * exp(-90.0 * t)
                val knockEnv = exp(-48.0 * t).toFloat()
                val woodKnock = sin(2.0 * PI * knockFreq * t).toFloat() * knockEnv * 0.28f

                val env1 = exp(-18.0 * t).toFloat()
                val envTine = exp(-42.0 * t).toFloat()
                val kalimba = (
                    sin(2.0 * PI * baseFreq * t).toFloat() * env1 * 0.58f +
                    sin(2.0 * PI * (baseFreq * 2.0) * t).toFloat() * envTine * 0.22f +
                    sin(2.0 * PI * (baseFreq * 2.92) * t).toFloat() * envTine * 0.14f
                )

                val sample = softClip((paperCreaseFoley + woodKnock + kalimba) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.66f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Authentic Steel Scissor-Blade Shear + Paper Severing Snap + Silver Tuning-Fork Ring.
     */
    fun playScissorsSnip() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.28f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lp1 = 0f
            var lp2 = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.002f).coerceIn(0f, 1f)

                val snip1Env = exp(-34.0 * t).toFloat()
                val snip2Env = if (t >= 0.082f) exp(-30.0 * (t - 0.082f)).toFloat() else 0f

                val raw = Random.nextFloat() * 2f - 1f
                lp1 = lp1 * 0.45f + raw * 0.55f
                lp2 = lp2 * 0.82f + lp1 * 0.18f
                val highSteelNoise = (lp1 - lp2) * (snip1Env + snip2Env * 1.1f) * 0.52f

                val ring1 = (
                    sin(2.0 * PI * 1480.0 * t) * 0.45 +
                    sin(2.0 * PI * 3890.0 * t) * 0.30 +
                    sin(2.0 * PI * 5420.0 * t) * 0.15
                ).toFloat() * snip1Env

                val t2 = (t - 0.082f).coerceAtLeast(0f)
                val ring2 = (
                    sin(2.0 * PI * 1975.5 * t2) * 0.50 +
                    sin(2.0 * PI * 5120.0 * t2) * 0.30
                ).toFloat() * snip2Env

                val sample = softClip((highSteelNoise + ring1 + ring2) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.65f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Tactile Wet Craft-Glue Uncapping Pop + Viscous Squish + Koto Confirmation Pluck.
     */
    fun playGlueSwap() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.27f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.003f).coerceIn(0f, 1f)

                val glideFreq = 280.0 + 540.0 * (t / 0.14f).coerceIn(0f, 1f).pow(1.6f)
                val squishEnv = exp(-12.0 * t).toFloat()
                val squishMod = 1.0f + 0.35f * sin(2.0 * PI * 38.0 * t).toFloat()
                val glueSquish = (
                    sin(2.0 * PI * glideFreq * t) * 0.62 +
                    sin(2.0 * PI * (glideFreq * 1.85) * t) * 0.28
                ).toFloat() * squishEnv * squishMod

                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.78f + raw * 0.22f
                val pop2 = if (t >= 0.095f) {
                    val lt = t - 0.095f
                    val env2 = exp(-20.0 * lt).toFloat()
                    val peelCrackle = lpNoise * exp(-45.0 * lt).toFloat() * 0.22f
                    (sin(2.0 * PI * 1174.66 * lt).toFloat() * 0.52f +
                        sin(2.0 * PI * 1760.00 * lt).toFloat() * 0.26f) * env2 + peelCrackle
                } else 0f

                val sample = softClip((glueSquish + pop2) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.65f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Corrugated Cardboard Carton Stomp + Kraft-Paper Rip + Taiko/Koto Power Chord.
     */
    fun playCardboardSmash() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.38f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.0025f).coerceIn(0f, 1f)

                val kickPhase = 2.0 * PI * (48.0 * t + (127.0 / 24.0) * (1.0 - exp(-24.0 * t)))
                val kickEnv = exp(-10.5 * t).toFloat()
                val boxCavity = sin(2.0 * PI * 142.0 * t).toFloat() * exp(-22.0 * t).toFloat() * 0.32f
                val cardboardStomp = sin(kickPhase).toFloat() * kickEnv * 0.68f + boxCavity

                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.70f + raw * 0.30f
                val tearBand = lpNoise - prevLp
                prevLp = lpNoise * 0.86f
                val tearGate = (0.65f + 0.35f * sin(2.0 * PI * 95.0 * t).toFloat()) * exp(-14.0 * t).toFloat()
                val paperRip = tearBand * tearGate * 0.42f

                val chordEnv = exp(-10.0 * t).toFloat()
                val kotoChord = (
                    sin(2.0 * PI * 587.33 * t) * 0.24 +
                    sin(2.0 * PI * 880.00 * t) * 0.22 +
                    sin(2.0 * PI * 1174.66 * t) * 0.18
                ).toFloat() * chordEnv

                val sample = softClip((cardboardStomp + paperRip + kotoChord) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.72f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Authentic Torn Washi Paper Rip Foley + Ascending Plucked Koto & Music-Box Arpeggio
     * in the Japanese Yo Pentatonic scale.
     */
    fun playComboChord(comboStep: Int) {
        if (!isEnabled) return
        if (comboStep >= 2) {
            feverIntensity = 1
        }
        scope.launch {
            val durationSec = 0.52f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            val semitoneShift = (comboStep - 1).coerceIn(0, 12) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)

            val notes = doubleArrayOf(
                587.33 * pitchMult,
                659.25 * pitchMult,
                783.99 * pitchMult,
                880.00 * pitchMult,
                987.77 * pitchMult,
                1174.66 * pitchMult
            )

            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var mix = 0f

                if (t < 0.13f) {
                    val raw = Random.nextFloat() * 2f - 1f
                    lpNoise = lpNoise * 0.68f + raw * 0.32f
                    val ripBand = lpNoise - prevLp
                    prevLp = lpNoise * 0.85f
                    val fiberGrain = (0.55f + 0.45f * sin(2.0 * PI * 145.0 * t).toFloat())
                    val ripEnv = sin((t / 0.13f) * PI).toFloat() * exp(-12.0 * t).toFloat()
                    mix += ripBand * fiberGrain * ripEnv * 0.36f
                }

                for (idx in notes.indices) {
                    val start = idx * 0.032f
                    if (t >= start) {
                        val lt = t - start
                        val noteAttack = (lt / 0.0025f).coerceIn(0f, 1f)
                        val freq = notes[idx] * (1.0 + 0.015 * exp(-65.0 * lt))
                        val envMain = exp(-7.5 * lt).toFloat()
                        val envHarm = exp(-17.0 * lt).toFloat()
                        val envTine = exp(-32.0 * lt).toFloat()

                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.52 * envMain +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.26 * envHarm +
                            sin(2.0 * PI * (freq * 2.96) * lt) * 0.14 * envTine +
                            sin(2.0 * PI * (freq * 3.0) * lt) * 0.10 * envTine
                        ).toFloat() * noteAttack

                        mix += voice * 0.32f
                    }
                }

                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Dry Crumpled-Paper Crinkle + Hollow Bamboo Woodblock Double-Knock (G4 -> D4) for invalid swaps.
     */
    fun playInvalidShake() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.22f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val isSecond = t >= 0.095f
                val lt = if (isSecond) t - 0.095f else t
                val freq = if (!isSecond) 392.00 else 293.66
                val attack = (lt / 0.0025f).coerceIn(0f, 1f)
                val env = exp(-28.0 * lt).toFloat()
                val tineEnv = exp(-55.0 * lt).toFloat()

                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.74f + raw * 0.26f
                val crinkle = (raw - lpNoise) * exp(-40.0 * lt).toFloat() * 0.22f

                val woodblock = (
                    sin(2.0 * PI * freq * lt).toFloat() * env * 0.72f +
                    sin(2.0 * PI * (freq * 2.42) * lt).toFloat() * tineEnv * 0.28f
                ) * attack

                samples[i] = (softClip(woodblock + crinkle) * Short.MAX_VALUE * 0.54f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Tactile Paper-Coaster Slide across Corrugated Cardboard + Crisp Mid-Swap Origami Crease
     * + Dual-Note Bamboo Flute & Koto Interval (D5 -> A5).
     */
    fun playSwapWhoosh() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.18f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val norm = (t / durationSec).coerceIn(0f, 1f)
                val attack = (t / 0.003f).coerceIn(0f, 1f)

                val raw = Random.nextFloat() * 2f - 1f
                val filterCoeff = (0.86f - 0.28f * sin(PI * norm).toFloat()).coerceIn(0.45f, 0.90f)
                lpNoise = lpNoise * filterCoeff + raw * (1f - filterCoeff)
                val slideFriction = (lpNoise - prevLp) * sin(PI * norm).toFloat() * 0.38f
                prevLp = lpNoise * 0.85f

                val foldSnap = if (abs(t - 0.065f) < 0.025f) {
                    val dt = abs(t - 0.065f)
                    (raw - lpNoise) * exp(-110.0 * dt).toFloat() * 0.28f
                } else 0f

                val isSecondNote = t >= 0.065f
                val noteT = if (isSecondNote) t - 0.065f else t
                val noteFreq = if (isSecondNote) 880.00 else 587.33
                val noteEnv = exp(-22.0 * noteT).toFloat()
                val kotoPluck = (
                    sin(2.0 * PI * noteFreq * noteT) * 0.62 +
                    sin(2.0 * PI * (noteFreq * 2.0) * noteT) * 0.24
                ).toFloat() * noteEnv

                val sample = softClip((slideFriction + foldSnap + kotoPluck) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.58f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Corrugated Cardboard Box-Lid Landing Thumps + Paper-Edge Flutter + Ascending Bamboo Marimba
     * & Koto Plucks in the Japanese Yo Pentatonic Scale!
     */
    fun playDropBounce(tileCount: Int = 3, comboStep: Int = 1) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.34f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val hits = tileCount.coerceIn(2, 6)
            val dropNotes = doubleArrayOf(293.66, 329.63, 392.00, 440.00, 493.88, 587.33)
            val semitoneShift = (comboStep - 1).coerceIn(0, 10) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)
            val comboBoost = (comboStep - 1).coerceIn(0, 5)
            val stepDelay = (0.025f - comboBoost * 0.002f).coerceAtLeast(0.016f)

            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.76f + raw * 0.24f

                var acc = 0f
                for (h in 0 until hits) {
                    val delay = h * stepDelay
                    if (t >= delay) {
                        val lt = t - delay
                        val attack = (lt / 0.0025f).coerceIn(0f, 1f)
                        val baseNote = dropNotes[h % dropNotes.size] * pitchMult

                        val boxFreq = 64.0 + (74.0 + comboBoost * 12.0) * exp(-65.0 * lt)
                        val boxEnv = exp(-28.0 * lt).toFloat()
                        val cardboardThump = sin(2.0 * PI * boxFreq * lt).toFloat() * boxEnv * (0.38f + comboBoost * 0.04f)

                        val slapEnv = exp(-75.0 * lt).toFloat()
                        val paperSlap = (raw - lpNoise) * slapEnv * (0.18f + comboBoost * 0.03f)

                        val marimbaEnv = exp(-19.0 * lt).toFloat()
                        val harmEnv = exp(-36.0 * lt).toFloat()
                        val furinSparkle = if (comboStep >= 2) {
                            (sin(2.0 * PI * (baseNote * 2.95) * lt) * 0.22 +
                                sin(2.0 * PI * (baseNote * 4.0) * lt) * 0.14).toFloat() *
                                exp(-42.0 * lt).toFloat() * (0.4f + comboBoost * 0.15f)
                        } else 0f

                        val marimba = (
                            sin(2.0 * PI * baseNote * lt).toFloat() * marimbaEnv * 0.62f +
                            sin(2.0 * PI * (baseNote * 2.0) * lt).toFloat() * harmEnv * 0.24f +
                            furinSparkle
                        )

                        acc += (cardboardThump + paperSlap + marimba) * attack * 0.38f
                    }
                }
                val value = softClip(acc)
                samples[i] = (value * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Celebratory Origami Grand Fanfare: Plucked Koto Glissando + Furin Wind-Chimes + Confetti Paper Burst!
     */
    fun playFeverFanfare() {
        if (!isEnabled) return
        feverIntensity = 1
        scope.launch {
            val durationSec = 0.72f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val notes = doubleArrayOf(587.33, 659.25, 783.99, 880.00, 987.77, 1174.66, 1567.98)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.75f + raw * 0.25f
                var mix = 0f

                if (t < 0.09f) {
                    val popEnv = exp(-35.0 * t).toFloat()
                    mix += (raw - lpNoise) * popEnv * 0.26f
                }

                for (idx in notes.indices) {
                    val start = idx * 0.046f
                    if (t >= start) {
                        val lt = t - start
                        val attack = (lt / 0.003f).coerceIn(0f, 1f)
                        val freq = notes[idx] * (1.0 + 0.012 * exp(-50.0 * lt))
                        val decayRate = if (idx == notes.lastIndex) 3.8 else 6.4
                        val env = exp(-decayRate * lt).toFloat()
                        val tineEnv = exp(-16.0 * lt).toFloat()

                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.54 * env +
                            sin(2.0 * PI * (freq * 1.003) * lt) * 0.22 * env +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.20 * tineEnv +
                            sin(2.0 * PI * (freq * 2.97) * lt) * 0.12 * tineEnv
                        ).toFloat() * attack
                        mix += voice * 0.28f
                    }
                }
                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Smooth analog-style soft clipper so overlapping harmonies stay warm and never distort.
     */
    private fun softClip(x: Float): Float {
        val clamped = x.coerceIn(-1.5f, 1.5f)
        return (clamped - (clamped * clamped * clamped) / 6.75f).coerceIn(-1f, 1f)
    }
}
