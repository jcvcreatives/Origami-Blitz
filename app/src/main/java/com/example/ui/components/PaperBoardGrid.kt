package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
import com.example.model.FloatingScorePopup
import com.example.model.GameStatus
import com.example.model.GridPos
import com.example.model.PaperGameState
import com.example.model.TileSpecial
import com.example.ui.theme.BoardCellColors
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.InkBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// Realistic gravity acceleration curve (starts crisp, accelerates hard toward landing impact)
private val GravityDropEasing = CubicBezierEasing(0.36f, 0.0f, 0.88f, 0.96f)

private fun comboTierAccentColor(combo: Int): Color = when {
    combo >= 5 -> Color(0xFFFF2A85) // Supernova Magenta-Gold
    combo == 4 -> Color(0xFF8A56AC) // Ultra Royal Amethyst
    combo == 3 -> PeppermintRed     // Mega Peppermint Crimson
    combo == 2 -> OrigamiFoxOrange  // Super Fox Orange
    else -> StarCardboardGold
}

/**
 * 7x7 Washi & Corrugated Cardboard Board engineered for locked 60fps rendering.
 * All per-frame block animations execute inside RenderNode `graphicsLayer` lambdas
 * to avoid layout/composition thrashing across all Android devices.
 */
@Composable
fun PaperCardboardBoard(
    state: PaperGameState,
    onTileTapped: (GridPos) -> Unit,
    onTileSwiped: (GridPos, GridPos) -> Unit,
    tilt: DeviceTilt = DeviceTilt(),
    modifier: Modifier = Modifier
) {
    val outerShape = RoundedCornerShape(16.dp)
    val innerShape = RoundedCornerShape(12.dp)
    val isFever = state.feverMovesLeft > 0
    val activeCombo = state.comboCount
    val comboAccent = comboTierAccentColor(activeCombo)

    // Single shared infinite transition for calm, non-dizzying idle hints & selection breathing
    val infiniteTransition = rememberInfiniteTransition(label = "board_shared_pulse")
    val sharedWaveState = infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shared_wave"
    )

    // Subtle, non-dizzying board pulse on big matches (no vertical shaking or tilt drift)
    val boardPunchAnim = remember { Animatable(0f) }
    LaunchedEffect(state.boardPunchSeq) {
        if (state.boardPunchSeq > 0L && state.boardPunchStrength > 0f) {
            val s = state.boardPunchStrength.coerceAtMost(1.4f)
            boardPunchAnim.snapTo(1f * s)
            boardPunchAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.42f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val punchVal = boardPunchAnim.value
                val punchScale = 1f + (punchVal * 0.012f)
                scaleX = punchScale
                scaleY = punchScale
            }
            .aspectRatio(1f)
            .shadow(if (isFever || activeCombo >= 2) 14.dp else 10.dp, outerShape)
            .clip(outerShape)
            .background(
                when {
                    isFever -> StarCardboardGold
                    activeCombo >= 3 -> comboAccent.copy(alpha = 0.32f)
                    else -> CardboardMedium
                },
                outerShape
            )
            .border(
                width = if (isFever || activeCombo >= 2) 3.5.dp else 3.dp,
                color = when {
                    isFever -> PeppermintRed
                    activeCombo >= 2 -> comboAccent
                    else -> CardboardDark
                },
                shape = outerShape
            )
            .padding(9.dp)
            .testTag("paper_board_container"),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clip(innerShape)
                .background(CardboardDark, innerShape)
                .padding(5.dp)
        ) {
            val boardWidth = maxWidth
            val cellSize: Dp = boardWidth / BOARD_SIZE

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (r in 0 until BOARD_SIZE) {
                    val rowHasTopArc = state.board.getOrNull(r)?.any {
                        it.swapArcSign > 0f && (abs(it.slideFromColDelta) > 0.01f || abs(it.slideFromRowDelta) > 0.01f)
                    } == true
                    val rowHasMotion = state.board.getOrNull(r)?.any {
                        abs(it.slideFromColDelta) > 0.01f || abs(it.slideFromRowDelta) > 0.01f || it.isCrushing
                    } == true || state.selectedPos?.row == r || state.glueFirstPos?.row == r

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .zIndex(
                                when {
                                    rowHasTopArc -> 10f
                                    rowHasMotion -> 5f
                                    else -> 0f
                                }
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until BOARD_SIZE) {
                            val pos = GridPos(r, c)
                            val tile = state.board.getOrNull(r)?.getOrNull(c)
                            val cellColor = BoardCellColors[(r + c) % BoardCellColors.size]
                            val hasGoldStamp = pos in state.goldCells
                            val tapeLockHp = state.tapeLocks[pos] ?: 0
                            val isSelected = state.selectedPos == pos || state.glueFirstPos == pos
                            val isInvalidShake = state.invalidSwapPair?.let {
                                it.first == pos || it.second == pos
                            } == true

                            // Compute directional vector toward the hinted partner tile
                            val hintPartnerDelta: Offset? = state.hintPair?.let { (first, second) ->
                                when (pos) {
                                    first -> Offset(
                                        (second.col - first.col).toFloat(),
                                        (second.row - first.row).toFloat()
                                    )
                                    second -> Offset(
                                        (first.col - second.col).toFloat(),
                                        (first.row - second.row).toFloat()
                                    )
                                    else -> null
                                }
                            }
                            val isHinted = hintPartnerDelta != null

                            val tileHasMotion = tile != null && (
                                abs(tile.slideFromColDelta) > 0.01f || abs(tile.slideFromRowDelta) > 0.01f || tile.isCrushing
                            )
                            val isTopArcTile = tile != null && tile.swapArcSign > 0f && tileHasMotion

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .zIndex(
                                        when {
                                            isTopArcTile -> 12f
                                            tileHasMotion || isSelected -> 6f
                                            else -> 0f
                                        }
                                    )
                            ) {
                                if (tile != null) {
                                    key(tile.id) {
                                        PaperBoardCell(
                                            tile = tile,
                                            pos = pos,
                                            cellSize = cellSize,
                                            cellPaperColor = cellColor,
                                            hasGoldStamp = hasGoldStamp,
                                            tapeLockHp = tapeLockHp,
                                            isSelected = isSelected,
                                            isHinted = isHinted,
                                            hintPartnerDelta = hintPartnerDelta,
                                            sharedWaveState = sharedWaveState,
                                            isInvalidShake = isInvalidShake,
                                            onTap = { onTileTapped(pos) },
                                            onSwipe = { targetPos -> onTileSwiped(pos, targetPos) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Score Popups right over matched cells
            state.floatingPopups.forEach { popup ->
                key(popup.id) {
                    FloatingScoreBadge(
                        popup = popup,
                        cellSize = cellSize
                    )
                }
            }

            // Corner Live Washi-Tape Cascade Stamp (FOLD x2!, CREASE x3!!, ORIGAMI x4!!!)
            AnimatedVisibility(
                visible = activeCombo >= 2,
                enter = scaleIn(spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .zIndex(28f)
            ) {
                val stampShape = TornPaperShape(teethCount = 6, toothDepthPx = 7f)
                val stampVerb = when {
                    activeCombo >= 5 -> "MASTERWORK"
                    activeCombo == 4 -> "ORIGAMI"
                    activeCombo == 3 -> "CREASE"
                    else -> "FOLD"
                }
                Surface(
                    shape = stampShape,
                    color = comboAccent,
                    shadowElevation = 8.dp,
                    modifier = Modifier.border(2.dp, StickyNoteYellow, stampShape)
                ) {
                    Text(
                        text = "✦ $stampVerb x$activeCombo${"!".repeat((activeCombo - 1).coerceIn(1, 4))}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (13 + activeCombo.coerceAtMost(5)).sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = PeppermintWhite,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }

            // Torn Washi-Tape Combo Banner with Hanko Stamp Badge
            AnimatedVisibility(
                visible = state.comboBannerText != null,
                enter = scaleIn(spring(dampingRatio = 0.44f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(25f)
            ) {
                val bannerShape = TornPaperShape(teethCount = 7, toothDepthPx = 8f)
                val bannerBorderColor = if (activeCombo >= 2) comboAccent else PeppermintRed
                Surface(
                    shape = bannerShape,
                    color = if (activeCombo >= 3) comboAccent else StickyNoteYellow,
                    shadowElevation = 10.dp,
                    modifier = Modifier.border(
                        2.5.dp,
                        if (activeCombo >= 3) StickyNoteYellow else bannerBorderColor,
                        bannerShape
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (activeCombo >= 2) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (activeCombo >= 3) StickyNoteYellow else comboAccent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        1.dp,
                                        InkBrown,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✹ x$activeCombo",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = if (activeCombo >= 3) InkBrown else PeppermintWhite
                                )
                            }
                        }
                        Text(
                            text = state.comboBannerText.orEmpty(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = if (activeCombo >= 3) 19.sp else 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (activeCombo >= 3) PeppermintWhite else InkBrown
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingScoreBadge(
    popup: FloatingScorePopup,
    cellSize: Dp
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }
    val progress = remember { Animatable(0f) }
    val mult = popup.comboMultiplier.coerceAtLeast(1)
    val multBoost = (mult - 1).coerceIn(0, 5) * 0.10f
    val ticketShape = TornPaperShape(teethCount = 5, toothDepthPx = 6f)

    LaunchedEffect(popup.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .zIndex(22f + mult)
            .graphicsLayer {
                val p = progress.value
                val peakScale = (if (popup.isBig) 1.38f else 1.15f) + multBoost
                val scale = if (p < 0.20f) {
                    0.45f + (p / 0.20f) * (peakScale - 0.45f)
                } else {
                    peakScale - (p - 0.20f) * 0.26f
                }
                val alphaVal = if (p < 0.72f) 1f else ((1f - p) / 0.28f).coerceIn(0f, 1f)
                val risePx = -p * cellPx * (1.18f + multBoost * 0.6f)
                translationX = (popup.col + 0.08f) * cellPx
                translationY = (popup.row + 0.12f) * cellPx + risePx
                scaleX = scale
                scaleY = scale
                rotationZ = if (mult >= 2) -3.5f else 0f
                alpha = alphaVal
            }
            .shadow(if (mult >= 2) 6.dp else 4.dp, ticketShape)
            .background(
                if (mult >= 3) comboTierAccentColor(mult) else MaskingTapeCream,
                ticketShape
            )
            .border(
                width = if (mult >= 2) 2.dp else 1.5.dp,
                color = if (mult >= 3) StickyNoteYellow else popup.color,
                shape = ticketShape
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = popup.text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = (if (popup.isBig) 15 else 13).plus((mult - 1).coerceIn(0, 4)).sp,
                fontWeight = FontWeight.ExtraBold
            ),
            color = if (mult >= 3) PeppermintWhite else InkBrown
        )
    }
}

@Composable
private fun PaperBoardCell(
    tile: BoardTile,
    pos: GridPos,
    cellSize: Dp,
    cellPaperColor: Color,
    hasGoldStamp: Boolean,
    tapeLockHp: Int,
    isSelected: Boolean,
    isHinted: Boolean,
    hintPartnerDelta: Offset?,
    sharedWaveState: State<Float>,
    isInvalidShake: Boolean,
    onTap: () -> Unit,
    onSwipe: (GridPos) -> Unit
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }
    val comboTier = tile.dropComboTier.coerceAtLeast(1)
    val tierBoost = (comboTier - 1).coerceIn(0, 5)
    val tierColor = comboTierAccentColor(comboTier)

    val offsetColAnim = remember(tile.id) { Animatable(tile.slideFromColDelta) }
    val offsetRowAnim = remember(tile.id) { Animatable(tile.slideFromRowDelta) }
    val swapArcProgress = remember(tile.id) { Animatable(0f) }
    val landingSquashAnim = remember(tile.id) { Animatable(0f) }
    val impactRingAnim = remember(tile.id) { Animatable(0f) }

    LaunchedEffect(tile.animSeq, tile.slideFromColDelta, tile.slideFromRowDelta) {
        val hasColSlide = abs(tile.slideFromColDelta) > 0.001f
        val hasRowSlide = abs(tile.slideFromRowDelta) > 0.001f
        val isGravityDrop = tile.spawnDistance > 0.001f

        if (hasColSlide || hasRowSlide) {
            offsetColAnim.snapTo(tile.slideFromColDelta)
            offsetRowAnim.snapTo(tile.slideFromRowDelta)

            if (isGravityDrop) {
                swapArcProgress.snapTo(0f)
                // Tighter stagger & faster terminal velocity on x2, x3, x4+ cascade waves!
                val staggerMs = ((pos.col * 12L) + (pos.row * 8L) - (tierBoost * 2L)).coerceAtLeast(4L)
                delay(staggerMs)
                val baseDropMs = 142 + (tile.spawnDistance * 25f).toInt() - (tierBoost * 10)
                offsetRowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = baseDropMs.coerceIn(120, 255),
                        easing = GravityDropEasing
                    )
                )
                launch {
                    impactRingAnim.snapTo(0.01f)
                    impactRingAnim.animateTo(
                        1f,
                        tween(230 + tierBoost * 22, easing = FastOutSlowInEasing)
                    )
                    impactRingAnim.snapTo(0f)
                }
                // High-energy elastic landing slam that scales up with x2, x3, x4+ combo drops!
                val slamPeak = (1f + tierBoost * 0.16f).coerceAtMost(1.75f)
                landingSquashAnim.snapTo(slamPeak)
                landingSquashAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = (0.31f - tierBoost * 0.02f).coerceAtLeast(0.22f),
                        stiffness = Spring.StiffnessMedium
                    )
                )
            } else {
                swapArcProgress.snapTo(1f)
                launch {
                    swapArcProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 235, easing = FastOutSlowInEasing)
                    )
                    landingSquashAnim.snapTo(0.48f)
                    landingSquashAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.40f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    offsetColAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.64f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    offsetRowAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.64f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        } else {
            offsetColAnim.snapTo(0f)
            offsetRowAnim.snapTo(0f)
            swapArcProgress.snapTo(0f)
        }
    }

    val crushScaleAnim = remember(tile.id) { Animatable(1f) }
    val crushRotAnim = remember(tile.id) { Animatable(0f) }
    val crushRingAnim = remember(tile.id) { Animatable(0f) }
    LaunchedEffect(tile.isCrushing) {
        if (tile.isCrushing) {
            val popPeak = (1.28f + tierBoost * 0.05f).coerceAtMost(1.50f)
            launch {
                crushRingAnim.snapTo(0.04f)
                crushRingAnim.animateTo(1f, tween(215, easing = FastOutSlowInEasing))
            }
            launch {
                crushRotAnim.animateTo(34f + tierBoost * 5f, tween(205))
            }
            crushScaleAnim.animateTo(popPeak, tween(68, easing = FastOutSlowInEasing))
            crushScaleAnim.animateTo(0.02f, tween(138, easing = FastOutSlowInEasing))
        } else {
            crushScaleAnim.snapTo(1f)
            crushRotAnim.snapTo(0f)
            crushRingAnim.snapTo(0f)
        }
    }

    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isInvalidShake) {
        if (isInvalidShake) {
            for (i in 0..3) {
                shakeAnim.animateTo(if (i % 2 == 0) 9f else -9f, tween(36))
            }
            shakeAnim.animateTo(0f, tween(35))
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    val baseScale by animateFloatAsState(
        targetValue = if (isSelected) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
        label = "tile_base_scale"
    )

    var dragAccum by remember { mutableStateOf(Offset.Zero) }
    var swipeTriggered by remember { mutableStateOf(false) }

    val cellShape = RoundedCornerShape(8.dp)
    val arcSign = if (tile.swapArcSign != 0f) tile.swapArcSign else 1f
    val isHorizontalSwap = abs(tile.slideFromColDelta) >= abs(tile.slideFromRowDelta)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(pos) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(pos) {
                detectDragGestures(
                    onDragStart = {
                        dragAccum = Offset.Zero
                        swipeTriggered = false
                    },
                    onDragEnd = {
                        dragAccum = Offset.Zero
                        swipeTriggered = false
                    },
                    onDragCancel = {
                        dragAccum = Offset.Zero
                        swipeTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!swipeTriggered) {
                            dragAccum += dragAmount
                            val threshold = cellPx * 0.34f
                            if (abs(dragAccum.x) > threshold || abs(dragAccum.y) > threshold) {
                                swipeTriggered = true
                                val target = if (abs(dragAccum.x) > abs(dragAccum.y)) {
                                    if (dragAccum.x > 0) GridPos(pos.row, pos.col + 1)
                                    else GridPos(pos.row, pos.col - 1)
                                } else {
                                    if (dragAccum.y > 0) GridPos(pos.row + 1, pos.col)
                                    else GridPos(pos.row - 1, pos.col)
                                }
                                dragAccum = Offset.Zero
                                onSwipe(target)
                            }
                        }
                    }
                )
            }
            .testTag("board_tile_${pos.row}_${pos.col}"),
        contentAlignment = Alignment.Center
    ) {
        // 1. Recessed Kraft Slot Well OR Gold Washi Stamp Foil Cell Underlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cellShape)
                .background(
                    when {
                        isSelected -> StickyNoteYellow
                        isHinted -> StickyNoteYellow.copy(alpha = 0.55f)
                        isInvalidShake -> PeppermintRed.copy(alpha = 0.22f)
                        hasGoldStamp -> StarCardboardGold.copy(alpha = 0.62f)
                        else -> cellPaperColor
                    },
                    cellShape
                )
                .border(
                    width = if (isSelected || isHinted || hasGoldStamp) 2.dp else 0.8.dp,
                    color = when {
                        isSelected -> PeppermintRed
                        isHinted -> OrigamiFoxOrange
                        hasGoldStamp -> Color(0xFFFFE066)
                        else -> CardboardDark.copy(alpha = 0.45f)
                    },
                    shape = cellShape
                )
        )

        // 2. Bespoke Origami Accordion-Ribbon Wake, Fold-Crease Starburst & Scissor-Cut Perforation FX
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rowOff = offsetRowAnim.value
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)

            // A) Accordion-Folded Washi Paper Ribbon & Fluttering Origami Diamond Wake above falling blocks
            if (tile.spawnDistance > 0.001f && rowOff < -0.06f) {
                val streakAlpha = ((-rowOff / tile.spawnDistance.coerceAtLeast(1f)).coerceIn(0f, 1f)) *
                    (0.55f + tierBoost * 0.10f).coerceAtMost(0.95f)
                val topY = (rowOff * size.height) - size.height * (0.34f + tierBoost * 0.10f)
                val botY = (rowOff * size.height) + size.height * 0.18f
                val ribbonColor = if (comboTier >= 2) tierColor.copy(alpha = streakAlpha)
                else tile.type.primaryColor.copy(alpha = streakAlpha * 0.85f)
                val foldLight = PeppermintWhite.copy(alpha = streakAlpha * 0.88f)

                // Zig-zag accordion-folded Washi ribbon in center
                val folds = 4 + tierBoost.coerceAtMost(3)
                val segH = (botY - topY) / folds
                val halfRibbonW = size.width * (0.14f + tierBoost * 0.02f)
                for (f in 0 until folds) {
                    val y0 = topY + f * segH
                    val y1 = y0 + segH
                    val xShift0 = if (f % 2 == 0) -halfRibbonW * 0.35f else halfRibbonW * 0.35f
                    val xShift1 = -xShift0
                    val quad = Path().apply {
                        moveTo(center.x - halfRibbonW + xShift0, y0)
                        lineTo(center.x + halfRibbonW + xShift0, y0)
                        lineTo(center.x + halfRibbonW + xShift1, y1)
                        lineTo(center.x - halfRibbonW + xShift1, y1)
                        close()
                    }
                    drawPath(
                        path = quad,
                        color = if (f % 2 == 0) ribbonColor else tile.type.secondaryColor.copy(alpha = streakAlpha * 0.85f)
                    )
                    // Crisp horizontal origami fold-crease line
                    drawLine(
                        color = foldLight,
                        start = Offset(center.x - halfRibbonW + xShift0, y0),
                        end = Offset(center.x + halfRibbonW + xShift0, y0),
                        strokeWidth = 1.5f
                    )
                }

                // Flanking fluttering folded paper diamonds in the wake
                val diamondSize = size.width * (0.09f + tierBoost * 0.015f)
                for (side in intArrayOf(-1, 1)) {
                    val dx = center.x + side * size.width * 0.30f
                    val dy = (topY + botY) * 0.5f + (side * segH * 0.3f)
                    val diamond = Path().apply {
                        moveTo(dx, dy - diamondSize)
                        lineTo(dx + diamondSize * 0.75f, dy)
                        lineTo(dx, dy + diamondSize)
                        lineTo(dx - diamondSize * 0.75f, dy)
                        close()
                    }
                    drawPath(diamond, ribbonColor)
                    drawLine(
                        color = foldLight,
                        start = Offset(dx, dy - diamondSize),
                        end = Offset(dx, dy + diamondSize),
                        strokeWidth = 1.3f
                    )
                }
            }

            // B) Origami 8-Point Star-Crease & Scissor-Cut Perforation Burst on Match Crush
            val crushP = crushRingAnim.value
            if (crushP > 0.01f) {
                val fade = (1f - crushP).coerceIn(0f, 1f)
                val outerR = size.minDimension * (0.28f + crushP * (0.58f + tierBoost * 0.06f))
                val innerR = outerR * 0.54f
                val starColor = if (comboTier >= 2) tierColor.copy(alpha = fade * 0.88f)
                else tile.type.primaryColor.copy(alpha = fade * 0.84f)

                val starPath = Path()
                val points = 16
                for (i in 0 until points) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = (i * PI / 8.0 + crushP * 0.45).toFloat()
                    val px = center.x + cos(angle) * r
                    val py = center.y + sin(angle) * r
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()

                drawPath(
                    path = starPath,
                    color = StickyNoteYellow.copy(alpha = fade * 0.38f)
                )
                drawPath(
                    path = starPath,
                    color = starColor,
                    style = Stroke(width = size.minDimension * 0.065f * fade)
                )

                // Dashed scissor-cut diagonal crease guidelines across the crushed cell
                val diagR = outerR * 0.92f
                for (d in 0 until 4) {
                    val a = (d * PI / 4.0).toFloat()
                    val cA = cos(a)
                    val sA = sin(a)
                    drawLine(
                        color = InkBrown.copy(alpha = fade * 0.70f),
                        start = Offset(center.x - cA * diagR, center.y - sA * diagR),
                        end = Offset(center.x + cA * diagR, center.y + sA * diagR),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                }
            }

            // C) Unfolding Origami Diamond-Crease & Flying Paper-Dart Landing Burst
            val impactP = impactRingAnim.value
            if (impactP > 0.01f) {
                val fade = (1f - impactP).coerceIn(0f, 1f)
                val reach = size.minDimension * (0.26f + impactP * (0.40f + tierBoost * 0.07f))
                val notch = reach * 0.62f
                val creaseColor = tierColor.copy(alpha = fade * (0.62f + tierBoost * 0.08f).coerceAtMost(0.95f))

                // 8-point folded origami diamond-star outline
                val diamondStar = Path().apply {
                    moveTo(center.x, center.y - reach)
                    lineTo(center.x + notch * 0.55f, center.y - notch * 0.55f)
                    lineTo(center.x + reach, center.y)
                    lineTo(center.x + notch * 0.55f, center.y + notch * 0.55f)
                    lineTo(center.x, center.y + reach)
                    lineTo(center.x - notch * 0.55f, center.y + notch * 0.55f)
                    lineTo(center.x - reach, center.y)
                    lineTo(center.x - notch * 0.55f, center.y - notch * 0.55f)
                    close()
                }
                drawPath(
                    path = diamondStar,
                    color = creaseColor,
                    style = Stroke(width = size.minDimension * (0.052f + tierBoost * 0.01f) * fade)
                )

                // Inner stitched/perforated square fold-crease frame
                val innerReach = reach * 0.72f
                val innerSquare = Path().apply {
                    moveTo(center.x - innerReach, center.y - innerReach)
                    lineTo(center.x + innerReach, center.y - innerReach)
                    lineTo(center.x + innerReach, center.y + innerReach)
                    lineTo(center.x - innerReach, center.y + innerReach)
                    close()
                }
                drawPath(
                    path = innerSquare,
                    color = StickyNoteYellow.copy(alpha = fade * 0.82f),
                    style = Stroke(width = 2f, pathEffect = dashEffect)
                )

                // Outward-flying folded paper triangle darts on x2, x3, x4+ cascade drops!
                if (comboTier >= 2) {
                    val dartCount = (4 + tierBoost * 2).coerceAtMost(8)
                    val dartDist = size.minDimension * (0.24f + impactP * (0.44f + tierBoost * 0.06f))
                    val dartLen = size.minDimension * (0.12f + tierBoost * 0.015f) * fade
                    for (i in 0 until dartCount) {
                        val angle = (i * (2.0 * PI / dartCount) + impactP * 0.35).toFloat()
                        val cA = cos(angle)
                        val sA = sin(angle)
                        val pA = -sA
                        val pB = cA
                        val tipX = center.x + cA * (dartDist + dartLen)
                        val tipY = center.y + sA * (dartDist + dartLen)
                        val baseX = center.x + cA * dartDist
                        val baseY = center.y + sA * dartDist
                        val wingW = dartLen * 0.58f

                        val leftWing = Path().apply {
                            moveTo(tipX, tipY)
                            lineTo(baseX + pA * wingW, baseY + pB * wingW)
                            lineTo(baseX, baseY)
                            close()
                        }
                        val rightWing = Path().apply {
                            moveTo(tipX, tipY)
                            lineTo(baseX - pA * wingW, baseY - pB * wingW)
                            lineTo(baseX, baseY)
                            close()
                        }
                        drawPath(leftWing, tierColor.copy(alpha = fade * 0.90f))
                        drawPath(rightWing, StickyNoteYellow.copy(alpha = fade * 0.90f))
                    }
                }
            }
        }

        // 3. Tactile 3D pressed-paper block tile + Craftable Special Power-Up Badge
        Box(
            modifier = Modifier
                .fillMaxSize(0.95f)
                .graphicsLayer {
                    val colOff = offsetColAnim.value
                    val rowOff = offsetRowAnim.value
                    val swapP = swapArcProgress.value
                    val squash = landingSquashAnim.value
                    val wave = sharedWaveState.value

                    val isFallingMidAir = tile.spawnDistance > 0.001f && abs(rowOff) > 0.08f
                    val fallRatio = if (tile.spawnDistance > 0.001f) {
                        (abs(rowOff) / tile.spawnDistance.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    } else 0f
                    val fallSpeedFactor = if (isFallingMidAir) (1f - fallRatio * 0.58f) else 0f

                    val arcSin = sin(swapP * PI).toFloat()
                    val arcOffsetX = if (!isHorizontalSwap && swapP > 0.01f) {
                        arcSin * arcSign * 0.24f * cellPx
                    } else 0f
                    val arcOffsetY = if (isHorizontalSwap && swapP > 0.01f) {
                        -arcSin * arcSign * 0.24f * cellPx
                    } else 0f

                    val hintNudgeFactor = ((wave + 1f) * 0.5f)
                    val hintNudgeX = if (hintPartnerDelta != null) {
                        hintPartnerDelta.x * hintNudgeFactor * cellPx * 0.08f
                    } else 0f
                    val hintNudgeY = if (hintPartnerDelta != null) {
                        hintPartnerDelta.y * hintNudgeFactor * cellPx * 0.08f
                    } else 0f

                    val dragFollowX = if (tapeLockHp == 0) {
                        (dragAccum.x * 0.38f).coerceIn(-cellPx * 0.24f, cellPx * 0.24f)
                    } else 0f
                    val dragFollowY = if (tapeLockHp == 0) {
                        (dragAccum.y * 0.38f).coerceIn(-cellPx * 0.24f, cellPx * 0.24f)
                    } else 0f
                    val isDraggingNow = abs(dragFollowX) > 1f || abs(dragFollowY) > 1f

                    // Crisp vertical landing rebound hop on impact that scales with x2, x3, x4+
                    val landingHopY = if (squash > 0.01f) {
                        -sin(squash * PI).toFloat() * cellPx * (0.055f + tierBoost * 0.018f)
                    } else 0f

                    translationX = colOff * cellPx + arcOffsetX + shakeAnim.value + hintNudgeX + dragFollowX
                    translationY = rowOff * cellPx + arcOffsetY + hintNudgeY + dragFollowY + landingHopY

                    val swapBoost = if (swapP > 0.01f) {
                        if (arcSign > 0f) 0.18f * arcSin else 0.06f * arcSin
                    } else 0f
                    val dragBoost = if (isDraggingNow) 0.07f else 0f
                    val hintScaleBoost = if (isHinted) 0.03f + wave * 0.02f else 0f
                    val selectedBobBoost = if (isSelected) wave * 0.018f else 0f

                    val squashWave = sin(squash * PI * 1.5).toFloat() * squash
                    val midAirNarrow = (0.09f + tierBoost * 0.022f).coerceAtMost(0.18f)
                    val midAirStretch = (0.12f + tierBoost * 0.035f).coerceAtMost(0.26f)
                    val slamSquashW = (0.14f + tierBoost * 0.025f).coerceAtMost(0.24f)
                    val slamSquashH = (0.12f + tierBoost * 0.022f).coerceAtMost(0.21f)

                    scaleX = when {
                        tile.isCrushing -> crushScaleAnim.value
                        isFallingMidAir -> 1f - (fallSpeedFactor * midAirNarrow)
                        squash > 0.01f -> 1f + (squashWave * slamSquashW)
                        else -> baseScale + swapBoost + dragBoost + hintScaleBoost + selectedBobBoost
                    }
                    scaleY = when {
                        tile.isCrushing -> crushScaleAnim.value
                        isFallingMidAir -> 1f + (fallSpeedFactor * midAirStretch)
                        squash > 0.01f -> 1f - (squashWave * slamSquashH)
                        else -> baseScale + swapBoost + dragBoost + hintScaleBoost + selectedBobBoost
                    }

                    rotationZ = when {
                        tile.isCrushing -> crushRotAnim.value * 0.35f
                        swapP > 0.01f -> arcSin * arcSign * 6f
                        isDraggingNow -> (dragFollowX / cellPx) * 6f
                        else -> 0f
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            PaperPieceVector(
                pieceType = tile.type,
                modifier = Modifier.fillMaxSize()
            )

            // Gold Washi Stamp Corner Star Indicator
            if (hasGoldStamp) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(1.dp)
                        .size(13.dp)
                        .background(StarCardboardGold, CircleShape)
                        .border(1.dp, InkBrown, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "★",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = InkBrown
                    )
                }
            }

            // Crafted Special Power-Up Badge (Row Cutter ↔, Col Cutter ↕, Paper Bomb ✦, Rainbow Pinwheel ✹)
            if (tile.special != TileSpecial.NONE) {
                val badgeBg = when (tile.special) {
                    TileSpecial.ROW_CUTTER, TileSpecial.COL_CUTTER -> StickyNoteYellow
                    TileSpecial.PAPER_BOMB -> OrigamiFoxOrange
                    TileSpecial.PINWHEEL_RAINBOW -> PeppermintRed
                    TileSpecial.NONE -> StickyNoteYellow
                }
                val badgeFg = when (tile.special) {
                    TileSpecial.ROW_CUTTER, TileSpecial.COL_CUTTER -> InkBrown
                    else -> PeppermintWhite
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(1.dp)
                        .size(18.dp)
                        .shadow(2.dp, CircleShape)
                        .background(badgeBg, CircleShape)
                        .border(1.5.dp, InkBrown, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tile.special.badgeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = badgeFg
                    )
                }
            }
        }

        // 4. Masking Tape Straps (1 HP) & Red Wax Seal (2 HP) Blocker Overlay
        if (tapeLockHp > 0) {
            Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                val w = size.width
                val h = size.height
                val tapeStroke = w * 0.19f
                val borderStroke = w * 0.24f

                // Diagonal Masking Tape Strip 1 (\)
                drawLine(
                    color = InkBrown,
                    start = Offset(w * 0.10f, h * 0.10f),
                    end = Offset(w * 0.90f, h * 0.90f),
                    strokeWidth = borderStroke
                )
                drawLine(
                    color = MaskingTapeCream,
                    start = Offset(w * 0.10f, h * 0.10f),
                    end = Offset(w * 0.90f, h * 0.90f),
                    strokeWidth = tapeStroke
                )

                // Diagonal Masking Tape Strip 2 (/)
                drawLine(
                    color = InkBrown,
                    start = Offset(w * 0.90f, h * 0.10f),
                    end = Offset(w * 0.10f, h * 0.90f),
                    strokeWidth = borderStroke
                )
                drawLine(
                    color = MaskingTapeCream,
                    start = Offset(w * 0.90f, h * 0.10f),
                    end = Offset(w * 0.10f, h * 0.90f),
                    strokeWidth = tapeStroke
                )

                // Red Wax Seal Stamp in center for 2-HP lock
                if (tapeLockHp >= 2) {
                    val sealRadius = w * 0.24f
                    drawCircle(
                        color = InkBrown,
                        radius = sealRadius + 2.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = PeppermintRed,
                        radius = sealRadius,
                        center = center
                    )
                    drawCircle(
                        color = StickyNoteYellow,
                        radius = sealRadius * 0.55f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun PaperStatusModal(
    state: PaperGameState,
    onNextLevel: () -> Unit,
    onRetryLevel: () -> Unit,
    onOpenWorldMap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.status != GameStatus.PLAYING && !state.showWorldMap,
        enter = scaleIn(spring(dampingRatio = 0.7f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        val isWin = state.status == GameStatus.WON
        val cardShape = RoundedCornerShape(16.dp)
        val stageInfo = state.currentStage

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x552E2A25))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .shadow(8.dp, cardShape)
                    .background(MaskingTapeCream, cardShape)
                    .border(1.5.dp, MaskingTapeBorder, cardShape)
                    .padding(24.dp)
                    .testTag("game_status_modal"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STAGE ${stageInfo.stageNumber} • LEVEL ${state.level}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = PeppermintRed
                )

                Text(
                    text = if (isWin) {
                        stringResource(R.string.dialog_win_title)
                    } else {
                        stringResource(R.string.dialog_lose_title)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBrown
                )

                if (isWin) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (s in 1..3) {
                            val earned = s <= state.lastEarnedStars.coerceAtLeast(1)
                            Text(
                                text = "★",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = if (s == 2) 34.sp else 28.sp
                                ),
                                color = if (earned) StarCardboardGold else CardboardLight
                            )
                        }
                    }
                }

                PaperPieceVector(
                    pieceType = state.goal.targetPiece,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = if (isWin) {
                        stringResource(R.string.dialog_win_subtitle)
                    } else {
                        stringResource(R.string.dialog_lose_subtitle)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkBrown,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${stringResource(R.string.label_score)}: ${state.score}",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
                    color = OrigamiFoxOrange
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = if (isWin) onNextLevel else onRetryLevel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isWin) OrigamiFoxOrange else PeppermintRed,
                        contentColor = PeppermintWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("modal_primary_action_button")
                ) {
                    Text(
                        text = if (isWin) {
                            stringResource(R.string.btn_next_level)
                        } else {
                            stringResource(R.string.btn_retry_level)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Button(
                    onClick = onOpenWorldMap,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StickyNoteYellow,
                        contentColor = InkBrown
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("modal_world_map_button")
                ) {
                    Text(
                        text = stringResource(R.string.btn_world_map),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
