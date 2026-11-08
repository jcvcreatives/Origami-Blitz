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
    private val toothDepthPx: Float = 5f,
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
        val depth = toothDepthPx.coerceAtMost(7f)
        val stepY = h / teethCount.coerceAtLeast(2)
        val stepX = w / (teethCount * 2).coerceAtLeast(4)

        path.moveTo(depth, 0f)
        if (tearTopBottom) {
            val topSteps = (w / stepX).toInt().coerceAtLeast(2)
            for (i in 1..topSteps) {
                val x = (i * w / topSteps).coerceAtMost(w - depth)
                val y = if (i % 2 == 1) depth * 0.40f else 0f
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
                val y = if (i % 2 == 1) h - depth * 0.40f else h
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
 * Tactile 3D pressed-paper block tiles with a thick cardstock coaster base,
 * color-tinted matte face, top paper bevel highlight, and clean single-hue origami emblems
 * for effortless visual recognition and zero eye strain during long sessions.
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

/**
 * Draws a physical 3D pressed-cardstock square tile coaster with a bottom thickness lip,
 * warm matte face, inner crease frame, and top-edge highlight.
 */
private fun DrawScope.drawPaperTileCardBase(
    faceColor: Color,
    lipColor: Color,
    creaseColor: Color
) {
    val w = size.width
    val h = size.height
    val pad = w * 0.03f
    val cardW = w - pad * 2f
    val cardH = h - pad * 2f
    val lipH = h * 0.085f
    val corner = CornerRadius(w * 0.19f, w * 0.19f)
    val strokeW = w * 0.024f

    // 1. Soft ambient contact shadow inside the board slot
    drawRoundRect(
        color = Color(0x28000000),
        topLeft = Offset(pad, pad + lipH * 0.45f),
        size = Size(cardW, cardH),
        cornerRadius = corner
    )

    // 2. 3D Bottom cardstock thickness lip
    drawRoundRect(
        color = lipColor,
        topLeft = Offset(pad, pad + lipH * 0.15f),
        size = Size(cardW, cardH - lipH * 0.15f),
        cornerRadius = corner
    )

    // 3. Matte top cardstock face
    val faceH = cardH - lipH
    drawRoundRect(
        color = faceColor,
        topLeft = Offset(pad, pad),
        size = Size(cardW, faceH),
        cornerRadius = corner
    )

    // 4. Top-edge warm paper highlight bevel
    drawRoundRect(
        color = Color.White.copy(alpha = 0.65f),
        topLeft = Offset(pad + strokeW, pad + strokeW * 0.8f),
        size = Size(cardW - strokeW * 2f, faceH * 0.35f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
    )
    drawRoundRect(
        color = faceColor,
        topLeft = Offset(pad + strokeW, pad + strokeW * 2.1f),
        size = Size(cardW - strokeW * 2f, faceH * 0.35f),
        cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
    )

    // 5. Subtle debossed inner paper frame
    val inset = w * 0.10f
    drawRoundRect(
        color = creaseColor.copy(alpha = 0.38f),
        topLeft = Offset(inset, inset * 0.92f),
        size = Size(w - inset * 2f, faceH - inset * 1.35f),
        cornerRadius = CornerRadius(w * 0.13f, w * 0.13f),
        style = Stroke(width = w * 0.015f)
    )

    // 6. Outer crisp ink border around the 3D tile
    drawRoundRect(
        color = InkBrown.copy(alpha = 0.82f),
        topLeft = Offset(pad, pad),
        size = Size(cardW, cardH),
        cornerRadius = corner,
        style = Stroke(width = strokeW)
    )
}

private fun DrawScope.drawPeppermintPiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFFFF1F3),
        lipColor = Color(0xFFFECDD3),
        creaseColor = PeppermintRed
    )

    val cx = size.width * 0.5f
    val cy = size.height * 0.465f
    val r = size.minDimension * 0.30f
    val strokeW = size.minDimension * 0.024f

    // Crisp white sticker halo
    drawCircle(
        color = PeppermintWhite,
        radius = r * 1.08f,
        center = Offset(cx, cy)
    )
    // Ruby candy base
    drawCircle(
        color = PeppermintRed,
        radius = r * 0.95f,
        center = Offset(cx, cy)
    )
    // 6 clean pinwheel folds
    for (i in 0 until 6) {
        rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
            val fin = Path().apply {
                moveTo(cx, cy)
                quadraticTo(cx + r * 0.44f, cy - r * 0.24f, cx + r * 0.90f, cy - r * 0.14f)
                lineTo(cx + r * 0.72f, cy + r * 0.26f)
                close()
            }
            drawPath(
                path = fin,
                color = if (i % 2 == 0) PeppermintWhite else Color(0xFFFFD4DC)
            )
        }
    }
    drawCircle(
        color = InkBrown,
        radius = r * 0.95f,
        center = Offset(cx, cy),
        style = Stroke(width = strokeW)
    )
    drawCircle(
        color = PeppermintRed,
        radius = r * 0.24f,
        center = Offset(cx, cy)
    )
    drawCircle(
        color = PeppermintWhite,
        radius = r * 0.09f,
        center = Offset(cx - r * 0.05f, cy - r * 0.05f)
    )
}

