package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BOARD_SIZE
import com.example.model.BoosterType
import com.example.model.TutorialPrompt
import com.example.model.TutorialTargetType
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.InkBrown
import com.example.ui.theme.InkSoftBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteShadow
import com.example.ui.theme.StickyNoteYellow
import com.example.ui.theme.ThumbtackHighlight
import com.example.ui.theme.ThumbtackRed
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Torn-Edge Washi & Sticky-Note Coach Banner displayed on Levels 1-3
 * (or when the player taps the '?' Tutorial Help button in the HUD).
 */
@Composable
fun PaperInteractiveTutorialCard(
    prompt: TutorialPrompt?,
    visible: Boolean,
    onNextStep: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && prompt != null,
        enter = scaleIn(spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        if (prompt == null) return@AnimatedVisibility
        val cardShape = TornPaperShape(teethCount = 7, toothDepthPx = 6f)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("interactive_tutorial_card"),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .fillMaxWidth()
                    .shadow(8.dp, cardShape)
                    .clip(cardShape)
                    .background(MaskingTapeCream, cardShape)
                    .border(2.dp, PeppermintRed, cardShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Header Row: Step Pill + Power-Up Icon Chip + Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(PeppermintRed, RoundedCornerShape(99.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = prompt.badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = PeppermintWhite
                            )
                        }

                        // Step progress dots
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (i in 1..prompt.totalSteps) {
                                Box(
                                    modifier = Modifier
                                        .size(if (i == prompt.stepIndex) 8.dp else 6.dp)
                                        .background(
                                            if (i == prompt.stepIndex) OrigamiFoxOrange else StickyNoteShadow,
                                            CircleShape
                                        )
                                        .border(1.dp, InkBrown, CircleShape)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        // Next Tip / Complete Step Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(StickyNoteYellow, RoundedCornerShape(99.dp))
                                .border(1.5.dp, InkBrown, RoundedCornerShape(99.dp))
                                .clickable(onClick = onNextStep)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                                .testTag("tutorial_next_button")
                        ) {
                            Text(
                                text = if (prompt.stepIndex < prompt.totalSteps) {
                                    "${stringResource(R.string.btn_tutorial_next)} →"
                                } else {
                                    "${stringResource(R.string.btn_tutorial_skip)} ✓"
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = InkBrown
                            )
                        }

                        // Dismiss / Skip Button
                        if (prompt.stepIndex < prompt.totalSteps) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(99.dp))
                                    .background(MaskingTapeBorder.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                                    .border(1.dp, CardboardDark, RoundedCornerShape(99.dp))
                                    .clickable(onClick = onDismiss)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .testTag("tutorial_skip_button")
                            ) {
                                Text(
                                    text = "✕",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = InkBrown
                                )
                            }
                        }
                    }
                }

                // Main Title & Instruction Row with Visual Power-Up Emblem
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TutorialVisualEmblem(prompt = prompt)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = prompt.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = InkBrown,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = prompt.instruction,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = InkSoftBrown,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Bottom Power-Up Recipe & Pro-Tip Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StickyNoteYellow.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                        .border(1.dp, StickyNoteShadow, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = prompt.powerUpTip,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = InkBrown,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Crimson 3D Thumbtack Pin at Top Center
            Box(
                modifier = Modifier
                    .size(13.dp)
                    .shadow(3.dp, CircleShape)
                    .background(ThumbtackRed, CircleShape)
                    .border(1.dp, Color(0xFFB71C1C), CircleShape),
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 2.5.dp, y = 2.5.dp)
                        .size(4.dp)
                        .background(ThumbtackHighlight, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TutorialVisualEmblem(prompt: TutorialPrompt) {
    val badgeBg = when (prompt.targetType) {
        TutorialTargetType.BOARD_SWAP -> OrigamiFoxOrange
        TutorialTargetType.BOARD_SPECIAL_TILE -> PeppermintRed
        TutorialTargetType.BOOSTER_BAR, TutorialTargetType.BOARD_BOOSTER_USE -> StarCardboardGold
    }
    Box(
        modifier = Modifier
            .size(34.dp)
            .shadow(3.dp, RoundedCornerShape(10.dp))
            .background(badgeBg, RoundedCornerShape(10.dp))
            .border(1.5.dp, InkBrown, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (prompt.recommendedBooster != null &&
            (prompt.targetType == TutorialTargetType.BOOSTER_BAR || prompt.targetType == TutorialTargetType.BOARD_BOOSTER_USE)
        ) {
            BoosterVectorIcon(
                boosterType = prompt.recommendedBooster,
                modifier = Modifier.size(24.dp)
            )
        } else {
            val symbol = when {
                prompt.level == 1 && prompt.stepIndex == 1 -> "3x"
                prompt.level == 1 && prompt.stepIndex == 2 -> "↔"
                prompt.level == 2 && prompt.stepIndex == 1 -> "✹"
                prompt.level == 2 && prompt.stepIndex == 2 -> "★"
                else -> "✦"
            }
            Text(
                text = symbol,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                ),
                color = PeppermintWhite
            )
        }
    }
}

/**
 * Visual, interactive on-board Canvas & Washi callout overlay that highlights the exact
 * swap tiles, special power-ups, or booster target cells with an animated Origami Paper Hand.
 */
@Composable
fun InteractiveBoardTutorialOverlay(
    prompt: TutorialPrompt,
    cellSize: Dp,
    sharedWaveState: State<Float>,
    modifier: Modifier = Modifier
) {
    val fromPos = prompt.fromPos ?: return
    val toPos = prompt.toPos ?: fromPos
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellPx = size.width / BOARD_SIZE
            val wave01 = (sharedWaveState.value + 1f) * 0.5f // Smooth 0f..1f oscillation
            val pulseScale = 1f + 0.06f * sin(wave01 * PI.toFloat() * 2f)

            // 1. Draw glowing Washi Cut-Out Spotlight Frames around all highlightCells
            val dashEffect = PathEffect.dashPathEffect(
                floatArrayOf(cellPx * 0.16f, cellPx * 0.10f),
                phase = wave01 * cellPx * 0.52f
            )
            prompt.highlightCells.forEach { cell ->
                val left = cell.col * cellPx + cellPx * 0.05f
                val top = cell.row * cellPx + cellPx * 0.05f
                val boxSide = cellPx * 0.90f
                val isPrimary = cell == fromPos || cell == toPos

                drawRoundRect(
                    color = if (isPrimary) StarCardboardGold.copy(alpha = 0.28f) else StickyNoteYellow.copy(alpha = 0.16f),
                    topLeft = Offset(left, top),
                    size = Size(boxSide, boxSide),
                    cornerRadius = CornerRadius(cellPx * 0.22f, cellPx * 0.22f)
                )
                drawRoundRect(
                    color = if (isPrimary) PeppermintRed else StarCardboardGold,
                    topLeft = Offset(left, top),
                    size = Size(boxSide, boxSide),
                    cornerRadius = CornerRadius(cellPx * 0.22f, cellPx * 0.22f),
                    style = Stroke(
                        width = if (isPrimary) 3.dp.toPx() else 2.dp.toPx(),
                        pathEffect = if (isPrimary) null else dashEffect
                    )
                )
            }

            val fromCenter = Offset(
                (fromPos.col + 0.5f) * cellPx,
                (fromPos.row + 0.5f) * cellPx
            )
            val toCenter = Offset(
                (toPos.col + 0.5f) * cellPx,
                (toPos.row + 0.5f) * cellPx
            )

            // 2. Draw Scissor-Cut Guide Path or Target Reticle + Animated Origami Paper Hand
            if (fromPos != toPos && prompt.targetType != TutorialTargetType.BOOSTER_BAR) {
                // Perforated Scissor-Cut Swap Line between fromCenter and toCenter
                drawLine(
                    color = InkBrown.copy(alpha = 0.85f),
                    start = fromCenter,
                    end = toCenter,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )
                drawLine(
                    color = StickyNoteYellow,
                    start = fromCenter,
                    end = toCenter,
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = dashEffect
                )

                // Pulsing target rings at start and end
                drawCircle(
                    color = StarCardboardGold.copy(alpha = 0.75f),
                    radius = cellPx * 0.36f * pulseScale,
                    center = fromCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(
                    color = OrigamiFoxGreen.copy(alpha = 0.85f),
                    radius = cellPx * 0.36f * (2f - pulseScale),
                    center = toCenter,
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Gliding Origami Paper Hand moving smoothly from fromCenter -> toCenter
                val handPos = Offset(
                    x = fromCenter.x + (toCenter.x - fromCenter.x) * wave01,
                    y = fromCenter.y + (toCenter.y - fromCenter.y) * wave01 + cellPx * 0.16f
                )
                val angleDeg = (atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x) * 180f / PI.toFloat())
                drawOrigamiPointerHand(
                    tipCenter = handPos,
                    sizePx = cellPx * 0.62f,
                    tiltDeg = (angleDeg * 0.15f).coerceIn(-18f, 18f)
                )
            } else {
                // Single-tile target reticle (Booster Use or Special Double-Tap)
                val ringRadius = cellPx * (0.36f + wave01 * 0.16f)
                drawCircle(
                    color = PeppermintRed.copy(alpha = 1f - wave01 * 0.55f),
                    radius = ringRadius,
                    center = fromCenter,
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = StarCardboardGold,
                    radius = cellPx * 0.42f,
                    center = fromCenter,
                    style = Stroke(width = 2.2.dp.toPx(), pathEffect = dashEffect)
                )

                val bobOffset = sin(wave01 * PI.toFloat() * 2f) * (cellPx * 0.10f)
                val handPos = Offset(
                    x = fromCenter.x + cellPx * 0.12f,
                    y = fromCenter.y + cellPx * 0.22f + bobOffset
                )
                drawOrigamiPointerHand(
                    tipCenter = handPos,
                    sizePx = cellPx * 0.62f,
                    tiltDeg = -10f
                )
            }
        }

        // 3. Floating Mini Washi Action Tag right above the highlighted cells
        val minRow = minOf(fromPos.row, toPos.row)
        val avgCol = (fromPos.col + toPos.col) * 0.5f
        val cellPx = with(density) { cellSize.toPx() }
        val tagRow = if (minRow >= 1) minRow - 0.72f else (maxOf(fromPos.row, toPos.row) + 0.95f)
        val actionLabel = when (prompt.targetType) {
            TutorialTargetType.BOARD_SWAP -> "✦ SWIPE BLOCKS ↔"
            TutorialTargetType.BOARD_SPECIAL_TILE -> "✦ SWAP OR 2x TAP!"
            TutorialTargetType.BOARD_BOOSTER_USE -> when (prompt.recommendedBooster) {
                BoosterType.SCISSORS -> "✂ TAP TO SNIP!"
                BoosterType.GLUE -> "✦ TAP 2 BLOCKS!"
                BoosterType.HAMMER, null -> "🔨 TAP TO SMASH!"
            }
            TutorialTargetType.BOOSTER_BAR -> "▼ PICK A BOOSTER ▼"
        }

        Box(
            modifier = Modifier
                .offset {
                    val xPx = ((avgCol + 0.5f) * cellPx - 64.dp.toPx()).coerceIn(
                        4.dp.toPx(),
                        (BOARD_SIZE * cellPx - 132.dp.toPx()).coerceAtLeast(4.dp.toPx())
                    )
                    val yPx = (tagRow * cellPx).coerceIn(
                        2.dp.toPx(),
                        (BOARD_SIZE * cellPx - 28.dp.toPx()).coerceAtLeast(2.dp.toPx())
                    )
                    IntOffset(xPx.roundToInt(), yPx.roundToInt())
                }
        ) {
            val pillShape = TornPaperShape(teethCount = 5, toothDepthPx = 5f)
            Surface(
                shape = pillShape,
                color = StickyNoteYellow,
                shadowElevation = 6.dp,
                modifier = Modifier.border(1.5.dp, PeppermintRed, pillShape)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = InkBrown,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Draws a tactile two-tone folded Origami Paper Pointer Hand / Cursor on Canvas.
 */
private fun DrawScope.drawOrigamiPointerHand(
    tipCenter: Offset,
    sizePx: Float,
    tiltDeg: Float
) {
    withTransform({
        translate(tipCenter.x, tipCenter.y)
        rotate(tiltDeg, Offset.Zero)
    }) {
        val w = sizePx
        val h = sizePx * 1.15f

        // 1. Soft paper drop shadow
        val shadowPath = Path().apply {
            moveTo(3f, 4f)
            lineTo(-w * 0.42f + 3f, h * 0.56f + 4f)
            lineTo(-w * 0.08f + 3f, h * 0.44f + 4f)
            lineTo(w * 0.14f + 3f, h * 0.78f + 4f)
            lineTo(w * 0.34f + 3f, h * 0.66f + 4f)
            lineTo(w * 0.12f + 3f, h * 0.34f + 4f)
            lineTo(w * 0.46f + 3f, h * 0.32f + 4f)
            close()
        }
        drawPath(shadowPath, color = Color(0x55221206))

        // 2. Left sunlit Washi paper wing
        val leftWing = Path().apply {
            moveTo(0f, 0f)
            lineTo(-w * 0.44f, h * 0.56f)
            lineTo(0f, h * 0.44f)
            close()
        }
        drawPath(leftWing, color = PeppermintWhite)

        // 3. Right warm-shaded Washi paper wing + folded stem
        val rightWing = Path().apply {
            moveTo(0f, 0f)
            lineTo(0f, h * 0.44f)
            lineTo(w * 0.14f, h * 0.76f)
            lineTo(w * 0.32f, h * 0.66f)
            lineTo(w * 0.14f, h * 0.36f)
            lineTo(w * 0.46f, h * 0.32f)
            close()
        }
        drawPath(rightWing, color = StickyNoteYellow)

        // 4. Crisp InkBrown Origami Outline & Center Valley Crease
        val fullOutline = Path().apply {
            moveTo(0f, 0f)
            lineTo(-w * 0.44f, h * 0.56f)
            lineTo(-w * 0.04f, h * 0.44f)
            lineTo(w * 0.14f, h * 0.76f)
            lineTo(w * 0.32f, h * 0.66f)
            lineTo(w * 0.14f, h * 0.36f)
            lineTo(w * 0.46f, h * 0.32f)
            close()
        }
        drawPath(
            path = fullOutline,
            color = InkBrown,
            style = Stroke(width = 2.2.dp.toPx())
        )
        drawLine(
            color = PeppermintRed,
            start = Offset(0f, 0f),
            end = Offset(0f, h * 0.44f),
            strokeWidth = 1.6.dp.toPx()
        )
        // Bright gold tip dot
        drawCircle(
            color = OrigamiFoxOrange,
            radius = 3.5.dp.toPx(),
            center = Offset.Zero
        )
    }
}
