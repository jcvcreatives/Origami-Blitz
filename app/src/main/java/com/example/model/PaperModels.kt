package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CupcakeFrostingPink
import com.example.ui.theme.CupcakeLinerBlue
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiCraneCyan
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.WrappedCandyOrange
import com.example.ui.theme.WrappedCandyPurple
import kotlin.math.abs

const val BOARD_SIZE = 7
const val LEVELS_PER_STAGE = 5
const val TOTAL_STAGES = 10
const val TOTAL_LEVELS = 50

enum class PieceType(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color
) {
    PEPPERMINT("Peppermint Pinwheel", PeppermintRed, PeppermintWhite),
    CUPCAKE("Paper Cupcake", CupcakeFrostingPink, CupcakeLinerBlue),
    WRAPPED_CANDY("Origami Candy", WrappedCandyPurple, WrappedCandyOrange),
    CARDBOARD_STAR("Cardboard Star", StarCardboardGold, com.example.ui.theme.CardboardDark),
    ORIGAMI_FOX("Origami Fox", OrigamiFoxOrange, OrigamiFoxGreen),
    ORIGAMI_CRANE("Origami Crane", OrigamiCraneBlue, OrigamiCraneCyan)
}

/**
 * Craftable on-board Special Origami Power-Up Tiles:
 * - ROW_CUTTER: Crafted from a 4-tile match -> slices entire horizontal row
 * - COL_CUTTER: Crafted from a 4-tile match -> slices entire vertical column
 * - PAPER_BOMB: Crafted from an L/T 5-tile match -> blasts a 3x3 square
 * - PINWHEEL_RAINBOW: Crafted from a 5-in-a-line match -> clears all tiles of a color
 */
enum class TileSpecial(val badgeLabel: String) {
    NONE(""),
    ROW_CUTTER("↔"),
    COL_CUTTER("↕"),
    PAPER_BOMB("✦"),
    PINWHEEL_RAINBOW("✹")
}

