package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LEVELS_PER_STAGE
import com.example.model.PaperGameState
import com.example.model.PaperStageInfo
import com.example.model.PieceType
import com.example.model.buildGoalsForLevel
import com.example.model.getMovesForLevel
import com.example.model.getStageInfoForLevel
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.CraftTableKraft
import com.example.ui.theme.InkBrown
import com.example.ui.theme.InkSoftBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.SkyBluePaper
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteShadow
import com.example.ui.theme.StickyNoteYellow
import com.example.ui.theme.ThumbtackRed
import kotlin.math.sin

/**
 * Candy Crush-inspired winding Paper Craft Saga World Map showing stages,
 * Winding stitched-paper paths, 1-3 star badges per level, a bouncing Origami Fox
 * avatar on the player's current level, and an interactive level goal inspector.
 */
@Composable
fun PaperWorldMapOverlay(
    state: PaperGameState,
    onSelectLevel: (Int) -> Unit,
    onPlayLevel: (Int) -> Unit,
    onCloseMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.showWorldMap) {
        BackHandler(onBack = onCloseMap)
    }

    AnimatedVisibility(
        visible = state.showWorldMap,
        enter = fadeIn(tween(220)) + slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(180)) + slideOutVertically(targetOffsetY = { it / 6 }),
        modifier = modifier
    ) {
        val totalStages = maxOf(4, ((state.highestUnlockedLevel + 4) / LEVELS_PER_STAGE) + 1)
        val stages = remember(totalStages) {
            (1..totalStages).map { stageIdx ->
                val firstLevel = (stageIdx - 1) * LEVELS_PER_STAGE + 1
                getStageInfoForLevel(firstLevel)
            }
        }

        val listState = rememberLazyListState()
        LaunchedEffect(state.showWorldMap, state.level) {
            if (state.showWorldMap) {
                val activeStageIndex = ((state.level - 1) / LEVELS_PER_STAGE).coerceIn(0, stages.lastIndex)
                listState.scrollToItem(activeStageIndex)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SkyBluePaper,
                            Color(0xFFD8F3DC),
                            CraftTableKraft
                        )
                    )
                )
                .testTag("paper_world_map_overlay")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .widthIn(max = 540.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Top World Map Header Bar
                WorldMapHeaderBar(
                    state = state,
                    onCloseMap = onCloseMap
                )

                // 2. Scrollable Winding Stage & Level Path
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.5.dp, CardboardDark, RoundedCornerShape(18.dp))
                        .background( Color(0xF2F7EFE1), RoundedCornerShape(18.dp) ),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(stages, key = { it.stageNumber }) { stageInfo ->
                        PaperStageMapSection(
                            stageInfo = stageInfo,
                            state = state,
                            onSelectLevel = onSelectLevel
                        )
                    }
                }

                // 3. Bottom Selected Level Inspector & Play Button
                SelectedLevelInspectorCard(
                    state = state,
                    onPlayLevel = onPlayLevel
                )
            }
        }
    }
}

@Composable
private fun WorldMapHeaderBar(
    state: PaperGameState,
    onCloseMap: () -> Unit
) {
    val tapeShape = TornPaperShape(teethCount = 7, toothDepthPx = 6f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, tapeShape)
            .background(MaskingTapeCream, tapeShape)
            .border(1.5.dp, CardboardDark, tapeShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Total Stars Counter Pill
        Row(
            modifier = Modifier
                .shadow(2.dp, RoundedCornerShape(99.dp))
                .background(CardboardDark, RoundedCornerShape(99.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = StickyNoteYellow,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${state.totalStars}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = PeppermintWhite
            )
        }

        // Map Title & Current Stage Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.title_world_map),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = InkBrown
            )
            Text(
                text = "Stage ${state.currentStage.stageNumber}: ${state.currentStage.title}",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp),
                color = PeppermintRed
            )
        }

        // Close / Resume Game Button
        IconButton(
            onClick = onCloseMap,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .shadow(3.dp, CircleShape)
                .background(StickyNoteYellow, CircleShape)
                .border(1.5.dp, CardboardDark, CircleShape)
                .testTag("close_world_map_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.btn_back_to_game),
                tint = InkBrown
            )
        }
    }
}

