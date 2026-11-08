package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BoosterType
import com.example.model.LevelGoal
import com.example.model.PaperGameState
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

@Composable
fun PaperTopHud(
    state: PaperGameState,
    onToggleSound: () -> Unit,
    onResetProgress: () -> Unit,
    onOpenWorldMap: () -> Unit = {},
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
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
                        tint = InkBrown
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
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("open_world_map_button"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = stringResource(R.string.btn_world_map),
                        tint = PeppermintRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Map",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = InkBrown
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onOpenWorldMap)
            ) {
                Box(
                    modifier = Modifier
                        .rotate(-1.5f)
                        .shadow(3.dp, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .background(MaskingTapeCream, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .border(1.dp, MaskingTapeBorder, TornPaperShape(teethCount = 6, toothDepthPx = 7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("header_paper_craft_tape")
                ) {
                    Text(
                        text = "Stage ${stageInfo.stageNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 14.sp,
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
                        .padding(horizontal = 11.dp, vertical = 4.dp)
                        .testTag("header_level_tape")
                ) {
                    Text(
                        text = stringResource(R.string.header_torn_level, state.level),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                        color = PeppermintRed
                    )
                }
            }

            IconButton(
                onClick = onResetProgress,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .shadow(3.dp, CircleShape)
                    .background(MaskingTapeCream, CircleShape)
                    .border(1.5.dp, CardboardMedium, CircleShape)
                    .testTag("reset_level_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.btn_retry_level),
                    tint = InkBrown
                )
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
            streakCount = state.streakCount,
            mechanicLabel = state.mechanicLabel
        )
    }
}

@Composable
private fun OrigamiFeverStrip(
    feverCharge: Float,
    feverMovesLeft: Int,
    streakCount: Int,
    mechanicLabel: String = ""
) {
    val isFever = feverMovesLeft > 0
    val animatedFill by animateFloatAsState(
        targetValue = if (isFever) 1f else feverCharge.coerceIn(0.05f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "fever_fill"
    )
    val barShape = RoundedCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .shadow(3.dp, barShape)
            .clip(barShape)
            .background(MaskingTapeCream, barShape)
            .border(
                width = if (isFever) 2.dp else 1.dp,
                color = if (isFever) PeppermintRed else CardboardDark,
                shape = barShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFill)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        if (isFever) {
                            listOf(PeppermintRed, OrigamiFoxOrange, StarCardboardGold)
                        } else {
                            listOf(StickyNoteYellow, StarCardboardGold, OrigamiFoxOrange)
                        }
                    )
                )
        )
        Text(
            text = when {
                isFever -> "★ ORIGAMI FEVER 2x SCORE! ($feverMovesLeft MOVES) ★"
                streakCount >= 2 -> "HOT STREAK x$streakCount • ${mechanicLabel.uppercase()}"
                mechanicLabel.isNotBlank() -> mechanicLabel.uppercase()
                else -> "FEVER METER • MATCH 4+ FOR LINE CUTTER!"
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isFever) PeppermintWhite else InkBrown,
            maxLines = 1
        )
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
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoosterType.entries.forEach { booster ->
                val count = state.boosters[booster] ?: 0
                val isSelected = state.activeBooster == booster
                PaperBoosterButton(
                    booster = booster,
                    count = count,
                    isSelected = isSelected,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cutShape = TornPaperShape(teethCount = 5, toothDepthPx = 6f)
    val bgColor = when {
        isSelected -> StickyNoteYellow
        count > 0 -> MaskingTapeCream
        else -> CardboardLight.copy(alpha = 0.65f)
    }
    val borderColor = if (isSelected) PeppermintRed else CardboardDark

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .offset(y = if (isSelected) (-4).dp else 0.dp)
            .shadow(if (isSelected) 6.dp else 3.dp, cutShape)
            .clip(cutShape)
            .background(bgColor, cutShape)
            .border(if (isSelected) 2.5.dp else 1.5.dp, borderColor, cutShape)
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
