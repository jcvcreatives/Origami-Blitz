package com.example.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PaperSoundSynthesizer
import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
import com.example.model.BoosterType
import com.example.model.ConfettiDepthLayer
import com.example.model.ConfettiScrap
import com.example.model.FloatingScorePopup
import com.example.model.GameStatus
import com.example.model.GridPos
import com.example.model.PaperGameState
import com.example.model.PieceType
import com.example.model.TOTAL_LEVELS
import com.example.model.TileSpecial
import com.example.model.computeDioramaLandingY
import com.example.model.getLevelBlueprint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class PaperCrushViewModel(
    private val prefs: SharedPreferences? = null,
    private val soundSynth: PaperSoundSynthesizer = PaperSoundSynthesizer()
) : ViewModel() {

    private var nextTileId = 1L
    private var nextScrapId = 1L
    private var nextAnimSeq = 1L
    private var nextPopupId = 1L
    private var nextPunchSeq = 1L
    private var particleLoopJob: Job? = null
    private var idleHintJob: Job? = null

    private val _uiState = MutableStateFlow(PaperGameState())
    val uiState: StateFlow<PaperGameState> = _uiState.asStateFlow()

    private val _confettiScraps = MutableStateFlow<List<ConfettiScrap>>(emptyList())
    val confettiScraps: StateFlow<List<ConfettiScrap>> = _confettiScraps.asStateFlow()

    init {
        loadSavedStateAndStart()
    }

    private fun loadSavedStateAndStart() {
        val savedLevel = prefs?.getInt(KEY_LEVEL, 1)?.coerceIn(1, TOTAL_LEVELS) ?: 1
        val savedHighest = prefs?.getInt(KEY_HIGHEST_LEVEL, savedLevel)?.coerceIn(savedLevel, TOTAL_LEVELS) ?: savedLevel
        val savedHighScore = prefs?.getInt(KEY_HIGH_SCORE, 0) ?: 0
        val savedScissors = prefs?.getInt(KEY_SCISSORS, 3) ?: 3
        val savedGlue = prefs?.getInt(KEY_GLUE, 3) ?: 3
        val savedHammer = prefs?.getInt(KEY_HAMMER, 2) ?: 2
        val savedSound = prefs?.getBoolean(KEY_SOUND, true) ?: true
        val savedStarsRaw = prefs?.getString(KEY_LEVEL_STARS, "") ?: ""
        val parsedStars = parseStarsMap(savedStarsRaw)

        soundSynth.isEnabled = savedSound
        startLevel(
            level = savedLevel,
            highestUnlockedLevel = savedHighest,
            levelStars = parsedStars,
            carryHighScore = savedHighScore,
            boosters = mapOf(
                BoosterType.SCISSORS to savedScissors,
                BoosterType.GLUE to savedGlue,
                BoosterType.HAMMER to savedHammer
            ),
            soundEnabled = savedSound
        )
    }

    fun startLevel(
        level: Int,
        highestUnlockedLevel: Int = maxOf(_uiState.value.highestUnlockedLevel, level).coerceAtMost(TOTAL_LEVELS),
        levelStars: Map<Int, Int> = _uiState.value.levelStars,
        carryHighScore: Int = _uiState.value.highScore,
        boosters: Map<BoosterType, Int> = _uiState.value.boosters,
        soundEnabled: Boolean = _uiState.value.soundEnabled,
        showMap: Boolean = false
    ) {
        idleHintJob?.cancel()
        val clampedLevel = level.coerceIn(1, TOTAL_LEVELS)
        val bp = getLevelBlueprint(clampedLevel)

        val initialBoard = generatePlayableBoard(bp.tapeLocks.keys)
        _uiState.value = PaperGameState(
            level = clampedLevel,
            highestUnlockedLevel = maxOf(highestUnlockedLevel, clampedLevel).coerceAtMost(TOTAL_LEVELS),
            levelStars = levelStars,
            lastEarnedStars = 0,
            showWorldMap = showMap,
            mapSelectedLevel = clampedLevel,
            score = 0,
            highScore = carryHighScore,
            movesRemaining = bp.moves,
            goal = bp.primaryGoal,
            secondaryGoal = bp.secondaryGoal,
            goldCells = bp.goldCells,
            initialGoldCount = bp.goldCells.size,
            tapeLocks = bp.tapeLocks,
            initialTapeCount = bp.tapeLocks.size,
            mechanicLabel = bp.mechanicLabel,
            board = initialBoard,
            boosters = boosters,
            soundEnabled = soundEnabled,
            status = GameStatus.PLAYING
        )
        persistProgress()
        if (!showMap) {
            scheduleIdleHint()
        }
    }

    fun openWorldMap() {
        idleHintJob?.cancel()
        soundSynth.playPaperCrinkle(1)
        _uiState.update {
            it.copy(
                showWorldMap = true,
                mapSelectedLevel = it.level,
                activeBooster = null,
                selectedPos = null,
                glueFirstPos = null,
                hintPair = null
            )
        }
    }

    fun closeWorldMap() {
        soundSynth.playPaperCrinkle(1)
        _uiState.update { it.copy(showWorldMap = false) }
        if (_uiState.value.status == GameStatus.PLAYING) {
            scheduleIdleHint()
        }
    }

    fun selectLevelOnMap(level: Int) {
        val current = _uiState.value
        if (level in 1..current.highestUnlockedLevel) {
            soundSynth.playPaperCrinkle(1)
            _uiState.update { it.copy(mapSelectedLevel = level) }
        } else {
            soundSynth.playInvalidShake()
        }
    }

    fun playLevelFromMap(level: Int) {
        val current = _uiState.value
        if (level !in 1..current.highestUnlockedLevel) {
            soundSynth.playInvalidShake()
            return
        }
        soundSynth.playSwapWhoosh()
        if (level == current.level && current.status == GameStatus.PLAYING && current.score > 0) {
            _uiState.update { it.copy(showWorldMap = false, mapSelectedLevel = level) }
            scheduleIdleHint()
        } else {
            val refreshedBoosters = current.boosters.mapValues { (_, count) -> count.coerceAtLeast(1) }
            startLevel(
                level = level,
                highestUnlockedLevel = current.highestUnlockedLevel,
                levelStars = current.levelStars,
                carryHighScore = current.highScore,
                boosters = refreshedBoosters,
                soundEnabled = current.soundEnabled,
                showMap = false
            )
        }
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        soundSynth.isEnabled = next
        _uiState.update { it.copy(soundEnabled = next) }
        prefs?.edit()?.putBoolean(KEY_SOUND, next)?.apply()
    }

    fun retryOrResetLevel() {
        val current = _uiState.value
        val refreshedBoosters = current.boosters.mapValues { (_, count) -> count.coerceAtLeast(1) }
        startLevel(
            level = current.level,
            highestUnlockedLevel = current.highestUnlockedLevel,
            levelStars = current.levelStars,
            carryHighScore = current.highScore,
            boosters = refreshedBoosters,
            soundEnabled = current.soundEnabled
        )
    }

    fun advanceToNextLevel() {
        val current = _uiState.value
        val nextLevel = if (current.level < TOTAL_LEVELS) current.level + 1 else 1
        val nextHighest = maxOf(current.highestUnlockedLevel, nextLevel).coerceAtMost(TOTAL_LEVELS)
        val rewardedBoosters = current.boosters.toMutableMap().apply {
            put(BoosterType.SCISSORS, (get(BoosterType.SCISSORS) ?: 0) + 1)
            val bonusType = if (nextLevel % 2 == 0) BoosterType.HAMMER else BoosterType.GLUE
            put(bonusType, (get(bonusType) ?: 0) + 1)
        }
        startLevel(
            level = nextLevel,
            highestUnlockedLevel = nextHighest,
            levelStars = current.levelStars,
            carryHighScore = current.highScore,
            boosters = rewardedBoosters,
            soundEnabled = current.soundEnabled
        )
    }

    fun selectBooster(booster: BoosterType) {
        val state = _uiState.value
        if (state.isAnimating || state.status != GameStatus.PLAYING) return
        idleHintJob?.cancel()
        val count = state.boosters[booster] ?: 0
        if (state.activeBooster == booster) {
            _uiState.update {
                it.copy(activeBooster = null, glueFirstPos = null, selectedPos = null, hintPair = null)
            }
            scheduleIdleHint()
            return
        }
        if (count <= 0) return
        soundSynth.playPaperCrinkle(1)
        _uiState.update {
            it.copy(
                activeBooster = booster,
                glueFirstPos = null,
                selectedPos = null,
                hintPair = null
            )
        }
    }

    fun onTileTapped(pos: GridPos) {
        val state = _uiState.value
        if (state.isAnimating || state.status != GameStatus.PLAYING) return
        if (pos.row !in 0 until BOARD_SIZE || pos.col !in 0 until BOARD_SIZE) return
        idleHintJob?.cancel()

        // Handle active booster first (Boosters can target even taped/wax-sealed tiles!)
        when (state.activeBooster) {
            BoosterType.SCISSORS -> {
                executeScissorsCut(pos)
                return
            }
            BoosterType.HAMMER -> {
                executeHammerSmash(pos)
                return
            }
            BoosterType.GLUE -> {
                val first = state.glueFirstPos
                if (first == null) {
                    soundSynth.playPaperCrinkle(1)
                    _uiState.update { it.copy(glueFirstPos = pos, hintPair = null) }
                } else if (first == pos) {
                    _uiState.update { it.copy(glueFirstPos = null) }
                } else {
                    executeGlueSwap(first, pos)
                }
                return
            }
            null -> Unit
        }

        // Check if tapped tile is locked under Masking Tape or a Red Wax Seal
        if ((state.tapeLocks[pos] ?: 0) > 0) {
            pulseLockedTileWarning(pos)
            return
        }

        val currentSelected = state.selectedPos
        val tappedTile = state.board[pos.row][pos.col]

        if (currentSelected == null) {
            soundSynth.playPaperCrinkle(1)
            _uiState.update { it.copy(selectedPos = pos, hintPair = null) }
            scheduleIdleHint()
        } else if (currentSelected == pos) {
            // Double-tapping a selected Special Power-Up Tile activates it right in place!
            if (tappedTile.special != TileSpecial.NONE) {
                triggerSingleSpecialTileInPlace(pos)
            } else {
                _uiState.update { it.copy(selectedPos = null) }
                scheduleIdleHint()
            }
        } else if (currentSelected.isAdjacentTo(pos)) {
            attemptSwap(currentSelected, pos)
        } else {
            soundSynth.playPaperCrinkle(1)
            _uiState.update { it.copy(selectedPos = pos, hintPair = null) }
            scheduleIdleHint()
        }
    }

    fun onTileSwiped(from: GridPos, to: GridPos) {
        val state = _uiState.value
        if (state.isAnimating || state.status != GameStatus.PLAYING) return
        if (state.activeBooster != null) {
            onTileTapped(from)
            return
        }
        if (from.row in 0 until BOARD_SIZE &&
            from.col in 0 until BOARD_SIZE &&
            to.row in 0 until BOARD_SIZE &&
            to.col in 0 until BOARD_SIZE &&
            from.isAdjacentTo(to)
        ) {
            idleHintJob?.cancel()
            if ((state.tapeLocks[from] ?: 0) > 0) {
                pulseLockedTileWarning(from)
                return
            }
            if ((state.tapeLocks[to] ?: 0) > 0) {
                pulseLockedTileWarning(to)
                return
            }
            attemptSwap(from, to)
        }
    }

    private fun pulseLockedTileWarning(pos: GridPos) {
        viewModelScope.launch {
            soundSynth.playInvalidShake()
            val hp = _uiState.value.tapeLocks[pos] ?: 1
            val msg = if (hp >= 2) "WAX SEALED! MATCH ADJACENT TO CRACK!" else "TAPED! MATCH ADJACENT TO FREE!"
            _uiState.update {
                it.copy(
                    selectedPos = null,
                    invalidSwapPair = pos to pos,
                    comboBannerText = msg
                )
            }
            delay(480)
            _uiState.update {
                it.copy(
                    invalidSwapPair = null,
                    comboBannerText = if (it.comboBannerText == msg) null else it.comboBannerText
                )
            }
            scheduleIdleHint()
        }
    }

    private fun triggerSingleSpecialTileInPlace(pos: GridPos) {
        viewModelScope.launch {
            val state = _uiState.value
            val tile = state.board[pos.row][pos.col]
            if (tile.special == TileSpecial.NONE) return@launch

            _uiState.update {
                it.copy(
                    isAnimating = true,
                    selectedPos = null,
                    hintPair = null,
                    movesRemaining = (it.movesRemaining - 1).coerceAtLeast(0),
                    streakCount = it.streakCount + 1,
                    feverMovesLeft = (it.feverMovesLeft - 1).coerceAtLeast(0)
                )
            }

            val blastSet = expandSpecialChain(state.board, setOf(pos))
            val banner = when (tile.special) {
                TileSpecial.ROW_CUTTER -> "ROW CUTTER SLICE!"
                TileSpecial.COL_CUTTER -> "COLUMN CUTTER SLICE!"
                TileSpecial.PAPER_BOMB -> "ORIGAMI BOMB BLAST!"
                TileSpecial.PINWHEEL_RAINBOW -> "RAINBOW PINWHEEL STORM!"
                TileSpecial.NONE -> null
            }
            soundSynth.playScissorsSnip()
            _uiState.update {
                it.copy(
                    comboBannerText = banner,
                    boardPunchSeq = nextPunchSeq++,
                    boardPunchStrength = 0.9f
                )
            }
            crushPositionsAndCascade(blastSet, bonusPerTile = 55, forceDestroyTape = true)
        }
    }

    private fun attemptSwap(posA: GridPos, posB: GridPos) {
        val initialState = _uiState.value
        if ((initialState.tapeLocks[posA] ?: 0) > 0) {
            pulseLockedTileWarning(posA)
            return
        }
        if ((initialState.tapeLocks[posB] ?: 0) > 0) {
            pulseLockedTileWarning(posB)
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isAnimating = true, selectedPos = null, hintPair = null)
            }
            soundSynth.playSwapWhoosh()
            val preBoard = _uiState.value.board
            val tileA = preBoard[posA.row][posA.col]
            val tileB = preBoard[posB.row][posB.col]
            val swappedBoard = swapPositions(preBoard, posA, posB)

            // Check for Special Power-Up Combo Swap (e.g., Rainbow Pinwheel or Two Special Tiles!)
            val isRainbowSwap = tileA.special == TileSpecial.PINWHEEL_RAINBOW || tileB.special == TileSpecial.PINWHEEL_RAINBOW
            val isDoubleSpecialSwap = tileA.special != TileSpecial.NONE && tileB.special != TileSpecial.NONE

            if (isRainbowSwap || isDoubleSpecialSwap) {
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        movesRemaining = (it.movesRemaining - 1).coerceAtLeast(0),
                        streakCount = it.streakCount + 1,
                        feverMovesLeft = (it.feverMovesLeft - 1).coerceAtLeast(0)
                    )
                }
                delay(245)
                val settledSwapBoard = clearTileMotion(swappedBoard)
                _uiState.update { it.copy(board = settledSwapBoard) }

                val specialBlast = mutableSetOf(posA, posB)
                val bannerText: String
                if (isRainbowSwap) {
                    val targetColor = if (tileA.special == TileSpecial.PINWHEEL_RAINBOW) tileB.type else tileA.type
                    for (r in 0 until BOARD_SIZE) {
                        for (c in 0 until BOARD_SIZE) {
                            if (settledSwapBoard[r][c].type == targetColor) {
                                specialBlast.add(GridPos(r, c))
                            }
                        }
                    }
                    soundSynth.playFeverFanfare()
                    bannerText = "RAINBOW PINWHEEL COLOR STORM!"
                } else {
                    // Double special combo: Cross Slice + 3x3 Bomb
                    for (c in 0 until BOARD_SIZE) {
                        specialBlast.add(GridPos(posA.row, c))
                        specialBlast.add(GridPos(posB.row, c))
                    }
                    for (r in 0 until BOARD_SIZE) {
                        specialBlast.add(GridPos(r, posA.col))
                        specialBlast.add(GridPos(r, posB.col))
                    }
                    soundSynth.playCardboardSmash()
                    bannerText = "MEGA ORIGAMI CROSS SLICE!"
                }

                val fullChain = expandSpecialChain(settledSwapBoard, specialBlast)
                _uiState.update {
                    it.copy(
                        comboBannerText = bannerText,
                        boardPunchSeq = nextPunchSeq++,
                        boardPunchStrength = 1.15f
                    )
                }
                crushPositionsAndCascade(fullChain, bonusPerTile = 65, forceDestroyTape = true)
                return@launch
            }

            val matches = findMatchedPositions(swappedBoard)

            if (matches.isEmpty()) {
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        invalidSwapPair = null
                    )
                }
                delay(240)

                soundSynth.playInvalidShake()
                _uiState.update {
                    it.copy(
                        invalidSwapPair = posA to posB,
                        streakCount = 0
                    )
                }
                delay(170)

                soundSynth.playSwapWhoosh()
                val restoredBoard = swapPositions(swappedBoard, posA, posB)
                _uiState.update {
                    it.copy(
                        board = restoredBoard,
                        invalidSwapPair = null
                    )
                }
                delay(230)

                val settledBoard = clearTileMotion(restoredBoard)
                _uiState.update {
                    it.copy(
                        board = settledBoard,
                        isAnimating = false
                    )
                }
                scheduleIdleHint()
            } else {
                soundSynth.playPaperCrinkle(2)
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        movesRemaining = (it.movesRemaining - 1).coerceAtLeast(0),
                        streakCount = it.streakCount + 1,
                        feverMovesLeft = (it.feverMovesLeft - 1).coerceAtLeast(0)
                    )
                }
                delay(245)
                val settledSwapBoard = clearTileMotion(swappedBoard)
                _uiState.update { it.copy(board = settledSwapBoard) }
                resolveCascades(settledSwapBoard, preferredAnchor = posB)
            }
        }
    }

    private fun executeScissorsCut(pos: GridPos) {
        viewModelScope.launch {
            val current = _uiState.value
            val count = current.boosters[BoosterType.SCISSORS] ?: 0
            if (count <= 0) return@launch

            soundSynth.playScissorsSnip()
            val updatedBoosters = current.boosters.toMutableMap().apply {
                put(BoosterType.SCISSORS, count - 1)
            }
            _uiState.update {
                it.copy(
                    isAnimating = true,
                    activeBooster = null,
                    hintPair = null,
                    boosters = updatedBoosters,
                    comboBannerText = "SNIP!",
                    boardPunchSeq = nextPunchSeq++,
                    boardPunchStrength = 0.55f
                )
            }
            val chain = expandSpecialChain(current.board, setOf(pos))
            crushPositionsAndCascade(chain, bonusPerTile = 65, forceDestroyTape = true)
        }
    }

    private fun executeGlueSwap(posA: GridPos, posB: GridPos) {
        viewModelScope.launch {
            val current = _uiState.value
            val count = current.boosters[BoosterType.GLUE] ?: 0
            if (count <= 0) return@launch

            soundSynth.playGlueSwap()
            soundSynth.playSwapWhoosh()
            val updatedBoosters = current.boosters.toMutableMap().apply {
                put(BoosterType.GLUE, count - 1)
            }
            // Glue swap also frees any tape on the two glued positions!
            val updatedTape = current.tapeLocks.toMutableMap().apply {
                remove(posA)
                remove(posB)
            }
            val swappedBoard = swapPositions(current.board, posA, posB)
            val tileA = current.board[posA.row][posA.col]
            val tileB = current.board[posB.row][posB.col]
            spawnConfettiForTiles(listOf(tileA, tileB))

            _uiState.update {
                it.copy(
                    board = swappedBoard,
                    tapeLocks = updatedTape,
                    isAnimating = true,
                    activeBooster = null,
                    glueFirstPos = null,
                    hintPair = null,
                    boosters = updatedBoosters,
                    comboBannerText = "GLUE SWAP!"
                )
            }
            delay(270)
            val settledSwapBoard = clearTileMotion(swappedBoard)
            _uiState.update { it.copy(board = settledSwapBoard) }
            resolveCascades(settledSwapBoard, preferredAnchor = posB)
        }
    }

    private fun executeHammerSmash(center: GridPos) {
        viewModelScope.launch {
            val current = _uiState.value
            val count = current.boosters[BoosterType.HAMMER] ?: 0
            if (count <= 0) return@launch

            soundSynth.playCardboardSmash()
            val updatedBoosters = current.boosters.toMutableMap().apply {
                put(BoosterType.HAMMER, count - 1)
            }
            val smashedPositions = mutableSetOf<GridPos>()
            for (dr in -1..1) {
                for (dc in -1..1) {
                    val r = center.row + dr
                    val c = center.col + dc
                    if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                        smashedPositions.add(GridPos(r, c))
                    }
                }
            }
            val fullChain = expandSpecialChain(current.board, smashedPositions)
            _uiState.update {
                it.copy(
                    isAnimating = true,
                    activeBooster = null,
                    hintPair = null,
                    boosters = updatedBoosters,
                    comboBannerText = "CARDBOARD SMASH!",
                    boardPunchSeq = nextPunchSeq++,
                    boardPunchStrength = 1.15f
                )
            }
            crushPositionsAndCascade(fullChain, bonusPerTile = 55, forceDestroyTape = true)
        }
    }

    private suspend fun crushPositionsAndCascade(
        positions: Set<GridPos>,
        bonusPerTile: Int,
        forceDestroyTape: Boolean = false
    ) {
        val stateBefore = _uiState.value
        val board = stateBefore.board

        val (actualCrushed, nextTapeLocks, tapeDamagedCount) = applyTapeDamage(
            matchedPositions = positions,
            currentTapeLocks = stateBefore.tapeLocks,
            forceDestroy = forceDestroyTape,
            damageAdjacent = !forceDestroyTape
        )

        val nextGoldCells = stateBefore.goldCells - actualCrushed
        val goldCollectedCount = (stateBefore.goldCells.size - nextGoldCells.size).coerceAtLeast(0)

        val crushedTiles = actualCrushed.map { board[it.row][it.col] }
        spawnConfettiForTiles(crushedTiles, scrapsPerTile = 18)

        val markedBoard = board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, tile ->
                if (GridPos(r, c) in actualCrushed) tile.copy(isCrushing = true) else tile
            }
        }
        _uiState.update {
            it.copy(
                goldCells = nextGoldCells,
                tapeLocks = nextTapeLocks
            )
        }
        applyProgressForClearedTiles(
            clearedTiles = crushedTiles,
            comboStep = 1,
            pointsPerTile = bonusPerTile,
            goldCollected = goldCollectedCount,
            tapeCleared = tapeDamagedCount
        )
        _uiState.update { it.copy(board = markedBoard) }
        delay(220)

        val cascadedBoard = dropAndRefillBoard(markedBoard, actualCrushed, emptyMap())
        soundSynth.playDropBounce(actualCrushed.size)
        _uiState.update { it.copy(board = cascadedBoard) }
        delay(340)

        val settledBoard = clearTileMotion(cascadedBoard)
        _uiState.update { it.copy(board = settledBoard) }
        resolveCascades(settledBoard, startCombo = 1)
    }

    /**
     * Expands any matched Special Tiles (Row Cutter, Col Cutter, 3x3 Origami Bomb, Rainbow Pinwheel)
     * recursively so chain-reactions detonate across the board.
     */
    private fun expandSpecialChain(
        board: List<List<BoardTile>>,
        initialMatched: Set<GridPos>
    ): Set<GridPos> {
        val result = initialMatched.toMutableSet()
        val queue = ArrayDeque<GridPos>()
        queue.addAll(initialMatched)
        val processedSpecials = mutableSetOf<GridPos>()

        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            if (pos in processedSpecials) continue
            val tile = board.getOrNull(pos.row)?.getOrNull(pos.col) ?: continue
            if (tile.special == TileSpecial.NONE) continue
            processedSpecials.add(pos)

            when (tile.special) {
                TileSpecial.ROW_CUTTER -> {
                    for (c in 0 until BOARD_SIZE) {
                        val p = GridPos(pos.row, c)
                        if (result.add(p)) queue.add(p)
                    }
                }
                TileSpecial.COL_CUTTER -> {
                    for (r in 0 until BOARD_SIZE) {
                        val p = GridPos(r, pos.col)
                        if (result.add(p)) queue.add(p)
                    }
                }
                TileSpecial.PAPER_BOMB -> {
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = pos.row + dr
                            val nc = pos.col + dc
                            if (nr in 0 until BOARD_SIZE && nc in 0 until BOARD_SIZE) {
                                val p = GridPos(nr, nc)
                                if (result.add(p)) queue.add(p)
                            }
                        }
                    }
                }
                TileSpecial.PINWHEEL_RAINBOW -> {
                    for (r in 0 until BOARD_SIZE) {
                        for (c in 0 until BOARD_SIZE) {
                            if (board[r][c].type == tile.type) {
                                val p = GridPos(r, c)
                                if (result.add(p)) queue.add(p)
                            }
                        }
                    }
                }
                TileSpecial.NONE -> Unit
            }
        }
        return result
    }

    private data class MatchAnalysis(
        val allMatched: Set<GridPos>,
        val spawnedSpecials: Map<GridPos, Pair<PieceType, TileSpecial>>,
        val triggeredLineRip: Boolean,
        val triggeredPaperBomb: Boolean,
        val triggeredColorStorm: Boolean
    )

    private fun analyzeMatchesWithSpecials(
        board: List<List<BoardTile>>,
        preferredAnchor: GridPos? = null
    ): MatchAnalysis {
        if (board.size != BOARD_SIZE) {
            return MatchAnalysis(emptySet(), emptyMap(), false, false, false)
        }
        val baseMatched = mutableSetOf<GridPos>()
        val horizCells = mutableSetOf<GridPos>()
        val vertCells = mutableSetOf<GridPos>()
        val spawnedSpecials = mutableMapOf<GridPos, Pair<PieceType, TileSpecial>>()
        val stormTypes = mutableSetOf<PieceType>()
        var hasLineCutter = false
        var hasBomb = false

        // Horizontal runs
        for (r in 0 until BOARD_SIZE) {
            var c = 0
            while (c < BOARD_SIZE) {
                val type = board[r][c].type
                var end = c + 1
                while (end < BOARD_SIZE && board[r][end].type == type) end++
                val len = end - c
                if (len >= 3) {
                    val runPositions = (c until end).map { GridPos(r, it) }
                    baseMatched.addAll(runPositions)
                    horizCells.addAll(runPositions)
                    val anchor = runPositions.firstOrNull { it == preferredAnchor }
                        ?: runPositions[runPositions.size / 2]
                    if (len == 4) {
                        spawnedSpecials[anchor] = type to TileSpecial.COL_CUTTER
                        hasLineCutter = true
                    } else if (len >= 5) {
                        spawnedSpecials[anchor] = type to TileSpecial.PINWHEEL_RAINBOW
                        stormTypes.add(type)
                    }
                }
                c = end
            }
        }

        // Vertical runs
        for (c in 0 until BOARD_SIZE) {
            var r = 0
            while (r < BOARD_SIZE) {
                val type = board[r][c].type
                var end = r + 1
                while (end < BOARD_SIZE && board[end][c].type == type) end++
                val len = end - r
                if (len >= 3) {
                    val runPositions = (r until end).map { GridPos(it, c) }
                    baseMatched.addAll(runPositions)
                    vertCells.addAll(runPositions)
                    val anchor = runPositions.firstOrNull { it == preferredAnchor }
                        ?: runPositions[runPositions.size / 2]
                    if (len == 4) {
                        spawnedSpecials[anchor] = type to TileSpecial.ROW_CUTTER
                        hasLineCutter = true
                    } else if (len >= 5) {
                        spawnedSpecials[anchor] = type to TileSpecial.PINWHEEL_RAINBOW
                        stormTypes.add(type)
                    }
                }
                r = end
            }
        }

        // Detect L-shape or T-shape intersections (cells belonging to both a horizontal and vertical 3+ run)
        val intersections = horizCells.intersect(vertCells)
        for (inter in intersections) {
            val type = board[inter.row][inter.col].type
            spawnedSpecials[inter] = type to TileSpecial.PAPER_BOMB
            hasBomb = true
        }

        // Expand any existing Special Tiles that were part of baseMatched
        val expanded = expandSpecialChain(board, baseMatched).toMutableSet()
        if (expanded.size > baseMatched.size) {
            hasLineCutter = true
        }

        // Expand 5-in-a-row into full-board Origami Color Storm
        if (stormTypes.isNotEmpty()) {
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (board[r][c].type in stormTypes) {
                        expanded.add(GridPos(r, c))
                    }
                }
            }
        }

        return MatchAnalysis(
            allMatched = expanded,
            spawnedSpecials = spawnedSpecials,
            triggeredLineRip = hasLineCutter,
            triggeredPaperBomb = hasBomb,
            triggeredColorStorm = stormTypes.isNotEmpty()
        )
    }

    /**
     * Applies damage to Masking Tape (1 HP) & Red Wax Seals (2 HP) for direct matches
     * and orthogonally adjacent matches.
     * If a directly matched tile had a 2-HP Red Wax Seal, the seal cracks down to 1-HP Masking Tape
     * and the tile remains on the board until the tape is ripped on the next hit.
     */
    private fun applyTapeDamage(
        matchedPositions: Set<GridPos>,
        currentTapeLocks: Map<GridPos, Int>,
        forceDestroy: Boolean = false,
        damageAdjacent: Boolean = true
    ): Triple<Set<GridPos>, Map<GridPos, Int>, Int> {
        if (currentTapeLocks.isEmpty()) {
            return Triple(matchedPositions, emptyMap(), 0)
        }
        val nextLocks = currentTapeLocks.toMutableMap()
        val actualCrushed = matchedPositions.toMutableSet()
        var damagedCount = 0

        // 1. Direct hits on matched positions
        for (pos in matchedPositions) {
            val hp = nextLocks[pos] ?: 0
            if (hp > 0) {
                damagedCount++
                if (forceDestroy || hp <= 1) {
                    nextLocks.remove(pos)
                } else {
                    // 2 HP Red Wax Seal cracks down to 1 HP Masking Tape; tile survives this wave
                    nextLocks[pos] = hp - 1
                    actualCrushed.remove(pos)
                }
            }
        }

        // 2. Orthogonally adjacent matches tear 1 layer of Wax/Tape off neighboring locked cells!
        if (damageAdjacent) {
            val adjacentLocked = mutableSetOf<GridPos>()
            for (pos in matchedPositions) {
                val neighbors = listOf(
                    GridPos(pos.row - 1, pos.col),
                    GridPos(pos.row + 1, pos.col),
                    GridPos(pos.row, pos.col - 1),
                    GridPos(pos.row, pos.col + 1)
                )
                for (n in neighbors) {
                    if (n !in matchedPositions && (nextLocks[n] ?: 0) > 0) {
                        adjacentLocked.add(n)
                    }
                }
            }
            for (adj in adjacentLocked) {
                val hp = nextLocks[adj] ?: 0
                if (hp > 0) {
                    damagedCount++
                    if (hp <= 1) {
                        nextLocks.remove(adj)
                    } else {
                        nextLocks[adj] = hp - 1
                    }
                }
            }
        }

        return Triple(actualCrushed, nextLocks, damagedCount)
    }

    private suspend fun resolveCascades(
        startingBoard: List<List<BoardTile>>,
        startCombo: Int = 0,
        preferredAnchor: GridPos? = null
    ) {
        var currentBoard = startingBoard
        var combo = startCombo
        var anchor = preferredAnchor

        while (true) {
            val analysis = analyzeMatchesWithSpecials(currentBoard, anchor)
            anchor = null
            val rawMatched = analysis.allMatched
            if (rawMatched.isEmpty()) break

            combo += 1
            soundSynth.playComboChord(combo)

            val preState = _uiState.value
            val (actualCrushed, nextTapeLocks, tapeDamagedCount) = applyTapeDamage(
                matchedPositions = rawMatched,
                currentTapeLocks = preState.tapeLocks,
                forceDestroy = analysis.triggeredColorStorm,
                damageAdjacent = true
            )

            // Collect Gold Washi Stamps on any crushed cell (or special-spawn anchor cell)
            val nextGoldCells = preState.goldCells - rawMatched
            val goldCollectedCount = (preState.goldCells.size - nextGoldCells.size).coerceAtLeast(0)

            val matchedTiles = actualCrushed.map { currentBoard[it.row][it.col] }
            spawnConfettiForTiles(
                tiles = matchedTiles,
                scrapsPerTile = when {
                    analysis.triggeredColorStorm -> 22
                    analysis.triggeredLineRip || analysis.triggeredPaperBomb || combo >= 2 -> 18
                    else -> 15
                }
            )

            if (analysis.triggeredColorStorm) {
                soundSynth.playFeverFanfare()
                _uiState.update { st ->
                    val rewardKey = BoosterType.entries[Random.nextInt(BoosterType.entries.size)]
                    val nextBoosters = st.boosters.toMutableMap().apply {
                        put(rewardKey, (get(rewardKey) ?: 0) + 1)
                    }
                    st.copy(boosters = nextBoosters)
                }
            }

            val matchLengthBonus = when {
                analysis.triggeredColorStorm -> 65
                analysis.triggeredPaperBomb -> 54
                analysis.triggeredLineRip -> 48
                else -> 32
            }
            val banner = when {
                analysis.triggeredColorStorm -> "5-FOLD RAINBOW PINWHEEL! +1 BOOSTER"
                analysis.triggeredPaperBomb -> "L/T FOLD • ORIGAMI BOMB CRAFTED!"
                analysis.triggeredLineRip -> "4-FOLD • LINE CUTTER CRAFTED!"
                goldCollectedCount > 0 -> "GOLD WASHI STAMP COLLECTED! +${goldCollectedCount * 80}"
                tapeDamagedCount > 0 -> "TAPE / WAX SEAL RIPPED!"
                combo >= 3 -> "ORIGAMI FEVER COMBO x$combo!"
                combo == 2 -> "DOUBLE FOLD x2!"
                _uiState.value.streakCount >= 3 -> "HOT STREAK x${_uiState.value.streakCount}!"
                else -> null
            }

            val punchStrength = when {
                analysis.triggeredColorStorm -> 1.25f
                analysis.triggeredPaperBomb || analysis.triggeredLineRip || combo >= 3 -> 0.9f
                combo == 2 -> 0.55f
                else -> 0.28f
            }

            val crushingBoard = currentBoard.mapIndexed { r, row ->
                row.mapIndexed { c, tile ->
                    if (GridPos(r, c) in actualCrushed) tile.copy(isCrushing = true) else tile
                }
            }
            _uiState.update {
                it.copy(
                    goldCells = nextGoldCells,
                    tapeLocks = nextTapeLocks
                )
            }
            applyProgressForClearedTiles(
                clearedTiles = matchedTiles,
                comboStep = combo,
                pointsPerTile = matchLengthBonus,
                goldCollected = goldCollectedCount,
                tapeCleared = tapeDamagedCount
            )
            _uiState.update {
                it.copy(
                    board = crushingBoard,
                    comboCount = combo,
                    comboBannerText = banner,
                    boardPunchSeq = nextPunchSeq++,
                    boardPunchStrength = punchStrength
                )
            }
            delay(225)

            // Filter spawnedSpecials to positions that were actually crushed and aren't locked
            val validSpawnedSpecials = analysis.spawnedSpecials.filterKeys { pos ->
                pos in actualCrushed && (nextTapeLocks[pos] ?: 0) == 0
            }

            val refilledBoard = dropAndRefillBoard(crushingBoard, actualCrushed, validSpawnedSpecials)
            soundSynth.playDropBounce(actualCrushed.size.coerceAtLeast(2))
            _uiState.update { it.copy(board = refilledBoard) }
            delay(345)

            currentBoard = clearTileMotion(refilledBoard)
            _uiState.update { it.copy(board = currentBoard) }
        }

        // Ensure board has at least one valid unlocked move
        val activeLocks = _uiState.value.tapeLocks.keys
        if (!hasValidMove(currentBoard, activeLocks)) {
            currentBoard = generatePlayableBoard(activeLocks)
        }

        val postState = _uiState.value
        val allGoalsDone = postState.areAllObjectivesComplete
        val nextStatus = when {
            allGoalsDone -> GameStatus.WON
            postState.movesRemaining <= 0 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }

        var earnedStars = postState.lastEarnedStars
        var updatedStarsMap = postState.levelStars
        var updatedHighestLevel = postState.highestUnlockedLevel

        if (nextStatus == GameStatus.WON) {
            soundSynth.playFeverFanfare()
            earnedStars = when {
                postState.movesRemaining >= 8 || postState.score >= 2200 -> 3
                postState.movesRemaining >= 4 || postState.score >= 1300 -> 2
                else -> 1
            }
            val prevBest = updatedStarsMap[postState.level] ?: 0
            updatedStarsMap = updatedStarsMap + (postState.level to maxOf(prevBest, earnedStars))
            updatedHighestLevel = maxOf(updatedHighestLevel, (postState.level + 1).coerceAtMost(TOTAL_LEVELS))
        }

        _uiState.update {
            it.copy(
                board = currentBoard,
                isAnimating = false,
                comboCount = 0,
                comboBannerText = null,
                status = nextStatus,
                lastEarnedStars = earnedStars,
                levelStars = updatedStarsMap,
                highestUnlockedLevel = updatedHighestLevel
            )
        }
        persistProgress()
        if (nextStatus == GameStatus.PLAYING) {
            scheduleIdleHint()
        }
    }

    private fun applyProgressForClearedTiles(
        clearedTiles: List<BoardTile>,
        comboStep: Int,
        pointsPerTile: Int,
        goldCollected: Int = 0,
        tapeCleared: Int = 0
    ) {
        if (clearedTiles.isEmpty() && goldCollected == 0 && tapeCleared == 0) return
        val popupId = nextPopupId++

        _uiState.update { state ->
            val primaryHits = clearedTiles.count { it.type == state.goal.targetPiece }
            val secondaryHits = state.secondaryGoal?.let { sec ->
                clearedTiles.count { it.type == sec.targetPiece }
            } ?: 0

            val isFeverActive = state.feverMovesLeft > 0
            val feverMult = if (isFeverActive) 2 else 1
            val mechanicBonus = (goldCollected * 80) + (tapeCleared * 60)
            val gainedScore = ((clearedTiles.size * pointsPerTile * comboStep) + mechanicBonus) * feverMult
            val newScore = state.score + gainedScore
            val newHigh = maxOf(state.highScore, newScore)

            val chargeDelta = (clearedTiles.size * 0.065f) + (if (comboStep >= 2) 0.14f else 0f) + (goldCollected * 0.05f)
            val rawNextCharge = state.feverCharge + chargeDelta
            val triggeredNewFever = rawNextCharge >= 1f
            if (triggeredNewFever) {
                soundSynth.playFeverFanfare()
            }
            val nextCharge = if (triggeredNewFever) 0f else rawNextCharge.coerceIn(0f, 1f)
            val nextFeverMoves = if (triggeredNewFever) {
                maxOf(state.feverMovesLeft, 3)
            } else {
                state.feverMovesLeft
            }

            val avgRow = if (clearedTiles.isNotEmpty()) clearedTiles.map { it.row }.average().toFloat() else 3f
            val avgCol = if (clearedTiles.isNotEmpty()) clearedTiles.map { it.col }.average().toFloat() else 3f
            val popupText = when {
                triggeredNewFever -> "+$gainedScore FEVER 2x!"
                goldCollected > 0 -> "+$gainedScore ★STAMP!"
                tapeCleared > 0 -> "+$gainedScore RIP!"
                isFeverActive -> "+$gainedScore (2x!)"
                comboStep >= 2 -> "+$gainedScore x$comboStep!"
                else -> "+$gainedScore"
            }
            val popupColor = clearedTiles.firstOrNull()?.type?.primaryColor
                ?: state.goal.targetPiece.primaryColor
            val popup = FloatingScorePopup(
                id = popupId,
                row = avgRow,
                col = avgCol,
                text = popupText,
                color = popupColor,
                isBig = isFeverActive || comboStep >= 2 || clearedTiles.size >= 4 || goldCollected > 0
            )

            state.copy(
                score = newScore,
                highScore = newHigh,
                feverCharge = nextCharge,
                feverMovesLeft = nextFeverMoves,
                floatingPopups = (state.floatingPopups + popup).takeLast(6),
                goal = state.goal.copy(collectedCount = state.goal.collectedCount + primaryHits),
                secondaryGoal = state.secondaryGoal?.copy(
                    collectedCount = state.secondaryGoal.collectedCount + secondaryHits
                )
            )
        }

        viewModelScope.launch {
            delay(760)
            _uiState.update { st ->
                st.copy(floatingPopups = st.floatingPopups.filterNot { it.id == popupId })
            }
        }
    }

    fun findMatchedPositions(board: List<List<BoardTile>>): Set<GridPos> {
        if (board.size != BOARD_SIZE) return emptySet()
        val matched = mutableSetOf<GridPos>()

        for (r in 0 until BOARD_SIZE) {
            var c = 0
            while (c < BOARD_SIZE) {
                val type = board[r][c].type
                var end = c + 1
                while (end < BOARD_SIZE && board[r][end].type == type) {
                    end++
                }
                val runLength = end - c
                if (runLength >= 3) {
                    for (k in c until end) matched.add(GridPos(r, k))
                }
                c = end
            }
        }

        for (c in 0 until BOARD_SIZE) {
            var r = 0
            while (r < BOARD_SIZE) {
                val type = board[r][c].type
                var end = r + 1
                while (end < BOARD_SIZE && board[end][c].type == type) {
                    end++
                }
                val runLength = end - r
                if (runLength >= 3) {
                    for (k in r until end) matched.add(GridPos(k, c))
                }
                r = end
            }
        }

        return matched
    }

    private fun dropAndRefillBoard(
        board: List<List<BoardTile>>,
        removed: Set<GridPos>,
        spawnedSpecials: Map<GridPos, Pair<PieceType, TileSpecial>>
    ): List<List<BoardTile>> {
        val newCols = Array(BOARD_SIZE) { ArrayList<BoardTile>(BOARD_SIZE) }

        for (c in 0 until BOARD_SIZE) {
            val survivors = mutableListOf<BoardTile>()
            for (r in (BOARD_SIZE - 1) downTo 0) {
                val pos = GridPos(r, c)
                val spawned = spawnedSpecials[pos]
                if (spawned != null) {
                    // Leave the newly crafted Special Power-Up Tile right on the board!
                    survivors.add(
                        BoardTile(
                            id = nextTileId++,
                            type = spawned.first,
                            row = r,
                            col = c,
                            special = spawned.second,
                            spawnDistance = 0.4f,
                            slideFromRowDelta = -0.4f,
                            animSeq = nextAnimSeq++
                        )
                    )
                } else if (pos !in removed) {
                    survivors.add(board[r][c])
                }
            }
            val missingCount = BOARD_SIZE - survivors.size
            val colResult = ArrayList<BoardTile>(BOARD_SIZE)

            for (i in 0 until missingCount) {
                val targetRow = i
                val fallDistance = (missingCount - i + 0.85f) + (i * 0.45f)
                colResult.add(
                    BoardTile(
                        id = nextTileId++,
                        type = randomPiece(),
                        row = targetRow,
                        col = c,
                        special = TileSpecial.NONE,
                        spawnDistance = fallDistance,
                        slideFromRowDelta = -fallDistance,
                        slideFromColDelta = 0f,
                        swapArcSign = 0f,
                        animSeq = nextAnimSeq++
                    )
                )
            }

            val topToBottomSurvivors = survivors.reversed()
            for (i in topToBottomSurvivors.indices) {
                val oldTile = topToBottomSurvivors[i]
                val targetRow = missingCount + i
                val dropRows = (targetRow - oldTile.row).coerceAtLeast(0).toFloat()
                colResult.add(
                    oldTile.copy(
                        row = targetRow,
                        col = c,
                        spawnDistance = dropRows,
                        slideFromRowDelta = -dropRows,
                        slideFromColDelta = 0f,
                        swapArcSign = 0f,
                        animSeq = if (dropRows > 0f) nextAnimSeq++ else oldTile.animSeq,
                        isCrushing = false
                    )
                )
            }
            newCols[c] = colResult
        }

        return List(BOARD_SIZE) { r ->
            List(BOARD_SIZE) { c ->
                newCols[c][r]
            }
        }
    }

    private fun swapPositions(
        board: List<List<BoardTile>>,
        a: GridPos,
        b: GridPos
    ): List<List<BoardTile>> {
        val tileA = board[a.row][a.col]
        val tileB = board[b.row][b.col]
        val seqA = nextAnimSeq++
        val seqB = nextAnimSeq++
        return board.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                when (GridPos(r, c)) {
                    a -> tileB.copy(
                        row = a.row,
                        col = a.col,
                        spawnDistance = 0f,
                        slideFromRowDelta = (b.row - a.row).toFloat(),
                        slideFromColDelta = (b.col - a.col).toFloat(),
                        swapArcSign = -1f,
                        animSeq = seqA
                    )
                    b -> tileA.copy(
                        row = b.row,
                        col = b.col,
                        spawnDistance = 0f,
                        slideFromRowDelta = (a.row - b.row).toFloat(),
                        slideFromColDelta = (a.col - b.col).toFloat(),
                        swapArcSign = 1f,
                        animSeq = seqB
                    )
                    else -> tile.copy(
                        spawnDistance = 0f,
                        slideFromRowDelta = 0f,
                        slideFromColDelta = 0f,
                        swapArcSign = 0f
                    )
                }
            }
        }
    }

    private fun clearTileMotion(board: List<List<BoardTile>>): List<List<BoardTile>> {
        return board.map { row ->
            row.map { tile ->
                tile.copy(
                    spawnDistance = 0f,
                    slideFromRowDelta = 0f,
                    slideFromColDelta = 0f,
                    swapArcSign = 0f
                )
            }
        }
    }

    private fun scheduleIdleHint() {
        idleHintJob?.cancel()
        idleHintJob = viewModelScope.launch {
            delay(3500)
            val st = _uiState.value
            if (!st.isAnimating && st.status == GameStatus.PLAYING && st.selectedPos == null && st.activeBooster == null) {
                val hint = findFirstValidMove(st.board, st.tapeLocks.keys)
                if (hint != null) {
                    _uiState.update { it.copy(hintPair = hint) }
                }
            }
        }
    }

    private fun findFirstValidMove(
        board: List<List<BoardTile>>,
        lockedPositions: Set<GridPos> = emptySet()
    ): Pair<GridPos, GridPos>? {
        val goalTypes = setOfNotNull(_uiState.value.goal.targetPiece, _uiState.value.secondaryGoal?.targetPiece)
        var fallbackPair: Pair<GridPos, GridPos>? = null
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val here = GridPos(r, c)
                if (here in lockedPositions) continue
                val neighbors = listOfNotNull(
                    if (c + 1 < BOARD_SIZE) GridPos(r, c + 1) else null,
                    if (r + 1 < BOARD_SIZE) GridPos(r + 1, c) else null
                )
                for (next in neighbors) {
                    if (next in lockedPositions) continue
                    val tA = board[here.row][here.col]
                    val tB = board[next.row][next.col]
                    if (tA.special == TileSpecial.PINWHEEL_RAINBOW || tB.special == TileSpecial.PINWHEEL_RAINBOW ||
                        (tA.special != TileSpecial.NONE && tB.special != TileSpecial.NONE)
                    ) {
                        return here to next
                    }
                    val swapped = swapPositions(board, here, next)
                    val matches = findMatchedPositions(swapped)
                    if (matches.isNotEmpty()) {
                        val hitsGoal = matches.any { swapped[it.row][it.col].type in goalTypes }
                        if (hitsGoal) return here to next
                        if (fallbackPair == null) fallbackPair = here to next
                    }
                }
            }
        }
        return fallbackPair
    }

    private fun generatePlayableBoard(lockedPositions: Set<GridPos> = emptySet()): List<List<BoardTile>> {
        var attempts = 0
        while (true) {
            attempts++
            val rows = ArrayList<ArrayList<BoardTile>>(BOARD_SIZE)
            for (r in 0 until BOARD_SIZE) {
                val rowList = ArrayList<BoardTile>(BOARD_SIZE)
                for (c in 0 until BOARD_SIZE) {
                    val forbidden = mutableSetOf<PieceType>()
                    if (c >= 2 && rowList[c - 1].type == rowList[c - 2].type) {
                        forbidden.add(rowList[c - 1].type)
                    }
                    if (r >= 2 && rows[r - 1][c].type == rows[r - 2][c].type) {
                        forbidden.add(rows[r - 1][c].type)
                    }
                    val candidates = PieceType.entries.filter { it !in forbidden }
                    val chosen = candidates[Random.nextInt(candidates.size)]
                    rowList.add(
                        BoardTile(
                            id = nextTileId++,
                            type = chosen,
                            row = r,
                            col = c
                        )
                    )
                }
                rows.add(rowList)
            }
            val effectiveLocks = if (attempts > 25) emptySet() else lockedPositions
            if (hasValidMove(rows, effectiveLocks)) return rows
        }
    }

    private fun hasValidMove(
        board: List<List<BoardTile>>,
        lockedPositions: Set<GridPos> = emptySet()
    ): Boolean {
        return findFirstValidMove(board, lockedPositions) != null
    }

    private fun spawnConfettiForTiles(
        tiles: List<BoardTile>,
        scrapsPerTile: Int = 15
    ) {
        val created = ArrayList<ConfettiScrap>(tiles.size * scrapsPerTile)
        val layers = ConfettiDepthLayer.entries
        tiles.forEach { tile ->
            val centerX = (tile.col + 0.5f) / BOARD_SIZE
            val centerY = 0.25f + ((tile.row + 0.5f) / BOARD_SIZE) * 0.50f
            repeat(scrapsPerTile) { idx ->
                val depthLayer = layers[idx % layers.size]
                val angle = Random.nextFloat() * (Math.PI * 2f).toFloat()
                val speed = (0.0045f + Random.nextFloat() * 0.013f) * (0.85f + depthLayer.targetScale * 0.22f)
                created.add(
                    ConfettiScrap(
                        id = nextScrapId++,
                        x = (centerX + (Random.nextFloat() - 0.5f) * 0.05f).coerceIn(0.03f, 0.97f),
                        y = centerY + (Random.nextFloat() - 0.5f) * 0.05f,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed - 0.0085f,
                        rotation = Random.nextFloat() * 360f,
                        vRotation = (Random.nextFloat() - 0.5f) * 22f,
                        widthDp = 7.5f + Random.nextFloat() * 6.5f,
                        heightDp = 5.5f + Random.nextFloat() * 5.5f,
                        color = if (idx % 2 == 0) tile.type.primaryColor else tile.type.secondaryColor,
                        foldColor = tile.type.secondaryColor,
                        depthLayer = depthLayer,
                        depthScale = 1f,
                        landingBandOffset = 0.08f + Random.nextFloat() * 0.84f,
                        flutterPhase = Random.nextFloat() * (Math.PI * 2f).toFloat(),
                        vFlutter = 0.16f + Random.nextFloat() * 0.16f,
                        bounceCount = 0,
                        isLanded = false
                    )
                )
            }
        }
        _confettiScraps.update { (it + created).takeLast(260) }
        ensureParticleLoopRunning()
    }

    private fun ensureParticleLoopRunning() {
        if (particleLoopJob?.isActive == true) return
        particleLoopJob = viewModelScope.launch {
            var lastTimeNs = System.nanoTime()
            while (_confettiScraps.value.isNotEmpty()) {
                delay(16)
                val nowNs = System.nanoTime()
                val dtScale = ((nowNs - lastTimeNs) / 16_666_667f).coerceIn(0.5f, 2.2f)
                lastTimeNs = nowNs
                _confettiScraps.update { current ->
                    current.mapNotNull { s ->
                        if (s.isLanded) {
                            val nextAlpha = s.alpha - (0.0062f * dtScale)
                            if (nextAlpha <= 0f) {
                                null
                            } else {
                                s.copy(alpha = nextAlpha)
                            }
                        } else {
                            val nextAlpha = s.alpha - (0.014f * dtScale)
                            if (nextAlpha <= 0f || s.y > 1.12f) {
                                null
                            } else {
                                val nextFlutter = s.flutterPhase + s.vFlutter * dtScale
                                val flutterSway = kotlin.math.sin(nextFlutter) * 0.00042f * dtScale
                                val nextVx = s.vx * (1f - 0.022f * dtScale) + flutterSway
                                val nextVy = s.vy + (0.00084f * s.depthLayer.gravityScale * dtScale)
                                val rawX = (s.x + nextVx * dtScale).coerceIn(0.02f, 0.98f)
                                val rawY = s.y + nextVy * dtScale
                                val nextScale = s.depthScale + (s.depthLayer.targetScale - s.depthScale) * (0.08f * dtScale).coerceIn(0f, 1f)

                                val contourY = computeDioramaLandingY(rawX, s.depthLayer, s.landingBandOffset)
                                if (rawY >= contourY && nextVy > 0f) {
                                    if (s.bounceCount == 0 && nextVy > 0.0032f) {
                                        s.copy(
                                            x = rawX,
                                            y = contourY,
                                            vx = nextVx * 0.52f,
                                            vy = -nextVy * 0.28f,
                                            rotation = s.rotation + s.vRotation * dtScale,
                                            vRotation = s.vRotation * 0.45f,
                                            depthScale = s.depthLayer.targetScale,
                                            flutterPhase = nextFlutter,
                                            bounceCount = 1,
                                            alpha = nextAlpha
                                        )
                                    } else {
                                        s.copy(
                                            x = rawX,
                                            y = contourY,
                                            vx = 0f,
                                            vy = 0f,
                                            vRotation = 0f,
                                            depthScale = s.depthLayer.targetScale,
                                            isLanded = true,
                                            alpha = nextAlpha.coerceAtLeast(0.82f)
                                        )
                                    }
                                } else {
                                    s.copy(
                                        x = rawX,
                                        y = rawY,
                                        vx = nextVx,
                                        vy = nextVy,
                                        rotation = s.rotation + s.vRotation * dtScale,
                                        depthScale = nextScale,
                                        flutterPhase = nextFlutter,
                                        alpha = nextAlpha
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun randomPiece(): PieceType {
        val entries = PieceType.entries
        return entries[Random.nextInt(entries.size)]
    }

    private fun parseStarsMap(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<Int, Int>()
        raw.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val lvl = parts[0].toIntOrNull()
                val stars = parts[1].toIntOrNull()
                if (lvl != null && stars != null) {
                    result[lvl] = stars.coerceIn(1, 3)
                }
            }
        }
        return result
    }

    private fun serializeStarsMap(map: Map<Int, Int>): String {
        return map.entries.joinToString(",") { "${it.key}:${it.value}" }
    }

    private fun persistProgress() {
        val state = _uiState.value
        prefs?.edit()?.apply {
            putInt(KEY_LEVEL, state.level)
            putInt(KEY_HIGHEST_LEVEL, state.highestUnlockedLevel)
            putString(KEY_LEVEL_STARS, serializeStarsMap(state.levelStars))
            putInt(KEY_HIGH_SCORE, state.highScore)
            putInt(KEY_SCISSORS, state.boosters[BoosterType.SCISSORS] ?: 0)
            putInt(KEY_GLUE, state.boosters[BoosterType.GLUE] ?: 0)
            putInt(KEY_HAMMER, state.boosters[BoosterType.HAMMER] ?: 0)
            putBoolean(KEY_SOUND, state.soundEnabled)
            apply()
        }
    }

    companion object {
        const val PREFS_NAME = "paper_craft_crush_storage"
        private const val KEY_LEVEL = "current_level"
        private const val KEY_HIGHEST_LEVEL = "highest_unlocked_level"
        private const val KEY_LEVEL_STARS = "level_stars_map"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SCISSORS = "booster_scissors"
        private const val KEY_GLUE = "booster_glue"
        private const val KEY_HAMMER = "booster_hammer"
        private const val KEY_SOUND = "sound_enabled"
    }
}
