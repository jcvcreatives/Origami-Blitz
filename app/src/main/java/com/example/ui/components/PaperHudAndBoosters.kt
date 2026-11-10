package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.R
import com.example.model.BoosterType
import com.example.model.GameStatus
import com.example.model.LevelGoal
import com.example.model.PaperGameState
import com.example.model.TutorialTargetType
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.InkBrown
import com.example.ui.theme.InkSoftBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteShadow
import com.example.ui.theme.StickyNoteYellow
import com.example.ui.theme.ThumbtackHighlight
import com.example.ui.theme.ThumbtackRed
import com.example.ui.theme.WrappedCandyPurple

@Composable
fun PaperTopHud(
    state: PaperGameState,
    onToggleSound: () -> Unit,
    onResetProgress: () -> Unit,
    onOpenWorldMap: () -> Unit = {},
    onOpenTutorial: () -> Unit = {},
    onNextTutorialStep: () -> Unit = {},
    onDismissTutorial: () -> Unit = {},
    onOpenHomeScreen: () -> Unit = {},
    onOpenGmPanel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val animatedScore by animateIntAsState(
        targetValue = state.score,
        animationSpec = tween(durationMillis = 350),
        label = "score_counter"
    )
    val stageInfo = state.currentStage

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Home / Account Portal Button
                IconButton(
                    onClick = onOpenHomeScreen,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .shadow(3.dp, CircleShape)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.5.dp, CardboardMedium, CircleShape)
                        .testTag("open_home_screen_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home & Login",
                        tint = InkBrown,
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .shadow(3.dp, CircleShape)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.5.dp, CardboardMedium, CircleShape)
                        .testTag("toggle_sound_button")
                ) {
                    Icon(
                        imageVector = if (state.soundEnabled) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = if (state.soundEnabled) {
                            stringResource(R.string.btn_sound_on)
                        } else {
                            stringResource(R.string.btn_sound_off)
                        },
                        tint = InkBrown,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // World Map Button (Candy Crush Saga Trail)
                Row(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .shadow(3.dp, RoundedCornerShape(99.dp))
                        .clip(RoundedCornerShape(99.dp))
                        .background(StickyNoteYellow, RoundedCornerShape(99.dp))
                        .border(1.5.dp, CardboardDark, RoundedCornerShape(99.dp))
                        .clickable(onClick = onOpenWorldMap)
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("open_world_map_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = stringResource(R.string.btn_world_map),
                        tint = PeppermintRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Map",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = InkBrown
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.clickable(onClick = onOpenWorldMap)
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-1.5f)
                        .shadow(3.dp, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .background(MaskingTapeCream, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .border(1.dp, MaskingTapeBorder, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("header_paper_craft_tape")
                ) {
                    Text(
                        text = "Stg ${stageInfo.stageNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = InkBrown
                    )
                }
                Box(
                    modifier = Modifier
                        .rotate(1.5f)
                        .shadow(3.dp, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .background(StickyNoteYellow, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .border(1.5.dp, StickyNoteShadow, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                        .testTag("header_level_tape")
                ) {
                    Text(
                        text = stringResource(R.string.header_torn_level, state.level),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = PeppermintRed
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (state.isGmAccount) {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .shadow(3.dp, RoundedCornerShape(99.dp))
                            .clip(RoundedCornerShape(99.dp))
                            .background(WrappedCandyPurple, RoundedCornerShape(99.dp))
                            .border(1.5.dp, StarCardboardGold, RoundedCornerShape(99.dp))
                            .clickable(onClick = onOpenGmPanel)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("open_gm_panel_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "★ GM",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = PeppermintWhite
                        )
                    }
                }

                // Interactive Tutorial Guide '?' Button
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .shadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (state.tutorialPrompt != null && !state.tutorialDismissed) StickyNoteYellow else MaskingTapeCream,
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            if (state.tutorialPrompt != null && !state.tutorialDismissed) PeppermintRed else CardboardMedium,
                            CircleShape
                        )
                        .clickable(onClick = onOpenTutorial)
                        .testTag("open_tutorial_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = if (state.tutorialPrompt != null && !state.tutorialDismissed) PeppermintRed else InkBrown
                    )
                }

                IconButton(
                    onClick = onResetProgress,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(38.dp)
                        .shadow(3.dp, CircleShape)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.5.dp, CardboardMedium, CircleShape)
                        .testTag("reset_level_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.btn_retry_level),
                        tint = InkBrown,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardboardScoreBanner(
                score = animatedScore,
                highScore = state.highScore,
                modifier = Modifier.weight(1.05f)
            )

            TornGoalNote(
                primaryGoal = state.goal,
                secondaryGoal = state.secondaryGoal,
                initialGoldCount = state.initialGoldCount,
                goldRemaining = state.goldRemaining,
                initialTapeCount = state.initialTapeCount,
                tapeRemaining = state.tapeRemaining,
                modifier = Modifier.weight(1.18f)
            )

            MovesStickyNote(
                movesRemaining = state.movesRemaining,
                modifier = Modifier.weight(0.92f)
            )
        }

        OrigamiFeverStrip(
            feverCharge = state.feverCharge,
            feverMovesLeft = state.feverMovesLeft,
            feverBurstSeq = state.feverBurstSeq,
            streakCount = state.streakCount,
            mechanicLabel = state.mechanicLabel
        )

        PaperInteractiveTutorialCard(
            prompt = state.tutorialPrompt,
            visible = state.tutorialPrompt != null &&
                !state.tutorialDismissed &&
                state.status == GameStatus.PLAYING &&
                !state.showWorldMap,
            onNextStep = onNextTutorialStep,
            onDismiss = onDismissTutorial
        )
    }
}

@Composable
private fun OrigamiFeverStrip(
    feverCharge: Float,
    feverMovesLeft: Int,
    feverBurstSeq: Long,
    streakCount: Int,
    mechanicLabel: String = ""
) {
    val isFever = feverMovesLeft > 0
    val isNearFull = !isFever && feverCharge >= 0.72f

    // 1. Full-Charge Ignition Celebration Burst (0f -> 1f over 920ms when feverBurstSeq increments)
    val burstProgress = remember { Animatable(1f) }
    LaunchedEffect(feverBurstSeq) {
        if (feverBurstSeq > 0L) {
            burstProgress.snapTo(0f)
            burstProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 920, easing = LinearEasing)
            )
        }
    }
    val burstT = burstProgress.value
    val isBursting = feverBurstSeq > 0L && burstT < 0.99f

    // 2. Smooth Spring Fill Animation
    val animatedFill by animateFloatAsState(
        targetValue = if (isFever || isBursting) 1f else feverCharge.coerceIn(0.06f, 1f),
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "fever_fill"
    )

    // 3. Continuous 60fps Infinite Animation during Fever Mode or Near-Full Charge
    val infiniteTransition = rememberInfiniteTransition(label = "fever_infinite")
    val flowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fever_flow_phase"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fever_pulse_phase"
    )

    // Elastic pop & celebratory shake on full-charge ignition + rhythmic heartbeat during Fever
    val ignitionPop = if (isBursting) {
        sin(burstT * PI.toFloat()) * 0.13f * (1f - burstT * 0.45f)
    } else 0f
    val feverHeartbeat = if (isFever) pulsePhase * 0.032f else if (isNearFull) pulsePhase * 0.015f else 0f
    val scaleX = 1f + ignitionPop + feverHeartbeat
    val scaleY = 1f + (ignitionPop * 1.35f) + (feverHeartbeat * 1.25f)
    val tiltZ = if (isBursting) {
        sin(burstT * 26f) * (1f - burstT) * 2.2f
    } else 0f

    val barShape = RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .testTag("fever_meter_strip")
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
                this.rotationZ = tiltZ
                this.clip = false
            },
        contentAlignment = Alignment.Center
    ) {
        // Outer Eruption Canvas (Shockwave Rings, Sunburst Rays & Origami Star Sparks on Full Charge)
        if (isBursting || isFever) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = false }
            ) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f

                // Continuous Golden Aura Glow while in Fever Mode
                if (isFever) {
                    val glowAlpha = 0.22f + pulsePhase * 0.18f
                    drawRoundRect(
                        color = StarCardboardGold.copy(alpha = glowAlpha),
                        topLeft = Offset(-6f, -5f),
                        size = Size(size.width + 12f, size.height + 10f),
                        cornerRadius = CornerRadius(32f, 32f),
                        style = Stroke(width = 6f)
                    )
                }

                // Full-Charge Ignition Shockwave Rings, Radial Rays & 12 Origami Sparks
                if (isBursting) {
                    val fade = (1f - burstT).coerceIn(0f, 1f)
                    val ringExpandX = size.width * (0.5f + burstT * 0.62f)
                    val ringExpandY = size.height * (0.65f + burstT * 2.2f)

                    // Primary Gold Shockwave Ring
                    drawRoundRect(
                        color = StarCardboardGold.copy(alpha = fade * 0.85f),
                        topLeft = Offset(cx - ringExpandX, cy - ringExpandY),
                        size = Size(ringExpandX * 2f, ringExpandY * 2f),
                        cornerRadius = CornerRadius(42f, 42f),
                        style = Stroke(width = (8f * fade).coerceAtLeast(1.5f))
                    )

                    // Secondary Crimson Shockwave Ring
                    val innerProgress = (burstT * 1.25f).coerceIn(0f, 1f)
                    val innerFade = (1f - innerProgress).coerceIn(0f, 1f)
                    val innerX = size.width * (0.48f + innerProgress * 0.45f)
                    val innerY = size.height * (0.55f + innerProgress * 1.6f)
                    drawRoundRect(
                        color = PeppermintRed.copy(alpha = innerFade * 0.75f),
                        topLeft = Offset(cx - innerX, cy - innerY),
                        size = Size(innerX * 2f, innerY * 2f),
                        cornerRadius = CornerRadius(36f, 36f),
                        style = Stroke(width = (5f * innerFade).coerceAtLeast(1f))
                    )

                    // 12 Erupting Origami Diamond & Star Sparks around the meter
                    val sparkColors = listOf(
                        StarCardboardGold,
                        PeppermintRed,
                        StickyNoteYellow,
                        OrigamiFoxOrange,
                        PeppermintWhite
                    )
                    for (i in 0 until 12) {
                        val angle = (i * (360f / 12f) + 15f) * (PI.toFloat() / 180f)
                        val distX = (size.width * 0.22f) + (size.width * 0.36f * burstT)
                        val distY = (size.height * 0.45f) + (size.height * 1.75f * burstT)
                        val sx = cx + cos(angle) * distX
                        val sy = cy + sin(angle) * distY
                        val sparkRadius = (11f * fade).coerceAtLeast(2f)
                        val sparkColor = sparkColors[i % sparkColors.size].copy(alpha = fade)

                        rotate(degrees = burstT * 320f + i * 30f, pivot = Offset(sx, sy)) {
                            val diamond = Path().apply {
                                moveTo(sx, sy - sparkRadius)
                                lineTo(sx + sparkRadius * 0.7f, sy)
                                lineTo(sx, sy + sparkRadius)
                                lineTo(sx - sparkRadius * 0.7f, sy)
                                close()
                            }
                            drawPath(path = diamond, color = sparkColor)
                        }
                    }
                }
            }
        }

        // Main Meter Pill Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(if (isFever || isBursting) 6.dp else 3.dp, barShape)
                .clip(barShape)
                .background(if (isFever) Color(0xFF2B160C) else MaskingTapeCream, barShape)
                .border(
                    width = if (isFever || isBursting) 2.2.dp else 1.2.dp,
                    color = when {
                        isBursting -> PeppermintWhite
                        isFever -> StarCardboardGold
                        isNearFull -> OrigamiFoxOrange
                        else -> CardboardDark
                    },
                    shape = barShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Animated Fill Bar + Washi Chevrons + Foil Shimmer + Leading Edge Spark Crest
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillW = (size.width * animatedFill).coerceIn(0f, size.width)
                val h = size.height

                // Base Gradient Fill
                val fillColors = if (isFever || isBursting) {
                    listOf(
                        PeppermintRed,
                        OrigamiFoxOrange,
                        StarCardboardGold,
                        PeppermintRed
                    )
                } else if (isNearFull) {
                    listOf(StarCardboardGold, OrigamiFoxOrange, PeppermintRed)
                } else {
                    listOf(StickyNoteYellow, StarCardboardGold, OrigamiFoxOrange)
                }

                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = fillColors,
                        startX = 0f,
                        endX = fillW.coerceAtLeast(1f)
                    ),
                    size = Size(fillW, h)
                )

                // Scrolling Diagonal Origami Washi Stripes during Fever Mode or Near-Full Charge
                if ((isFever || isBursting || isNearFull) && fillW > 12f) {
                    val stripeSpacing = 28f
                    val offsetShift = flowPhase * stripeSpacing
                    val stripeAlpha = if (isFever || isBursting) 0.28f else 0.16f
                    var x = -h + offsetShift
                    while (x < fillW + h) {
                        val stripePath = Path().apply {
                            moveTo(x, h)
                            lineTo(x + 12f, h)
                            lineTo(x + 12f + h * 0.65f, 0f)
                            lineTo(x + h * 0.65f, 0f)
                            close()
                        }
                        drawPath(
                            path = stripePath,
                            color = PeppermintWhite.copy(alpha = stripeAlpha)
                        )
                        x += stripeSpacing
                    }

                    // High-Energy Foil Shimmer Sweep across the bar
                    val shimmerCenter = ((flowPhase * 1.4f) - 0.2f) * fillW
                    val shimmerHalfW = 46f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                PeppermintWhite.copy(alpha = if (isFever) 0.48f else 0.28f),
                                Color.Transparent
                            ),
                            startX = shimmerCenter - shimmerHalfW,
                            endX = shimmerCenter + shimmerHalfW
                        ),
                        size = Size(fillW, h)
                    )
                }

                // Glowing Leading-Edge Charge Crest when building charge
                if (!isFever && fillW > 8f && fillW < size.width - 2f) {
                    val crestAlpha = if (isNearFull) 0.65f + pulsePhase * 0.35f else 0.55f
                    drawCircle(
                        color = PeppermintWhite.copy(alpha = crestAlpha),
                        radius = if (isNearFull) h * 0.48f else h * 0.32f,
                        center = Offset(fillW, h * 0.5f)
                    )
                }

                // Full-Charge Ignition White-Gold Flash Overlay (first 320ms of burst)
                if (isBursting && burstT < 0.38f) {
                    val flashAlpha = ((0.38f - burstT) / 0.38f).coerceIn(0f, 1f) * 0.72f
                    drawRect(
                        color = PeppermintWhite.copy(alpha = flashAlpha),
                        size = size
                    )
                }
            }

            // Left & Right Spinning Origami Pinwheel Emblems during Fever Mode
            if (isFever || isBursting) {
                OrigamiSpinningPinwheelIcon(
                    rotationDeg = flowPhase * 360f,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(16.dp)
                )
                OrigamiSpinningPinwheelIcon(
                    rotationDeg = -flowPhase * 360f,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .size(16.dp)
                )
            }

            // Center Label + Fever Move Star Pips
            Text(
                text = when {
                    isBursting && burstT < 0.45f -> "★ FULL CHARGE! ORIGAMI FEVER IGNITED! 2x SCORE! ★"
                    isFever -> {
                        val pips = "★".repeat(feverMovesLeft.coerceIn(1, 5))
                        "$pips ORIGAMI FEVER 2x SCORE! ($feverMovesLeft MOVES) $pips"
                    }
                    isNearFull -> "⚡ ALMOST FEVER! (${(feverCharge * 100).toInt()}%) • ${mechanicLabel.uppercase()}"
                    streakCount >= 2 -> "HOT STREAK x$streakCount • ${mechanicLabel.uppercase()}"
                    mechanicLabel.isNotBlank() -> mechanicLabel.uppercase()
                    else -> "FEVER METER • MATCH 4+ FOR LINE CUTTER!"
                },
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (isFever || isBursting) 11.sp else 10.5.sp,
                    fontWeight = FontWeight.Black
                ),
                color = if (isFever || isBursting) PeppermintWhite else InkBrown,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun OrigamiSpinningPinwheelIcon(
    rotationDeg: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val r = size.minDimension * 0.46f
        val bladeColors = listOf(
            StarCardboardGold,
            PeppermintWhite,
            StickyNoteYellow,
            OrigamiFoxOrange
        )
        rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
            for (i in 0 until 4) {
                rotate(degrees = i * 90f, pivot = Offset(cx, cy)) {
                    val blade = Path().apply {
                        moveTo(cx, cy)
                        lineTo(cx + r * 0.45f, cy - r * 0.25f)
                        lineTo(cx + r, cy)
                        lineTo(cx + r * 0.35f, cy + r * 0.35f)
                        close()
                    }
                    drawPath(path = blade, color = bladeColors[i])
                }
            }
            drawCircle(
                color = PeppermintRed,
                radius = r * 0.24f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun CardboardScoreBanner(
    score: Int,
    highScore: Int,
    modifier: Modifier = Modifier
) {
    val bannerShape = TornPaperShape(teethCount = 5, toothDepthPx = 7f)
    Column(
        modifier = modifier
            .shadow(4.dp, bannerShape)
            .background(CardboardMedium, bannerShape)
            .border(2.dp, CardboardDark, bannerShape)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("score_banner"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StickyNoteYellow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = stringResource(R.string.label_score),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                color = PeppermintWhite
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StickyNoteYellow,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 21.sp),
            color = PeppermintWhite
        )
        Text(
            text = stringResource(R.string.label_high_score, highScore),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
            color = StickyNoteYellow
        )
    }
}

@Composable
fun TornGoalNote(
    primaryGoal: LevelGoal,
    secondaryGoal: LevelGoal?,
    initialGoldCount: Int = 0,
    goldRemaining: Int = 0,
    initialTapeCount: Int = 0,
    tapeRemaining: Int = 0,
    modifier: Modifier = Modifier
) {
    val noteShape = TornPaperShape(teethCount = 6, toothDepthPx = 6f, tearTopBottom = true)
    Column(
        modifier = modifier
            .shadow(4.dp, noteShape)
            .background(MaskingTapeCream, noteShape)
            .border(1.5.dp, MaskingTapeBorder, noteShape)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag("next_goal_box"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.label_next_goal),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 10.sp),
            color = InkSoftBrown
        )
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            GoalItemChip(goal = primaryGoal)
            if (secondaryGoal != null) {
                GoalItemChip(goal = secondaryGoal)
            }
            if (initialGoldCount > 0) {
                MechanicGoalChip(
                    badgeText = "★",
                    badgeBg = StarCardboardGold,
                    remaining = goldRemaining
                )
            }
            if (initialTapeCount > 0) {
                MechanicGoalChip(
                    badgeText = "✕",
                    badgeBg = PeppermintRed,
                    badgeFg = PeppermintWhite,
                    remaining = tapeRemaining
                )
            }
        }
    }
}

