package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.model.BoosterType
import com.example.model.PieceType
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.CupcakeFrostingPink
import com.example.ui.theme.CupcakeLinerBlue
import com.example.ui.theme.InkBrown
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiCraneCyan
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.WrappedCandyOrange
import com.example.ui.theme.WrappedCandyPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactile torn-paper shape for masking tape, banners, and notes.
 */
class TornPaperShape(
    private val teethCount: Int = 7,
    private val toothDepthPx: Float = 7f,
    private val tearTopBottom: Boolean = false
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val w = size.width
        val h = size.height
        val depth = toothDepthPx.coerceAtMost(9f)
        val stepY = h / teethCount.coerceAtLeast(2)
        val stepX = w / (teethCount * 2).coerceAtLeast(4)

        path.moveTo(depth, 0f)
        if (tearTopBottom) {
            val topSteps = (w / stepX).toInt().coerceAtLeast(2)
            for (i in 1..topSteps) {
                val x = (i * w / topSteps).coerceAtMost(w - depth)
                val y = if (i % 2 == 1) depth * 0.45f else 0f
                path.lineTo(x, y)
            }
        } else {
            path.lineTo(w - depth, 0f)
        }

        for (i in 1..teethCount) {
            val yMid = (i - 0.5f) * stepY
            val yEnd = i * stepY
            path.lineTo(w, yMid)
            path.lineTo(w - depth, yEnd)
        }

        if (tearTopBottom) {
            val botSteps = (w / stepX).toInt().coerceAtLeast(2)
            for (i in botSteps - 1 downTo 1) {
                val x = (i * w / botSteps).coerceAtLeast(depth)
                val y = if (i % 2 == 1) h - depth * 0.45f else h
                path.lineTo(x, y)
            }
        } else {
            path.lineTo(depth, h)
        }

        for (i in teethCount downTo 1) {
            val yMid = (i - 0.5f) * stepY
            val yStart = (i - 1) * stepY
            path.lineTo(0f, yMid)
            path.lineTo(depth, yStart)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Vibrant, high-contrast die-cut cardstock and origami pieces with white sticker rims,
 * drop shadows, and crisp ink outlines that pop off the board.
 */
@Composable
fun PaperPieceVector(
    pieceType: PieceType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        when (pieceType) {
            PieceType.PEPPERMINT -> drawPeppermintPiece()
            PieceType.CUPCAKE -> drawCupcakePiece()
            PieceType.WRAPPED_CANDY -> drawWrappedCandyPiece()
            PieceType.CARDBOARD_STAR -> drawCardboardStarPiece()
            PieceType.ORIGAMI_FOX -> drawOrigamiFoxPiece()
            PieceType.ORIGAMI_CRANE -> drawOrigamiCranePiece()
        }
    }
}

private fun DrawScope.drawPeppermintPiece() {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension * 0.40f
    val shadowOffset = size.minDimension * 0.04f
    val strokeW = size.minDimension * 0.025f

    // Drop shadow
    drawCircle(
        color = Color(0x33000000),
        radius = r * 1.05f,
        center = Offset(cx + shadowOffset, cy + shadowOffset)
    )
    // Crisp white die-cut sticker rim
    drawCircle(
        color = PeppermintWhite,
        radius = r * 1.05f,
        center = Offset(cx, cy)
    )
    // Vibrant candy red base
    drawCircle(
        color = PeppermintRed,
        radius = r * 0.92f,
        center = Offset(cx, cy)
    )
    // 6 Swirled white & blush pinwheel fins
    for (i in 0 until 6) {
        rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
            val fin = Path().apply {
                moveTo(cx, cy)
                quadraticTo(cx + r * 0.44f, cy - r * 0.24f, cx + r * 0.88f, cy - r * 0.14f)
                lineTo(cx + r * 0.72f, cy + r * 0.26f)
                close()
            }
            drawPath(
                path = fin,
                color = if (i % 2 == 0) PeppermintWhite else Color(0xFFFFCCD5)
            )
        }
    }
    drawCircle(
        color = InkBrown,
        radius = r * 0.94f,
        center = Offset(cx, cy),
        style = Stroke(width = strokeW)
    )
    // Center candy button
    drawCircle(
        color = PeppermintRed,
        radius = r * 0.22f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = PeppermintWhite,
        radius = r * 0.09f,
        center = Offset(cx - r * 0.04f, cy - r * 0.04f)
    )
}

