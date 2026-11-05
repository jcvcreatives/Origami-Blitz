package com.example.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PaperSoundSynthesizer
import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
import com.example.model.BoosterType
import com.example.model.ConfettiScrap
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
    private var particleLoopJob: Job? = null

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
    }

    fun toggleSound() {
        val next = !_uiState.value.soundEnabled
        soundSynth.isEnabled = next
        _uiState.update { it.copy(soundEnabled = next) }
        prefs?.edit()?.putBoolean(KEY_SOUND, next)?.apply()
    }

    fun retryOrResetLevel() {
        val current = _uiState.value
        // Ensure player has at least 1 of each booster if they ran out on retry
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
        // Reward +1 Scissors and +1 Glue or Hammer on level victory
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
        val count = state.boosters[booster] ?: 0
        if (state.activeBooster == booster) {
            _uiState.update { it.copy(activeBooster = null, glueFirstPos = null, selectedPos = null) }
            return
        }
        if (count <= 0) return
        soundSynth.playPaperCrinkle(1)
        _uiState.update {
            it.copy(
                activeBooster = booster,
                glueFirstPos = null,
                selectedPos = null
            )
        }
    }

    fun onTileTapped(pos: GridPos) {
        val state = _uiState.value
        if (state.isAnimating || state.status != GameStatus.PLAYING) return
        if (pos.row !in 0 until BOARD_SIZE || pos.col !in 0 until BOARD_SIZE) return

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
                    _uiState.update { it.copy(glueFirstPos = pos) }
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
            _uiState.update { it.copy(selectedPos = pos) }
        } else if (currentSelected == pos) {
            _uiState.update { it.copy(selectedPos = null) }
        } else if (currentSelected.isAdjacentTo(pos)) {
            attemptSwap(currentSelected, pos)
        } else {
            soundSynth.playPaperCrinkle(1)
            _uiState.update { it.copy(selectedPos = pos) }
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
            attemptSwap(from, to)
        }
    }

    private fun attemptSwap(posA: GridPos, posB: GridPos) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnimating = true, selectedPos = null) }
            val swappedBoard = swapPositions(_uiState.value.board, posA, posB)
            val matches = findMatchedPositions(swappedBoard)

            if (matches.isEmpty()) {
                // Invalid swap: animate shake and swap back
                soundSynth.playInvalidShake()
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        invalidSwapPair = posA to posB
                    )
                }
                delay(240)
                val restoredBoard = swapPositions(swappedBoard, posA, posB)
                _uiState.update {
                    it.copy(
                        board = restoredBoard,
                        invalidSwapPair = null,
                        isAnimating = false
                    )
                }
            } else {
                // Valid swap: consume 1 move and run match & cascade loop
                soundSynth.playPaperCrinkle(2)
                _uiState.update {
                    it.copy(
                        board = swappedBoard,
                        movesRemaining = (it.movesRemaining - 1).coerceAtLeast(0)
                    )
                }
                delay(140)
                resolveCascades(swappedBoard)
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
                    boosters = updatedBoosters,
                    comboBannerText = "SNIP!"
                )
            }
            crushPositionsAndCascade(setOf(pos), bonusPerTile = 60)
        }
    }

    private fun executeGlueSwap(posA: GridPos, posB: GridPos) {
        viewModelScope.launch {
            val current = _uiState.value
            val count = current.boosters[BoosterType.GLUE] ?: 0
            if (count <= 0) return@launch

            soundSynth.playGlueSwap()
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
                    boosters = updatedBoosters,
                    comboBannerText = "GLUE SWAP!"
                )
            }
            delay(180)
            resolveCascades(swappedBoard)
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
                    boosters = updatedBoosters,
                    comboBannerText = "CARDBOARD SMASH!"
                )
            }
            crushPositionsAndCascade(smashedPositions, bonusPerTile = 50)
        }
    }

    private suspend fun crushPositionsAndCascade(
        positions: Set<GridPos>,
        bonusPerTile: Int
    ) {
        val board = _uiState.value.board
        val crushedTiles = positions.map { board[it.row][it.col] }
        spawnConfettiForTiles(crushedTiles, scrapsPerTile = 16)

        val markedBoard = board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, tile ->
                if (GridPos(r, c) in positions) tile.copy(isCrushing = true) else tile
            }
        }
        applyProgressForClearedTiles(crushedTiles, comboStep = 1, pointsPerTile = bonusPerTile)
        _uiState.update { it.copy(board = markedBoard) }
        delay(200)

        val cascadedBoard = dropAndRefillBoard(markedBoard, positions)
        _uiState.update { it.copy(board = cascadedBoard) }
        delay(240)

        val settledBoard = cascadedBoard.map { row -> row.map { it.copy(spawnDistance = 0f) } }
        _uiState.update { it.copy(board = settledBoard) }
        resolveCascades(settledBoard, startCombo = 1)
    }

    private suspend fun resolveCascades(
        startingBoard: List<List<BoardTile>>,
        startCombo: Int = 0
    ) {
        var currentBoard = startingBoard
        var combo = startCombo

        while (true) {
            val matchedPositions = findMatchedPositions(currentBoard)
            if (matchedPositions.isEmpty()) break

            combo += 1
            soundSynth.playComboChord(combo)
            soundSynth.playPaperCrinkle(combo.coerceAtMost(4))

            val matchedTiles = matchedPositions.map { currentBoard[it.row][it.col] }
            spawnConfettiForTiles(
                tiles = matchedTiles,
                scrapsPerTile = if (matchedTiles.size >= 5) 18 else 15
            )

            val matchLengthBonus = when {
                matchedTiles.size >= 5 -> 55
                matchedTiles.size == 4 -> 40
                else -> 30
            }
            val banner = when {
                combo >= 3 -> "ORIGAMI COMBO x$combo!"
                matchedTiles.size >= 5 -> "5-PIECE MASTERPIECE!"
                matchedTiles.size == 4 -> "4-FOLD CRUSH!"
                combo == 2 -> "DOUBLE FOLD x2!"
                else -> null
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
                    comboBannerText = banner
                )
            }
            delay(210)

            val refilledBoard = dropAndRefillBoard(crushingBoard, matchedPositions)
            _uiState.update { it.copy(board = refilledBoard) }
            delay(240)

            currentBoard = refilledBoard.map { row -> row.map { it.copy(spawnDistance = 0f) } }
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
            soundSynth.playComboChord(5)
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
    }

    private fun applyProgressForClearedTiles(
        clearedTiles: List<BoardTile>,
        comboStep: Int,
        pointsPerTile: Int
    ) {
        _uiState.update { state ->
            val primaryHits = clearedTiles.count { it.type == state.goal.targetPiece }
            val secondaryHits = state.secondaryGoal?.let { sec ->
                clearedTiles.count { it.type == sec.targetPiece }
            } ?: 0

            val gainedScore = clearedTiles.size * pointsPerTile * comboStep
            val newScore = state.score + gainedScore
            val newHigh = maxOf(state.highScore, newScore)

            state.copy(
                score = newScore,
                highScore = newHigh,
                goal = state.goal.copy(collectedCount = state.goal.collectedCount + primaryHits),
                secondaryGoal = state.secondaryGoal?.copy(
                    collectedCount = state.secondaryGoal.collectedCount + secondaryHits
                )
            )
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

            // Top new tiles cascading down
            for (i in 0 until missingCount) {
                val targetRow = i
                colResult.add(
                    BoardTile(
                        id = nextTileId++,
                        type = randomPiece(),
                        row = targetRow,
                        col = c,
                        spawnDistance = (missingCount + 1).toFloat()
                    )
                )
            }

            // Existing tiles dropping down
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
        return board.mapIndexed { r, row ->
            row.mapIndexed { c, tile ->
                when (GridPos(r, c)) {
                    a -> tileB.copy(row = a.row, col = a.col)
                    b -> tileA.copy(row = b.row, col = b.col)
                    else -> tile
                }
            }
        }
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