@Composable
private fun GoalItemChip(goal: LevelGoal) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        PaperPieceVector(
            pieceType = goal.targetPiece,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = if (goal.isCompleted) "✓" else "${goal.remaining}",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
            color = if (goal.isCompleted) OrigamiFoxGreen else InkBrown
        )
    }
}

@Composable
private fun MechanicGoalChip(
    badgeText: String,
    badgeBg: Color,
    badgeFg: Color = InkBrown,
    remaining: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .background(badgeBg, CircleShape)
                .border(1.2.dp, InkBrown, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = badgeFg
            )
        }
        Text(
            text = if (remaining <= 0) "✓" else "$remaining",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
            color = if (remaining <= 0) OrigamiFoxGreen else InkBrown
        )
    }
}

@Composable
fun MovesStickyNote(
    movesRemaining: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(2.8f)
            .testTag("moves_sticky_note"),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .background(StickyNoteYellow, RoundedCornerShape(4.dp))
                .border(1.5.dp, StickyNoteShadow, RoundedCornerShape(4.dp))
                .padding(top = 10.dp, bottom = 7.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.label_moves, movesRemaining),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (movesRemaining <= 5) PeppermintRed else InkBrown,
                textAlign = TextAlign.Center
            )
        }

        // Glossy Crimson 3D Thumbtack
        Box(
            modifier = Modifier
                .size(15.dp)
                .shadow(3.dp, CircleShape)
                .background(ThumbtackRed, CircleShape)
                .border(1.dp, Color(0xFFB71C1C), CircleShape),
            contentAlignment = Alignment.TopStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 3.dp, y = 3.dp)
                    .size(4.5.dp)
                    .background(ThumbtackHighlight, CircleShape)
            )
        }
    }
}

