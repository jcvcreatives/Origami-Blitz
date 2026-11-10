package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.model.ConfettiDepthLayer
import com.example.model.ConfettiScrap
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.CraftTableKraft
import com.example.ui.theme.CupcakeFrostingPink
import com.example.ui.theme.FarHillGreen
import com.example.ui.theme.InkBrown
import com.example.ui.theme.NearHillGreen
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.WrappedCandyPurple
import com.example.ui.theme.SkyBluePaper
import com.example.ui.theme.SkyHorizonPaper
import com.example.ui.theme.StarCardboardGold
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stage-reactive palette and theme configuration for the multi-layer paper diorama backdrop.
 */
data class DioramaStageTheme(
    val stageNumber: Int,
    val skyColor: Color,
    val cloudColor: Color,
    val sunOuterColor: Color,
    val sunInnerColor: Color,
    val farHillColor: Color,
    val nearHillColor: Color,
    val tableColor: Color,
    val accentPrimary: Color,
    val accentSecondary: Color
)

fun getDioramaStageTheme(stageNumber: Int): DioramaStageTheme {
    return when (((stageNumber - 1).coerceIn(0, 9)) + 1) {
        1 -> DioramaStageTheme(
            stageNumber = 1,
            skyColor = SkyBluePaper,
            cloudColor = SkyHorizonPaper,
            sunOuterColor = Color(0xFFFFEA79),
            sunInnerColor = StarCardboardGold,
            farHillColor = FarHillGreen,
            nearHillColor = NearHillGreen,
            tableColor = CraftTableKraft,
            accentPrimary = Color(0xFF2EC4B6),
            accentSecondary = Color(0xFF0F9F90)
        )
        2 -> DioramaStageTheme(
            stageNumber = 2,
            skyColor = Color(0xFFFFD6E0),
            cloudColor = Color(0xFFFFF0F5),
            sunOuterColor = Color(0xFFFFFDF7),
            sunInnerColor = PeppermintRed,
            farHillColor = Color(0xFF80ED99),
            nearHillColor = Color(0xFF57CC99),
            tableColor = Color(0xFFF9EAE1),
            accentPrimary = PeppermintRed,
            accentSecondary = PeppermintWhite
        )
        3 -> DioramaStageTheme(
            stageNumber = 3,
            skyColor = Color(0xFFFDE4CF),
            cloudColor = Color(0xFFFFF5EB),
            sunOuterColor = Color(0xFFFFEA79),
            sunInnerColor = Color(0xFFD97706),
            farHillColor = Color(0xFFE09F67),
            nearHillColor = Color(0xFFC67D4E),
            tableColor = Color(0xFFF4E2C6),
            accentPrimary = StarCardboardGold,
            accentSecondary = Color(0xFFB45309)
        )
        4 -> DioramaStageTheme(
            stageNumber = 4,
            skyColor = Color(0xFFC7D2FE),
            cloudColor = Color(0xFFE0E7FF),
            sunOuterColor = Color(0xFFFEF08A),
            sunInnerColor = Color(0xFFFACC15),
            farHillColor = Color(0xFF64B5F6),
            nearHillColor = Color(0xFF26A69A),
            tableColor = Color(0xFFEBF3F5),
            accentPrimary = OrigamiCraneBlue,
            accentSecondary = PeppermintWhite
        )
        5 -> DioramaStageTheme(
            stageNumber = 5,
            skyColor = Color(0xFFFBCFE8),
            cloudColor = Color(0xFFFCE7F3),
            sunOuterColor = Color(0xFFFDE68A),
            sunInnerColor = Color(0xFFF472B6),
            farHillColor = Color(0xFF86EFAC),
            nearHillColor = Color(0xFF4ADE80),
            tableColor = Color(0xFFFDF2F8),
            accentPrimary = Color(0xFFF472B6),
            accentSecondary = Color(0xFFEC4899)
        )
        6 -> DioramaStageTheme(
            stageNumber = 6,
            skyColor = Color(0xFFA5F3FC),
            cloudColor = Color(0xFFECFEFF),
            sunOuterColor = Color(0xFFFEF08A),
            sunInnerColor = Color(0xFF0284C7),
            farHillColor = Color(0xFF38BDF8),
            nearHillColor = Color(0xFF0284C7),
            tableColor = Color(0xFFF5EBE0),
            accentPrimary = PeppermintRed,
            accentSecondary = PeppermintWhite
        )
        7 -> DioramaStageTheme(
            stageNumber = 7,
            skyColor = Color(0xFFF5D0FE),
            cloudColor = Color(0xFFFAE8FF),
            sunOuterColor = Color(0xFFFDE047),
            sunInnerColor = CupcakeFrostingPink,
            farHillColor = Color(0xFFF9A8D4),
            nearHillColor = Color(0xFFEC4899),
            tableColor = Color(0xFFFFF1F2),
            accentPrimary = CupcakeFrostingPink,
            accentSecondary = PeppermintWhite
        )
        8 -> DioramaStageTheme(
            stageNumber = 8,
            skyColor = Color(0xFFDDD6FE),
            cloudColor = Color(0xFFEDE9FE),
            sunOuterColor = Color(0xFFFDE68A),
            sunInnerColor = WrappedCandyPurple,
            farHillColor = Color(0xFFA78BFA),
            nearHillColor = Color(0xFF7C3AED),
            tableColor = Color(0xFFF3E8FF),
            accentPrimary = WrappedCandyPurple,
            accentSecondary = Color(0xFFF472B6)
        )
        9 -> DioramaStageTheme(
            stageNumber = 9,
            skyColor = Color(0xFFFED7AA),
            cloudColor = Color(0xFFFFEDD5),
            sunOuterColor = Color(0xFFFEF08A),
            sunInnerColor = OrigamiFoxOrange,
            farHillColor = Color(0xFFFBBF24),
            nearHillColor = Color(0xFFD97706),
            tableColor = Color(0xFFFEF3C7),
            accentPrimary = PeppermintRed,
            accentSecondary = OrigamiCraneBlue
        )
        else -> DioramaStageTheme(
            stageNumber = 10,
            skyColor = Color(0xFFFECDD3),
            cloudColor = Color(0xFFFFE4E6),
            sunOuterColor = Color(0xFFFDE047),
            sunInnerColor = Color(0xFFE11D48),
            farHillColor = Color(0xFF34D399),
            nearHillColor = Color(0xFF059669),
            tableColor = Color(0xFFFEF9C3),
            accentPrimary = StarCardboardGold,
            accentSecondary = Color(0xFFE11D48)
        )
    }
}

