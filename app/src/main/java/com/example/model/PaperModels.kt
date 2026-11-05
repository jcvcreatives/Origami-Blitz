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

const val BOARD_SIZE = 7

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

data class GridPos(val row: Int, val col: Int) {
    fun isAdjacentTo(other: GridPos): Boolean {
        val dr = kotlin.math.abs(row - other.row)
        val dc = kotlin.math.abs(col - other.col)
        return (dr == 1 && dc == 0) || (dr == 0 && dc == 1)
    }
}

data class BoardTile(
    val id: Long,
    val type: PieceType,
    val row: Int,
    val col: Int,
    val spawnDistance: Float = 0f,
    val slideFromRowDelta: Float = 0f,
    val slideFromColDelta: Float = 0f,
    val swapArcSign: Float = 0f,
    val animSeq: Long = 0L,
    val isCrushing: Boolean = false
)

data class FloatingScorePopup(
    val id: Long,
    val row: Float,
    val col: Float,
    val text: String,
    val color: Color,
    val isBig: Boolean = false
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

data class ConfettiScrap(
    val id: Long,
    val x: Float, // normalized 0..1 of board or screen width
    val y: Float, // normalized 0..1 of board or screen height
    val vx: Float,
    val vy: Float,
    val rotation: Float,
    val vRotation: Float,
    val widthDp: Float,
    val heightDp: Float,
    val color: Color,
    val foldColor: Color,
    val alpha: Float = 1f,
    val shapeVariant: Int = 0
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

const val LEVELS_PER_STAGE = 5

fun getStageInfoForLevel(level: Int): PaperStageInfo {
    val stageNum = ((level - 1) / LEVELS_PER_STAGE) + 1
    return when ((stageNum - 1) % 4) {
        0 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Whispering Pine Woods",
            subtitle = "Woodland Origami Trail",
            bannerColor = Color(0xFF52B788),
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.ORIGAMI_FOX
        )
        1 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Peppermint Boardwalk",
            subtitle = "Sweet Paper Pinwheel Pier",
            bannerColor = PeppermintRed,
            accentColor = CupcakeFrostingPink,
            mascotPiece = PieceType.PEPPERMINT
        )
        2 -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Golden Cardboard Canyon",
            subtitle = "Corrugated Mesa Ridge",
            bannerColor = StarCardboardGold,
            accentColor = OrigamiFoxOrange,
            mascotPiece = PieceType.CARDBOARD_STAR
        )
        else -> PaperStageInfo(
            stageNumber = stageNum,
            title = "Starlight Origami Summit",
            subtitle = "Cloud-Folded Sky Peaks",
            bannerColor = WrappedCandyPurple,
            accentColor = OrigamiCraneCyan,
            mascotPiece = PieceType.ORIGAMI_CRANE
        )
    }
}

fun buildGoalsForLevel(level: Int): Pair<LevelGoal, LevelGoal> {
    val pieces = PieceType.entries
    val primaryType = pieces[(level - 1) % pieces.size]
    val secondaryType = pieces[(level + 2) % pieces.size]
    val primaryCount = 10 + (level * 2).coerceAtMost(24)
    val secondaryCount = 8 + (level * 2).coerceAtMost(20)
    return LevelGoal(primaryType, primaryCount, 0) to LevelGoal(secondaryType, secondaryCount, 0)
}

fun getMovesForLevel(level: Int): Int = (24 + (level % 3)).coerceIn(20, 30)

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
}
