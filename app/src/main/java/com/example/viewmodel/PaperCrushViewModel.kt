package com.example.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PaperSoundSynthesizer
import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
import com.example.model.BoosterType
import com.example.model.ConfettiScrap
import com.example.model.FloatingScorePopup
import com.example.model.GameStatus
import com.example.model.GridPos
import com.example.model.LevelGoal
import com.example.model.PaperGameState
import com.example.model.PieceType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        val savedLevel = prefs?.getInt(KEY_LEVEL, 1)?.coerceAtLeast(1) ?: 1
        val savedHighScore = prefs?.getInt(KEY_HIGH_SCORE, 0) ?: 0
        val savedScissors = prefs?.getInt(KEY_SCISSORS, 3) ?: 3
        val savedGlue = prefs?.getInt(KEY_GLUE, 3) ?: 3
        val savedHammer = prefs?.getInt(KEY_HAMMER, 2) ?: 2
        val savedSound = prefs?.getBoolean(KEY_SOUND, true) ?: true

        soundSynth.isEnabled = savedSound
        startLevel(
            level = savedLevel,
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
        carryHighScore: Int = _uiState.value.highScore,
        boosters: Map<BoosterType, Int> = _uiState.value.boosters,
        soundEnabled: Boolean = _uiState.value.soundEnabled
    ) {
        idleHintJob?.cancel()
        val pieces = PieceType.entries
        val primaryType = pieces[(level - 1) % pieces.size]
        val secondaryType = pieces[(level + 2) % pieces.size]
        val primaryCount = 10 + (level * 2).coerceAtMost(24)
        val secondaryCount = 8 + (level * 2).coerceAtMost(20)
        val moves = (24 + (level % 3)).coerceIn(20, 30)

        val initialBoard = generatePlayableBoard()
        _uiState.value = PaperGameState(
            level = level,
            score = 0,
            highScore = carryHighScore,
            movesRemaining = moves,
            goal = LevelGoal(primaryType, primaryCount, 0),
            secondaryGoal = LevelGoal(secondaryType, secondaryCount, 0),
            board = initialBoard,
            boosters = boosters,
            soundEnabled = soundEnabled,
            status = GameStatus.PLAYING
        )
        persistProgress()
        scheduleIdleHint()
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
            carryHighScore = current.highScore,
            boosters = refreshedBoosters,
            soundEnabled = current.soundEnabled
        )
    }

    fun advanceToNextLevel() {
        val current = _uiState.value
        val nextLevel = current.level + 1
        val rewardedBoosters = current.boosters.toMutableMap().apply {
            put(BoosterType.SCISSORS, (get(BoosterType.SCISSORS) ?: 0) + 1)
            val bonusType = if (nextLevel % 2 == 0) BoosterType.HAMMER else BoosterType.GLUE
            put(bonusType, (get(bonusType) ?: 0) + 1)
        }
        startLevel(
            level = nextLevel,
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

        // Handle active booster first
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

        // Standard tap-to-select / tap-adjacent-to-swap
        val currentSelected = state.selectedPos
        if (currentSelected == null) {
            soundSynth.playPaperCrinkle(1)
            _uiState.update { it.copy(selectedPos = pos, hintPair = null) }
            scheduleIdleHint()
        } else if (currentSelected == pos) {
            _uiState.update { it.copy(selectedPos = null) }
            scheduleIdleHint()
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
            attemptSwap(from, to)
        }
    }

    private fun attemptSwap(posA: GridPos, posB: GridPos) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isAnimating = true, selectedPos = null, hintPair = null)
            }
            soundSynth.playSwapWhoosh()
            val swappedBoard = swapPositions(_uiState.value.board, posA, posB)
            val matches = findMatchedPositions(swappedBoard)

            if (matches.isEmpty()) {
                // Step 1: Animate the two blocks arcing across to each other's positions
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        invalidSwapPair = null
                    )
                }
                delay(240)

                // Step 2: Brief invalid shake at swapped position
                soundSynth.playInvalidShake()
                _uiState.update {
                    it.copy(
                        invalidSwapPair = posA to posB,
                        streakCount = 0
                    )
                }
                delay(170)

                // Step 3: Animate the two blocks arcing back to their original positions
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
                // Valid swap: animate the two blocks arcing into place, then run match & cascade loop
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
                resolveCascades(settledSwapBoard)
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
            crushPositionsAndCascade(setOf(pos), bonusPerTile = 65)
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
            val swappedBoard = swapPositions(current.board, posA, posB)
            val tileA = current.board[posA.row][posA.col]
            val tileB = current.board[posB.row][posB.col]
            spawnConfettiForTiles(listOf(tileA, tileB))

            _uiState.update {
                it.copy(
                    board = swappedBoard,
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
            resolveCascades(settledSwapBoard)
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
            crushPositionsAndCascade(smashedPositions, bonusPerTile = 55)
        }
    }

    private suspend fun crushPositionsAndCascade(
        positions: Set<GridPos>,
        bonusPerTile: Int
    ) {
        val board = _uiState.value.board
        val crushedTiles = positions.map { board[it.row][it.col] }
        spawnConfettiForTiles(crushedTiles, scrapsPerTile = 18)

        val markedBoard = board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, tile ->
                if (GridPos(r, c) in positions) tile.copy(isCrushing = true) else tile
            }
        }
        applyProgressForClearedTiles(crushedTiles, comboStep = 1, pointsPerTile = bonusPerTile)
        _uiState.update { it.copy(board = markedBoard) }
        delay(220)

        val cascadedBoard = dropAndRefillBoard(markedBoard, positions)
        soundSynth.playDropBounce(positions.size)
        _uiState.update { it.copy(board = cascadedBoard) }
        delay(340)

        val settledBoard = clearTileMotion(cascadedBoard)
        _uiState.update { it.copy(board = settledBoard) }
        resolveCascades(settledBoard, startCombo = 1)
    }

    private data class MatchAnalysis(
        val allMatched: Set<GridPos>,
        val maxRunLength: Int,
        val triggeredLineRip: Boolean,
        val triggeredColorStorm: Boolean
    )

    private fun analyzeMatchesWithSpecials(board: List<List<BoardTile>>): MatchAnalysis {
        if (board.size != BOARD_SIZE) return MatchAnalysis(emptySet(), 0, false, false)
        val matched = mutableSetOf<GridPos>()
        var maxRun = 0
        val ripRows = mutableSetOf<Int>()
        val ripCols = mutableSetOf<Int>()
        val stormTypes = mutableSetOf<PieceType>()

        // Horizontal runs
        for (r in 0 until BOARD_SIZE) {
            var c = 0
            while (c < BOARD_SIZE) {
                val type = board[r][c].type
                var end = c + 1
                while (end < BOARD_SIZE && board[r][end].type == type) end++
                val len = end - c
                if (len >= 3) {
                    maxRun = maxOf(maxRun, len)
                    for (k in c until end) matched.add(GridPos(r, k))
                    if (len == 4) ripRows.add(r)
                    if (len >= 5) stormTypes.add(type)
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
                    maxRun = maxOf(maxRun, len)
                    for (k in r until end) matched.add(GridPos(k, c))
                    if (len == 4) ripCols.add(c)
                    if (len >= 5) stormTypes.add(type)
                }
                r = end
            }
        }

        // Expand 4-in-a-row into full Row/Column Paper Rip
        for (r in ripRows) {
            for (c in 0 until BOARD_SIZE) matched.add(GridPos(r, c))
        }
        for (c in ripCols) {
            for (r in 0 until BOARD_SIZE) matched.add(GridPos(r, c))
        }

        // Expand 5-in-a-row into full-board Origami Color Storm
        if (stormTypes.isNotEmpty()) {
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (board[r][c].type in stormTypes) {
                        matched.add(GridPos(r, c))
                    }
                }
            }
        }

        return MatchAnalysis(
            allMatched = matched,
            maxRunLength = maxRun,
            triggeredLineRip = ripRows.isNotEmpty() || ripCols.isNotEmpty(),
            triggeredColorStorm = stormTypes.isNotEmpty()
        )
    }

    private suspend fun resolveCascades(
        startingBoard: List<List<BoardTile>>,
        startCombo: Int = 0
    ) {
        var currentBoard = startingBoard
        var combo = startCombo

        while (true) {
            val analysis = analyzeMatchesWithSpecials(currentBoard)
            val matchedPositions = analysis.allMatched
            if (matchedPositions.isEmpty()) break

            combo += 1
            soundSynth.playComboChord(combo)
            soundSynth.playPaperCrinkle(combo.coerceAtMost(4))

            val matchedTiles = matchedPositions.map { currentBoard[it.row][it.col] }
            spawnConfettiForTiles(
                tiles = matchedTiles,
                scrapsPerTile = when {
                    analysis.triggeredColorStorm -> 22
                    analysis.triggeredLineRip || combo >= 2 -> 18
                    else -> 15
                }
            )

            // Reward +1 random booster on a 5-match Color Storm!
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
                analysis.triggeredLineRip -> 48
                else -> 32
            }
            val banner = when {
                analysis.triggeredColorStorm -> "5-FOLD COLOR STORM! +1 BOOSTER"
                analysis.triggeredLineRip -> "4-FOLD LINE RIP!"
                combo >= 3 -> "ORIGAMI FEVER COMBO x$combo!"
                combo == 2 -> "DOUBLE FOLD x2!"
                _uiState.value.streakCount >= 3 -> "HOT STREAK x${_uiState.value.streakCount}!"
                else -> null
            }

            val punchStrength = when {
                analysis.triggeredColorStorm -> 1.25f
                analysis.triggeredLineRip || combo >= 3 -> 0.9f
                combo == 2 -> 0.55f
                else -> 0.28f
            }

            val crushingBoard = currentBoard.mapIndexed { r, row ->
                row.mapIndexed { c, tile ->
                    if (GridPos(r, c) in matchedPositions) tile.copy(isCrushing = true) else tile
                }
            }
            applyProgressForClearedTiles(matchedTiles, comboStep = combo, pointsPerTile = matchLengthBonus)
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

            val refilledBoard = dropAndRefillBoard(crushingBoard, matchedPositions)
            soundSynth.playDropBounce(matchedPositions.size)
            _uiState.update { it.copy(board = refilledBoard) }
            delay(345)

            currentBoard = clearTileMotion(refilledBoard)
            _uiState.update { it.copy(board = currentBoard) }
        }

        // Ensure board has at least one valid move
        if (!hasValidMove(currentBoard)) {
            currentBoard = generatePlayableBoard()
        }

        val postState = _uiState.value
        val allGoalsDone = postState.goal.isCompleted && (postState.secondaryGoal?.isCompleted != false)
        val nextStatus = when {
            allGoalsDone -> GameStatus.WON
            postState.movesRemaining <= 0 -> GameStatus.LOST
            else -> GameStatus.PLAYING
        }

        if (nextStatus == GameStatus.WON) {
            soundSynth.playFeverFanfare()
        }

        _uiState.update {
            it.copy(
                board = currentBoard,
                isAnimating = false,
                comboCount = 0,
                comboBannerText = null,
                status = nextStatus
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
        pointsPerTile: Int
    ) {
        if (clearedTiles.isEmpty()) return
        val popupId = nextPopupId++

        _uiState.update { state ->
            val primaryHits = clearedTiles.count { it.type == state.goal.targetPiece }
            val secondaryHits = state.secondaryGoal?.let { sec ->
                clearedTiles.count { it.type == sec.targetPiece }
            } ?: 0

            val isFeverActive = state.feverMovesLeft > 0
            val feverMult = if (isFeverActive) 2 else 1
            val gainedScore = clearedTiles.size * pointsPerTile * comboStep * feverMult
            val newScore = state.score + gainedScore
            val newHigh = maxOf(state.highScore, newScore)

            // Charge Fever Meter
            val chargeDelta = (clearedTiles.size * 0.065f) + (if (comboStep >= 2) 0.14f else 0f)
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

            // Compute average row/col for floating score popup
            val avgRow = clearedTiles.map { it.row }.average().toFloat()
            val avgCol = clearedTiles.map { it.col }.average().toFloat()
            val popupText = when {
                triggeredNewFever -> "+$gainedScore FEVER 2x!"
                isFeverActive -> "+$gainedScore (2x!)"
                comboStep >= 2 -> "+$gainedScore x$comboStep!"
                else -> "+$gainedScore"
            }
            val popup = FloatingScorePopup(
                id = popupId,
                row = avgRow,
                col = avgCol,
                text = popupText,
                color = clearedTiles.first().type.primaryColor,
                isBig = isFeverActive || comboStep >= 2 || clearedTiles.size >= 4
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

    /**
     * Detects all horizontal and vertical matches of 3, 4, and 5+ across the 7x7 board.
     */
    fun findMatchedPositions(board: List<List<BoardTile>>): Set<GridPos> {
        if (board.size != BOARD_SIZE) return emptySet()
        val matched = mutableSetOf<GridPos>()

        // Horizontal matches
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

        // Vertical matches
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
        removed: Set<GridPos>
    ): List<List<BoardTile>> {
        val newCols = Array(BOARD_SIZE) { ArrayList<BoardTile>(BOARD_SIZE) }

        for (c in 0 until BOARD_SIZE) {
            val survivors = mutableListOf<BoardTile>()
            for (r in (BOARD_SIZE - 1) downTo 0) {
                if (GridPos(r, c) !in removed) {
                    survivors.add(board[r][c])
                }
            }
            val missingCount = BOARD_SIZE - survivors.size
            val colResult = ArrayList<BoardTile>(BOARD_SIZE)

            // Top new tiles cascading down from above the board rim
            for (i in 0 until missingCount) {
                val targetRow = i
                val fallDistance = (missingCount - i + 0.85f) + (i * 0.45f)
                colResult.add(
                    BoardTile(
                        id = nextTileId++,
                        type = randomPiece(),
                        row = targetRow,
                        col = c,
                        spawnDistance = fallDistance,
                        slideFromRowDelta = -fallDistance,
                        slideFromColDelta = 0f,
                        swapArcSign = 0f,
                        animSeq = nextAnimSeq++
                    )
                )
            }

            // Existing tiles dropping down by gravity
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
                val hint = findFirstValidMove(st.board)
                if (hint != null) {
                    _uiState.update { it.copy(hintPair = hint) }
                }
            }
        }
    }

    private fun findFirstValidMove(board: List<List<BoardTile>>): Pair<GridPos, GridPos>? {
        val goalTypes = setOfNotNull(_uiState.value.goal.targetPiece, _uiState.value.secondaryGoal?.targetPiece)
        var fallbackPair: Pair<GridPos, GridPos>? = null
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val here = GridPos(r, c)
                val neighbors = listOfNotNull(
                    if (c + 1 < BOARD_SIZE) GridPos(r, c + 1) else null,
                    if (r + 1 < BOARD_SIZE) GridPos(r + 1, c) else null
                )
                for (next in neighbors) {
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

    private fun generatePlayableBoard(): List<List<BoardTile>> {
        while (true) {
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
            if (hasValidMove(rows)) return rows
        }
    }

    private fun hasValidMove(board: List<List<BoardTile>>): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val here = GridPos(r, c)
                if (c + 1 < BOARD_SIZE) {
                    val swapped = swapPositions(board, here, GridPos(r, c + 1))
                    if (findMatchedPositions(swapped).isNotEmpty()) return true
                }
                if (r + 1 < BOARD_SIZE) {
                    val swapped = swapPositions(board, here, GridPos(r + 1, c))
                    if (findMatchedPositions(swapped).isNotEmpty()) return true
                }
            }
        }
        return false
    }

    private fun spawnConfettiForTiles(
        tiles: List<BoardTile>,
        scrapsPerTile: Int = 15
    ) {
        val created = ArrayList<ConfettiScrap>(tiles.size * scrapsPerTile)
        tiles.forEach { tile ->
            val centerX = (tile.col + 0.5f) / BOARD_SIZE
            val centerY = 0.25f + ((tile.row + 0.5f) / BOARD_SIZE) * 0.52f
            repeat(scrapsPerTile) {
                val angle = Random.nextFloat() * (Math.PI * 2f).toFloat()
                val speed = 0.004f + Random.nextFloat() * 0.014f
                created.add(
                    ConfettiScrap(
                        id = nextScrapId++,
                        x = centerX + (Random.nextFloat() - 0.5f) * 0.05f,
                        y = centerY + (Random.nextFloat() - 0.5f) * 0.05f,
                        vx = kotlin.math.cos(angle) * speed,
                        vy = kotlin.math.sin(angle) * speed - 0.008f,
                        rotation = Random.nextFloat() * 360f,
                        vRotation = (Random.nextFloat() - 0.5f) * 22f,
                        widthDp = 7f + Random.nextFloat() * 7f,
                        heightDp = 5f + Random.nextFloat() * 6f,
                        color = if (it % 2 == 0) tile.type.primaryColor else tile.type.secondaryColor,
                        foldColor = tile.type.secondaryColor
                    )
                )
            }
        }
        _confettiScraps.update { (it + created).takeLast(240) }
        ensureParticleLoopRunning()
    }

    private fun ensureParticleLoopRunning() {
        if (particleLoopJob?.isActive == true) return
        particleLoopJob = viewModelScope.launch {
            while (_confettiScraps.value.isNotEmpty()) {
                delay(16)
                _confettiScraps.update { current ->
                    current.mapNotNull { s ->
                        val nextAlpha = s.alpha - 0.024f
                        if (nextAlpha <= 0f || s.y > 1.15f) {
                            null
                        } else {
                            s.copy(
                                x = s.x + s.vx,
                                y = s.y + s.vy,
                                vx = s.vx * 0.98f + ( kotlin.math.sin(s.rotation * 0.05f) * 0.0004f ),
                                vy = s.vy + 0.00085f, // gravity
                                rotation = s.rotation + s.vRotation,
                                alpha = nextAlpha
                            )
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

    private fun persistProgress() {
        val state = _uiState.value
        prefs?.edit()?.apply {
            putInt(KEY_LEVEL, state.level)
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
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_SCISSORS = "booster_scissors"
        private const val KEY_GLUE = "booster_glue"
        private const val KEY_HAMMER = "booster_hammer"
        private const val KEY_SOUND = "sound_enabled"
    }
}