@Composable
private fun PaperStageMapSection(
    stageInfo: PaperStageInfo,
    state: PaperGameState,
    onSelectLevel: (Int) -> Unit
) {
    val firstLevel = (stageInfo.stageNumber - 1) * LEVELS_PER_STAGE + 1
    val levelsInStage = (firstLevel until (firstLevel + LEVELS_PER_STAGE)).toList()
    val stageStarsEarned = levelsInStage.sumOf { state.levelStars[it] ?: 0 }
    val maxStageStars = LEVELS_PER_STAGE * 3
    val isCurrentStage = state.level in levelsInStage
    val isStageUnlocked = state.highestUnlockedLevel >= firstLevel

    val cardShape = RoundedCornerShape(16.dp)
    val horizontalFractions = remember(stageInfo.stageNumber) {
        levelsInStage.mapIndexed { idx, lvl ->
            // Winding Candy-Crush S-curve horizontal positions (0.20f .. 0.80f)
            val phase = (lvl * 1.15f) + (if (stageInfo.stageNumber % 2 == 0) 0.8f else 0f)
            (0.50f + sin(phase.toDouble()).toFloat() * 0.28f).coerceIn(0.18f, 0.82f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrentStage) 6.dp else 3.dp, cardShape)
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        stageInfo.bannerColor.copy(alpha = 0.18f),
                        MaskingTapeCream,
                        stageInfo.accentColor.copy(alpha = 0.12f)
                    )
                ),
                cardShape
            )
            .border(
                width = if (isCurrentStage) 2.5.dp else 1.5.dp,
                color = if (isCurrentStage) stageInfo.bannerColor else MaskingTapeBorder,
                shape = cardShape
            )
            .padding(12.dp)
            .testTag("stage_card_${stageInfo.stageNumber}")
    ) {
        // Stage Banner Header (Torn Tape Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, TornPaperShape(teethCount = 6, toothDepthPx = 6f))
                .background(
                    if (isStageUnlocked) stageInfo.bannerColor else CardboardMedium,
                    TornPaperShape(teethCount = 6, toothDepthPx = 6f)
                )
                .border(1.5.dp, CardboardDark, TornPaperShape(teethCount = 6, toothDepthPx = 6f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.dp, CardboardDark, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    PaperPieceVector(
                        pieceType = stageInfo.mascotPiece,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Column {
                    Text(
                        text = "STAGE ${stageInfo.stageNumber} • ${stageInfo.title.uppercase()}",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = PeppermintWhite
                    )
                    Text(
                        text = stageInfo.subtitle,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp),
                        color = StickyNoteYellow
                    )
                }
            }

            // Stage Stars Counter
            Row(
                modifier = Modifier
                    .background( Color(0x38000000), RoundedCornerShape(99.dp) )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = StickyNoteYellow,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "$stageStarsEarned/$maxStageStars",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                    color = PeppermintWhite
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Winding Trail + Level Nodes
        val rowHeightDp = 82.dp
        val totalTrailHeight = rowHeightDp * LEVELS_PER_STAGE

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalTrailHeight)
        ) {
            // Winding Dashed Paper Trail Canvas behind the nodes
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stepH = h / LEVELS_PER_STAGE

                val points = horizontalFractions.mapIndexed { idx, frac ->
                    Offset(
                        x = w * frac,
                        y = stepH * (idx + 0.5f)
                    )
                }

                if (points.size >= 2) {
                    val trailPath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.lastIndex) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val midY = (p0.y + p1.y) * 0.5f
                            cubicTo(
                                p0.x, midY,
                                p1.x, midY,
                                p1.x, p1.y
                            )
                        }
                    }

                    // Outer kraft-paper road backing
                    drawPath(
                        path = trailPath,
                        color = CardboardMedium.copy(alpha = 0.65f),
                        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Inner ivory paper ribbon
                    drawPath(
                        path = trailPath,
                        color = MaskingTapeCream,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Stitched dashed Candy-Crush trail line
                    drawPath(
                        path = trailPath,
                        color = stageInfo.bannerColor,
                        style = Stroke(
                            width = 3.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f)
                        )
                    )
                }
            }

            // Place the 5 Level Pin Nodes along the winding trail
            Column(modifier = Modifier.fillMaxSize()) {
                levelsInStage.forEachIndexed { idx, lvl ->
                    val frac = horizontalFractions[idx]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeightDp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(frac.coerceIn(0.18f, 0.88f)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            LevelPinNode(
                                level = lvl,
                                isUnlocked = lvl <= state.highestUnlockedLevel,
                                isCurrentPlaying = lvl == state.level,
                                isFrontier = lvl == state.highestUnlockedLevel,
                                isSelectedOnMap = lvl == state.mapSelectedLevel,
                                stars = state.levelStars[lvl] ?: 0,
                                stageColor = stageInfo.bannerColor,
                                onClick = { onSelectLevel(lvl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelPinNode(
    level: Int,
    isUnlocked: Boolean,
    isCurrentPlaying: Boolean,
    isFrontier: Boolean,
    isSelectedOnMap: Boolean,
    stars: Int,
    stageColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "node_pulse_$level")
    val avatarBounce by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_bounce"
    )
    val frontierPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isFrontier || isCurrentPlaying) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "frontier_pulse"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelectedOnMap) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "pin_scale"
    )

    val pinBg = when {
        !isUnlocked -> CardboardLight
        isCurrentPlaying -> PeppermintRed
        isFrontier -> OrigamiFoxOrange
        stars > 0 -> stageColor
        else -> StickyNoteYellow
    }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(80.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Pulsing halo ring behind active/frontier level
        if (isCurrentPlaying || isFrontier) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = frontierPulse
                        scaleY = frontierPulse
                    }
                    .background(StickyNoteYellow.copy(alpha = 0.55f), CircleShape)
                    .border(2.dp, PeppermintRed, CircleShape)
            )
        }

        // Main circular 3D paper level button
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(if (isUnlocked) 5.dp else 2.dp, CircleShape)
                .clip(CircleShape)
                .background(pinBg, CircleShape)
                .border(
                    width = if (isSelectedOnMap) 3.dp else 2.dp,
                    color = if (isSelectedOnMap) StickyNoteYellow else CardboardDark,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .testTag("map_level_node_$level"),
            contentAlignment = Alignment.Center
        ) {
            if (isUnlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (isCurrentPlaying || isFrontier || stars > 0) PeppermintWhite else InkBrown
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = InkSoftBrown,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                        color = InkSoftBrown
                    )
                }
            }
        }

        // Bouncing Origami Fox Avatar Marker on the player's current level
        if (isCurrentPlaying) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = avatarBounce.dp - 6.dp)
                    .shadow(3.dp, RoundedCornerShape(99.dp))
                    .background(StickyNoteYellow, RoundedCornerShape(99.dp))
                    .border(1.5.dp, CardboardDark, RoundedCornerShape(99.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PaperPieceVector(
                    pieceType = PieceType.ORIGAMI_FOX,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "YOU",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = PeppermintRed
                )
            }
        }

        // 1 to 3 Star Ribbon underneath unlocked levels
        if (isUnlocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 2.dp)
                    .shadow(2.dp, RoundedCornerShape(99.dp))
                    .background(MaskingTapeCream, RoundedCornerShape(99.dp))
                    .border(1.dp, CardboardDark, RoundedCornerShape(99.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (s in 1..3) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (s <= stars) StarCardboardGold else CardboardLight,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedLevelInspectorCard(
    state: PaperGameState,
    onPlayLevel: (Int) -> Unit
) {
    val selectedLvl = state.mapSelectedLevel.coerceAtLeast(1)
    val stageInfo = getStageInfoForLevel(selectedLvl)
    val (primaryGoal, secondaryGoal) = remember(selectedLvl) { buildGoalsForLevel(selectedLvl) }
    val moves = remember(selectedLvl) { getMovesForLevel(selectedLvl) }
    val earnedStars = state.levelStars[selectedLvl] ?: 0
    val isCurrentActive = selectedLvl == state.level && state.score > 0

    val noteShape = TornPaperShape(teethCount = 7, toothDepthPx = 6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, noteShape)
            .background(StickyNoteYellow, noteShape)
            .border(2.dp, CardboardDark, noteShape)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("map_selected_level_card"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_stage_level, stageInfo.stageNumber, selectedLvl),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = InkBrown
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (s in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (s <= earnedStars) OrigamiFoxOrange else StickyNoteShadow,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // Goal Preview Chips + Moves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PaperPieceVector(
                        pieceType = primaryGoal.targetPiece,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${primaryGoal.targetCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = InkBrown
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PaperPieceVector(
                        pieceType = secondaryGoal.targetPiece,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${secondaryGoal.targetCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = InkBrown
                    )
                }
                Text(
                    text = "• $moves Moves",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = PeppermintRed
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = { onPlayLevel(selectedLvl) },
            colors = ButtonDefaults.buttonColors(
                containerColor = PeppermintRed,
                contentColor = PeppermintWhite
            ),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier
                .height(48.dp)
                .testTag("map_play_level_button")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isCurrentActive) {
                    stringResource(R.string.btn_back_to_game)
                } else {
                    stringResource(R.string.btn_play_level, selectedLvl)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
