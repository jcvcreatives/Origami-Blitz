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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LEVELS_PER_STAGE
import com.example.model.LevelBlueprint
import com.example.model.PaperGameState
import com.example.model.PaperStageInfo
import com.example.model.PieceType
import com.example.model.TOTAL_STAGES
import com.example.model.getLevelBlueprint
import com.example.model.getStageInfoForLevel
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.CraftTableKraft
import com.example.ui.theme.CupcakeFrostingPink
import com.example.ui.theme.InkBrown
import com.example.ui.theme.InkSoftBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.SkyBluePaper
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteShadow
import com.example.ui.theme.StickyNoteYellow
import com.example.ui.theme.WrappedCandyPurple
import kotlin.math.sin

/**
 * Illustrated Pop-Up Origami Storybook World Map showing all 10 Stages (50 Levels),
 * lush paper pine forests, woodland animals (Origami Foxes, Bears, Bunnies, Cranes),
 * paper-cutout villagers & bakers holding frosted Cupcakes and Candy blocks,
 * and live on-map Block Goal Previews showing what is inside every level's blocks.
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
        val stages = remember {
            (1..TOTAL_STAGES).map { stageIdx ->
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

                // 2. Scrollable Winding Stage & Level Path with Forest, Animals & Cupcake Villagers
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .border(2.5.dp, CardboardDark, RoundedCornerShape(18.dp))
                        .background(Color(0xF5F4EBD9), RoundedCornerShape(18.dp)),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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

    val cardShape = RoundedCornerShape(18.dp)
    val horizontalFractions = remember(stageInfo.stageNumber) {
        levelsInStage.map { lvl ->
            // Winding Candy-Crush S-curve horizontal positions (0.24f .. 0.76f)
            val phase = (lvl * 1.15f) + (if (stageInfo.stageNumber % 2 == 0) 0.8f else 0f)
            (0.50f + sin(phase.toDouble()).toFloat() * 0.25f).coerceIn(0.22f, 0.78f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stage_diorama_${stageInfo.stageNumber}")
    val dioramaSway by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diorama_sway"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isCurrentStage) 7.dp else 4.dp, cardShape)
            .clip(cardShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFDFF2FA),
                        Color(0xFFD8F3DC),
                        Color(0xFFF6EBD8)
                    )
                ),
                cardShape
            )
            .border(
                width = if (isCurrentStage) 2.5.dp else 1.5.dp,
                color = if (isCurrentStage) stageInfo.bannerColor else CardboardDark,
                shape = cardShape
            )
            .padding(10.dp)
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
                        .size(36.dp)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.5.dp, CardboardDark, CircleShape)
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
                    .background(Color(0x38000000), RoundedCornerShape(99.dp))
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

        Spacer(modifier = Modifier.height(6.dp))

        // Kid-and-Adult-Friendly "How to Play This Stage" Rule Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(StickyNoteYellow.copy(alpha = 0.88f), RoundedCornerShape(10.dp))
                .border(1.dp, CardboardDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = stageInfo.easyRuleBadge,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.5.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = InkBrown
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Winding Trail + Pop-Up Forest, Animals, Cupcake People & Level Nodes
        val rowHeightDp = 92.dp
        val totalTrailHeight = rowHeightDp * LEVELS_PER_STAGE

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalTrailHeight)
                .clip(RoundedCornerShape(14.dp))
                .border(1.5.dp, MaskingTapeBorder, RoundedCornerShape(14.dp))
        ) {
            // Illustrated Pop-Up Forest, Animals, Cupcake Villagers & Winding Paper Trail Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stepH = h / LEVELS_PER_STAGE

                // 1. Layered Paper Meadow Hills & Winding Woodland Stream
                drawStageMeadowAndStream(w, h, stageInfo.stageNumber)

                // 2. Winding Stitched Paper Road connecting the 5 levels
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

                    // Road drop shadow
                    drawPath(
                        path = trailPath,
                        color = Color(0x283B2314),
                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Outer kraft-paper road backing
                    drawPath(
                        path = trailPath,
                        color = CardboardMedium,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Inner ivory paper ribbon
                    drawPath(
                        path = trailPath,
                        color = MaskingTapeCream,
                        style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
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

                // 3. Origami Pine Forest Clusters, Woodland Animals & Cupcake Villagers along each row
                points.forEachIndexed { idx, nodePt ->
                    val nodeOnLeft = horizontalFractions[idx] < 0.5f
                    // Place the illustrated diorama vignette on the side opposite the block-goal badge,
                    // and trees/flowers on the outer margins!
                    val treeLeftX = w * 0.08f
                    val treeRightX = w * 0.92f
                    val rowCenterY = nodePt.y

                    // Outer forest border pines & mushrooms
                    drawOrigamiPineTree(
                        center = Offset(treeLeftX, rowCenterY - 12.dp.toPx()),
                        scale = 0.85f + (idx % 2) * 0.15f,
                        tint = if ((idx + stageInfo.stageNumber) % 2 == 0) OrigamiCraneBlue else OrigamiFoxGreen
                    )
                    drawOrigamiPineTree(
                        center = Offset(treeRightX, rowCenterY + 8.dp.toPx()),
                        scale = 0.90f,
                        tint = if (idx % 2 == 0) OrigamiFoxGreen else OrigamiCraneBlue
                    )

                    // Scenic Character / Animal / Bakery Vignette beside the road
                    val vignetteX = if (nodeOnLeft) w * 0.82f else w * 0.18f
                    val vignetteCenter = Offset(vignetteX, rowCenterY + (dioramaSway * 2.5f).dp.toPx())

                    when ((idx + stageInfo.stageNumber) % 5) {
                        0 -> drawCupcakeBakerVillager(vignetteCenter)
                        1 -> drawOrigamiFoxAndBunny(vignetteCenter)
                        2 -> drawCandyVillagerWithPeppermint(vignetteCenter, stageInfo.bannerColor)
                        3 -> drawWoodlandBearWithHoneyStar(vignetteCenter)
                        else -> drawPaperSweetBakeryCottage(vignetteCenter, stageInfo.bannerColor)
                    }
                }
            }

            // Place the 5 Level Pin Nodes + Live "What's In The Blocks" Preview Badges along the trail
            Column(modifier = Modifier.fillMaxSize()) {
                levelsInStage.forEachIndexed { idx, lvl ->
                    val frac = horizontalFractions[idx]
                    val bp = remember(lvl) { getLevelBlueprint(lvl) }
                    LevelRowWithBlockPreview(
                        level = lvl,
                        blueprint = bp,
                        horizontalFraction = frac,
                        rowHeightDp = rowHeightDp,
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

/**
 * Renders a level row containing both the interactive 3D Paper Level Pin Node AND a floating
 * "What's in the Blocks" Goal Preview Bubble right beside the node so players can see the
 * Cupcakes, Origami Foxes, Cranes, Peppermints, Wrapped Candies, and Stars inside every level!
 */
