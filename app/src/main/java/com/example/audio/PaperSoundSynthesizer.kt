package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Bespoke "Origami Craft-Table" Acoustic & Foley Synthesizer + Interactive BGM Engine.
 *
 * Replaces generic synth tones with authentic papercraft foley (torn washi paper fibers,
 * crisp origami crease snaps, corrugated cardboard box cavity resonance, steel scissor shears)
 * layered with traditional Japanese & craft-room acoustic instruments (Plucked Silk Koto,
 * Bamboo Marimba, Music-Box Kalimba tines, and Furin wind-chimes), plus a dynamic
 * stage-reactive Origami Workshop background music loop.
 */
class PaperSoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

    var isEnabled: Boolean = true
        set(value) {
            field = value
            if (value) {
                startWorkshopMusicLoop()
            } else {
                stopWorkshopMusicLoop()
            }
        }

    @Volatile
    private var currentStageIndex: Int = 1

    @Volatile
    private var feverIntensity: Int = 0

    private var bgmJob: Job? = null

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
        startWorkshopMusicLoop()
    }

    /**
     * Updates the musical mood of the Origami Workshop BGM loop to match the current stage
     * and active Fever / Cascade state.
     */
    fun updateMusicalMood(stageNumber: Int, isFeverOrHighCombo: Boolean) {
        currentStageIndex = stageNumber.coerceIn(1, 10)
        feverIntensity = if (isFeverOrHighCombo) 1 else 0
        if (isEnabled && (bgmJob == null || bgmJob?.isActive != true)) {
            startWorkshopMusicLoop()
        }
    }

    /**
     * Starts the procedural "Origami Workshop" Background Music sequencer:
     * Plucked Koto + Wooden Kalimba Music-Box + Washi Paper-Brush Shaker in Yo Pentatonic.
     */
    fun startWorkshopMusicLoop() {
        if (!isEnabled) return
        if (bgmJob?.isActive == true) return
        bgmJob = scope.launch {
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
            var step = 0
            while (isActive && isEnabled) {
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

                val melIdx = melodyPattern[step % melodyPattern.size]
                val bassIdx = bassPattern[step % bassPattern.size]
                val counterIdx = counterPattern[step % counterPattern.size]

                val melFreq = yoPentatonic[melIdx] * pitchShift
                val bassFreq = if (bassIdx >= 0) yoPentatonic[bassIdx] * pitchShift * 0.5 else 0.0
                val counterFreq = if (isExcited && counterIdx >= 0) yoPentatonic[counterIdx] * pitchShift else 0.0
                val hasPaperShaker = (step % 2 == 1)

                val phraseSamples = synthesizeBgmStep(
                    durationSec = stepDurationSec,
                    melodyFreq = melFreq,
                    bassFreq = bassFreq,
                    counterFreq = counterFreq,
                    paperBrush = hasPaperShaker,
                    accent = (step % 4 == 0)
                )
                playPcmBuffer(phraseSamples, stepDurationSec)
                step = (step + 1) % 16
                delay((stepDurationSec * 960L).toLong())
            }
        }
    }

    fun stopWorkshopMusicLoop() {
        bgmJob?.cancel()
        bgmJob = null
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
            // Subtle koto pitch grace bend on accented beats
            val kotoBend = if (accent) 1.0 + 0.018 * exp(-35.0 * t) else 1.0
            val fMel = melodyFreq * kotoBend
            val melEnv = exp(-8.5 * t).toFloat()
            val harmEnv = exp(-22.0 * t).toFloat()
            val tineEnv = exp(-45.0 * t).toFloat()

            // Koto string harmonics + Music-box inharmonic tine (2.98x)
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
     * Combines micro-impulse paper fiber folding foley with a resonant wood-cavity kalimba tine.
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

                // 1. Tactile Origami Paper Fold Crease Foley (3 micro-crackle fiber impulses)
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.72f + raw * 0.28f
                val paperBand = lpNoise - prevLp
                prevLp = lpNoise * 0.88f

                val creaseBurst1 = exp(-120.0 * t).toFloat()
                val creaseBurst2 = if (t >= 0.014f) exp(-95.0 * (t - 0.014f)).toFloat() * 0.75f else 0f
                val creaseBurst3 = if (t >= 0.032f) exp(-75.0 * (t - 0.032f)).toFloat() * 0.50f else 0f
                val paperCreaseFoley = paperBand * (creaseBurst1 + creaseBurst2 + creaseBurst3) * 0.42f

                // 2. Hollow Corrugated Box Wood-Cavity Knock (310Hz -> 185Hz)
                val knockFreq = 185.0 + 125.0 * exp(-90.0 * t)
                val knockEnv = exp(-48.0 * t).toFloat()
                val woodKnock = sin(2.0 * PI * knockFreq * t).toFloat() * knockEnv * 0.28f

                // 3. Plucked Bamboo Kalimba Tine (Fundamental + 2.92x Metallic Tine Partial)
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
            playPcmBuffer(samples, durationSec)
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

                // Twin scissor blade shears at t=0s and t=0.082s
                val snip1Env = exp(-34.0 * t).toFloat()
                val snip2Env = if (t >= 0.082f) exp(-30.0 * (t - 0.082f)).toFloat() else 0f

                // High-frequency steel blade friction noise (bandpassed high)
                val raw = Random.nextFloat() * 2f - 1f
                lp1 = lp1 * 0.45f + raw * 0.55f
                lp2 = lp2 * 0.82f + lp1 * 0.18f
                val highSteelNoise = (lp1 - lp2) * (snip1Env + snip2Env * 1.1f) * 0.52f

                // Metallic blade ring (inharmonic steel partials: 1480Hz + 3890Hz, then 1975Hz + 5120Hz)
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
            playPcmBuffer(samples, durationSec)
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

                // 1. Viscous glue-stick suction glide (280Hz -> 820Hz with formant modulation)
                val glideFreq = 280.0 + 540.0 * (t / 0.14f).coerceIn(0f, 1f).pow(1.6f)
                val squishEnv = exp(-12.0 * t).toFloat()
                val squishMod = 1.0f + 0.35f * sin(2.0 * PI * 38.0 * t).toFloat()
                val glueSquish = (
                    sin(2.0 * PI * glideFreq * t) * 0.62 +
                    sin(2.0 * PI * (glideFreq * 1.85) * t) * 0.28
                ).toFloat() * squishEnv * squishMod

                // 2. Soft paper-press thud + sticky peel noise at t=0.095s
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
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Corrugated Cardboard Carton Stomp + Kraft-Paper Rip + Taiko/Koto Power Chord
     * for the Wooden Mallet Smash and Special Paper Cutter Blasts.
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

                // 1. Deep Hollow Corrugated Box Lid Thud (175Hz -> 48Hz) + Box Cavity Resonance (142Hz)
                val kickPhase = 2.0 * PI * (48.0 * t + (127.0 / 24.0) * (1.0 - exp(-24.0 * t)))
                val kickEnv = exp(-10.5 * t).toFloat()
                val boxCavity = sin(2.0 * PI * 142.0 * t).toFloat() * exp(-22.0 * t).toFloat() * 0.32f
                val cardboardStomp = sin(kickPhase).toFloat() * kickEnv * 0.68f + boxCavity

                // 2. Bursting Kraft-Paper Rip Foley (staggered tear impulses)
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.70f + raw * 0.30f
                val tearBand = lpNoise - prevLp
                prevLp = lpNoise * 0.86f
                val tearGate = (0.65f + 0.35f * sin(2.0 * PI * 95.0 * t).toFloat()) * exp(-14.0 * t).toFloat()
                val paperRip = tearBand * tearGate * 0.42f

                // 3. Resonant Japanese Yo Fifth + Octave Koto Chord (D5 + A5 + D6)
                val chordEnv = exp(-10.0 * t).toFloat()
                val kotoChord = (
                    sin(2.0 * PI * 587.33 * t) * 0.24 +
                    sin(2.0 * PI * 880.00 * t) * 0.22 +
                    sin(2.0 * PI * 1174.66 * t) * 0.18
                ).toFloat() * chordEnv

                val sample = softClip((cardboardStomp + paperRip + kotoChord) * attack)
                samples[i] = (sample * Short.MAX_VALUE * 0.72f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Authentic Torn Washi Paper Rip Foley + Ascending Plucked Koto & Music-Box Arpeggio
     * in the Japanese Yo Pentatonic scale. Escalates in pitch and shimmer with each cascade tier!
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

            // Climb up the Yo scale with every cascade step (x1, x2, x3, x4, x5+)
            val semitoneShift = (comboStep - 1).coerceIn(0, 12) * 2
            val pitchMult = 2.0.pow(semitoneShift / 12.0)

            // Yo Pentatonic Koto Arpeggio: D5, E5, G5, A5, B5, D6
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

                // 1. Tactile Torn Washi Paper Rip Foley during the first 120ms of the crush
                if (t < 0.13f) {
                    val raw = Random.nextFloat() * 2f - 1f
                    lpNoise = lpNoise * 0.68f + raw * 0.32f
                    val ripBand = lpNoise - prevLp
                    prevLp = lpNoise * 0.85f
                    // Rapid fiber-tearing grain modulation (140Hz grain)
                    val fiberGrain = (0.55f + 0.45f * sin(2.0 * PI * 145.0 * t).toFloat())
                    val ripEnv = sin((t / 0.13f) * PI).toFloat() * exp(-12.0 * t).toFloat()
                    mix += ripBand * fiberGrain * ripEnv * 0.36f
                }

                // 2. Strumming 6-String Silk Koto & Music-Box Tines
                for (idx in notes.indices) {
                    val start = idx * 0.032f
                    if (t >= start) {
                        val lt = t - start
                        val noteAttack = (lt / 0.0025f).coerceIn(0f, 1f)
                        // Subtle koto pluck pitch-transient
                        val freq = notes[idx] * (1.0 + 0.015 * exp(-65.0 * lt))
                        val envMain = exp(-7.5 * lt).toFloat()
                        val envHarm = exp(-17.0 * lt).toFloat()
                        val envTine = exp(-32.0 * lt).toFloat()

                        // Plucked Koto string (1x, 2x, 3x) + Music-box metallic tine (2.96x)
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
            playPcmBuffer(samples, durationSec)
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
                val freq = if (!isSecond) 392.00 else 293.66 // G4 -> D4 hollow bamboo tok
                val attack = (lt / 0.0025f).coerceIn(0f, 1f)
                val env = exp(-28.0 * lt).toFloat()
                val tineEnv = exp(-55.0 * lt).toFloat()

                // Dry paper crinkle rustle
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.74f + raw * 0.26f
                val crinkle = (raw - lpNoise) * exp(-40.0 * lt).toFloat() * 0.22f

                // Hollow bamboo woodblock (fundamental + 2.42x hollow cavity partial)
                val woodblock = (
                    sin(2.0 * PI * freq * lt).toFloat() * env * 0.72f +
                    sin(2.0 * PI * (freq * 2.42) * lt).toFloat() * tineEnv * 0.28f
                ) * attack

                samples[i] = (softClip(woodblock + crinkle) * Short.MAX_VALUE * 0.54f).toInt().toShort()
            }
            playPcmBuffer(samples, durationSec)
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

                // 1. Textured construction-paper coaster sliding friction + mid-arc fold snap
                val raw = Random.nextFloat() * 2f - 1f
                val filterCoeff = (0.86f - 0.28f * sin(PI * norm).toFloat()).coerceIn(0.45f, 0.90f)
                lpNoise = lpNoise * filterCoeff + raw * (1f - filterCoeff)
                val slideFriction = (lpNoise - prevLp) * sin(PI * norm).toFloat() * 0.38f
                prevLp = lpNoise * 0.85f

                // Crisp fold snap when the two coasters cross at mid-swap (t = 0.065s)
                val foldSnap = if (abs(t - 0.065f) < 0.025f) {
                    val dt = abs(t - 0.065f)
                    (raw - lpNoise) * exp(-110.0 * dt).toFloat() * 0.28f
                } else 0f

                // 2. Two-note Bamboo & Koto fifth interval (D5 587.33Hz -> A5 880.0Hz)
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
            playPcmBuffer(samples, durationSec)
        }
    }

    /**
     * Corrugated Cardboard Box-Lid Landing Thumps + Paper-Edge Flutter + Ascending Bamboo Marimba
     * & Koto Plucks in the Japanese Yo Pentatonic Scale!
     * Escalates in pitch, cardboard slam punch, and Furin wind-chime shimmer on x2, x3, x4+ cascades.
     */
    fun playDropBounce(tileCount: Int = 3, comboStep: Int = 1) {
        if (!isEnabled) return
        scope.launch {
            val durationSec = 0.34f
            val numSamples = (sampleRate * durationSec).toInt()
            val samples = ShortArray(numSamples)
            val hits = tileCount.coerceIn(2, 6)
            // Yo Pentatonic Bamboo Marimba notes: D4, E4, G4, A4, B4, D5
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

                        // 1. Hollow Corrugated Cardboard Box Cavity Thump (138Hz -> 64Hz)
                        val boxFreq = 64.0 + (74.0 + comboBoost * 12.0) * exp(-65.0 * lt)
                        val boxEnv = exp(-28.0 * lt).toFloat()
                        val cardboardThump = sin(2.0 * PI * boxFreq * lt).toFloat() * boxEnv * (0.38f + comboBoost * 0.04f)

                        // 2. Crisp Paper-Coaster Edge Slap Transient
                        val slapEnv = exp(-75.0 * lt).toFloat()
                        val paperSlap = (raw - lpNoise) * slapEnv * (0.18f + comboBoost * 0.03f)

                        // 3. Resonant Bamboo Marimba + Koto Tine (Fundamental + 2x + 2.95x Tine)
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
            playPcmBuffer(samples, durationSec)
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
            // Yo Pentatonic Royal Koto Glissando: D5 - E5 - G5 - A5 - B5 - D6 - G6
            val notes = doubleArrayOf(587.33, 659.25, 783.99, 880.00, 987.77, 1174.66, 1567.98)
            var lpNoise = 0f

            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val raw = Random.nextFloat() * 2f - 1f
                lpNoise = lpNoise * 0.75f + raw * 0.25f
                var mix = 0f

                // Initial celebratory paper-popper confetti burst
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