/**
 * Multi-layered paper diorama backdrop with stage-reactive colors and props, tilt-driven
 * parallax offsets, dynamic drop-shadow shifts, and depth-interleaved confetti shreds.
 */
@Composable
fun PaperDioramaBackdrop(
    tilt: DeviceTilt = DeviceTilt(),
    scraps: List<ConfettiScrap> = emptyList(),
    stageNumber: Int = 1,
    modifier: Modifier = Modifier
) {
    val theme = getDioramaStageTheme(stageNumber)
    val sunTransition = rememberInfiniteTransition(label = "origami_sky_sun_transition")
    val sunSpinDeg by sunTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "origami_sky_sun_spin"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxShiftX = w * 0.010f
        val maxShiftY = h * 0.007f

        // Shadow vector moves subtly opposite to the light source tilt
        val shadowShiftX = -tilt.x * 3f + 3f
        val shadowShiftY = -tilt.y * 3f + 4f

        // 1. Stage-themed sky paper base
        drawRect(color = theme.skyColor)

        // 2. Torn cloud-white top sky layer (Parallax depth: 0.25x)
        withTransform({
            translate(left = tilt.x * maxShiftX * 0.25f, top = tilt.y * maxShiftY * 0.2f)
        }) {
            val cloudStrip = Path().apply {
                moveTo(-w * 0.05f, -h * 0.03f)
                lineTo(w * 1.05f, -h * 0.03f)
                lineTo(w * 1.05f, h * 0.22f)
                val steps = 16
                for (i in steps downTo 0) {
                    val x = -w * 0.05f + (w * 1.1f) * (i.toFloat() / steps)
                    val jag = if (i % 2 == 0) h * 0.008f else -h * 0.008f
                    lineTo(x, h * 0.21f + jag)
                }
                close()
            }
            drawPath(
                path = cloudStrip,
                color = Color(0x18000000)
            )
            drawPath(cloudStrip, theme.cloudColor)
        }

        // 3. Stage-specific origami celestial body with circling rays & orbital motion (Parallax depth: 0.40x)
        val orbitRad = (sunSpinDeg * (Math.PI / 180.0)).toFloat()
        val sunCenter = Offset(
            x = w * 0.84f + cos(orbitRad) * (w * 0.012f) + tilt.x * maxShiftX * 0.40f,
            y = h * 0.135f + sin(orbitRad) * (h * 0.008f) + tilt.y * maxShiftY * 0.40f
        )
        drawStageCelestialBody(
            theme = theme,
            center = sunCenter,
            radius = w * 0.075f,
            shadowOffset = Offset(shadowShiftX, shadowShiftY),
            spinDegrees = sunSpinDeg
        )

        // 4. Far paper hill + stage-specific landmarks + FAR_HILL confetti shreds (Parallax depth: 0.60x)
        withTransform({
            translate(left = tilt.x * maxShiftX * 0.60f, top = tilt.y * maxShiftY * 0.55f)
        }) {
            val farHillShadow = buildTornWavePath(
                w = w,
                h = h,
                baseY = h * 0.58f,
                amp = h * 0.04f,
                offsetY = shadowShiftY - 2f
            )
            drawPath(farHillShadow, Color(0x1E000000))

            val farHillTear = buildTornWavePath(w, h, baseY = h * 0.58f, amp = h * 0.04f, offsetY = -4f)
            drawPath(farHillTear, PeppermintWhite.copy(alpha = 0.92f))
            val farHill = buildTornWavePath(w, h, baseY = h * 0.58f, amp = h * 0.04f, offsetY = 0f)
            drawPath(farHill, theme.farHillColor)

            drawStageFarHillProps(
                theme = theme,
                w = w,
                h = h,
                shadowShiftX = shadowShiftX
            )

            // Confetti shreds that have settled or descended into the Far Hill layer
            scraps.forEach { scrap ->
                if (scrap.depthLayer == ConfettiDepthLayer.FAR_HILL && isScrapInBackdropPlane(scrap)) {
                    drawConfettiScrapWithDepth(
                        scrap = scrap,
                        w = w,
                        h = h,
                        shadowShiftX = shadowShiftX * 0.35f,
                        shadowShiftY = shadowShiftY * 0.35f
                    )
                }
            }
        }

        // 5. Near meadow paper hill + origami critters & stage props + NEAR_HILL confetti shreds (Parallax depth: 0.90x)
        withTransform({
            translate(left = tilt.x * maxShiftX * 0.90f, top = tilt.y * maxShiftY * 0.85f)
        }) {
            val nearHillShadow = buildTornWavePath(
                w = w,
                h = h,
                baseY = h * 0.70f,
                amp = -h * 0.035f,
                offsetY = shadowShiftY - 2f
            )
            drawPath(nearHillShadow, Color(0x22000000))

            val nearHillTear = buildTornWavePath(w, h, baseY = h * 0.70f, amp = -h * 0.035f, offsetY = -4f)
            drawPath(nearHillTear, PeppermintWhite.copy(alpha = 0.92f))
            val nearHill = buildTornWavePath(w, h, baseY = h * 0.70f, amp = -h * 0.035f, offsetY = 0f)
            drawPath(nearHill, theme.nearHillColor)

            drawStageNearHillProps(
                theme = theme,
                w = theme.let { w },
                h = h
            )

            // Confetti shreds that have settled or descended into the Near Meadow Hill layer
            scraps.forEach { scrap ->
                if (scrap.depthLayer == ConfettiDepthLayer.NEAR_HILL && isScrapInBackdropPlane(scrap)) {
                    drawConfettiScrapWithDepth(
                        scrap = scrap,
                        w = w,
                        h = h,
                        shadowShiftX = shadowShiftX * 0.50f,
                        shadowShiftY = shadowShiftY * 0.50f
                    )
                }
            }
        }

        // 6. Stage-tinted Kraft craft-table foreground mat + landed CRAFT_TABLE confetti shreds (Parallax depth: 1.15x)
        withTransform({
            translate(left = tilt.x * maxShiftX * 1.15f, top = tilt.y * maxShiftY * 1.10f)
        }) {
            val tableTear = buildTornWavePath(w, h, baseY = h * 0.84f, amp = h * 0.012f, offsetY = -4f)
            drawPath(tableTear, PeppermintWhite.copy(alpha = 0.88f))
            val tableMat = buildTornWavePath(w, h, baseY = h * 0.84f, amp = h * 0.012f, offsetY = 0f)
            drawPath(tableMat, theme.tableColor)

            // Confetti shreds resting on the foreground Kraft craft table
            scraps.forEach { scrap ->
                if (scrap.depthLayer == ConfettiDepthLayer.CRAFT_TABLE && isScrapInBackdropPlane(scrap)) {
                    drawConfettiScrapWithDepth(
                        scrap = scrap,
                        w = w,
                        h = h,
                        shadowShiftX = shadowShiftX * 0.65f,
                        shadowShiftY = shadowShiftY * 0.65f
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStageCelestialBody(
    theme: DioramaStageTheme,
    center: Offset,
    radius: Float,
    shadowOffset: Offset,
    spinDegrees: Float = 0f
) {
    // 1. Outer Ring of 12 Folded Origami Triangular Sun Rays (rotating clockwise)
    val rayCount = 12
    rotate(degrees = spinDegrees, pivot = center) {
        for (i in 0 until rayCount) {
            val angleDeg = i * (360f / rayCount)
            rotate(degrees = angleDeg, pivot = center) {
                val baseRadius = radius * 0.86f
                val tipRadius = radius * 1.48f
                val halfSpread = radius * 0.22f

                // Drop shadow for folded ray
                val shadowRay = Path().apply {
                    moveTo(center.x - halfSpread + shadowOffset.x * 0.6f, center.y - baseRadius + shadowOffset.y * 0.6f)
                    lineTo(center.x + shadowOffset.x * 0.6f, center.y - tipRadius + shadowOffset.y * 0.6f)
                    lineTo(center.x + halfSpread + shadowOffset.x * 0.6f, center.y - baseRadius + shadowOffset.y * 0.6f)
                    close()
                }
                drawPath(shadowRay, Color(0x1C000000))

                // Left sunlit origami facet
                val leftFacet = Path().apply {
                    moveTo(center.x - halfSpread, center.y - baseRadius)
                    lineTo(center.x, center.y - tipRadius)
                    lineTo(center.x, center.y - baseRadius)
                    close()
                }
                drawPath(leftFacet, theme.sunOuterColor)

                // Right folded shaded origami facet
                val rightFacet = Path().apply {
                    moveTo(center.x, center.y - baseRadius)
                    lineTo(center.x, center.y - tipRadius)
                    lineTo(center.x + halfSpread, center.y - baseRadius)
                    close()
                }
                drawPath(rightFacet, theme.sunInnerColor.copy(alpha = 0.88f))

                // Center origami crease & edge outline
                drawLine(
                    color = InkBrown.copy(alpha = 0.35f),
                    start = Offset(center.x, center.y - baseRadius),
                    end = Offset(center.x, center.y - tipRadius),
                    strokeWidth = 1.4f
                )
            }
        }
    }

    // 2. Inner Counter-Rotating Ring of 8 Secondary Folded Paper Rays + Orbiting Origami Sparks
    rotate(degrees = -spinDegrees * 0.75f, pivot = center) {
        for (i in 0 until 8) {
            val angleDeg = i * 45f + 22.5f
            rotate(degrees = angleDeg, pivot = center) {
                val innerBase = radius * 0.88f
                val innerTip = radius * 1.28f
                val innerSpread = radius * 0.16f
                val secondaryRay = Path().apply {
                    moveTo(center.x - innerSpread, center.y - innerBase)
                    lineTo(center.x, center.y - innerTip)
                    lineTo(center.x + innerSpread, center.y - innerBase)
                    close()
                }
                drawPath(secondaryRay, PeppermintWhite.copy(alpha = 0.85f))

                // Orbiting paper diamond spark beyond the ray tip
                val sparkCenterY = center.y - radius * 1.62f
                val sparkR = radius * 0.09f
                val sparkDiamond = Path().apply {
                    moveTo(center.x, sparkCenterY - sparkR)
                    lineTo(center.x + sparkR, sparkCenterY)
                    lineTo(center.x, sparkCenterY + sparkR)
                    lineTo(center.x - sparkR, sparkCenterY)
                    close()
                }
                drawPath(sparkDiamond, theme.sunOuterColor)
            }
        }
    }

    // 3. Core Origami Sun Disc & Stage Motif
    drawCircle(
        color = Color(0x24000000),
        radius = radius,
        center = center + shadowOffset
    )
    drawCircle(theme.sunOuterColor, radius = radius, center = center)

    when (theme.stageNumber) {
        2 -> {
            // Peppermint Swirl Sun
            drawCircle(theme.sunInnerColor, radius = radius * 0.72f, center = center)
            for (i in 0 until 6) {
                val angle = (i * 60f) * (Math.PI / 180.0)
                val end = Offset(
                    x = center.x + cos(angle).toFloat() * radius * 0.70f,
                    y = center.y + sin(angle).toFloat() * radius * 0.70f
                )
                drawLine(PeppermintWhite, start = center, end = end, strokeWidth = radius * 0.16f)
            }
            drawCircle(PeppermintWhite, radius = radius * 0.22f, center = center)
        }
        3, 10 -> {
            // 8-point Origami Star Sun / Royal Crown Sunburst
            val starPath = Path().apply {
                for (i in 0 until 16) {
                    val r = if (i % 2 == 0) radius * 0.86f else radius * 0.45f
                    val a = i * (Math.PI / 8.0) - Math.PI / 2.0
                    val px = center.x + cos(a).toFloat() * r
                    val py = center.y + sin(a).toFloat() * r
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
                close()
            }
            drawPath(starPath, theme.sunInnerColor)
            if (theme.stageNumber == 10) {
                drawCircle(StarCardboardGold, radius = radius * 0.36f, center = center)
            }
        }
        4 -> {
            // Crescent Paper Moon + Star
            drawCircle(theme.sunInnerColor, radius = radius * 0.76f, center = center)
            drawCircle(
                color = theme.skyColor,
                radius = radius * 0.56f,
                center = center + Offset(radius * 0.28f, -radius * 0.14f)
            )
        }
        8 -> {
            // Golden Button Sun with 4 Stitch Holes
            drawCircle(theme.sunInnerColor, radius = radius * 0.70f, center = center)
            val holeOff = radius * 0.24f
            drawCircle(PeppermintWhite, radius = radius * 0.09f, center = center + Offset(-holeOff, -holeOff))
            drawCircle(PeppermintWhite, radius = radius * 0.09f, center = center + Offset(holeOff, -holeOff))
            drawCircle(PeppermintWhite, radius = radius * 0.09f, center = center + Offset(-holeOff, holeOff))
            drawCircle(PeppermintWhite, radius = radius * 0.09f, center = center + Offset(holeOff, holeOff))
        }
        else -> {
            drawCircle(theme.sunInnerColor, radius = radius * 0.72f, center = center)
            // Subtle origami octagonal fold creases inside the sun core
            rotate(degrees = spinDegrees * 0.35f, pivot = center) {
                for (i in 0 until 8) {
                    val rad = (i * 45f) * (Math.PI / 180.0)
                    drawLine(
                        color = theme.sunOuterColor.copy(alpha = 0.55f),
                        start = center,
                        end = Offset(
                            x = center.x + cos(rad).toFloat() * radius * 0.70f,
                            y = center.y + sin(rad).toFloat() * radius * 0.70f
                        ),
                        strokeWidth = 1.5f
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawStageFarHillProps(
    theme: DioramaStageTheme,
    w: Float,
    h: Float,
    shadowShiftX: Float
) {
    val leftX = w * 0.11f
    val leftY = h * 0.60f
    val rightX = w * 0.89f
    val rightY = h * 0.58f
    val scaleL = w * 0.085f
    val scaleR = w * 0.095f

    when (theme.stageNumber) {
        2 -> {
            // Giant Peppermint Lollipop Trees
            drawPeppermintLollipopTree(leftX, leftY, scaleL)
            drawPeppermintLollipopTree(rightX, rightY, scaleR)
        }
        3 -> {
            // Cardboard Canyon Mesas & Star Totems
            drawCardboardMesa(leftX, leftY, scaleL)
            drawCardboardMesa(rightX, rightY, scaleR)
        }
        6 -> {
            // Striped Harbor Lighthouse on right, Coastal Pine on left
            drawPaperPineTree(leftX, leftY, scaleL, shadowShiftX, theme.accentPrimary, theme.accentSecondary)
            drawPaperLighthouse(rightX, rightY, scaleR)
        }
        7 -> {
            // Giant Cupcake Summit Towers
            drawGiantPaperCupcake(leftX, leftY, scaleL)
            drawGiantPaperCupcake(rightX, rightY, scaleR)
        }
        8 -> {
            // Silk Thread Spool Pillars
            drawThreadSpoolPillar(leftX, leftY, scaleL, theme.accentPrimary)
            drawThreadSpoolPillar(rightX, rightY, scaleR, theme.accentSecondary)
        }
        10 -> {
            // Twin Royal Origami Castle Turrets
            drawRoyalCastleTurret(leftX, leftY, scaleL)
            drawRoyalCastleTurret(rightX, rightY, scaleR)
        }
        else -> {
            // Stage-tinted Layered Origami Trees (Meadow Teal, Sakura Pink, Sunburst Gold, etc.)
            drawPaperPineTree(
                cx = leftX,
                baseY = leftY,
                scale = scaleL,
                shadowShiftX = shadowShiftX,
                leftColor = theme.accentPrimary,
                rightColor = theme.accentSecondary
            )
            drawPaperPineTree(
                cx = rightX,
                baseY = rightY,
                scale = scaleR,
                shadowShiftX = shadowShiftX,
                leftColor = theme.accentPrimary,
                rightColor = theme.accentSecondary
            )
        }
    }
}

private fun DrawScope.drawStageNearHillProps(
    theme: DioramaStageTheme,
    w: Float,
    h: Float
) {
    val sizePx = w.coerceAtMost(h) * 0.12f
    val leftX = w * 0.11f
    val leftY = h * 0.71f
    val rightX = w * 0.89f
    val rightY = h * 0.70f

    drawPeekingOrigamiBear(
        cx = leftX,
        cy = leftY,
        sizePx = sizePx,
        hasCrown = theme.stageNumber == 10,
        hasCupcakeHat = theme.stageNumber == 7 || theme.stageNumber == 2
    )
    drawPeekingOrigamiSquirrel(
        cx = rightX,
        cy = rightY,
        sizePx = sizePx,
        hasBow = theme.stageNumber == 8 || theme.stageNumber == 5
    )

    // Extra stage prop badge near the center-left/right hill crest
    when (theme.stageNumber) {
        4, 6 -> {
            // Paper Boat on the creek/harbor crest
            val boatCx = w * 0.24f
            val boatCy = h * 0.695f
            val r = sizePx * 0.36f
            val hull = Path().apply {
                moveTo(boatCx - r, boatCy)
                lineTo(boatCx + r, boatCy)
                lineTo(boatCx + r * 0.65f, boatCy + r * 0.45f)
                lineTo(boatCx - r * 0.65f, boatCy + r * 0.45f)
                close()
            }
            val sail = Path().apply {
                moveTo(boatCx, boatCy - r * 0.85f)
                lineTo(boatCx + r * 0.55f, boatCy)
                lineTo(boatCx - r * 0.55f, boatCy)
                close()
            }
            drawPath(sail, PeppermintWhite)
            drawPath(hull, OrigamiCraneBlue)
        }
        7, 2 -> {
            // Frosted Paper Cupcake perched beside the Bear
            drawMiniHillCupcake(cx = w * 0.23f, cy = h * 0.70f, r = sizePx * 0.30f)
        }
        3, 9, 10 -> {
            // Golden Origami Star perched on the hill crest
            drawMiniHillStar(cx = w * 0.23f, cy = h * 0.70f, r = sizePx * 0.30f)
        }
    }
}

/**
 * Determines whether a scrap has transitioned from the foreground board burst into its
 * assigned diorama landscape layer (either landed, bounced on the contour, or falling onto the slope).
 */
private fun isScrapInBackdropPlane(scrap: ConfettiScrap): Boolean {
    if (scrap.isLanded || scrap.bounceCount > 0) return true
    return when (scrap.depthLayer) {
        ConfettiDepthLayer.FAR_HILL -> scrap.vy > 0f && scrap.y >= 0.46f
        ConfettiDepthLayer.NEAR_HILL -> scrap.vy > 0f && scrap.y >= 0.58f
        ConfettiDepthLayer.CRAFT_TABLE -> false // Craft table scraps stay in foreground overlay until they land
    }
}

/**
 * Full-screen confetti shred overlay for airborne particles bursting out of the cardboard board,
 * with depth-scaled perspective, 3D paper flutter, and tilt-reactive parallax.
 */
@Composable
fun ConfettiShredOverlay(
    scraps: List<ConfettiScrap>,
    tilt: DeviceTilt = DeviceTilt(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxShiftX = w * 0.010f
        val maxShiftY = h * 0.007f
        val shadowShiftX = -tilt.x * 3f + 2.5f
        val shadowShiftY = -tilt.y * 3f + 3.5f

        // Render airborne particles ordered back-to-front by depth layer
        for (layer in ConfettiDepthLayer.entries) {
            scraps.forEach { scrap ->
                if (scrap.depthLayer == layer && !isScrapInBackdropPlane(scrap)) {
                    withTransform({
                        translate(
                            left = tilt.x * maxShiftX * layer.parallaxFactorX,
                            top = tilt.y * maxShiftY * layer.parallaxFactorY
                        )
                    }) {
                        drawConfettiScrapWithDepth(
                            scrap = scrap,
                            w = w,
                            h = h,
                            shadowShiftX = shadowShiftX * scrap.depthScale,
                            shadowShiftY = shadowShiftY * scrap.depthScale
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawConfettiScrapWithDepth(
    scrap: ConfettiScrap,
    w: Float,
    h: Float,
    shadowShiftX: Float,
    shadowShiftY: Float
) {
    val px = scrap.x * w
    val py = scrap.y * h
    val depthSc = scrap.depthScale
    val sw = scrap.widthDp * density * depthSc
    val sh = scrap.heightDp * density * depthSc
    // 3D paper tumble foreshortening in mid-air; flat perspective when resting on a hill/table
    val flutterScaleY = if (scrap.isLanded) {
        0.78f
    } else {
        0.34f + 0.66f * abs(cos(scrap.flutterPhase))
    }
    val alpha = scrap.alpha.coerceIn(0f, 1f)
    val shadowMult = if (scrap.isLanded) 0.35f else 0.85f
    val shapeVariant = ((scrap.id % 4L) + 4L).toInt() % 4

    withTransform({
        translate(left = px, top = py)
        rotate(degrees = scrap.rotation, pivot = Offset.Zero)
        scale(scaleX = 1f, scaleY = flutterScaleY, pivot = Offset.Zero)
    }) {
        val sx = shadowShiftX * shadowMult
        val sy = shadowShiftY * shadowMult
        val shadowAlpha = (alpha * 0.24f).coerceIn(0f, 1f)
        val mainColor = scrap.color.copy(alpha = alpha)
        val foldColor = scrap.foldColor.copy(alpha = (alpha * 0.92f).coerceIn(0f, 1f))
        val creaseColor = PeppermintWhite.copy(alpha = (alpha * 0.82f).coerceIn(0f, 1f))

        when (shapeVariant) {
            0 -> {
                // 0: Folded Origami Paper Dart / Crane Wing (two-tone wings + center valley fold)
                val shadowDart = Path().apply {
                    moveTo(sw * 0.65f + sx, sy)
                    lineTo(-sw * 0.55f + sx, -sh * 0.62f + sy)
                    lineTo(-sw * 0.32f + sx, sy)
                    lineTo(-sw * 0.55f + sx, sh * 0.62f + sy)
                    close()
                }
                drawPath(shadowDart, Color.Black.copy(alpha = shadowAlpha))

                val topWing = Path().apply {
                    moveTo(sw * 0.65f, 0f)
                    lineTo(-sw * 0.55f, -sh * 0.62f)
                    lineTo(-sw * 0.32f, 0f)
                    close()
                }
                val botWing = Path().apply {
                    moveTo(sw * 0.65f, 0f)
                    lineTo(-sw * 0.32f, 0f)
                    lineTo(-sw * 0.55f, sh * 0.62f)
                    close()
                }
                drawPath(topWing, mainColor)
                drawPath(botWing, foldColor)
                drawLine(
                    color = creaseColor,
                    start = Offset(-sw * 0.32f, 0f),
                    end = Offset(sw * 0.65f, 0f),
                    strokeWidth = 1.4f
                )
            }
            1 -> {
                // 1: V-Notched Washi Masking-Tape Strip with center fold-stitch
                val shadowTape = Path().apply {
                    moveTo(-sw * 0.62f + sx, -sh * 0.42f + sy)
                    lineTo(sw * 0.62f + sx, -sh * 0.42f + sy)
                    lineTo(sw * 0.42f + sx, sy)
                    lineTo(sw * 0.62f + sx, sh * 0.42f + sy)
                    lineTo(-sw * 0.62f + sx, sh * 0.42f + sy)
                    lineTo(-sw * 0.42f + sx, sy)
                    close()
                }
                drawPath(shadowTape, Color.Black.copy(alpha = shadowAlpha))

                val tapePath = Path().apply {
                    moveTo(-sw * 0.62f, -sh * 0.42f)
                    lineTo(sw * 0.62f, -sh * 0.42f)
                    lineTo(sw * 0.42f, 0f)
                    lineTo(sw * 0.62f, sh * 0.42f)
                    lineTo(-sw * 0.62f, sh * 0.42f)
                    lineTo(-sw * 0.42f, 0f)
                    close()
                }
                drawPath(tapePath, mainColor)
                drawLine(
                    color = foldColor,
                    start = Offset(-sw * 0.36f, 0f),
                    end = Offset(sw * 0.36f, 0f),
                    strokeWidth = 1.6f
                )
            }
            2 -> {
                // 2: 4-Point Origami Paper Pinwheel Star
                val rOuter = maxOf(sw, sh) * 0.58f
                val rInner = rOuter * 0.38f
                val starShadow = Path().apply {
                    moveTo(sx, -rOuter + sy)
                    lineTo(rInner + sx, -rInner + sy)
                    lineTo(rOuter + sx, sy)
                    lineTo(rInner + sx, rInner + sy)
                    lineTo(sx, rOuter + sy)
                    lineTo(-rInner + sx, rInner + sy)
                    lineTo(-rOuter + sx, sy)
                    lineTo(-rInner + sx, -rInner + sy)
                    close()
                }
                drawPath(starShadow, Color.Black.copy(alpha = shadowAlpha))

                val starBody = Path().apply {
                    moveTo(0f, -rOuter)
                    lineTo(rInner, -rInner)
                    lineTo(rOuter, 0f)
                    lineTo(rInner, rInner)
                    lineTo(0f, rOuter)
                    lineTo(-rInner, rInner)
                    lineTo(-rOuter, 0f)
                    lineTo(-rInner, -rInner)
                    close()
                }
                drawPath(starBody, mainColor)
                drawCircle(color = foldColor, radius = rInner * 0.55f, center = Offset.Zero)
            }
            else -> {
                // 3: Two-Tone Creased Origami Diamond Square
                val shadowDiamond = Path().apply {
                    moveTo(sx, -sh * 0.58f + sy)
                    lineTo(sw * 0.58f + sx, sy)
                    lineTo(sx, sh * 0.58f + sy)
                    lineTo(-sw * 0.58f + sx, sy)
                    close()
                }
                drawPath(shadowDiamond, Color.Black.copy(alpha = shadowAlpha))

                val leftFacet = Path().apply {
                    moveTo(0f, -sh * 0.58f)
                    lineTo(0f, sh * 0.58f)
                    lineTo(-sw * 0.58f, 0f)
                    close()
                }
                val rightFacet = Path().apply {
                    moveTo(0f, -sh * 0.58f)
                    lineTo(sw * 0.58f, 0f)
                    lineTo(0f, sh * 0.58f)
                    close()
                }
                drawPath(leftFacet, mainColor)
                drawPath(rightFacet, foldColor)
                drawLine(
                    color = creaseColor,
                    start = Offset(0f, -sh * 0.58f),
                    end = Offset(0f, sh * 0.58f),
                    strokeWidth = 1.4f
                )
            }
        }
    }
}

/**
 * Subtle global lighting overlay that shifts with the device's tilt, casting a warm
 * directional paper-sheen highlight on the tilted side and a soft ambient depth gradient
 * on the opposite side to emphasize the 3D layered paper diorama.
 */
@Composable
fun GlobalPaperLightingOverlay(
    tilt: DeviceTilt,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxDim = size.maxDimension.coerceAtLeast(1f)

        // Focal point of the overhead craft-lamp shifts gently with device tilt
        val lightCenter = Offset(
            x = w * (0.50f + tilt.x * 0.12f),
            y = h * (0.38f + tilt.y * 0.10f)
        )

        // 1. Soft, steady warm directional paper-sheen highlight
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0x1EFFFDF5),
                    Color(0x0CFFF8E1),
                    Color.Transparent
                ),
                center = lightCenter,
                radius = maxDim * 0.78f
            )
        )

        // 2. Directional linear sheen & opposite-edge depth shadow
        val startEdge = Offset(
            x = w * (0.5f + tilt.x * 0.5f),
            y = h * (0.5f + tilt.y * 0.5f)
        )
        val endEdge = Offset(
            x = w * (0.5f - tilt.x * 0.5f),
            y = h * (0.5f - tilt.y * 0.5f)
        )
        if ((startEdge - endEdge).getDistanceSquared() > 4f) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x1EFFFFFF),
                        Color.Transparent,
                        Color(0x222B180A)
                    ),
                    start = startEdge,
                    end = endEdge
                )
            )
        }
    }
}

private fun buildTornWavePath(
    w: Float,
    h: Float,
    baseY: Float,
    amp: Float,
    offsetY: Float
): Path {
    return Path().apply {
        val steps = 26
        val extraW = w * 0.08f
        moveTo(-extraW, baseY + offsetY)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            val x = -extraW + t * (w + extraW * 2f)
            val wave = kotlin.math.sin(t * Math.PI * 2.0).toFloat() * amp
            val tearTooth = if (i % 2 == 0) 3.5f else -3.5f
            lineTo(x, baseY + wave + tearTooth + offsetY)
        }
        lineTo(w + extraW, h + 40f)
        lineTo(-extraW, h + 40f)
        close()
    }
}

private fun DrawScope.drawPaperPineTree(
    cx: Float,
    baseY: Float,
    scale: Float,
    shadowShiftX: Float = 3f,
    leftColor: Color = Color(0xFF2EC4B6),
    rightColor: Color = Color(0xFF0F9F90)
) {
    drawRect(
        color = CardboardDark,
        topLeft = Offset(cx - scale * 0.13f, baseY - scale * 0.32f),
        size = Size(scale * 0.26f, scale * 0.42f)
    )
    for (tier in 0..1) {
        val topY = baseY - scale * (1.30f - tier * 0.36f)
        val botY = baseY - scale * (0.30f - tier * 0.12f)
        val halfW = scale * (0.52f + tier * 0.14f)
        val shadowPath = Path().apply {
            moveTo(cx + shadowShiftX, topY + 4f)
            lineTo(cx - halfW + shadowShiftX, botY + 4f)
            lineTo(cx + halfW + shadowShiftX, botY + 4f)
            close()
        }
        drawPath(shadowPath, Color(0x1E000000))

        val leftHalf = Path().apply {
            moveTo(cx, topY)
            lineTo(cx - halfW, botY)
            lineTo(cx, botY)
            close()
        }
        val rightHalf = Path().apply {
            moveTo(cx, topY)
            lineTo(cx + halfW, botY)
            lineTo(cx, botY)
            close()
        }
        drawPath(leftHalf, leftColor)
        drawPath(rightHalf, rightColor)
    }
}

private fun DrawScope.drawPeppermintLollipopTree(cx: Float, baseY: Float, scale: Float) {
    drawRect(
        color = PeppermintWhite,
        topLeft = Offset(cx - scale * 0.08f, baseY - scale * 0.65f),
        size = Size(scale * 0.16f, scale * 0.72f)
    )
    val candyCenter = Offset(cx, baseY - scale * 0.85f)
    val r = scale * 0.46f
    drawCircle(PeppermintWhite, radius = r, center = candyCenter)
    drawCircle(PeppermintRed, radius = r * 0.72f, center = candyCenter)
    drawCircle(PeppermintWhite, radius = r * 0.42f, center = candyCenter)
    drawCircle(PeppermintRed, radius = r * 0.18f, center = candyCenter)
}

private fun DrawScope.drawCardboardMesa(cx: Float, baseY: Float, scale: Float) {
    val mesa = Path().apply {
        moveTo(cx - scale * 0.65f, baseY + scale * 0.08f)
        lineTo(cx - scale * 0.38f, baseY - scale * 0.95f)
        lineTo(cx + scale * 0.38f, baseY - scale * 0.95f)
        lineTo(cx + scale * 0.65f, baseY + scale * 0.08f)
        close()
    }
    drawPath(mesa, Color(0xFFB45309))
    drawMiniHillStar(cx = cx, cy = baseY - scale * 1.15f, r = scale * 0.25f)
}

private fun DrawScope.drawPaperLighthouse(cx: Float, baseY: Float, scale: Float) {
    val tower = Path().apply {
        moveTo(cx - scale * 0.32f, baseY + scale * 0.06f)
        lineTo(cx - scale * 0.18f, baseY - scale * 1.15f)
        lineTo(cx + scale * 0.18f, baseY - scale * 1.15f)
        lineTo(cx + scale * 0.32f, baseY + scale * 0.06f)
        close()
    }
    drawPath(tower, PeppermintWhite)
    drawRect(
        color = PeppermintRed,
        topLeft = Offset(cx - scale * 0.25f, baseY - scale * 0.72f),
        size = Size(scale * 0.50f, scale * 0.28f)
    )
    drawCircle(StarCardboardGold, radius = scale * 0.20f, center = Offset(cx, baseY - scale * 1.25f))
}

private fun DrawScope.drawGiantPaperCupcake(cx: Float, baseY: Float, scale: Float) {
    val cup = Path().apply {
        moveTo(cx - scale * 0.42f, baseY - scale * 0.45f)
        lineTo(cx + scale * 0.42f, baseY - scale * 0.45f)
        lineTo(cx + scale * 0.30f, baseY + scale * 0.06f)
        lineTo(cx - scale * 0.30f, baseY + scale * 0.06f)
        close()
    }
    drawPath(cup, StarCardboardGold)
    drawCircle(CupcakeFrostingPink, radius = scale * 0.42f, center = Offset(cx, baseY - scale * 0.58f))
    drawCircle(PeppermintRed, radius = scale * 0.14f, center = Offset(cx, baseY - scale * 0.98f))
}

private fun DrawScope.drawThreadSpoolPillar(cx: Float, baseY: Float, scale: Float, threadColor: Color) {
    drawRect(
        color = threadColor,
        topLeft = Offset(cx - scale * 0.32f, baseY - scale * 0.92f),
        size = Size(scale * 0.64f, scale * 0.85f)
    )
    drawRect(
        color = CardboardDark,
        topLeft = Offset(cx - scale * 0.44f, baseY - scale * 1.05f),
        size = Size(scale * 0.88f, scale * 0.16f)
    )
    drawRect(
        color = CardboardDark,
        topLeft = Offset(cx - scale * 0.44f, baseY - scale * 0.12f),
        size = Size(scale * 0.88f, scale * 0.16f)
    )
}

private fun DrawScope.drawRoyalCastleTurret(cx: Float, baseY: Float, scale: Float) {
    drawRect(
        color = PeppermintWhite,
        topLeft = Offset(cx - scale * 0.34f, baseY - scale * 0.95f),
        size = Size(scale * 0.68f, scale * 1.02f)
    )
    val roof = Path().apply {
        moveTo(cx, baseY - scale * 1.48f)
        lineTo(cx - scale * 0.46f, baseY - scale * 0.92f)
        lineTo(cx + scale * 0.46f, baseY - scale * 0.92f)
        close()
    }
    drawPath(roof, Color(0xFFE11D48))
    drawCircle(StarCardboardGold, radius = scale * 0.12f, center = Offset(cx, baseY - scale * 1.52f))
}

private fun DrawScope.drawMiniHillCupcake(cx: Float, cy: Float, r: Float) {
    drawRect(
        color = StarCardboardGold,
        topLeft = Offset(cx - r * 0.65f, cy - r * 0.1f),
        size = Size(r * 1.3f, r * 0.85f)
    )
    drawCircle(CupcakeFrostingPink, radius = r * 0.72f, center = Offset(cx, cy - r * 0.28f))
    drawCircle(PeppermintRed, radius = r * 0.25f, center = Offset(cx, cy - r * 0.92f))
}

private fun DrawScope.drawMiniHillStar(cx: Float, cy: Float, r: Float) {
    val star = Path().apply {
        for (i in 0 until 10) {
            val rad = if (i % 2 == 0) r else r * 0.45f
            val a = i * (Math.PI / 5.0) - Math.PI / 2.0
            val px = cx + cos(a).toFloat() * rad
            val py = cy + sin(a).toFloat() * rad
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(star, StarCardboardGold)
}

private fun DrawScope.drawPeekingOrigamiBear(
    cx: Float,
    cy: Float,
    sizePx: Float,
    hasCrown: Boolean = false,
    hasCupcakeHat: Boolean = false
) {
    val r = sizePx * 0.5f
    drawCircle(CardboardDark, radius = r * 0.35f, center = Offset(cx - r * 0.62f, cy - r * 0.60f))
    drawCircle(CardboardDark, radius = r * 0.35f, center = Offset(cx + r * 0.62f, cy - r * 0.60f))
    drawCircle(CardboardLight, radius = r * 0.18f, center = Offset(cx - r * 0.62f, cy - r * 0.60f))
    drawCircle(CardboardLight, radius = r * 0.18f, center = Offset(cx + r * 0.62f, cy - r * 0.60f))

    val head = Path().apply {
        moveTo(cx - r * 0.82f, cy - r * 0.24f)
        lineTo(cx, cy - r * 0.76f)
        lineTo(cx + r * 0.82f, cy - r * 0.24f)
        lineTo(cx + r * 0.66f, cy + r * 0.62f)
        lineTo(cx - r * 0.66f, cy + r * 0.62f)
        close()
    }
    drawPath(head, CardboardMedium)
    val snout = Path().apply {
        moveTo(cx, cy - r * 0.10f)
        lineTo(cx + r * 0.35f, cy + r * 0.44f)
        lineTo(cx - r * 0.35f, cy + r * 0.44f)
        close()
    }
    drawPath(snout, CardboardLight)
    drawCircle(InkBrown, radius = r * 0.10f, center = Offset(cx, cy + r * 0.06f))
    drawCircle(InkBrown, radius = r * 0.08f, center = Offset(cx - r * 0.35f, cy - r * 0.14f))
    drawCircle(InkBrown, radius = r * 0.08f, center = Offset(cx + r * 0.35f, cy - r * 0.14f))

    if (hasCrown) {
        val crown = Path().apply {
            moveTo(cx - r * 0.42f, cy - r * 0.72f)
            lineTo(cx - r * 0.32f, cy - r * 1.12f)
            lineTo(cx, cy - r * 0.88f)
            lineTo(cx + r * 0.32f, cy - r * 1.12f)
            lineTo(cx + r * 0.42f, cy - r * 0.72f)
            close()
        }
        drawPath(crown, StarCardboardGold)
    } else if (hasCupcakeHat) {
        drawCircle(CupcakeFrostingPink, radius = r * 0.30f, center = Offset(cx, cy - r * 0.85f))
        drawCircle(PeppermintRed, radius = r * 0.12f, center = Offset(cx, cy - r * 1.12f))
    }
}

private fun DrawScope.drawPeekingOrigamiSquirrel(
    cx: Float,
    cy: Float,
    sizePx: Float,
    hasBow: Boolean = false
) {
    val r = sizePx * 0.5f
    val tail = Path().apply {
        moveTo(cx + r * 0.24f, cy + r * 0.48f)
        lineTo(cx + r * 1.06f, cy - r * 0.18f)
        lineTo(cx + r * 0.78f, cy - r * 0.86f)
        lineTo(cx + r * 0.18f, cy - r * 0.32f)
        close()
    }
    drawPath(tail, Color(0xFFE0531F))
    val head = Path().apply {
        moveTo(cx - r * 0.52f, cy - r * 0.72f)
        lineTo(cx - r * 0.14f, cy - r * 0.42f)
        lineTo(cx + r * 0.24f, cy - r * 0.72f)
        lineTo(cx + r * 0.40f, cy + r * 0.44f)
        lineTo(cx - r * 0.62f, cy + r * 0.44f)
        close()
    }
    drawPath(head, OrigamiFoxOrange)
    drawPath(head, PeppermintWhite.copy(alpha = 0.4f), style = Stroke(width = 2f))
    drawCircle(InkBrown, radius = r * 0.08f, center = Offset(cx - r * 0.24f, cy - r * 0.10f))

    if (hasBow) {
        drawCircle(WrappedCandyPurple, radius = r * 0.18f, center = Offset(cx - r * 0.14f, cy + r * 0.32f))
    }
}
