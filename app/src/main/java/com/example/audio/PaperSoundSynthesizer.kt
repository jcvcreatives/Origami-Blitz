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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Playful "Origami Toy-Box & Craft Carnival" Acoustic Synthesizer + Swing BGM Engine.
 *
 * Combines:
 * - A bouncy 32-step swung Toy-Marimba, Whistling Ocarina, Rubber-Band Pizzicato Bass,
 *   and Cardboard-Box / Washi-Shaker Percussion BGM orchestra with stage-specific keys
 *   and a double-time Fever Carnival layer.
 * - Distinct, tactile cartoon-craft Foley SFX (Cheek-Pop Cork Plucks, Slide-Whistle Swaps,
 *   Jaw-Harp Rubber-Band Boings, Barber-Scissor Double-Snips, Squeezy Glue Squirt-Pops,
 *   and Kazoo-Cheer + Bubble-Wrap Cascade Arpeggios).
 * - Uses a SINGLE persistent streaming AudioTrack (MODE_STREAM) so Android's AudioFlinger
 *   never drops audio or exhausts native track handles.
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

    @Volatile
    private var lifecyclePaused: Boolean = false

    private val bgmStepCache = ConcurrentHashMap<Int, ShortArray>()

    // Playful Major Pentatonic + Mixolydian Toy-Box Scale (C, D, E, G, A, Bb, C...)
    // Gives a bright, cheerful, infectious Nintendo/Craft-World bounce!
    private val playfulScale = doubleArrayOf(
        261.63, // 0: C4
        293.66, // 1: D4
        329.63, // 2: E4
        392.00, // 3: G4
        440.00, // 4: A4
        466.16, // 5: Bb4 (cheeky bluesy/playful 7th)
        523.25, // 6: C5
        587.33, // 7: D5
        659.25, // 8: E5
        783.99, // 9: G5
        880.00, // 10: A5
        932.33, // 11: Bb5
        1046.50, // 12: C6
        1174.66, // 13: D6
        1318.51, // 14: E6
        1567.98  // 15: G6
    )

    private var tapCycleIndex = 0

    init {
        ensureMixerLoopRunning()
    }

    /**
     * Updates the musical mood of the Origami Toy-Box BGM loop to match the current stage
     * and active Fever / Cascade state.
     */
    fun updateMusicalMood(stageNumber: Int, isFeverOrHighCombo: Boolean) {
        currentStageIndex = stageNumber.coerceIn(1, 10)
        feverIntensity = if (isFeverOrHighCombo) 1 else 0
        if (isEnabled && !lifecyclePaused) {
            bgmEnabled = true
            ensureMixerLoopRunning()
        }
    }

    fun pauseForLifecycle() {
        lifecyclePaused = true
        pendingVoices.clear()
        queuedVoiceCount.set(0)
    }

    fun resumeForLifecycle() {
        lifecyclePaused = false
        if (isEnabled) {
            ensureMixerLoopRunning()
        }
    }

    fun startWorkshopMusicLoop() {
        bgmEnabled = true
        if (isEnabled && !lifecyclePaused) {
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
        if (!isEnabled || lifecyclePaused || samples.isEmpty()) return
        ensureMixerLoopRunning()
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
     * Single persistent streaming AudioTrack mixer & swung 32-step Toy-Box BGM sequencer.
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

            // 32-step catchy, bouncy "Origami Toy-Box" melody with playful syncopation & rests (-1)
            val melodyPattern = intArrayOf(
                // Bar 1: Bouncy call
                6, 8, 9, -1,   10, 9, 8, 6,
                // Bar 2: Cheeky answer with bluesy 7th & octave hop
                7, 8, 9, 11,   12, -1, 9, -1,
                // Bar 3: Playful ascending toy-marimba run
                6, 7, 8, 9,    10, 12, 13, 12,
                // Bar 4: Turnaround glockenspiel wink
                10, 9, 8, 7,   6, 9, 12, -1
            )

            // Whistling Ocarina / Toy-Accordion harmony & counter-hooks
            val ocarinaPattern = intArrayOf(
                -1, -1, 12, -1,  -1, 13, 12, -1,
                -1, -1, 13, 14,  15, -1, 12, -1,
                8, -1, 10, -1,   12, -1, 15, -1,
                13, 12, 10, 9,   8, 12, 15, -1
            )

            // Bouncy Rubber-Band Pizzicato Bassline (walking root-fifth-octave bounce)
            val bassPattern = intArrayOf(
                0, -1, 3, -1,   4, -1, 3, -1,
                1, -1, 3, -1,   6, -1, 3, 1,
                0, -1, 2, -1,   3, -1, 4, -1,
                1, -1, 3, -1,   0, 3, 6, -1
            )

            var bgmStep = 0
            var samplesUntilNextBgmStep = 0
            var track: AudioTrack? = createStreamingTrack(chunkSamples * 2)

            try {
                while (isActive && mixerRunning) {
                    if (!isEnabled || lifecyclePaused) {
                        activeVoices.clear()
                        pendingVoices.clear()
                        queuedVoiceCount.set(0)
                        delay(80L)
                        continue
                    }

                    while (true) {
                        val next = pendingVoices.poll() ?: break
                        queuedVoiceCount.decrementAndGet()
                        if (activeVoices.size >= 14) {
                            val bgmIdx = activeVoices.indexOfFirst { it.isBgm }
                            if (bgmIdx >= 0) {
                                activeVoices.removeAt(bgmIdx)
                            } else {
                                activeVoices.removeAt(0)
                            }
                        }
                        activeVoices.add(next)
                    }

                    if (bgmEnabled) {
                        samplesUntilNextBgmStep -= chunkSamples
                        if (samplesUntilNextBgmStep <= 0) {
                            val stageMod = currentStageIndex % 5
                            val stageShiftSemitones = when (stageMod) {
                                1 -> 0   // C Major Toy-Box (Whispering Pine Woods)
                                2 -> 2   // D Major Carnival (Peppermint Boardwalk)
                                3 -> 5   // F Major Canyon Bounce (Golden Canyon)
                                4 -> -3  // A Major Starlight (Summit / Harbor)
                                else -> 4 // E Major Royal Parade (Bakery / Castle)
                            }
                            val isExcited = feverIntensity > 0
                            val stepIdx = bgmStep % 32
                            // Swing groove! Even steps are slightly longer, odd steps are snappy
                            val baseStepSec = if (isExcited) 0.165f else 0.195f
                            val swingRatio = if (stepIdx % 2 == 0) 1.14f else 0.86f
                            val stepDurationSec = baseStepSec * swingRatio
                            samplesUntilNextBgmStep = (sampleRate * stepDurationSec * 0.96f).toInt()

                            val cacheKey = (stageMod * 128) + (if (isExcited) 64 else 0) + stepIdx
                            val bgmSamples = bgmStepCache.getOrPut(cacheKey) {
                                val pitchShift = 2.0.pow(stageShiftSemitones / 12.0)
                                val melIdx = melodyPattern[stepIdx]
                                val ocaIdx = ocarinaPattern[stepIdx]
                                val bassIdx = bassPattern[stepIdx]

                                val melFreq = if (melIdx >= 0) playfulScale[melIdx] * pitchShift else 0.0
                                val ocaFreq = if (ocaIdx >= 0 && (isExcited || stepIdx % 4 >= 2)) {
                                    playfulScale[ocaIdx] * pitchShift
                                } else 0.0
                                val bassFreq = if (bassIdx >= 0) playfulScale[bassIdx] * pitchShift * 0.5 else 0.0

                                synthesizePlayfulBgmStep(
                                    stepIndex = stepIdx,
                                    durationSec = stepDurationSec * 1.25f,
                                    melodyFreq = melFreq,
                                    ocarinaFreq = ocaFreq,
                                    bassFreq = bassFreq,
                                    isExcited = isExcited
                                )
                            }
                            activeVoices.add(ActiveVoice(samples = bgmSamples, isBgm = true))
                            bgmStep = (bgmStep + 1) % 32
                        }
                    }

                    if (activeVoices.isEmpty()) {
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

    /**
     * Synthesizes a single swung step of the "Origami Toy-Box" BGM:
     * 1. Hollow Wooden Toy Marimba + Mallet Click + Cheeky Pitch-Scoop Grace Note
     * 2. Whimsical Whistling Ocarina with Breath Vibrato
     * 3. Bouncy Rubber-Band Pizzicato Bass
     * 4. Craft-Table Percussion (Cardboard Thumb-Kick, Washi-Tape Finger-Snap, Paper Maraca Shaker)
     */
    private fun synthesizePlayfulBgmStep(
        stepIndex: Int,
        durationSec: Float,
        melodyFreq: Double,
        ocarinaFreq: Double,
        bassFreq: Double,
        isExcited: Boolean
    ): ShortArray {
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        var lpNoise = 0f
        var hpPrev = 0f

        val isDownbeatKick = (stepIndex % 8 == 0) || (isExcited && stepIndex % 4 == 0)
        val isTapeSnap = (stepIndex % 8 == 4)
        val isPaperShaker = (stepIndex % 2 == 1)
        val isCheekyScoop = (stepIndex % 4 == 0)

        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val attack = (t / 0.0035f).coerceIn(0f, 1f)

            // 1. Toy Marimba + Xylophone Mallet Voice (with cheeky upward pitch scoop on accented beats)
            val marimbaVoice = if (melodyFreq > 10.0) {
                val scoop = if (isCheekyScoop) 1.0 - 0.045 * exp(-55.0 * t) else 1.0
                val fMel = melodyFreq * scoop
                val bodyEnv = exp(-10.5 * t).toFloat()
                val woodHarmEnv = exp(-28.0 * t).toFloat()
                val malletClickEnv = exp(-85.0 * t).toFloat()

                // Marimba has a strong 1st and 4th harmonic (tube resonator)
                (
                    sin(2.0 * PI * fMel * t) * 0.60 * bodyEnv +
                    sin(2.0 * PI * (fMel * 2.0) * t) * 0.18 * woodHarmEnv +
                    sin(2.0 * PI * (fMel * 3.98) * t) * 0.26 * woodHarmEnv +
                    sin(2.0 * PI * (fMel * 6.12) * t) * 0.12 * malletClickEnv
                ).toFloat() * attack
            } else 0f

            // 2. Whimsical Whistling Ocarina / Toy Accordion Counter-Hook
            val ocarinaVoice = if (ocarinaFreq > 10.0) {
                val ocaAttack = (t / 0.015f).coerceIn(0f, 1f)
                val vibrato = 1.0 + 0.008 * sin(2.0 * PI * 6.5 * t)
                val fOca = ocarinaFreq * vibrato
                val ocaEnv = exp(-5.8 * t).toFloat()
                (
                    sin(2.0 * PI * fOca * t) * 0.52 +
                    sin(2.0 * PI * (fOca * 3.0) * t) * 0.12
                ).toFloat() * ocaEnv * ocaAttack
            } else 0f

            // 3. Bouncy Rubber-Band Pizzicato Bass (with pitch-drop twang)
            val bassVoice = if (bassFreq > 10.0) {
                val fBass = bassFreq * (1.0 + 0.08 * exp(-40.0 * t))
                val bEnv = exp(-8.0 * t).toFloat()
                val bTwang = exp(-22.0 * t).toFloat()
                (
                    sin(2.0 * PI * fBass * t) * 0.66 * bEnv +
                    sin(2.0 * PI * (fBass * 2.0) * t) * 0.28 * bTwang +
                    sin(2.0 * PI * (fBass * 3.0) * t) * 0.14 * bTwang
                ).toFloat() * attack
            } else 0f

            // 4. Craft-Table Percussion Kit (Cardboard Box Kick + Washi Snap + Paper Shaker)
            val raw = Random.nextFloat() * 2f - 1f
            lpNoise = lpNoise * 0.78f + raw * 0.22f
            val bpNoise = lpNoise - hpPrev
            hpPrev = lpNoise * 0.91f

            val kickVoice = if (isDownbeatKick && t < 0.11f) {
                val kFreq = 58.0 + 110.0 * exp(-48.0 * t)
                val kEnv = exp(-22.0 * t).toFloat()
                sin(2.0 * PI * kFreq * t).toFloat() * kEnv * 0.26f
            } else 0f

            val snapVoice = if (isTapeSnap && t < 0.07f) {
                val sEnv = exp(-55.0 * t).toFloat()
                val woodPop = sin(2.0 * PI * (920.0 - 340.0 * (t / 0.07f)) * t).toFloat() * sEnv * 0.18f
                val paperCrisp = bpNoise * sEnv * 0.22f
                woodPop + paperCrisp
            } else 0f

            val shakerVoice = if (isPaperShaker && t < 0.06f) {
                val shEnv = sin((t / 0.06f) * PI).toFloat() * exp(-32.0 * t).toFloat()
                bpNoise * shEnv * 0.11f
            } else 0f

            val mix = (marimbaVoice * 0.22f) +
                (ocarinaVoice * 0.15f) +
                (bassVoice * 0.18f) +
                kickVoice + snapVoice + shakerVoice

            samples[i] = (softClip(mix) * Short.MAX_VALUE * 0.46f).toInt().toShort()
        }
        return samples
    }

    /**
     * Playful "Cheek-Pop / Cork-Pluck" + Crisp Origami Fold Zip + Musical Toy Kalimba
     * that ascends a cheerful scale as the player taps tiles!
     */
    fun playPaperCrinkle(intensity: Int = 1) {
        if (!isEnabled) return
        val noteIndex = (tapCycleIndex++ + intensity - 1) % playfulScale.size
        val baseFreq = playfulScale[noteIndex.coerceIn(4, 13)]
        scope.launch {
            val durationSec = 0.19f + (intensity.coerceIn(1, 4) * 0.018f)
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.002f).coerceIn(0f, 1f)

                // 1. Tactile Origami Paper Crease Zip
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.70f + raw * 0.30f
                val paperBand = lpNoise - prevLp
                prevLp = lpNoise * 0.87f
                val zipEnv = exp(-95.0 * t).toFloat() +
                    (if (t >= 0.016f) exp(-80.0 * (t - 0.016f)).toFloat() * 0.65f else 0f)
                val paperCreaseFoley = paperBand * zipEnv * 0.36f

                // 2. Cheeky "Bubble/Cork Pop" upward pitch sweep (0..38ms)
                val popVoice = if (t < 0.042f) {
                    val popSweep = 240.0 + 680.0 * (t / 0.042f).pow(1.7f)
                    val popEnv = sin((t / 0.042f) * PI).toFloat()
                    sin(2.0 * PI * popSweep * t).toFloat() * popEnv * 0.42f
                } else 0f

                // 3. Bright Toy Kalimba + Bell Harmonic
                val env1 = exp(-16.0 * t).toFloat()
                val envTine = exp(-38.0 * t).toFloat()
                val kalimba = (
                    sin(2.0 * PI * baseFreq * t).toFloat() * env1 * 0.56f +
                    sin(2.0 * PI * (baseFreq * 2.0) * t).toFloat() * envTine * 0.24f +
                    sin(2.0 * PI * (baseFreq * 3.98) * t).toFloat() * envTine * 0.18f
                )

                val sample = softClip((paperCreaseFoley + popVoice + kalimba) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Playful "Double Barber-Scissor Snip-Snip (`Snip-Snip!`) + Paper Ribbon Zip + Sparkling Bell".
     */
    fun playScissorsSnip() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.31f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lp1 = 0f
            var lp2 = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.002f).coerceIn(0f, 1f)

                val snip1Env = exp(-36.0 * t).toFloat()
                val snip2Env = if (t >= 0.078f) exp(-32.0 * (t - 0.078f)).toFloat() else 0f
                val sparkleEnv = if (t >= 0.14f) exp(-18.0 * (t - 0.14f)).toFloat() else 0f

                val raw = Random.nextFloat() * 2f - 1f
                lp1 = lp1 * 0.42f + raw * 0.58f
                lp2 = lp2 * 0.80f + lp1 * 0.20f
                val shearNoise = (lp1 - lp2) * (snip1Env + snip2Env * 1.15f) * 0.54f

                // First blade shear (crisp metallic zing)
                val ring1 = (
                    sin(2.0 * PI * (1520.0 + 400.0 * t) * t) * 0.45 +
                    sin(2.0 * PI * 3950.0 * t) * 0.28
                ).toFloat() * snip1Env

                // Second blade shear (higher playful pitch)
                val t2 = (t - 0.078f).coerceAtLeast(0f)
                val ring2 = (
                    sin(2.0 * PI * (2080.0 + 520.0 * t2) * t2) * 0.50 +
                    sin(2.0 * PI * 5240.0 * t2) * 0.30
                ).toFloat() * snip2Env

                // Happy completion twinkle chime
                val t3 = (t - 0.14f).coerceAtLeast(0f)
                val twinkle = (
                    sin(2.0 * PI * 1567.98 * t3) * 0.42 +
                    sin(2.0 * PI * 2093.00 * t3) * 0.26
                ).toFloat() * sparkleEnv

                val sample = softClip((shearNoise + ring1 + ring2 + twinkle) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Playful "Squeezy Glue-Bottle Squirt (`Bloop-Squelch-Pop!`) + Elastic Rubber-Band Boing".
     */
    fun playGlueSwap() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.30f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.0025f).coerceIn(0f, 1f)

                // Bubble-squelch FM modulation
                val bubbleMod = sin(2.0 * PI * 46.0 * t) * 140.0 * exp(-8.0 * t)
                val glideFreq = 260.0 + 620.0 * (t / 0.16f).coerceIn(0f, 1f).pow(1.4f) + bubbleMod
                val squishEnv = exp(-10.5 * t).toFloat()
                val glueSquish = (
                    sin(2.0 * PI * glideFreq * t) * 0.60 +
                    sin(2.0 * PI * (glideFreq * 1.92) * t) * 0.28
                ).toFloat() * squishEnv

                // Second stage: Suction-cup POP!
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.78f + raw * 0.22f
                val suctionPop = if (t >= 0.105f) {
                    val lt = t - 0.105f
                    val env2 = exp(-20.0 * lt).toFloat()
                    val popPitch = 780.0 + 920.0 * (lt / 0.05f).coerceIn(0f, 1f)
                    val peelCrackle = lpNoise * exp(-45.0 * lt).toFloat() * 0.20f
                    (sin(2.0 * PI * popPitch * lt).toFloat() * 0.56f +
                        sin(2.0 * PI * 1567.98 * lt).toFloat() * 0.28f) * env2 + peelCrackle
                } else 0f

                val sample = softClip((glueSquish + suctionPop) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Playful "Cartoon Accordion-Squash + Giant Cardboard Box Boom + Springy Toy Cymbal".
     */
    fun playCardboardSmash() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.42f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.002f).coerceIn(0f, 1f)

                // 1. Punchy Cardboard Carton Stomp
                val kickPhase = 2.0 * PI * (52.0 * t + (135.0 / 24.0) * (1.0 - exp(-24.0 * t)))
                val kickEnv = exp(-9.5 * t).toFloat()
                val boxCavity = sin(2.0 * PI * 148.0 * t).toFloat() * exp(-20.0 * t).toFloat() * 0.34f
                val cardboardStomp = sin(kickPhase).toFloat() * kickEnv * 0.66f + boxCavity

                // 2. Washi Paper Rip & Confetti Burst
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.68f + raw * 0.32f
                val tearBand = lpNoise - prevLp
                prevLp = lpNoise * 0.85f
                val tearGate = (0.65f + 0.35f * sin(2.0 * PI * 98.0 * t).toFloat()) * exp(-12.0 * t).toFloat()
                val paperRip = tearBand * tearGate * 0.40f

                // 3. Playful Toy-Brass / Accordion Power Chord + Spring Boing
                val chordEnv = exp(-8.5 * t).toFloat()
                val springWobble = 1.0 + 0.025 * sin(2.0 * PI * 28.0 * t)
                val toyChord = (
                    sin(2.0 * PI * 523.25 * springWobble * t) * 0.25 +
                    sin(2.0 * PI * 659.25 * springWobble * t) * 0.22 +
                    sin(2.0 * PI * 783.99 * springWobble * t) * 0.20 +
                    sin(2.0 * PI * 1046.50 * springWobble * t) * 0.16
                ).toFloat() * chordEnv

                val sample = softClip((cardboardStomp + paperRip + toyChord) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.74f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Addictive, Escalating "Bubble-Wrap Pop + Ascending Toy-Marimba Arpeggio + Slide-Whistle Cheer"
     * that pitches up and adds celebratory harmonics with every cascade step (x1, x2, x3, x4, x5+)!
     */
    fun playComboChord(comboStep: Int) {
        if (!isEnabled) return
        if (comboStep >= 2) {
            feverIntensity = 1
        }
        scope.launch {
            val durationSec = (0.48f + comboStep.coerceIn(1, 5) * 0.04f).coerceAtMost(0.68f)
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)

            val semitoneShift = (comboStep - 1).coerceIn(0, 12) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)

            // Cheerful Major 9th / Pentatonic Arpeggio (C5, E5, G5, A5, C6, D6, E6)
            val notes = doubleArrayOf(
                523.25 * pitchMult,
                659.25 * pitchMult,
                783.99 * pitchMult,
                880.00 * pitchMult,
                1046.50 * pitchMult,
                1174.66 * pitchMult,
                1318.51 * pitchMult
            )

            var lpNoise = 0f
            var prevLp = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                var mix = 0f

                // 1. Rapid Bubble-Wrap & Paper Confetti Pop burst (0..140ms)
                if (t < 0.14f) {
                    val raw = Random.nextFloat() * 2f - 1f
                    lpNoise = lpNoise * 0.66f + raw * 0.34f
                    val ripBand = lpNoise - prevLp
                    prevLp = lpNoise * 0.84f
                    val popGrain = (0.55f + 0.45f * sin(2.0 * PI * 165.0 * t).toFloat())
                    val ripEnv = sin((t / 0.14f) * PI).toFloat() * exp(-10.0 * t).toFloat()
                    mix += ripBand * popGrain * ripEnv * 0.34f
                }

                // 2. Playful Upward Slide-Whistle "Whoop!" on x2+ Combos!
                if (comboStep >= 2 && t < 0.24f) {
                    val whistleNorm = (t / 0.24f).coerceIn(0f, 1f)
                    val whistleFreq = (440.0 + 720.0 * whistleNorm.pow(1.5f)) * pitchMult.coerceAtMost(1.8)
                    val whistleEnv = sin(whistleNorm * PI).toFloat() * 0.22f
                    mix += sin(2.0 * PI * whistleFreq * t).toFloat() * whistleEnv
                }

                // 3. Sparkling Toy-Marimba + Glockenspiel Arpeggio
                val activeNoteCount = (4 + comboStep).coerceIn(5, notes.size)
                for (idx in 0 until activeNoteCount) {
                    val start = idx * 0.028f
                    if (t >= start) {
                        val lt = t - start
                        val noteAttack = (lt / 0.002f).coerceIn(0f, 1f)
                        // Playful pitch-scoop on each arpeggio note
                        val freq = notes[idx] * (1.0 - 0.025 * exp(-70.0 * lt))
                        val envMain = exp(-7.2 * lt).toFloat()
                        val envTube = exp(-18.0 * lt).toFloat()
                        val envSparkle = exp(-30.0 * lt).toFloat()

                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.52 * envMain +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.24 * envTube +
                            sin(2.0 * PI * (freq * 3.98) * lt) * 0.18 * envSparkle
                        ).toFloat() * noteAttack

                        mix += voice * 0.30f
                    }
                }

                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.70f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Cheeky Cartoon "Rubber-Band Twang / Jaw-Harp Boing-Wobble" for invalid swaps.
     */
    fun playInvalidShake() {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.26f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val attack = (t / 0.0025f).coerceIn(0f, 1f)

                // Cheeky two-step cartoon "Boioing-Bonk!" with pitch-wobble vibrato
                val isSecond = t >= 0.105f
                val lt = if (isSecond) t - 0.105f else t
                val baseFreq = if (!isSecond) 349.23 else 246.94 // F4 -> B3 comical tritone/fourth drop
                val wobble = 1.0 + 0.085 * sin(2.0 * PI * 34.0 * lt) * exp(-8.0 * lt)
                val freq = baseFreq * wobble

                val env = exp(-16.0 * lt).toFloat()
                val twangEnv = exp(-34.0 * lt).toFloat()

                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.74f + raw * 0.26f
                val crinkle = (raw - lpNoise) * exp(-42.0 * lt).toFloat() * 0.20f

                val boingVoice = (
                    sin(2.0 * PI * freq * lt).toFloat() * env * 0.62f +
                    sin(2.0 * PI * (freq * 2.0) * lt).toFloat() * twangEnv * 0.28f +
                    sin(2.0 * PI * (freq * 3.0) * lt).toFloat() * twangEnv * 0.16f
                ) * attack

                samples[i] = (softClip(boingVoice + crinkle) * Short.MAX_VALUE * 0.58f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Playful "Slide-Whistle Zip + Origami Wing Flap (`Fwoop-Tink!`)" when swapping tiles.
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
                val attack = (t / 0.0025f).coerceIn(0f, 1f)

                // 1. Crisp Paper Wing Flap
                val raw = Random.nextFloat() * 2f - 1f
                val filterCoeff = (0.84f - 0.30f * sin(PI * norm).toFloat()).coerceIn(0.45f, 0.88f)
                lpNoise = lpNoise * filterCoeff + raw * (1f - filterCoeff)
                val wingFlap = (lpNoise - prevLp) * sin(PI * norm).toFloat() * 0.34f
                prevLp = lpNoise * 0.85f

                // 2. Playful Slide-Whistle Pitch Arc (523Hz -> 880Hz)
                val slideFreq = 523.25 + 390.0 * norm.pow(1.4f)
                val whistleEnv = sin(PI * norm).toFloat() * exp(-6.0 * t).toFloat()
                val slideWhistle = (
                    sin(2.0 * PI * slideFreq * t) * 0.54 +
                    sin(2.0 * PI * (slideFreq * 2.0) * t) * 0.20
                ).toFloat() * whistleEnv

                val sample = softClip((wingFlap + slideWhistle) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.60f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Bouncy "Bubble-Pop + Wooden Toy-Xylophone Staircase" as newly dropped tiles land!
     */
    fun playDropBounce(tileCount: Int = 3, comboStep: Int = 1) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.35f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val hits = tileCount.coerceIn(2, 6)
            val dropNotes = doubleArrayOf(392.00, 440.00, 523.25, 587.33, 659.25, 783.99)
            val semitoneShift = (comboStep - 1).coerceIn(0, 10) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)
            val comboBoost = (comboStep - 1).coerceIn(0, 5)
            val stepDelay = (0.024f - comboBoost * 0.002f).coerceAtLeast(0.015f)

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
                        val attack = (lt / 0.002f).coerceIn(0f, 1f)
                        val baseNote = dropNotes[h % dropNotes.size] * pitchMult

                        // Bouncy rubber-ball thump + upward bubble pop
                        val bounceFreq = 95.0 + (120.0 + comboBoost * 15.0) * exp(-45.0 * lt)
                        val bounceEnv = exp(-24.0 * lt).toFloat()
                        val rubberBounce = sin(2.0 * PI * bounceFreq * lt).toFloat() * bounceEnv * 0.36f

                        val slapEnv = exp(-75.0 * lt).toFloat()
                        val paperSlap = (raw - lpNoise) * slapEnv * 0.16f

                        // Toy xylophone note with cheeky upward pitch bend
                        val bend = 1.0 - 0.03 * exp(-60.0 * lt)
                        val xyloEnv = exp(-18.0 * lt).toFloat()
                        val tubeEnv = exp(-34.0 * lt).toFloat()
                        val sparkle = if (comboStep >= 2) {
                            sin(2.0 * PI * (baseNote * 3.98) * lt).toFloat() *
                                exp(-40.0 * lt).toFloat() * (0.24f + comboBoost * 0.06f)
                        } else 0f

                        val xylo = (
                            sin(2.0 * PI * (baseNote * bend) * lt).toFloat() * xyloEnv * 0.60f +
                            sin(2.0 * PI * (baseNote * 2.0) * lt).toFloat() * tubeEnv * 0.24f +
                            sparkle
                        )

                        acc += (rubberBounce + paperSlap + xylo) * attack * 0.38f
                    }
                }
                val value = softClip(acc)
                samples[i] = (value * Short.MAX_VALUE * 0.68f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    /**
     * Joyful "Origami Carnival Fanfare": Toy-Trumpet & Glockenspiel Triad Arpeggio +
     * Party-Blower Trill + Confetti Popper Burst!
     */
    fun playFeverFanfare() {
        if (!isEnabled) return
        feverIntensity = 1
        scope.launch {
            val durationSec = 0.76f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            // C Major -> G Major -> High C6 Celebration Fanfare!
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 987.77, 1174.66, 1567.98)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.74f + raw * 0.26f
                var mix = 0f

                // Confetti cannon pop at t=0 and t=0.18s
                if (t < 0.09f) {
                    val popEnv = exp(-32.0 * t).toFloat()
                    mix += (raw - lpNoise) * popEnv * 0.28f
                }

                for (idx in notes.indices) {
                    val start = idx * 0.044f
                    if (t >= start) {
                        val lt = t - start
                        val attack = (lt / 0.0025f).coerceIn(0f, 1f)
                        val vibrato = if (idx == notes.lastIndex) 1.0 + 0.012 * sin(2.0 * PI * 9.0 * lt) else 1.0
                        val freq = notes[idx] * vibrato * (1.0 - 0.02 * exp(-55.0 * lt))
                        val decayRate = if (idx == notes.lastIndex) 3.4 else 6.0
                        val env = exp(-decayRate * lt).toFloat()
                        val tineEnv = exp(-14.0 * lt).toFloat()

                        val voice = (
                            sin(2.0 * PI * freq * lt) * 0.54 * env +
                            sin(2.0 * PI * (freq * 2.0) * lt) * 0.26 * env +
                            sin(2.0 * PI * (freq * 3.0) * lt) * 0.16 * tineEnv +
                            sin(2.0 * PI * (freq * 3.98) * lt) * 0.14 * tineEnv
                        ).toFloat() * attack
                        mix += voice * 0.28f
                    }
                }
                val sample = softClip(mix)
                samples[i] = (sample * Short.MAX_VALUE * 0.70f).toInt().toShort()
            }
            enqueueVoice(samples)
        }
    }

    private fun softClip(x: Float): Float {
        val clamped = x.coerceIn(-1.5f, 1.5f)
        return (clamped - (clamped * clamped * clamped) / 6.75f).coerceIn(-1f, 1f)
    }
}