data class GridPos(val row: Int, val col: Int) {
    fun isAdjacentTo(other: GridPos): Boolean {
        val dr = abs(row - other.row)
        val dc = abs(col - other.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }
}

data class BoardTile(
    val id: Long,
    val type: PieceType,
    val row: Int,
    val col: Int,
    val special: TileSpecial = TileSpecial.NONE,
    val spawnDistance: Float = 0f,
    val slideFromRowDelta: Float = 0f,
    val slideFromColDelta: Float = 0f,
    val swapArcSign: Float = 0f,
    val dropComboTier: Int = 1,
    val animSeq: Long = 0L,
    val isCrushing: Boolean = false
)

data class FloatingScorePopup(
    val id: Long,
    val row: Float,
    val col: Float,
    val text: String,
    val color: Color,
    val isBig: Boolean = false,
    val comboMultiplier: Int = 1
)

enum class BoosterType(
    val title: String,
    val actionLabel: String
) {
    SCISSORS("Scissors", "Cut"),
    GLUE("Glue Bottle", "Swap"),
    HAMMER("Cardboard Hammer", "Smash")
}

data class LevelGoal(
    val targetPiece: PieceType,
    val targetCount: Int,
    val collectedCount: Int = 0
) {
    val remaining: Int
        get() = (targetCount - collectedCount).coerceAtLeast(0)
    val isCompleted: Boolean
        get() = collectedCount >= targetCount
}

enum class ConfettiDepthLayer(
    val parallaxFactorX: Float,
    val parallaxFactorY: Float,
    val targetScale: Float,
    val gravityScale: Float
) {
    FAR_HILL(
        parallaxFactorX = 0.60f,
        parallaxFactorY = 0.55f,
        targetScale = 0.66f,
        gravityScale = 0.72f
    ),
    NEAR_HILL(
        parallaxFactorX = 0.90f,
        parallaxFactorY = 0.85f,
        targetScale = 0.92f,
        gravityScale = 0.95f
    ),
    CRAFT_TABLE(
        parallaxFactorX = 1.15f,
        parallaxFactorY = 1.10f,
        targetScale = 1.22f,
        gravityScale = 1.18f
    )
}

fun computeDioramaLandingY(
    nx: Float,
    layer: ConfettiDepthLayer,
    bandOffset: Float
): Float {
    val t = ((nx.coerceIn(0f, 1f) + 0.08f) / 1.16f)
    val waveSin = kotlin.math.sin(t * Math.PI * 2.0).toFloat()
    return when (layer) {
        ConfettiDepthLayer.FAR_HILL -> {
            val ridgeY = 0.58f + waveSin * 0.04f
            ridgeY + 0.008f + bandOffset.coerceIn(0f, 1f) * 0.068f
        }
        ConfettiDepthLayer.NEAR_HILL -> {
            val ridgeY = 0.70f - waveSin * 0.035f
            ridgeY + 0.010f + bandOffset.coerceIn(0f, 1f) * 0.092f
        }
        ConfettiDepthLayer.CRAFT_TABLE -> {
            val ridgeY = 0.84f + waveSin * 0.012f
            ridgeY + 0.012f + bandOffset.coerceIn(0f, 1f) * 0.105f
        }
    }
}

data class ConfettiScrap(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRotation: Float,
    val widthDp: Float,
    val heightDp: Float,
    val color: Color,
    val foldColor: Color,
    val alpha: Float = 1f,
    val depthLayer: ConfettiDepthLayer = ConfettiDepthLayer.NEAR_HILL,
    val depthScale: Float = 1f,
    val landingBandOffset: Float = 0.4f,
    val flutterPhase: Float = 0f,
    val vFlutter: Float = 0.22f,
    val bounceCount: Int = 0,
    val isLanded: Boolean = false
)

enum class GameStatus {
    PLAYING,
    WON,
    LOST
}

data class PaperStageInfo(
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val bannerColor: Color,
    val accentColor: Color,
    val mascotPiece: PieceType
)

fun getStageInfoForLevel(level: Int): PaperStageInfo {
    val stageNum = (((level.coerceAtLeast(1)) - 1) / LEVELS_PER_STAGE) + 1
    return when ((stageNum - 1) % 10) {
        0 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Whispering Pine Woods",
            subtitle = "Woodland Origami & Gold Stamps",
            bannerColor = Color(0xFF52B788),
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.ORIGAMI_FOX
        )
        1 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Peppermint Boardwalk",
            subtitle = "Masking Tape Pinwheel Pier",
            bannerColor = PeppermintRed,
            accentColor = CupcakeFrostingPink,
            mascotPiece = PieceType.PEPPERMINT
        )
        2 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Golden Cardboard Canyon",
            subtitle = "Red Wax Seal Mesa Ridge",
            bannerColor = StarCardboardGold,
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.CARDBOARD_STAR
        )
        3 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Starlight Origami Summit",
            subtitle = "Foil Stamps & Tape Peaks",
            bannerColor = WrappedCandyPurple,
            accentColor = OrigamiCraneCyan,
            mascotPiece = PieceType.ORIGAMI_CRANE
        )
        4 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Sakura Washi Garden",
            subtitle = "Folded Petal Pavilion",
            bannerColor = CupcakeFrostingPink,
            accentColor = PeppermintRed,
            mascotPiece = PieceType.CUPCAKE
        )
        5 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Origami Lantern Harbor",
            subtitle = "Paper Boat & Crane Quay",
            bannerColor = OrigamiCraneBlue,
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.ORIGAMI_CRANE
        )
        6 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Berry Confectionery Box",
            subtitle = "Wax-Sealed Sweet Studio",
            bannerColor = Color(0xFFD9480F),
            accentColor = CupcakeFrostingPink,
            mascotPiece = PieceType.CUPCAKE
        )
        7 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Lavender Ribbon Atelier",
            subtitle = "Bookbinder's Press Hall",
            bannerColor = Color(0xFF7048E8),
            accentColor = WrappedCandyOrange,
            mascotPiece = PieceType.WRAPPED_CANDY
        )
        8 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Amber Sunburst Glasshouse",
            subtitle = "Stained-Paper Conservatory",
            bannerColor = Color(0xFFE67700),
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.CARDBOARD_STAR
        )
        else -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Grand Origami Citadel",
            subtitle = "Royal Paper Crown Finale",
            bannerColor = Color(0xFFC92A2A),
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.PEPPERMINT
        )
    }
}

