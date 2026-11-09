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
    val easyRuleBadge: String,
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
            subtitle = "Easy Match-3 & ★ Gold Squares",
            easyRuleBadge = "HOW TO PLAY: Swap 2 blocks to match 3 of the same picture! Match on ★ Gold Squares to collect them.",
            bannerColor = Color(0xFF52B788),
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.ORIGAMI_FOX
        )
        1 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Peppermint Boardwalk",
            subtitle = "Snip ✕ Paper Tape Next to Matches",
            easyRuleBadge = "STAGE RULE: Blocks under ✕ Paper Tape can't move yet! Match right next to ✕ Tape to snip it open.",
            bannerColor = PeppermintRed,
            accentColor = CupcakeFrostingPink,
            mascotPiece = PieceType.PEPPERMINT
        )
        2 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Golden Cardboard Canyon",
            subtitle = "Collect ★ Gold Squares with Line Cutters",
            easyRuleBadge = "STAGE RULE: Match blocks on top of ★ Gold Squares! Every level starts with a free ↔ Line Cutter on the board.",
            bannerColor = StarCardboardGold,
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.CARDBOARD_STAR
        )
        3 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Starlight Origami Summit",
            subtitle = "Pop 🔴 Red Wax Seals (2 Matches Nearby)",
            easyRuleBadge = "STAGE RULE: 🔴 Red Wax Seals take 2 hits! Match next to a Wax Seal once to crack the wax, then once more to free it.",
            bannerColor = WrappedCandyPurple,
            accentColor = OrigamiCraneCyan,
            mascotPiece = PieceType.ORIGAMI_CRANE
        )
        4 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Sakura Cupcake Garden",
            subtitle = "Sweet Cupcakes & Free ✦ 3x3 Paper Bomb",
            easyRuleBadge = "STAGE RULE: Double-tap or swap the ✦ Paper Bomb on the board to blast a big 3x3 square of blocks & ★ Gold!",
            bannerColor = CupcakeFrostingPink,
            accentColor = PeppermintRed,
            mascotPiece = PieceType.CUPCAKE
        )
        5 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Origami Lantern Harbor",
            subtitle = "Open ✕ Tape Bridges to Reach ★ Gold",
            easyRuleBadge = "STAGE RULE: Match next to the ✕ Paper Tape in the middle to open the bridge and collect ★ Gold Squares!",
            bannerColor = OrigamiCraneBlue,
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.ORIGAMI_CRANE
        )
        6 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Berry Sweet Bakery",
            subtitle = "✹ Rainbow Pinwheel Clears All of 1 Color",
            easyRuleBadge = "STAGE RULE: Swap the ✹ Rainbow Pinwheel with ANY block next to it to collect every block of that picture!",
            bannerColor = Color(0xFFD9480F),
            accentColor = CupcakeFrostingPink,
            mascotPiece = PieceType.CUPCAKE
        )
        7 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Lavender Candy Workshop",
            subtitle = "Use Scissors, Glue & Hammer Anytime",
            easyRuleBadge = "STAGE RULE: Tap Scissors, Glue, or Hammer at the bottom anytime—they cost 0 moves and break any lock!",
            bannerColor = Color(0xFF7048E8),
            accentColor = WrappedCandyOrange,
            mascotPiece = PieceType.WRAPPED_CANDY
        )
        8 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Sunny Star Conservatory",
            subtitle = "Big Cascade Combos & 2x Fever Score",
            easyRuleBadge = "STAGE RULE: Match 4 or 5 blocks to fill the Fever Bar for 2x Score & automatic chain cascades!",
            bannerColor = Color(0xFFE67700),
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.CARDBOARD_STAR
        )
        else -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Royal Origami Castle",
            subtitle = "Swap 2 Power-Ups Together for Mega Blast!",
            easyRuleBadge = "STAGE RULE: Swap 2 glowing Power-Up blocks together to unleash a Mega Blast across the board!",
            bannerColor = Color(0xFFC92A2A),
            accentColor = StarCardboardGold,
            mascotPiece = PieceType.PEPPERMINT
        )
    }
}

/**
 * Complete blueprint for each of the 50 campaign levels, designed so kids and adults
 * can immediately understand the stage goal and enjoy smooth, rewarding progression:
 * - Primary & Secondary piece collection goals (friendly, achievable counts)
 * - Gold Washi Stamp cells (★: match on top or blast to collect)
 * - Masking Tape (1 HP) & Red Wax Seal (2 HP) locked cells (✕: match next to or blast to free)
 * - Plain-English, easy-to-read mechanic label shown in the HUD and World Map
 */
