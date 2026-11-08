package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.model.ConfettiDepthLayer
import com.example.model.ConfettiScrap
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.CraftTableKraft
import com.example.ui.theme.FarHillGreen
import com.example.ui.theme.InkBrown
import com.example.ui.theme.NearHillGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.SkyBluePaper
import com.example.ui.theme.SkyHorizonPaper
import com.example.ui.theme.StarCardboardGold
import kotlin.math.abs
import kotlin.math.cos

/**
 * Multi-layered paper diorama backdrop with tilt-driven parallax offsets, dynamic
 * drop-shadow shifts, and depth-interleaved confetti shreds that land and rest directly
 * on the Far Hill, Near Meadow Hill, and Foreground Kraft Craft Table layers.
 */
@Composable
fun PaperDioramaBackdrop(
    tilt: DeviceTilt = DeviceTilt(),
    scraps: List<ConfettiScrap> = emptyList(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxShiftX = w * 0.010f
        val maxShiftY = h * 0.007f

        // Shadow vector moves subtly opposite to the light source tilt
        val shadowShiftX = -tilt.x * 3f + 3f
        val shadowShiftY = -tilt.y * 3f + 4f

        // 1. Bright sky-blue paper base
        drawRect(color = SkyBluePaper)

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
            drawPath(cloudStrip, SkyHorizonPaper)
        }

        // 3. Sunny golden paper-cutout sun (Parallax depth: 0.40x)
        val sunCenter = Offset(
            x = w * 0.85f + tilt.x * maxShiftX * 0.40f,
            y = h * 0.13f + tilt.y * maxShiftY * 0.40f
        )
        drawCircle(
            color = Color(0x24000000),
            radius = w * 0.075f,
            center = sunCenter + Offset(shadowShiftX, shadowShiftY)
        )
        drawCircle(Color(0xFFFFEA79), radius = w * 0.075f, center = sunCenter)
        drawCircle(StarCardboardGold, radius = w * 0.055f, center = sunCenter)

        // 4. Far mint-emerald paper hill + pine trees + FAR_HILL confetti shreds (Parallax depth: 0.60x)
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
            drawPath(farHill, FarHillGreen)

            drawPaperPineTree(cx = w * 0.11f, baseY = h * 0.60f, scale = w * 0.085f, shadowShiftX = shadowShiftX)
            drawPaperPineTree(cx = w * 0.89f, baseY = h * 0.58f, scale = w * 0.095f, shadowShiftX = shadowShiftX)

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

        // 5. Near meadow-green paper hill + origami critters + NEAR_HILL confetti shreds (Parallax depth: 0.90x)
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
            drawPath(nearHill, NearHillGreen)

            drawPeekingOrigamiBear(cx = w * 0.11f, cy = h * 0.71f, sizePx = w.coerceAtMost(h) * 0.12f)
            drawPeekingOrigamiSquirrel(cx = w * 0.89f, cy = h * 0.70f, sizePx = w.coerceAtMost(h) * 0.12f)

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

        // 6. Warm Kraft craft-table foreground mat + landed CRAFT_TABLE confetti shreds (Parallax depth: 1.15x)
        withTransform({
            translate(left = tilt.x * maxShiftX * 1.15f, top = tilt.y * maxShiftY * 1.10f)
        }) {
            val tableTear = buildTornWavePath(w, h, baseY = h * 0.84f, amp = h * 0.012f, offsetY = -4f)
            drawPath(tableTear, PeppermintWhite.copy(alpha = 0.88f))
            val tableMat = buildTornWavePath(w, h, baseY = h * 0.84f, amp = h * 0.012f, offsetY = 0f)
            drawPath(tableMat, CraftTableKraft)

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
        0.76f
    } else {
        0.34f + 0.66f * abs(cos(scrap.flutterPhase))
    }
    val alpha = scrap.alpha.coerceIn(0f, 1f)
    val shadowMult = if (scrap.isLanded) 0.35f else 0.85f

    withTransform({
        translate(left = px, top = py)
        rotate(degrees = scrap.rotation, pivot = Offset.Zero)
        scale(scaleX = 1f, scaleY = flutterScaleY, pivot = Offset.Zero)
    }) {
        val sx = shadowShiftX * shadowMult
        val sy = shadowShiftY * shadowMult
        val shadowPath = Path().apply {
            moveTo(-sw * 0.5f + sx, -sh * 0.4f + sy)
            lineTo(sw * 0.45f + sx, -sh * 0.5f + sy)
            lineTo(sw * 0.55f + sx, sh * 0.35f + sy)
            lineTo(-sw * 0.35f + sx, sh * 0.5f + sy)
            close()
        }
        drawPath(
            path = shadowPath,
            color = Color.Black.copy(alpha = (alpha * 0.22f).coerceIn(0f, 1f))
        )

        val bodyPath = Path().apply {
            moveTo(-sw * 0.5f, -sh * 0.4f)
            lineTo(sw * 0.45f, -sh * 0.5f)
            lineTo(sw * 0.55f, sh * 0.35f)
            lineTo(-sw * 0.35f, sh * 0.5f)
            close()
        }
        drawPath(
            path = bodyPath,
            color = scrap.color.copy(alpha = alpha)
        )

        val foldPath = Path().apply {
            moveTo(-sw * 0.5f, -sh * 0.4f)
            lineTo(sw * 0.45f, -sh * 0.5f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(
            path = foldPath,
            color = scrap.foldColor.copy(alpha = (alpha * 0.88f).coerceIn(0f, 1f))
        )
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
    shadowShiftX: Float = 3f
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
        drawPath(leftHalf, Color(0xFF2EC4B6))
        drawPath(rightHalf, Color(0xFF0F9F90))
    }
}

private fun DrawScope.drawPeekingOrigamiBear(cx: Float, cy: Float, sizePx: Float) {
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
}

private fun DrawScope.drawPeekingOrigamiSquirrel(cx: Float, cy: Float, sizePx: Float) {
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
}