/**
 * Complete blueprint for each of the 50 campaign levels, including:
 * - Primary & Secondary piece collection goals
 * - Gold Washi Stamp cells (collect by matching on top)
 * - Masking Tape (1 HP) & Red Wax Seal (2 HP) locked cells (free by matching or adjacent match)
 * - Descriptive mechanic label shown in the HUD and World Map
 */
data class LevelBlueprint(
    val level: Int,
    val moves: Int,
    val primaryGoal: LevelGoal,
    val secondaryGoal: LevelGoal,
    val goldCells: Set<GridPos>,
    val tapeLocks: Map<GridPos, Int>, // 1 = Masking Tape Strap, 2 = Red Wax Seal + Tape
    val mechanicLabel: String
)

fun getLevelBlueprint(level: Int): LevelBlueprint {
    val lvl = level.coerceAtLeast(1)
    val pieces = PieceType.entries
    val primaryType = pieces[(lvl - 1) % pieces.size]
    val secondaryType = pieces[(lvl + 2) % pieces.size]

    val primaryCount = (10 + (lvl * 3) / 2).coerceIn(12, 34)
    val secondaryCount = (8 + (lvl * 5) / 4).coerceIn(10, 28)
    val moves = (23 + (lvl % 4) + (if (lvl % 5 == 0) 3 else 0)).coerceIn(22, 32)

    val gold = mutableSetOf<GridPos>()
    val tape = mutableMapOf<GridPos, Int>()

    fun addDiamondGold(radius: Int) {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (abs(r - 3) + abs(c - 3) <= radius) {
                    gold.add(GridPos(r, c))
                }
            }
        }
    }

    fun addRingGold(radius: Int) {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (maxOf(abs(r - 3), abs(c - 3)) == radius) {
                    gold.add(GridPos(r, c))
                }
            }
        }
    }

    fun addCrossTape(hp: Int, span: Int) {
        for (i in (3 - span)..(3 + span)) {
            if (i in 0 until BOARD_SIZE) {
                tape[GridPos(3, i)] = hp
                tape[GridPos(i, 3)] = hp
            }
        }
    }

    fun addCornersTape(hp: Int) {
        val coords = listOf(1 to 1, 1 to 5, 5 to 1, 5 to 5, 2 to 2, 2 to 4, 4 to 2, 4 to 4)
        coords.take(4 + (lvl % 5)).forEach { (r, c) -> tape[GridPos(r, c)] = hp }
    }

    val stageIndex = ((lvl - 1) / LEVELS_PER_STAGE) % 10
    val stepInStage = (lvl - 1) % LEVELS_PER_STAGE // 0..4

    val mechanicLabel = when (stageIndex) {
        0 -> {
            // Stage 1 (Levels 1-5): Introduces Classic Match & Washi Gold Stamps
            when (stepInStage) {
                0 -> "Classic Paper Fold • Match 4 for Line Cutter!"
                1 -> {
                    listOf(2 to 2, 2 to 4, 3 to 3, 4 to 2, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Gold Washi Stamps • Match on Gold Cells!"
                }
                2 -> {
                    addDiamondGold(1)
                    "Gold Stamp Diamond • Collect 5 Foil Stamps"
                }
                3 -> {
                    for (c in 1..5) gold.add(GridPos(3, c))
                    for (r in 1..5) gold.add(GridPos(r, 3))
                    "Woodland Gold Cross • Collect 9 Foil Stamps"
                }
                else -> {
                    addDiamondGold(2)
                    "Stage 1 Finale • Golden Pine Crest"
                }
            }
        }
        1 -> {
            // Stage 2 (Levels 6-10): Introduces Masking Tape Straps (1 HP)
            when (stepInStage) {
                0 -> {
                    listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "Masking Tape Straps • Match Adjacent to Free!"
                }
                1 -> {
                    for (c in 1..5) tape[GridPos(3, c)] = 1
                    "Boardwalk Tape Line • Rip Through 5 Straps"
                }
                2 -> {
                    addCornersTape(1)
                    "Pinwheel Tape Corners • Free Locked Tiles"
                }
                3 -> {
                    addCrossTape(1, 2)
                    "Criss-Cross Masking Tape • 9 Taped Tiles"
                }
                else -> {
                    addCrossTape(1, 2)
                    addDiamondGold(1)
                    "Stage 2 Finale • Tape & Gold Foil Pier"
                }
            }
        }
        2 -> {
            // Stage 3 (Levels 11-15): Introduces Red Wax Seals (2 HP)
            when (stepInStage) {
                0 -> {
                    listOf(2 to 2, 2 to 4, 4 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    "Red Wax Seals (2-Hit) • Crack Seal then Rip Tape!"
                }
                1 -> {
                    tape[GridPos(3, 3)] = 2
                    listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "Canyon Seal Core • Wax Seal + Tape Guard"
                }
                2 -> {
                    for (c in 1..5 step 2) {
                        tape[GridPos(2, c)] = 2
                        tape[GridPos(4, c)] = 2
                    }
                    "Mesa Wax Pillars • 6 Double-Layer Seals"
                }
                3 -> {
                    addDiamondGold(2)
                    listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    "Sealed Gold Vault • Unlock the Gold Stamps"
                }
                else -> {
                    addCrossTape(2, 1)
                    addRingGold(2)
                    "Stage 3 Finale • Golden Canyon Fortress"
                }
            }
        }
        3 -> {
            // Stage 4 (Levels 16-20): Starlight Summit (Gold Stamps + Tape + Wax)
            addDiamondGold(if (stepInStage >= 3) 2 else 1)
            addCornersTape(if (stepInStage >= 2) 2 else 1)
            if (stepInStage == 4) tape[GridPos(3, 3)] = 2
            "Starlight Summit • Foil Stamps & Wax Locks"
        }
        4 -> {
            // Stage 5 (Levels 21-25): Sakura Washi Garden
            addRingGold(1)
            if (stepInStage >= 1) {
                listOf(1 to 3, 3 to 1, 3 to 5, 5 to 3).forEach {
                    gold.add(GridPos(it.first, it.second))
                    tape[GridPos(it.first, it.second)] = if (stepInStage >= 3) 2 else 1
                }
            }
            if (stepInStage >= 2) tape[GridPos(3, 3)] = 2
            "Sakura Washi Blossom • Petal Stamps & Seals"
        }
        5 -> {
            // Stage 6 (Levels 26-30): Origami Lantern Harbor
            for (r in 1..5) {
                gold.add(GridPos(r, 1))
                gold.add(GridPos(r, 5))
            }
            for (c in 2..4) {
                tape[GridPos(2 + (stepInStage % 2), c)] = if (c == 3 || stepInStage >= 3) 2 else 1
                gold.add(GridPos(3, c))
            }
            "Lantern Harbor • Twin Gold Quays & Tape Bridges"
        }
        6 -> {
            // Stage 7 (Levels 31-35): Berry Confectionery Box
            for (r in 1..5) {
                for (c in 1..5) {
                    if ((r + c) % 2 == 0) gold.add(GridPos(r, c))
                }
            }
            addCrossTape(if (stepInStage >= 2) 2 else 1, 1)
            "Confectionery Checkerboard • Gold Foil & Seals"
        }
        7 -> {
            // Stage 8 (Levels 36-40): Lavender Ribbon Atelier
            addRingGold(2)
            addCrossTape(1, 2)
            listOf(2 to 2, 2 to 4, 4 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
            "Bookbinder's Press • Stitched Ribbon Frame"
        }
        8 -> {
            // Stage 9 (Levels 41-45): Amber Sunburst Glasshouse
            addDiamondGold(2)
            for (i in 1..5) {
                tape[GridPos(i, i)] = if (i == 3 || stepInStage >= 2) 2 else 1
                tape[GridPos(i, 6 - i)] = if (i == 3 || stepInStage >= 3) 2 else 1
            }
            "Sunburst Conservatory • Diagonal Seal Rays"
        }
        else -> {
            // Stage 10 (Levels 46-50): Grand Origami Citadel
            addDiamondGold(2)
            addRingGold(2)
            addCrossTape(2, 2)
            listOf(1 to 1, 1 to 5, 5 to 1, 5 to 5).forEach { tape[GridPos(it.first, it.second)] = 1 }
            if (lvl == 50) "LEVEL 50 GRAND FINALE • Royal Origami Crown!"
            else "Grand Origami Citadel • Master Paper Trial"
        }
    }

    return LevelBlueprint(
        level = lvl,
        moves = moves,
        primaryGoal = LevelGoal(primaryType, primaryCount, 0),
        secondaryGoal = LevelGoal(secondaryType, secondaryCount, 0),
        goldCells = gold,
        tapeLocks = tape,
        mechanicLabel = mechanicLabel
    )
}

fun buildGoalsForLevel(level: Int): Pair<LevelGoal, LevelGoal> {
    val bp = getLevelBlueprint(level)
    return bp.primaryGoal to bp.secondaryGoal
}

fun getMovesForLevel(level: Int): Int = getLevelBlueprint(level).moves

data class PaperGameState(
    val level: Int = 1,
    val highestUnlockedLevel: Int = 1,
    val levelStars: Map<Int, Int> = emptyMap(),
    val lastEarnedStars: Int = 0,
    val showWorldMap: Boolean = false,
    val mapSelectedLevel: Int = 1,
    val score: Int = 0,
    val highScore: Int = 0,
    val movesRemaining: Int = 25,
    val goal: LevelGoal = LevelGoal(PieceType.ORIGAMI_FOX, 12, 0),
    val secondaryGoal: LevelGoal? = LevelGoal(PieceType.PEPPERMINT, 10, 0),
    val goldCells: Set<GridPos> = emptySet(),
    val initialGoldCount: Int = 0,
    val tapeLocks: Map<GridPos, Int> = emptyMap(),
    val initialTapeCount: Int = 0,
    val mechanicLabel: String = "Classic Paper Fold",
    val board: List<List<BoardTile>> = emptyList(),
    val selectedPos: GridPos? = null,
    val glueFirstPos: GridPos? = null,
    val invalidSwapPair: Pair<GridPos, GridPos>? = null,
    val hintPair: Pair<GridPos, GridPos>? = null,
    val activeBooster: BoosterType? = null,
    val boosters: Map<BoosterType, Int> = mapOf(
        BoosterType.SCISSORS to 3,
        BoosterType.GLUE to 3,
        BoosterType.HAMMER to 2
    ),
    val comboCount: Int = 0,
    val streakCount: Int = 0,
    val feverCharge: Float = 0f,
    val feverMovesLeft: Int = 0,
    val boardPunchSeq: Long = 0L,
    val boardPunchStrength: Float = 0f,
    val floatingPopups: List<FloatingScorePopup> = emptyList(),
    val comboBannerText: String? = null,
    val isAnimating: Boolean = false,
    val soundEnabled: Boolean = true,
    val status: GameStatus = GameStatus.PLAYING
) {
    val currentStage: PaperStageInfo
        get() = getStageInfoForLevel(level)

    val totalStars: Int
        get() = levelStars.values.sum()

    val goldRemaining: Int
        get() = goldCells.size

    val tapeRemaining: Int
        get() = tapeLocks.size

    val areAllObjectivesComplete: Boolean
        get() = goal.isCompleted &&
            (secondaryGoal?.isCompleted != false) &&
            goldCells.isEmpty() &&
            tapeLocks.isEmpty()
}