data class LevelBlueprint(
    val level: Int,
    val moves: Int,
    val primaryGoal: LevelGoal,
    val secondaryGoal: LevelGoal,
    val goldCells: Set<GridPos>,
    val tapeLocks: Map<GridPos, Int>, // 1 = Masking Tape Strap (1 hit), 2 = Red Wax Seal (2 hits)
    val mechanicLabel: String
)

fun getLevelBlueprint(level: Int): LevelBlueprint {
    val lvl = level.coerceAtLeast(1)
    val pieces = PieceType.entries
    val primaryType = pieces[(lvl - 1) % pieces.size]
    val secondaryType = pieces[(lvl + 2) % pieces.size]

    // Kid-and-adult-friendly target counts & generous move limits
    val primaryCount = (10 + (lvl * 2) / 5).coerceIn(10, 22)
    val secondaryCount = (8 + (lvl * 2) / 6).coerceIn(8, 18)
    val moves = (27 + (lvl % 4) + (if (lvl % 5 == 0) 3 else 0)).coerceIn(27, 34)

    val gold = mutableSetOf<GridPos>()
    val tape = mutableMapOf<GridPos, Int>()

    val stageIndex = ((lvl - 1) / LEVELS_PER_STAGE) % 10
    val stepInStage = (lvl - 1) % LEVELS_PER_STAGE // 0..4

    val mechanicLabel = when (stageIndex) {
        0 -> {
            // Stage 1 (Levels 1-5): Easy Match-3 & ★ Gold Squares
            when (stepInStage) {
                0 -> "Match 3 Same Blocks • Match 4 for ↔ Line Cutter!"
                1 -> "Swap ✹ Rainbow Pinwheel with Any Block!"
                2 -> {
                    listOf(3 to 2, 3 to 3, 3 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "★ Gold Squares • Match ON Gold Squares (3)"
                }
                3 -> {
                    listOf(2 to 3, 3 to 2, 3 to 3, 3 to 4, 4 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
                    "★ Gold Plus • Match ON Gold Squares (5)"
                }
                else -> {
                    listOf(2 to 2, 2 to 4, 3 to 3, 4 to 2, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Stage 1 Boss • Collect 5 ★ Gold Squares!"
                }
            }
        }
        1 -> {
            // Stage 2 (Levels 6-10): Snip ✕ Paper Tape (1 Hit Next to Tape)
            when (stepInStage) {
                0 -> {
                    listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "✕ Paper Tape • Match NEXT TO Tape to Snip It (2)"
                }
                1 -> {
                    listOf(3 to 2, 3 to 3, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "✕ Paper Tape Row • Match Next to Tape to Free (3)"
                }
                2 -> {
                    listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "✕ Tape Diamond • Match Next to Tape to Free (4)"
                }
                3 -> {
                    listOf(2 to 2, 2 to 4, 4 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "✕ Tape Corners • Match Next to Tape or Use Scissors!"
                }
                else -> {
                    listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    listOf(2 to 3, 3 to 3, 4 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Stage 2 Boss • Snip 2 ✕ Tape & Collect 3 ★ Gold!"
                }
            }
        }
        2 -> {
            // Stage 3 (Levels 11-15): ★ Gold Squares + Free Line Cutter
            when (stepInStage) {
                0 -> {
                    listOf(3 to 1, 3 to 2, 3 to 4, 3 to 5).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Free ↔ Line Cutter! • Match ON 4 ★ Gold Squares"
                }
                1 -> {
                    listOf(2 to 2, 2 to 3, 2 to 4, 4 to 2, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Golden Smile • Match ON 5 ★ Gold Squares"
                }
                2 -> {
                    listOf(1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Golden Tower • Slice or Match 5 ★ Gold Squares"
                }
                3 -> {
                    listOf(2 to 2, 2 to 4, 3 to 2, 3 to 4, 4 to 2, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Golden Bridge • Match ON 6 ★ Gold Squares"
                }
                else -> {
                    listOf(2 to 3, 3 to 2, 3 to 3, 3 to 4, 4 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
                    listOf(2 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "Stage 3 Boss • 5 ★ Gold Squares & 2 ✕ Tape!"
                }
            }
        }
        3 -> {
            // Stage 4 (Levels 16-20): Pop 🔴 Red Wax Seals (2 Hits Nearby)
            when (stepInStage) {
                0 -> {
                    listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    "🔴 Red Wax Seal • Match Next to Seal 2 Times! (2)"
                }
                1 -> {
                    tape[GridPos(3, 3)] = 2
                    listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
                    "🔴 Wax Seal Center • Match Nearby or Use Hammer!"
                }
                2 -> {
                    listOf(2 to 2, 2 to 4, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    "🔴 3 Wax Seals • Match Twice Nearby to Pop Open!"
                }
                3 -> {
                    listOf(2 to 3, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    listOf(3 to 2, 3 to 3, 3 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Pop 2 🔴 Wax Seals & Collect 3 ★ Gold Squares!"
                }
                else -> {
                    listOf(2 to 2, 2 to 4, 4 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
                    listOf(3 to 2, 3 to 3, 3 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
                    "Stage 4 Boss • 4 🔴 Wax Seals & 3 ★ Gold Squares!"
                }
            }
        }
        4 -> {
            // Stage 5 (Levels 21-25): Sakura Cupcake Garden (Free ✦ 3x3 Paper Bomb)
            listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
            if (stepInStage >= 2) {
                listOf(2 to 2, 4 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
            }
            "Free ✦ 3x3 Paper Bomb! • Double-Tap It to Blast ★ Gold!"
        }
        5 -> {
            // Stage 6 (Levels 26-30): Origami Lantern Harbor (Tape Bridges + Gold)
            listOf(2 to 1, 3 to 1, 2 to 5, 3 to 5).forEach { gold.add(GridPos(it.first, it.second)) }
            listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = if (stepInStage == 4) 2 else 1 }
            "Open 2 ✕ Tape Bridges & Collect 4 ★ Gold Squares!"
        }
        6 -> {
            // Stage 7 (Levels 31-35): Berry Sweet Bakery (Free ✹ Rainbow Pinwheel)
            listOf(2 to 2, 2 to 4, 3 to 3, 4 to 2, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
            if (stepInStage >= 2) {
                listOf(2 to 3, 4 to 3).forEach { tape[GridPos(it.first, it.second)] = 1 }
            }
            "Free ✹ Rainbow Pinwheel! • Swap It with Any Block!"
        }
        7 -> {
            // Stage 8 (Levels 36-40): Lavender Candy Workshop
            listOf(2 to 3, 3 to 2, 3 to 4, 4 to 3).forEach { gold.add(GridPos(it.first, it.second)) }
            listOf(2 to 2, 2 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
            "Pop 2 🔴 Wax Seals & Collect 4 ★ Gold (Use Boosters!)"
        }
        8 -> {
            // Stage 9 (Levels 41-45): Sunny Star Conservatory
            listOf(2 to 2, 2 to 3, 2 to 4, 4 to 2, 4 to 3, 4 to 4).forEach { gold.add(GridPos(it.first, it.second)) }
            listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 1 }
            "Match 4+ for 2x Fever Score! • 6 ★ Gold & 2 ✕ Tape"
        }
        else -> {
            // Stage 10 (Levels 46-50): Royal Origami Castle
            listOf(2 to 2, 2 to 3, 2 to 4, 3 to 2, 3 to 4, 4 to 2, 4 to 3, 4 to 4).take(5 + (stepInStage % 2)).forEach {
                gold.add(GridPos(it.first, it.second))
            }
            listOf(3 to 2, 3 to 4).forEach { tape[GridPos(it.first, it.second)] = 2 }
            if (lvl == 50) "LEVEL 50 FINALE • Swap 2 Power-Ups for Royal Crown!"
            else "Royal Castle • Use Power-Ups to Clear ★ Gold & Seals!"
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

enum class TutorialTargetType {
    BOARD_SWAP,
    BOARD_SPECIAL_TILE,
    BOOSTER_BAR,
    BOARD_BOOSTER_USE
}

data class TutorialPrompt(
    val level: Int,
    val stepIndex: Int,
    val totalSteps: Int = 2,
    val badgeText: String,
    val title: String,
    val instruction: String,
    val powerUpTip: String,
    val targetType: TutorialTargetType,
    val fromPos: GridPos? = null,
    val toPos: GridPos? = null,
    val highlightCells: Set<GridPos> = emptySet(),
    val recommendedBooster: BoosterType? = null
)

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
    val status: GameStatus = GameStatus.PLAYING,
    val tutorialStep: Int = 1,
    val tutorialDismissed: Boolean = false,
    val tutorialPrompt: TutorialPrompt? = null
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
