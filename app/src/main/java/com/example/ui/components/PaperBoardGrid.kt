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
import kotlin.math.sin

// Realistic gravity acceleration curve (starts slow, accelerates toward landing impact)
private val GravityDropEasing = CubicBezierEasing(0.32f, 0.0f, 0.82f, 0.94f)

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
    val density = LocalDensity.current

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
            val s = state.boardPunchStrength.coerceAtMost(1f)
            boardPunchAnim.snapTo(1f * s)
            boardPunchAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                val punchVal = boardPunchAnim.value
                val punchScale = 1f + (punchVal * 0.009f)
                scaleX = punchScale
                scaleY = punchScale
            }
            .aspectRatio(1f)
            .shadow(if (isFever) 14.dp else 10.dp, outerShape)
            .clip(outerShape)
            .background(if (isFever) StarCardboardGold else CardboardMedium, outerShape)
            .border(
                width = if (isFever) 3.5.dp else 3.dp,
                color = if (isFever) PeppermintRed else CardboardDark,
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

            // Juicy floating combo pill
            AnimatedVisibility(
                visible = state.comboBannerText != null,
                enter = scaleIn(spring(dampingRatio = 0.52f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(25f)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StickyNoteYellow,
                    shadowElevation = 8.dp,
                    modifier = Modifier.border(2.dp, PeppermintRed, RoundedCornerShape(14.dp))
                ) {
                    Text(
                        text = state.comboBannerText.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = InkBrown,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                    )
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

    LaunchedEffect(popup.id) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .zIndex(22f)
            .graphicsLayer {
                val p = progress.value
                val scale = if (p < 0.22f) {
                    0.5f + (p / 0.22f) * (if (popup.isBig) 0.85f else 0.65f)
                } else {
                    (if (popup.isBig) 1.35f else 1.15f) - (p - 0.22f) * 0.25f
                }
                val alphaVal = if (p < 0.7f) 1f else ((1f - p) / 0.3f).coerceIn(0f, 1f)
                val risePx = -p * cellPx * 1.15f
                translationX = (popup.col + 0.1f) * cellPx
                translationY = (popup.row + 0.15f) * cellPx + risePx
                scaleX = scale
                scaleY = scale
                alpha = alphaVal
            }
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(MaskingTapeCream, RoundedCornerShape(10.dp))
            .border(1.5.dp, popup.color, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = popup.text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = if (popup.isBig) 15.sp else 13.sp,
                fontWeight = FontWeight.Bold
            ),
            color = InkBrown
        )
    }
}

@Composable
private fun PaperBoardCell(
    tile: BoardTile,
    pos: GridPos,
    cellSize: Dp,
    cellPaperColor: Color,
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
                // Stagger column/row so falling blocks cascade like real domino cards
                delay((pos.col * 14L) + (pos.row * 9L))
                offsetRowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = (150 + (tile.spawnDistance * 28f).toInt()).coerceIn(155, 275),
                        easing = GravityDropEasing
                    )
                )
                // Trigger expanding paper impact ring + multi-bounce squash & stretch!
                launch {
                    impactRingAnim.snapTo(0.01f)
                    impactRingAnim.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
                    impactRingAnim.snapTo(0f)
                }
                landingSquashAnim.snapTo(1f)
                landingSquashAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.34f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            } else {
                // 3D Parabolic Origami Vault Swap Animation
                swapArcProgress.snapTo(1f)
                launch {
                    swapArcProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 235, easing = FastOutSlowInEasing)
                    )
                    // Subtle landing settle when the swapped block touches down
                    landingSquashAnim.snapTo(0.45f)
                    landingSquashAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.42f,
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

    // Explosive Crush Pop: inflate to 1.38x with 3D tilt & shockwave ring, then snap to 0
    val crushScaleAnim = remember(tile.id) { Animatable(1f) }
    val crushRotAnim = remember(tile.id) { Animatable(0f) }
    val crushRingAnim = remember(tile.id) { Animatable(0f) }
    LaunchedEffect(tile.isCrushing) {
        if (tile.isCrushing) {
            launch {
                crushRingAnim.snapTo(0.05f)
                crushRingAnim.animateTo(1f, tween(210, easing = FastOutSlowInEasing))
            }
            launch {
                crushRotAnim.animateTo(32f, tween(205))
            }
            crushScaleAnim.animateTo(1.38f, tween(65, easing = FastOutSlowInEasing))
            crushScaleAnim.animateTo(0.02f, tween(140, easing = FastOutSlowInEasing))
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
                shakeAnim.animateTo(if (i % 2 == 0) 11f else -11f, tween(36))
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
        // Recessed warm kraft slot well
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cellShape)
                .background(
                    when {
                        isSelected -> StickyNoteYellow
                        isHinted -> StickyNoteYellow.copy(alpha = 0.55f)
                        isInvalidShake -> PeppermintRed.copy(alpha = 0.22f)
                        else -> cellPaperColor
                    },
                    cellShape
                )
                .border(
                    width = if (isSelected || isHinted) 2.dp else 0.8.dp,
                    color = when {
                        isSelected -> PeppermintRed
                        isHinted -> OrigamiFoxOrange
                        else -> CardboardDark.copy(alpha = 0.45f)
                    },
                    shape = cellShape
                )
        )

        // Landing shockwave & match crush ring overlay (drawn purely in Canvas draw phase)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val crushP = crushRingAnim.value
            if (crushP > 0.01f) {
                val maxR = size.minDimension * 0.62f
                drawCircle(
                    color = tile.type.primaryColor.copy(alpha = (1f - crushP) * 0.65f),
                    radius = maxR * (0.35f + crushP * 0.70f),
                    style = Stroke(width = size.minDimension * 0.09f * (1f - crushP * 0.5f))
                )
            }
            val impactP = impactRingAnim.value
            if (impactP > 0.01f) {
                drawCircle(
                    color = StarCardboardGold.copy(alpha = (1f - impactP) * 0.35f),
                    radius = size.minDimension * (0.25f + impactP * 0.30f),
                    style = Stroke(width = size.minDimension * 0.045f * (1f - impactP))
                )
            }
        }

        // Tactile 3D pressed-paper block tile with smooth, planar 60fps transforms (zero dizzying 3D pitch/yaw)
        PaperPieceVector(
            pieceType = tile.type,
            modifier = Modifier
                .fillMaxSize(0.95f)
                .graphicsLayer {
                    val colOff = offsetColAnim.value
                    val rowOff = offsetRowAnim.value
                    val swapP = swapArcProgress.value
                    val squash = landingSquashAnim.value
                    val wave = sharedWaveState.value

                    val isFallingMidAir = tile.spawnDistance > 0.001f && abs(rowOff) > 0.10f
                    val fallRatio = if (tile.spawnDistance > 0.001f) {
                        (abs(rowOff) / tile.spawnDistance.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    } else 0f
                    val fallSpeedFactor = if (isFallingMidAir) (1f - fallRatio * 0.65f) else 0f

                    val arcSin = sin(swapP * PI).toFloat()
                    val arcOffsetX = if (!isHorizontalSwap && swapP > 0.01f) {
                        arcSin * arcSign * 0.24f * cellPx
                    } else 0f
                    val arcOffsetY = if (isHorizontalSwap && swapP > 0.01f) {
                        -arcSin * arcSign * 0.24f * cellPx
                    } else 0f

                    // Calm directional hint nudge toward partner tile
                    val hintNudgeFactor = ((wave + 1f) * 0.5f)
                    val hintNudgeX = if (hintPartnerDelta != null) {
                        hintPartnerDelta.x * hintNudgeFactor * cellPx * 0.08f
                    } else 0f
                    val hintNudgeY = if (hintPartnerDelta != null) {
                        hintPartnerDelta.y * hintNudgeFactor * cellPx * 0.08f
                    } else 0f

                    // Live tactile finger drag follow before swipe threshold triggers
                    val dragFollowX = (dragAccum.x * 0.38f).coerceIn(-cellPx * 0.24f, cellPx * 0.24f)
                    val dragFollowY = (dragAccum.y * 0.38f).coerceIn(-cellPx * 0.24f, cellPx * 0.24f)
                    val isDraggingNow = abs(dragFollowX) > 1f || abs(dragFollowY) > 1f

                    translationX = colOff * cellPx + arcOffsetX + shakeAnim.value + hintNudgeX + dragFollowX
                    translationY = rowOff * cellPx + arcOffsetY + hintNudgeY + dragFollowY

                    val swapBoost = if (swapP > 0.01f) {
                        if (arcSign > 0f) 0.18f * arcSin else 0.06f * arcSin
                    } else 0f
                    val dragBoost = if (isDraggingNow) 0.07f else 0f
                    val hintScaleBoost = if (isHinted) 0.03f + wave * 0.02f else 0f
                    val selectedBobBoost = if (isSelected) wave * 0.018f else 0f

                    // Cushioned squash-and-stretch on landing
                    val squashWave = sin(squash * PI * 1.5).toFloat() * squash
                    scaleX = when {
                        tile.isCrushing -> crushScaleAnim.value
                        isFallingMidAir -> 1f - (fallSpeedFactor * 0.07f)
                        squash > 0.01f -> 1f + (squashWave * 0.12f)
                        else -> baseScale + swapBoost + dragBoost + hintScaleBoost + selectedBobBoost
                    }
                    scaleY = when {
                        tile.isCrushing -> crushScaleAnim.value
                        isFallingMidAir -> 1f + (fallSpeedFactor * 0.09f)
                        squash > 0.01f -> 1f - (squashWave * 0.10f)
                        else -> baseScale + swapBoost + dragBoost + hintScaleBoost + selectedBobBoost
                    }

                    // Gentle planar rotation only during active swap or crush (zero idle wobble)
                    rotationZ = when {
                        tile.isCrushing -> crushRotAnim.value * 0.35f
                        swapP > 0.01f -> arcSin * arcSign * 6f
                        isDraggingNow -> (dragFollowX / cellPx) * 6f
                        else -> 0f
                    }
                }
        )
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