@Composable
fun PaperBoosterBar(
    state: PaperGameState,
    onSelectBooster: (BoosterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isBoosterTutorialActive = state.tutorialPrompt != null &&
        !state.tutorialDismissed &&
        state.tutorialPrompt.targetType == TutorialTargetType.BOOSTER_BAR

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (state.activeBooster != null) {
            val hintText = when (state.activeBooster) {
                BoosterType.SCISSORS -> stringResource(R.string.booster_hint_scissors)
                BoosterType.GLUE -> if (state.glueFirstPos == null) {
                    stringResource(R.string.booster_hint_glue_first)
                } else {
                    stringResource(R.string.booster_hint_glue_second)
                }
                BoosterType.HAMMER -> stringResource(R.string.booster_hint_hammer)
            }
            Surface(
                shape = TornPaperShape(teethCount = 6, toothDepthPx = 7f),
                color = StickyNoteYellow,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = InkBrown,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        } else if (isBoosterTutorialActive) {
            val calloutShape = TornPaperShape(teethCount = 6, toothDepthPx = 6f)
            Surface(
                shape = calloutShape,
                color = StickyNoteYellow,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .border(2.dp, PeppermintRed, calloutShape)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "▼ TAP ANY CRAFT BOOSTER BELOW! (FREE POWER-UP) ▼",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = PeppermintRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterType.entries.forEach { booster ->
                val count = state.boosters[booster] ?: 0
                val isSelected = state.activeBooster == booster
                val isTutorialSpotlight = isBoosterTutorialActive
                PaperBoosterButton(
                    booster = booster,
                    count = count,
                    isSelected = isSelected,
                    isTutorialSpotlight = isTutorialSpotlight,
                    onClick = { onSelectBooster(booster) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PaperBoosterButton(
    booster: BoosterType,
    count: Int,
    isSelected: Boolean,
    isTutorialSpotlight: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cutShape = TornPaperShape(teethCount = 5, toothDepthPx = 6f)
    val bgColor = when {
        isSelected -> StickyNoteYellow
        isTutorialSpotlight -> StickyNoteYellow.copy(alpha = 0.92f)
        count > 0 -> MaskingTapeCream
        else -> CardboardLight.copy(alpha = 0.65f)
    }
    val borderColor = when {
        isSelected || isTutorialSpotlight -> PeppermintRed
        else -> CardboardDark
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .offset(y = if (isSelected || isTutorialSpotlight) (-4).dp else 0.dp)
            .shadow(if (isSelected || isTutorialSpotlight) 7.dp else 3.dp, cutShape)
            .clip(cutShape)
            .background(bgColor, cutShape)
            .border(if (isSelected || isTutorialSpotlight) 2.5.dp else 1.5.dp, borderColor, cutShape)
            .clickable(enabled = count > 0 || isSelected, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("booster_btn_${booster.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BoosterVectorIcon(
                    boosterType = booster,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = booster.actionLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = InkBrown
                    )
                    Text(
                        text = booster.title.substringBefore(" "),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                        color = InkSoftBrown
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .shadow(2.dp, CircleShape)
                    .background(if (count > 0) PeppermintRed else CardboardMedium, CircleShape)
                    .border(1.5.dp, PeppermintWhite, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = PeppermintWhite
                )
            }
        }
    }
}