@Composable
private fun LevelRowWithBlockPreview(
    level: Int,
    blueprint: LevelBlueprint,
    horizontalFraction: Float,
    rowHeightDp: androidx.compose.ui.unit.Dp,
    isUnlocked: Boolean,
    isCurrentPlaying: Boolean,
    isFrontier: Boolean,
    isSelectedOnMap: Boolean,
    stars: Int,
    stageColor: Color,
    onClick: () -> Unit
) {
    val placeBadgeToRight = horizontalFraction <= 0.52f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeightDp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(
                if (placeBadgeToRight) {
                    (horizontalFraction + 0.34f).coerceIn(0.45f, 0.96f)
                } else {
                    (horizontalFraction + 0.12f).coerceIn(0.32f, 0.92f)
                }
            ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!placeBadgeToRight) {
                    LevelBlockContentsPill(
                        blueprint = blueprint,
                        isUnlocked = isUnlocked,
                        isBoss = level % LEVELS_PER_STAGE == 0,
                        onClick = onClick
                    )
                }

                LevelPinNode(
                    level = level,
                    isUnlocked = isUnlocked,
                    isCurrentPlaying = isCurrentPlaying,
                    isFrontier = isFrontier,
                    isSelectedOnMap = isSelectedOnMap,
                    stars = stars,
                    stageColor = stageColor,
                    onClick = onClick
                )

                if (placeBadgeToRight) {
                    LevelBlockContentsPill(
                        blueprint = blueprint,
                        isUnlocked = isUnlocked,
                        isBoss = level % LEVELS_PER_STAGE == 0,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

/**
 * Miniature Washi Coaster Pill beside each level node showing the actual Origami & Candy blocks
 * inside that level (e.g. Cupcake, Origami Fox, Peppermint, Crane, Gold Stamp ★, Tape ✕).
 */
@Composable
private fun LevelBlockContentsPill(
    blueprint: LevelBlueprint,
    isUnlocked: Boolean,
    isBoss: Boolean,
    onClick: () -> Unit
) {
    val pillShape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .shadow(if (isUnlocked) 3.dp else 1.dp, pillShape)
            .background(
                if (isUnlocked) MaskingTapeCream else Color(0xFFEADBC8),
                pillShape
            )
            .border(
                width = 1.2.dp,
                color = if (isBoss) PeppermintRed else CardboardDark,
                shape = pillShape
            )
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isBoss) {
            Text(
                text = "👑",
                fontSize = 10.sp
            )
        }
        // Primary Block Icon + Count
        Box(modifier = Modifier.size(18.dp)) {
            PaperPieceVector(
                pieceType = blueprint.primaryGoal.targetPiece,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "${blueprint.primaryGoal.targetCount}",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = InkBrown
        )

        // Secondary Block Icon + Count
        Box(modifier = Modifier.size(18.dp)) {
            PaperPieceVector(
                pieceType = blueprint.secondaryGoal.targetPiece,
                modifier = Modifier.fillMaxSize()
            )
        }
        Text(
            text = "${blueprint.secondaryGoal.targetCount}",
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = InkBrown
        )

        // Optional Gold Stamp or Tape Lock indicator
        if (blueprint.goldCells.isNotEmpty()) {
            Text(
                text = "★",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color(0xFFD97706)
            )
        }
        if (blueprint.tapeLocks.isNotEmpty()) {
            Text(
                text = "✕",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                ),
                color = PeppermintRed
            )
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
            .width(74.dp)
            .height(82.dp)
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
                    .offset(y = avatarBounce.dp - 4.dp)
                    .shadow(3.dp, RoundedCornerShape(99.dp))
                    .background(StickyNoteYellow, RoundedCornerShape(99.dp))
                    .border(1.5.dp, CardboardDark, RoundedCornerShape(99.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                PaperPieceVector(
                    pieceType = PieceType.ORIGAMI_FOX,
                    modifier = Modifier.size(15.dp)
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
                    .offset(y = 1.dp)
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

/**
 * Draws layered paper hills, a sparkling paper river stream, wooden footbridges, and wildflower tufts.
 */
private fun DrawScope.drawStageMeadowAndStream(
    w: Float,
    h: Float,
    stageNum: Int
) {
    // Soft rolling paper hills with white torn-paper deckle borders
    val hill1 = Path().apply {
        moveTo(0f, h * 0.22f)
        quadraticTo(w * 0.35f, h * 0.14f, w * 0.70f, h * 0.22f)
        quadraticTo(w * 0.88f, h * 0.26f, w, h * 0.20f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hill1, color = Color(0xFFB7E4C7))
    drawPath(
        hill1,
        color = Color.White.copy(alpha = 0.8f),
        style = Stroke(width = 3.dp.toPx())
    )

    val hill2 = Path().apply {
        moveTo(0f, h * 0.56f)
        quadraticTo(w * 0.30f, h * 0.64f, w * 0.62f, h * 0.54f)
        quadraticTo(w * 0.84f, h * 0.48f, w, h * 0.58f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(hill2, color = Color(0xFF95D5B2))
    drawPath(
        hill2,
        color = Color.White.copy(alpha = 0.8f),
        style = Stroke(width = 3.dp.toPx())
    )

    // Winding sky-blue paper brook / stream across the middle of the meadow
    val streamPath = Path().apply {
        val startX = if (stageNum % 2 == 0) 0f else w
        val endX = if (stageNum % 2 == 0) w else 0f
        moveTo(startX, h * 0.38f)
        cubicTo(
            w * 0.35f, h * 0.32f,
            w * 0.65f, h * 0.46f,
            endX, h * 0.40f
        )
    }
    drawPath(
        path = streamPath,
        color = Color.White.copy(alpha = 0.85f),
        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = streamPath,
        color = Color(0xFF90E0EF),
        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
    )
}

/**
 * Draws a two-tone folded origami pine tree with a kraft trunk and shadow.
 */
private fun DrawScope.drawOrigamiPineTree(
    center: Offset,
    scale: Float,
    tint: Color
) {
    val s = scale * 1.dp.toPx()
    // Shadow
    drawCircle(
        color = Color(0x22000000),
        radius = 12f * s,
        center = Offset(center.x + 2f * s, center.y + 16f * s)
    )
    // Trunk
    drawRoundRect(
        color = Color(0xFF7F4F24),
        topLeft = Offset(center.x - 3.5f * s, center.y + 4f * s),
        size = Size(7f * s, 13f * s),
        cornerRadius = CornerRadius(2f * s, 2f * s)
    )
    // Left sunlit origami canopy half
    val leftHalf = Path().apply {
        moveTo(center.x, center.y - 22f * s)
        lineTo(center.x - 15f * s, center.y + 6f * s)
        lineTo(center.x, center.y + 6f * s)
        close()
    }
    drawPath(leftHalf, color = tint)

    // Right shaded origami canopy half
    val rightHalf = Path().apply {
        moveTo(center.x, center.y - 22f * s)
        lineTo(center.x + 15f * s, center.y + 6f * s)
        lineTo(center.x, center.y + 6f * s)
        close()
    }
    drawPath(
        rightHalf,
        color = Color(
            red = (tint.red * 0.82f).coerceIn(0f, 1f),
            green = (tint.green * 0.82f).coerceIn(0f, 1f),
            blue = (tint.blue * 0.82f).coerceIn(0f, 1f),
            alpha = 1f
        )
    )

    // Crisp outline
    val fullOutline = Path().apply {
        moveTo(center.x, center.y - 22f * s)
        lineTo(center.x - 15f * s, center.y + 6f * s)
        lineTo(center.x + 15f * s, center.y + 6f * s)
        close()
    }
    drawPath(
        fullOutline,
        color = InkBrown,
        style = Stroke(width = 1.6f * s, join = StrokeJoin.Round)
    )
}

/**
 * 1. Draws a cheerful Paper-Cutout Baker Villager holding a giant Frosted Cupcake with a Cherry!
 */
private fun DrawScope.drawCupcakeBakerVillager(center: Offset) {
    val u = 1.dp.toPx()
    // White paper-cutout backing halo
    drawCircle(
        color = MaskingTapeCream,
        radius = 23f * u,
        center = center
    )
    drawCircle(
        color = CardboardDark,
        radius = 23f * u,
        center = center,
        style = Stroke(width = 1.5f * u)
    )

    // Baker body / apron (coral-red tunic + white apron)
    val bodyPath = Path().apply {
        moveTo(center.x - 11f * u, center.y + 18f * u)
        lineTo(center.x - 7f * u, center.y + 1f * u)
        lineTo(center.x + 7f * u, center.y + 1f * u)
        lineTo(center.x + 11f * u, center.y + 18f * u)
        close()
    }
    drawPath(bodyPath, color = PeppermintRed)
    drawPath(bodyPath, color = InkBrown, style = Stroke(width = 1.5f * u, join = StrokeJoin.Round))

    // Baker head
    drawCircle(
        color = Color(0xFFFFE5D0),
        radius = 7.5f * u,
        center = Offset(center.x - 2f * u, center.y - 5f * u)
    )
    drawCircle(
        color = InkBrown,
        radius = 7.5f * u,
        center = Offset(center.x - 2f * u, center.y - 5f * u),
        style = Stroke(width = 1.5f * u)
    )

    // Paper Chef Toque Hat
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(center.x - 9f * u, center.y - 18f * u),
        size = Size(14f * u, 7f * u),
        cornerRadius = CornerRadius(4f * u, 4f * u)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(center.x - 9f * u, center.y - 18f * u),
        size = Size(14f * u, 7f * u),
        cornerRadius = CornerRadius(4f * u, 4f * u),
        style = Stroke(width = 1.3f * u)
    )

    // Smile & eyes
    drawCircle(color = InkBrown, radius = 1.2f * u, center = Offset(center.x - 4.5f * u, center.y - 6f * u))
    drawCircle(color = InkBrown, radius = 1.2f * u, center = Offset(center.x + 0.5f * u, center.y - 6f * u))
    drawCircle(color = CupcakeFrostingPink.copy(alpha = 0.6f), radius = 1.8f * u, center = Offset(center.x - 6f * u, center.y - 3.5f * u))

    // Giant Frosted Cupcake held in the baker's hands!
    val cupX = center.x + 10f * u
    val cupY = center.y + 2f * u
    val cupBase = Path().apply {
        moveTo(cupX - 6f * u, cupY)
        lineTo(cupX + 6f * u, cupY)
        lineTo(cupX + 4.5f * u, cupY + 8f * u)
        lineTo(cupX - 4.5f * u, cupY + 8f * u)
        close()
    }
    drawPath(cupBase, color = Color(0xFFD99B66))
    drawPath(cupBase, color = InkBrown, style = Stroke(width = 1.3f * u))
    // Pink frosting swirl
    drawRoundRect(
        color = CupcakeFrostingPink,
        topLeft = Offset(cupX - 7.5f * u, cupY - 5f * u),
        size = Size(15f * u, 6f * u),
        cornerRadius = CornerRadius(3f * u, 3f * u)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(cupX - 7.5f * u, cupY - 5f * u),
        size = Size(15f * u, 6f * u),
        cornerRadius = CornerRadius(3f * u, 3f * u),
        style = Stroke(width = 1.3f * u)
    )
    // Cherry on top
    drawCircle(color = PeppermintRed, radius = 2.6f * u, center = Offset(cupX, cupY - 7f * u))
}

/**
 * 2. Draws an Origami Woodland Fox & Paper Bunny with a soaring Origami Crane overhead.
 */
private fun DrawScope.drawOrigamiFoxAndBunny(center: Offset) {
    val u = 1.dp.toPx()
    drawCircle(color = MaskingTapeCream, radius = 23f * u, center = center)
    drawCircle(color = CardboardDark, radius = 23f * u, center = center, style = Stroke(width = 1.5f * u))

    // Origami Fox Head + Ears
    val leftEar = Path().apply {
        moveTo(center.x - 13f * u, center.y - 12f * u)
        lineTo(center.x - 4f * u, center.y - 4f * u)
        lineTo(center.x - 14f * u, center.y)
        close()
    }
    val rightEar = Path().apply {
        moveTo(center.x + 5f * u, center.y - 12f * u)
        lineTo(center.x - 4f * u, center.y - 4f * u)
        lineTo(center.x + 6f * u, center.y)
        close()
    }
    drawPath(leftEar, color = Color(0xFFD9480F))
    drawPath(rightEar, color = Color(0xFFD9480F))

    val foxFace = Path().apply {
        moveTo(center.x - 4f * u, center.y - 6f * u)
        lineTo(center.x - 14f * u, center.y)
        lineTo(center.x - 4f * u, center.y + 12f * u)
        lineTo(center.x + 6f * u, center.y)
        close()
    }
    drawPath(foxFace, color = OrigamiFoxOrange)
    drawPath(foxFace, color = InkBrown, style = Stroke(width = 1.5f * u, join = StrokeJoin.Round))
    drawCircle(color = InkBrown, radius = 2f * u, center = Offset(center.x - 4f * u, center.y + 11f * u))
    drawCircle(color = InkBrown, radius = 1.3f * u, center = Offset(center.x - 8f * u, center.y + 1f * u))
    drawCircle(color = InkBrown, radius = 1.3f * u, center = Offset(center.x, center.y + 1f * u))

    // Cute White Paper Bunny peeking beside the Fox
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x + 6f * u, center.y - 12f * u),
        size = Size(4.5f * u, 11f * u)
    )
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x + 11f * u, center.y - 12f * u),
        size = Size(4.5f * u, 11f * u)
    )
    drawCircle(
        color = Color.White,
        radius = 6.5f * u,
        center = Offset(center.x + 10.5f * u, center.y + 3f * u)
    )
    drawCircle(
        color = InkBrown,
        radius = 6.5f * u,
        center = Offset(center.x + 10.5f * u, center.y + 3f * u),
        style = Stroke(width = 1.3f * u)
    )
    drawCircle(color = CupcakeFrostingPink, radius = 1.5f * u, center = Offset(center.x + 10.5f * u, center.y + 4f * u))
}

/**
 * 3. Draws a Paper-Craft Villager Kid holding a Peppermint Swirl & Wrapped Candy!
 */
private fun DrawScope.drawCandyVillagerWithPeppermint(center: Offset, stageColor: Color) {
    val u = 1.dp.toPx()
    drawCircle(color = MaskingTapeCream, radius = 23f * u, center = center)
    drawCircle(color = CardboardDark, radius = 23f * u, center = center, style = Stroke(width = 1.5f * u))

    // Villager poncho in stage theme color
    val cape = Path().apply {
        moveTo(center.x - 4f * u, center.y - 1f * u)
        lineTo(center.x - 13f * u, center.y + 16f * u)
        lineTo(center.x + 5f * u, center.y + 16f * u)
        close()
    }
    drawPath(cape, color = stageColor)
    drawPath(cape, color = InkBrown, style = Stroke(width = 1.4f * u, join = StrokeJoin.Round))

    // Head
    drawCircle(color = Color(0xFFFFDFC4), radius = 7f * u, center = Offset(center.x - 4f * u, center.y - 6f * u))
    drawCircle(color = InkBrown, radius = 7f * u, center = Offset(center.x - 4f * u, center.y - 6f * u), style = Stroke(width = 1.4f * u))
    drawCircle(color = InkBrown, radius = 1.2f * u, center = Offset(center.x - 6.5f * u, center.y - 6.5f * u))
    drawCircle(color = InkBrown, radius = 1.2f * u, center = Offset(center.x - 1.5f * u, center.y - 6.5f * u))

    // Giant Peppermint Lollipop held by the villager
    val lolliCenter = Offset(center.x + 10f * u, center.y - 3f * u)
    drawLine(
        color = InkBrown,
        start = lolliCenter,
        end = Offset(center.x + 6f * u, center.y + 14f * u),
        strokeWidth = 2.2f * u,
        cap = StrokeCap.Round
    )
    drawCircle(color = PeppermintRed, radius = 8f * u, center = lolliCenter)
    drawCircle(color = Color.White, radius = 4.5f * u, center = lolliCenter)
    drawCircle(color = PeppermintRed, radius = 2f * u, center = lolliCenter)
    drawCircle(color = InkBrown, radius = 8f * u, center = lolliCenter, style = Stroke(width = 1.4f * u))
}

/**
 * 4. Draws a Woodland Paper Bear holding a Golden Cardboard Star & Wrapped Candy!
 */
private fun DrawScope.drawWoodlandBearWithHoneyStar(center: Offset) {
    val u = 1.dp.toPx()
    drawCircle(color = MaskingTapeCream, radius = 23f * u, center = center)
    drawCircle(color = CardboardDark, radius = 23f * u, center = center, style = Stroke(width = 1.5f * u))

    // Bear ears
    drawCircle(color = Color(0xFF7F4F24), radius = 4.5f * u, center = Offset(center.x - 10f * u, center.y - 10f * u))
    drawCircle(color = Color(0xFF7F4F24), radius = 4.5f * u, center = Offset(center.x + 4f * u, center.y - 10f * u))

    // Bear head
    drawCircle(color = Color(0xFFD4A373), radius = 10.5f * u, center = Offset(center.x - 3f * u, center.y - 1f * u))
    drawCircle(color = InkBrown, radius = 10.5f * u, center = Offset(center.x - 3f * u, center.y - 1f * u), style = Stroke(width = 1.5f * u))
    // Snout
    drawCircle(color = Color(0xFFE6CCB2), radius = 4.5f * u, center = Offset(center.x - 3f * u, center.y + 2f * u))
    drawCircle(color = InkBrown, radius = 1.8f * u, center = Offset(center.x - 3f * u, center.y + 0.5f * u))
    drawCircle(color = InkBrown, radius = 1.3f * u, center = Offset(center.x - 7f * u, center.y - 3f * u))
    drawCircle(color = InkBrown, radius = 1.3f * u, center = Offset(center.x + 1f * u, center.y - 3f * u))

    // Golden Cardboard Star held in paw
    val starCenter = Offset(center.x + 11f * u, center.y + 5f * u)
    drawCircle(color = StarCardboardGold, radius = 7.5f * u, center = starCenter)
    drawCircle(color = InkBrown, radius = 7.5f * u, center = starCenter, style = Stroke(width = 1.4f * u))
    drawCircle(color = StickyNoteYellow, radius = 3.5f * u, center = starCenter)
}

/**
 * 5. Draws a cozy Paper-Cutout Sweet Bakery Cottage with a Cupcake Sign & Origami Crane.
 */
private fun DrawScope.drawPaperSweetBakeryCottage(center: Offset, roofColor: Color) {
    val u = 1.dp.toPx()
    drawCircle(color = MaskingTapeCream, radius = 23f * u, center = center)
    drawCircle(color = CardboardDark, radius = 23f * u, center = center, style = Stroke(width = 1.5f * u))

    // Cottage walls
    drawRoundRect(
        color = Color(0xFFFFF8EB),
        topLeft = Offset(center.x - 12f * u, center.y - 2f * u),
        size = Size(24f * u, 16f * u),
        cornerRadius = CornerRadius(2f * u, 2f * u)
    )
    drawRoundRect(
        color = InkBrown,
        topLeft = Offset(center.x - 12f * u, center.y - 2f * u),
        size = Size(24f * u, 16f * u),
        cornerRadius = CornerRadius(2f * u, 2f * u),
        style = Stroke(width = 1.4f * u)
    )

    // Pitched paper roof
    val roof = Path().apply {
        moveTo(center.x, center.y - 16f * u)
        lineTo(center.x - 15f * u, center.y - 1f * u)
        lineTo(center.x + 15f * u, center.y - 1f * u)
        close()
    }
    drawPath(roof, color = roofColor)
    drawPath(roof, color = InkBrown, style = Stroke(width = 1.5f * u, join = StrokeJoin.Round))

    // Door & Wrapped Candy Window
    drawRoundRect(
        color = Color(0xFFD4A373),
        topLeft = Offset(center.x - 3.5f * u, center.y + 4f * u),
        size = Size(7f * u, 10f * u),
        cornerRadius = CornerRadius(3f * u, 3f * u)
    )
    drawCircle(color = WrappedCandyPurple, radius = 3.2f * u, center = Offset(center.x, center.y - 6f * u))
}

@Composable
private fun SelectedLevelInspectorCard(
    state: PaperGameState,
    onPlayLevel: (Int) -> Unit
) {
    val selectedLvl = state.mapSelectedLevel.coerceAtLeast(1)
    val stageInfo = getStageInfoForLevel(selectedLvl)
    val bp = remember(selectedLvl) { getLevelBlueprint(selectedLvl) }
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
            verticalArrangement = Arrangement.spacedBy(3.dp)
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

            Text(
                text = bp.mechanicLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = InkSoftBrown,
                maxLines = 1
            )

            // Goal Preview Chips + Mechanic Chips + Moves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PaperPieceVector(
                        pieceType = bp.primaryGoal.targetPiece,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${bp.primaryGoal.targetCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                        color = InkBrown
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    PaperPieceVector(
                        pieceType = bp.secondaryGoal.targetPiece,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${bp.secondaryGoal.targetCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.5.sp),
                        color = InkBrown
                    )
                }
                if (bp.goldCells.isNotEmpty()) {
                    Text(
                        text = "★${bp.goldCells.size}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFFB45309)
                    )
                }
                if (bp.tapeLocks.isNotEmpty()) {
                    Text(
                        text = "✕${bp.tapeLocks.size}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = PeppermintRed
                    )
                }
                Text(
                    text = "• ${bp.moves} Moves",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 13.5.sp,
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