private fun DrawScope.drawCupcakePiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFFDF2F8),
        lipColor = Color(0xFFFBCFE8),
        creaseColor = CupcakeFrostingPink
    )

    val w = size.width
    val h = size.height * 0.93f
    val strokeW = size.minDimension * 0.024f

    // Warm golden waffle-paper pleated liner (no blue to clash with the Crane!)
    val liner = Path().apply {
        moveTo(w * 0.27f, h * 0.53f)
        lineTo(w * 0.73f, h * 0.53f)
        lineTo(w * 0.64f, h * 0.82f)
        lineTo(w * 0.36f, h * 0.82f)
        close()
    }
    drawPath(liner, CupcakeLinerBlue)
    for (i in 1..3) {
        val topX = w * (0.29f + i * 0.105f)
        val botX = w * (0.37f + i * 0.065f)
        drawLine(
            color = Color(0xFFB47642),
            start = Offset(topX, h * 0.56f),
            end = Offset(botX, h * 0.79f),
            strokeWidth = w * 0.024f,
            cap = StrokeCap.Round
        )
    }
    drawPath(liner, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    // Sakura berry pink layered paper frosting
    drawRoundRect(
        color = CupcakeFrostingPink,
        topLeft = Offset(w * 0.22f, h * 0.40f),
        size = Size(w * 0.56f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(w * 0.22f, h * 0.40f),
        size = Size(w * 0.56f, h * 0.16f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
        style = Stroke(width = strokeW)
    )
    drawRoundRect(
        color = Color(0xFFF472B6),
        topLeft = Offset(w * 0.29f, h * 0.28f),
        size = Size(w * 0.42f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.075f, w * 0.075f)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(w * 0.29f, h * 0.28f),
        size = Size(w * 0.42f, h * 0.15f),
        cornerRadius = CornerRadius(w * 0.075f, w * 0.075f),
        style = Stroke(width = strokeW)
    )
    // Glossy berry pearl on top
    drawCircle(
        color = Color(0xFFBE185D),
        radius = w * 0.072f,
        center = Offset(w * 0.50f, h * 0.21f)
    )
    drawCircle(
        color = PeppermintWhite,
        radius = w * 0.025f,
        center = Offset(w * 0.48f, h * 0.19f)
    )
}

private fun DrawScope.drawWrappedCandyPiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFF5F3FF),
        lipColor = Color(0xFFDDD6FE),
        creaseColor = WrappedCandyPurple
    )

    val w = size.width
    val h = size.height * 0.93f
    val cx = w * 0.5f
    val cy = h * 0.5f
    val strokeW = size.minDimension * 0.024f

    rotate(degrees = -20f, pivot = Offset(cx, cy)) {
        val leftTail = Path().apply {
            moveTo(w * 0.30f, cy)
            lineTo(w * 0.13f, h * 0.32f)
            lineTo(w * 0.18f, cy)
            lineTo(w * 0.13f, h * 0.68f)
            close()
        }
        val rightTail = Path().apply {
            moveTo(w * 0.70f, cy)
            lineTo(w * 0.87f, h * 0.32f)
            lineTo(w * 0.82f, cy)
            lineTo(w * 0.87f, h * 0.68f)
            close()
        }
        drawPath(leftTail, WrappedCandyOrange)
        drawPath(leftTail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
        drawPath(rightTail, WrappedCandyOrange)
        drawPath(rightTail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

        drawRoundRect(
            color = WrappedCandyPurple,
            topLeft = Offset(w * 0.28f, h * 0.34f),
            size = Size(w * 0.44f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
        drawLine(
            color = WrappedCandyOrange,
            start = Offset(w * 0.39f, h * 0.37f),
            end = Offset(w * 0.46f, h * 0.63f),
            strokeWidth = w * 0.048f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = PeppermintWhite,
            start = Offset(w * 0.52f, h * 0.37f),
            end = Offset(w * 0.59f, h * 0.63f),
            strokeWidth = w * 0.040f,
            cap = StrokeCap.Round
        )
        drawRoundRect(
            color = InkBrown,
            topLeft = Offset(w * 0.28f, h * 0.34f),
            size = Size(w * 0.44f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
            style = Stroke(width = strokeW)
        )
    }
}

private fun DrawScope.drawCardboardStarPiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFFFFBEB),
        lipColor = Color(0xFFFDE68A),
        creaseColor = StarCardboardGold
    )

    val cx = size.width * 0.5f
    val cy = size.height * 0.47f
    val outerR = size.minDimension * 0.33f
    val innerR = outerR * 0.46f
    val strokeW = size.minDimension * 0.024f

    fun buildStarPath(scale: Float = 1f): Path {
        val p = Path()
        for (i in 0 until 10) {
            val angle = -PI / 2.0 + i * (PI / 5.0)
            val rad = (if (i % 2 == 0) outerR else innerR) * scale
            val x = cx + (rad * cos(angle)).toFloat()
            val y = cy + (rad * sin(angle)).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    val mainStar = buildStarPath(1.0f)
    drawPath(mainStar, StarCardboardGold)
    drawPath(mainStar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    for (i in 0 until 5) {
        val angle = -PI / 2.0 + i * (2.0 * PI / 5.0)
        val x = cx + (outerR * 0.88f * cos(angle)).toFloat()
        val y = cy + (outerR * 0.88f * sin(angle)).toFloat()
        drawLine(
            color = Color(0xFFD97706).copy(alpha = 0.55f),
            start = Offset(cx, cy),
            end = Offset(x, y),
            strokeWidth = size.minDimension * 0.018f
        )
    }

    val stitchStar = buildStarPath(0.64f)
    drawPath(
        path = stitchStar,
        color = PeppermintWhite,
        style = Stroke(
            width = size.minDimension * 0.022f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawOrigamiFoxPiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFFFF7ED),
        lipColor = Color(0xFFFED7AA),
        creaseColor = OrigamiFoxOrange
    )

    val w = size.width
    val h = size.height * 0.92f
    val strokeW = size.minDimension * 0.024f

    val leftEar = Path().apply {
        moveTo(w * 0.24f, h * 0.20f)
        lineTo(w * 0.43f, h * 0.36f)
        lineTo(w * 0.21f, h * 0.48f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.76f, h * 0.20f)
        lineTo(w * 0.57f, h * 0.36f)
        lineTo(w * 0.79f, h * 0.48f)
        close()
    }
    drawPath(leftEar, Color(0xFFD9460F))
    drawPath(leftEar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightEar, Color(0xFFD9460F))
    drawPath(rightEar, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val leftFace = Path().apply {
        moveTo(w * 0.50f, h * 0.33f)
        lineTo(w * 0.21f, h * 0.48f)
        lineTo(w * 0.50f, h * 0.81f)
        close()
    }
    val rightFace = Path().apply {
        moveTo(w * 0.50f, h * 0.33f)
        lineTo(w * 0.79f, h * 0.48f)
        lineTo(w * 0.50f, h * 0.81f)
        close()
    }
    drawPath(leftFace, Color(0xFFFB923C))
    drawPath(rightFace, OrigamiFoxOrange)
    drawPath(leftFace, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightFace, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val leftCheek = Path().apply {
        moveTo(w * 0.21f, h * 0.48f)
        lineTo(w * 0.38f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.81f)
        close()
    }
    val rightCheek = Path().apply {
        moveTo(w * 0.79f, h * 0.48f)
        lineTo(w * 0.62f, h * 0.58f)
        lineTo(w * 0.50f, h * 0.81f)
        close()
    }
    drawPath(leftCheek, PeppermintWhite)
    drawPath(rightCheek, PeppermintWhite)
    drawPath(leftCheek, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))
    drawPath(rightCheek, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    drawCircle(InkBrown, radius = w * 0.040f, center = Offset(w * 0.50f, h * 0.78f))
    drawCircle(InkBrown, radius = w * 0.026f, center = Offset(w * 0.39f, h * 0.49f))
    drawCircle(InkBrown, radius = w * 0.026f, center = Offset(w * 0.61f, h * 0.49f))
}

private fun DrawScope.drawOrigamiCranePiece() {
    drawPaperTileCardBase(
        faceColor = Color(0xFFF0F9FF),
        lipColor = Color(0xFFBAE6FD),
        creaseColor = OrigamiCraneBlue
    )

    val w = size.width
    val h = size.height * 0.92f
    val strokeW = size.minDimension * 0.024f

    val backWing = Path().apply {
        moveTo(w * 0.48f, h * 0.51f)
        lineTo(w * 0.74f, h * 0.20f)
        lineTo(w * 0.62f, h * 0.55f)
        close()
    }
    drawPath(backWing, OrigamiCraneBlue)
    drawPath(backWing, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val tail = Path().apply {
        moveTo(w * 0.36f, h * 0.60f)
        lineTo(w * 0.19f, h * 0.28f)
        lineTo(w * 0.27f, h * 0.65f)
        close()
    }
    drawPath(tail, OrigamiCraneBlue)
    drawPath(tail, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val body = Path().apply {
        moveTo(w * 0.50f, h * 0.44f)
        lineTo(w * 0.68f, h * 0.60f)
        lineTo(w * 0.48f, h * 0.72f)
        lineTo(w * 0.27f, h * 0.65f)
        close()
    }
    drawPath(body, OrigamiCraneBlue)
    drawPath(body, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val neck = Path().apply {
        moveTo(w * 0.60f, h * 0.62f)
        lineTo(w * 0.76f, h * 0.29f)
        lineTo(w * 0.86f, h * 0.34f)
        lineTo(w * 0.75f, h * 0.38f)
        lineTo(w * 0.68f, h * 0.65f)
        close()
    }
    drawPath(neck, OrigamiCraneCyan)
    drawPath(neck, InkBrown, style = Stroke(width = strokeW, join = StrokeJoin.Round))

    val frontWing = Path().apply {
        moveTo(w * 0.34f, h * 0.55f)
        lineTo(w * 0.64f, h * 0.55f)
        lineTo(w * 0.45f, h * 0.82f)
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
                drawPath(nozzle, StarCardboardGold)
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