private fun DrawScope.drawCupcakePiece() {
    val w = size.width
    val h = size.height
    val s = size.minDimension * 0.04f
    val strokeW = size.minDimension * 0.024f

    // Drop shadow
    drawCircle(Color(0x33000000), radius = w * 0.32f, center = Offset(w * 0.5f + s, h * 0.46f + s))

    // White die-cut halo
    drawCircle(PeppermintWhite, radius = w * 0.34f, center = Offset(w * 0.5f, h * 0.45f))

    // Bright sky-blue pleated liner
    val liner = Path().apply {
        moveTo(w * 0.23f, h * 0.52f)
        lineTo(w * 0.77f, h * 0.52f)
        lineTo(w * 0.67f, h * 0.85f)
        lineTo(w * 0.33f, h * 0.85f)
        close()
    }
    drawPath(liner, CupcakeLinerBlue)
    for (i in 1..4) {
        val topX = w * (0.23f + i * 0.108f)
        val botX = w * (0.33f + i * 0.068f)
        drawLine(
            color = Color(0xFF0288D1),
            start = Offset(topX, h * 0.54f),
            end = Offset(botX, h * 0.83f),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )
    }
    drawPath(liner, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    // Bubblegum pink layered paper frosting
    drawRoundRect(
        color = CupcakeFrostingPink,
        topLeft = Offset(w * 0.19f, h * 0.39f),
        size = Size(w * 0.62f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(w * 0.19f, h * 0.39f),
        size = Size(w * 0.62f, h * 0.18f),
        cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
        style = Stroke(width = strokeW)
    )
    drawRoundRect(
        color = Color(0xFFFF85B3),
        topLeft = Offset(w * 0.26f, h * 0.27f),
        size = Size(w * 0.48f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(w * 0.26f, h * 0.27f),
        size = Size(w * 0.48f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = strokeW)
    )
    // Bright red cherry on top
    drawCircle(
        color = PeppermintRed,
        radius = w * 0.085f,
        center = Offset(w * 0.50f, h * 0.19f)
    )
    drawCircle(
        color = PeppermintWhite,
        radius = w * 0.03f,
        center = Offset(w * 0.48f, h * 0.17f)
    )
}

private fun DrawScope.drawWrappedCandyPiece() {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.5f
    val strokeW = size.minDimension * 0.025f

    rotate(degrees = -24f, pivot = Offset(cx, cy)) {
        val leftTail = Path().apply {
            moveTo(w * 0.28f, cy)
            lineTo(w * 0.08f, h * 0.29f)
            lineTo(w * 0.14f, cy)
            lineTo(w * 0.08f, h * 0.71f)
            close()
        }
        val rightTail = Path().apply {
            moveTo(w * 0.72f, cy)
            lineTo(w * 0.92f, h * 0.29f)
            lineTo(w * 0.86f, cy)
            lineTo(w * 0.92f, h * 0.71f)
            close()
        }
        drawPath(leftTail, WrappedCandyOrange)
        drawPath(leftTail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
        drawPath(rightTail, WrappedCandyOrange)
        drawPath(rightTail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

        drawRoundRect(
            color = Color(0x33000000),
            topLeft = Offset(w * 0.26f, h * 0.34f + w * 0.035f),
            size = Size(w * 0.48f, h * 0.35f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawRoundRect(
            color = WrappedCandyPurple,
            topLeft = Offset(w * 0.25f, h * 0.32f),
            size = Size(w * 0.50f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f)
        )
        drawLine(
            color = WrappedCandyOrange,
            start = Offset(w * 0.37f, h * 0.35f),
            end = Offset(w * 0.45f, h * 0.65f),
            strokeWidth = w * 0.055f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PeppermintWhite,
            start = Offset(w * 0.51f, h * 0.35f),
            end = Offset(w * 0.59f, h * 0.65f),
            strokeWidth = w * 0.045f,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = InkBrown,
            topLeft = Offset(w * 0.25f, h * 0.32f),
            size = Size(w * 0.50f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = Stroke(width = strokeW)
        )
    }
}

private fun DrawScope.drawCardboardStarPiece() {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val outerR = size.minDimension * 0.43f
    val innerR = outerR * 0.45f
    val shadowShift = size.minDimension * 0.04f
    val strokeW = size.minDimension * 0.025f

    fun buildStarPath(offsetX: Float, offsetY: Float, scale: Float = 1f): Path {
        val p = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2.0 + i * (PI / 5.0)
            val rad = (if (i % 2 == 0) outerR else innerR) * scale
            val x = cx + offsetX + (rad * cos(angle)).toFloat()
            val y = cy + offsetY + (rad * sin(angle)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    drawPath(buildStarPath(shadowShift, shadowShift, 1.0f), Color(0x33000000))
    val mainStar = buildStarPath(0f, 0f, 0.96f)
    drawPath(mainStar, StarCardboardGold)
    drawPath(mainStar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    for (i in 0 until 5) {
        val angle = -PI / 2.0 + i * (2.0 * PI / 5.0)
        val x = cx + (outerR * 0.90f * cos(angle)).toFloat()
        val y = cy + (outerR * 0.90f * sin(angle)).toFloat()
        drawLine(
            color = Color(0xFFD97706).copy(alpha = 0.65f),
            start = Offset(cx, cy),
            end = Offset(x, y),
            strokeWidth = size.minDimension * 0.02f
        )
    }

    val stitchStar = buildStarPath(0f, 0f, 0.68f)
    drawPath(
        path = stitchStar,
        color = PeppermintWhite,
        style = Stroke(
            width = size.minDimension * 0.024f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 5f), 0f),
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawOrigamiFoxPiece() {
    val w = size.width
    val h = size.height
    val strokeW = size.minDimension * 0.024f

    // Lush emerald origami leaf collar
    val greenLeaf = Path().apply {
        moveTo(w * 0.15f, h * 0.64f)
        lineTo(w * 0.50f, h * 0.89f)
        lineTo(w * 0.85f, h * 0.64f)
        lineTo(w * 0.70f, h * 0.48f)
        lineTo(w * 0.30f, h * 0.48f)
        close()
    }
    drawPath(greenLeaf, OrigamiFoxGreen)
    drawPath(greenLeaf, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val leftEar = Path().apply {
        moveTo(w * 0.20f, h * 0.15f)
        lineTo(w * 0.42f, h * 0.35f)
        lineTo(w * 0.17f, h * 0.48f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.80f, h * 0.15f)
        lineTo(w * 0.58f, h * 0.35f)
        lineTo(w * 0.83f, h * 0.48f)
        close()
    }
    drawPath(leftEar, Color(0xFFE04F16))
    drawPath(leftEar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightEar, Color(0xFFE04F16))
    drawPath(rightEar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val leftFace = Path().apply {
        moveTo(w * 0.50f, h * 0.32f)
        lineTo(w * 0.17f, h * 0.48f)
        lineTo(w * 0.50f, h * 0.84f)
        close()
    }
    val rightFace = Path().apply {
        moveTo(w * 0.50f, h * 0.32f)
        lineTo(w * 0.83f, h * 0.48f)
        lineTo(w * 0.50f, h * 0.84f)
        close()
    }
    drawPath(leftFace, Color(0xFFFF8552))
    drawPath(rightFace, OrigamiFoxOrange)
    drawPath(leftFace, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightFace, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val leftCheek = Path().apply {
        moveTo(w * 0.17f, h * 0.48f)
        lineTo(w * 0.36f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.84f)
        close()
    }
    val rightCheek = Path().apply {
        moveTo(w * 0.83f, h * 0.48f)
        lineTo(w * 0.64f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.84f)
        close()
    }
    drawPath(leftCheek, PeppermintWhite)
    drawPath(rightCheek, PeppermintWhite)
    drawPath(leftCheek, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightCheek, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    drawCircle(InkBrown, radius = w * 0.045f, center = Offset(w * 0.50f, h * 0.81f))
    drawCircle(InkBrown, radius = w * 0.03f, center = Offset(w * 0.38f, h * 0.49f))
    drawCircle(InkBrown, radius = w * 0.03f, center = Offset(w * 0.62f, h * 0.49f))
}

private fun DrawScope.drawOrigamiCranePiece() {
    val w = size.width
    val h = size.height
    val strokeW = size.minDimension * 0.024f

    val backWing = Path().apply {
        moveTo(w * 0.48f, h * 0.52f)
        lineTo(w * 0.78f, h * 0.16f)
        lineTo(w * 0.64f, h * 0.56f)
        close()
    }
    drawPath(backWing, OrigamiCraneBlue)
    drawPath(backWing, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val tail = Path().apply {
        moveTo(w * 0.34f, h * 0.62f)
        lineTo(w * 0.14f, h * 0.26f)
        lineTo(w * 0.24f, h * 0.68f)
        close()
    }
    drawPath(tail, OrigamiCraneBlue)
    drawPath(tail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val body = Path().apply {
        moveTo(w * 0.50f, h * 0.44f)
        lineTo(w * 0.70f, h * 0.62f)
        lineTo(w * 0.48f, h * 0.76f)
        lineTo(w * 0.24f, h * 0.68f)
        close()
    }
    drawPath(body, OrigamiCraneBlue)
    drawPath(body, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val neck = Path().apply {
        moveTo(w * 0.62f, h * 0.64f)
        lineTo(w * 0.80f, h * 0.26f)
        lineTo(w * 0.91f, h * 0.32f)
        lineTo(w * 0.79f, h * 0.36f)
        lineTo(w * 0.70f, h * 0.68f)
        close()
    }
    drawPath(neck, OrigamiCraneCyan)
    drawPath(neck, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val frontWing = Path().apply {
        moveTo(w * 0.32f, h * 0.56f)
        lineTo(w * 0.66f, h * 0.56f)
        lineTo(w * 0.44f, h * 0.88f)
        close()
    }
    drawPath(frontWing, OrigamiCraneCyan)
    drawPath(frontWing, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
}

@Composable
fun BoosterVectorIcon(
    boosterType: BoosterType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (boosterType) {
            BoosterType.SCISSORS -> {
                drawLine(
                    color = Color(0xFF90A4AE),
                    start = Offset(w * 0.32f, h * 0.65f),
                    end = Offset(w * 0.82f, h * 0.18f),
                    strokeWidth = w * 0.10f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF78909C),
                    start = Offset(w * 0.68f, h * 0.65f),
                    end = Offset(w * 0.18f, h * 0.18f),
                    strokeWidth = w * 0.10f,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = PeppermintRed,
                    radius = w * 0.17f,
                    center = Offset(w * 0.30f, h * 0.74f),
                    style = Stroke(width = w * 0.08f)
                )
                drawCircle(
                    color = PeppermintRed,
                    radius = w * 0.17f,
                    center = Offset(w * 0.70f, h * 0.74f),
                    style = Stroke(width = w * 0.08f)
                )
                drawCircle(InkBrown, radius = w * 0.05f, center = Offset(w * 0.50f, h * 0.45f))
            }

            BoosterType.GLUE -> {
                drawRoundRect(
                    color = PeppermintWhite,
                    topLeft = Offset(w * 0.26f, h * 0.38f),
                    size = Size(w * 0.48f, h * 0.50f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                )
                drawRoundRect(
                    color = InkBrown,
                    topLeft = Offset(w * 0.26f, h * 0.38f),
                    size = Size(w * 0.48f, h * 0.50f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = Stroke(width = w * 0.05f)
                )
                drawRect(
                    color = OrigamiCraneBlue,
                    topLeft = Offset(w * 0.26f, h * 0.54f),
                    size = Size(w * 0.48f, h * 0.20f)
                )
                val nozzle = Path().apply {
                    moveTo(w * 0.38f, h * 0.38f)
                    lineTo(w * 0.62f, h * 0.38f)
                    lineTo(w * 0.54f, h * 0.12f)
                    lineTo(w * 0.46f, h * 0.12f)
                    close()
                }
                drawPath(nozzle, WrappedCandyOrange)
            }

            BoosterType.HAMMER -> {
                drawLine(
                    color = CardboardDark,
                    start = Offset(w * 0.22f, h * 0.82f),
                    end = Offset(w * 0.64f, h * 0.36f),
                    strokeWidth = w * 0.12f,
                    cap = StrokeCap.Round
                )
                rotate(degrees = -42f, pivot = Offset(w * 0.62f, h * 0.34f)) {
                    drawRoundRect(
                        color = StarCardboardGold,
                        topLeft = Offset(w * 0.36f, h * 0.18f),
                        size = Size(w * 0.52f, h * 0.32f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    drawRoundRect(
                        color = InkBrown,
                        topLeft = Offset(w * 0.36f, h * 0.18f),
                        size = Size(w * 0.52f, h * 0.32f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                        style = Stroke(width = w * 0.05f)
                    )
                }
            }
        }
    }
}
