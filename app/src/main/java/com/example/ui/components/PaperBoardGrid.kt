package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BOARD_SIZE
import com.example.model.BoardTile
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
import com.example.ui.theme.StickyNoteYellow
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.zIndex
import com.example.model.FloatingScorePopup
import com.example.ui.theme.StarCardboardGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 7x7 Washi & Corrugated Cardboard Board with 3D arced swap animations,
 * squash-and-stretch gravity drops, board impact punch, fever glow, and floating score popups.
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

    // Board impact recoil/shake on big matches, cascades, and smashes
    val boardPunchAnim = remember { Animatable(0f) }
    LaunchedEffect(state.boardPunchSeq) {
        if (state.boardPunchSeq > 0L && state.boardPunchStrength > 0f) {
            val s = state.boardPunchStrength
            boardPunchAnim.snapTo(1f * s)
            boardPunchAnim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.35f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val punchScale = 1f + (boardPunchAnim.value * 0.028f)
    val punchOffsetY = (sin(boardPunchAnim.value * PI * 3.0) * 7.0 * state.boardPunchStrength).toFloat()

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (tilt.x * 10f).roundToInt(),
                    y = (tilt.y * 8f + punchOffsetY).roundToInt()
                )
            }
            .graphicsLayer {
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
                            val isHinted = state.hintPair?.let {
                                it.first == pos || it.second == pos
                            } == true
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

    val p = progress.value
    val scale = if (p < 0.22f) {
        0.5f + (p / 0.22f) * (if (popup.isBig) 0.85f else 0.65f)
    } else {
        (if (popup.isBig) 1.35f else 1.15f) - (p - 0.22f) * 0.25f
    }
    val alpha = if (p < 0.7f) 1f else ((1f - p) / 0.3f).coerceIn(0f, 1f)
    val risePx = -p * cellPx * 1.15f

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = ((popup.col + 0.1f) * cellPx).roundToInt(),
                    y = ((popup.row + 0.15f) * cellPx + risePx).roundToInt()
                )
            }
            .zIndex(22f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
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

    LaunchedEffect(tile.animSeq, tile.slideFromColDelta, tile.slideFromRowDelta) {
        val hasColSlide = abs(tile.slideFromColDelta) > 0.001f
        val hasRowSlide = abs(tile.slideFromRowDelta) > 0.001f
        val isGravityDrop = tile.spawnDistance > 0.001f

        if (hasColSlide || hasRowSlide) {
            offsetColAnim.snapTo(tile.slideFromColDelta)
            offsetRowAnim.snapTo(tile.slideFromRowDelta)

            if (isGravityDrop) {
                swapArcProgress.snapTo(0f)
                // Stagger column/row slightly so falling blocks cascade like real domino cards
                delay((pos.col * 14L) + (pos.row * 9L))
                offsetRowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = (155 + (tile.spawnDistance * 26f).toInt()).coerceIn(160, 270),
                        easing = FastOutSlowInEasing
                    )
                )
                // Impact Squash & Rebound Bounce when the block hits its slot!
                landingSquashAnim.snapTo(1f)
                landingSquashAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.36f,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            } else {
                // Popping 3D Arced Swap Animation: blocks orbit past each other with lift & spin!
                swapArcProgress.snapTo(1f)
                launch {
                    swapArcProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 225, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    offsetColAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    offsetRowAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
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

    // Explosive Crush Pop: inflate to 1.34x first, then spin & snap to 0
    val crushScaleAnim = remember(tile.id) { Animatable(1f) }
    val crushRotAnim = remember(tile.id) { Animatable(0f) }
    LaunchedEffect(tile.isCrushing) {
        if (tile.isCrushing) {
            launch {
                crushRotAnim.animateTo(28f, tween(200))
            }
            crushScaleAnim.animateTo(1.34f, tween(65))
            crushScaleAnim.animateTo(0.04f, tween(140))
        } else {
            crushScaleAnim.snapTo(1f)
            crushRotAnim.snapTo(0f)
        }
    }

    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isInvalidShake) {
        if (isInvalidShake) {
            for (i in 0..3) {
                shakeAnim.animateTo(if (i % 2 == 0) 10f else -10f, tween(38))
            }
            shakeAnim.animateTo(0f, tween(35))
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    // Gentle idle hint pulse
    val infiniteTransition = rememberInfiniteTransition(label = "hint_pulse")
    val hintWave by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_wave"
    )

    val isFallingMidAir = tile.spawnDistance > 0.001f && abs(offsetRowAnim.value) > 0.14f
    val arcSin = sin(swapArcProgress.value * PI).toFloat()
    val arcSign = if (tile.swapArcSign != 0f) tile.swapArcSign else 1f

    // Perpendicular arc offset during a block swap so the two blocks curve past each other
    val isHorizontalSwap = abs(tile.slideFromColDelta) >= abs(tile.slideFromRowDelta)
    val arcOffsetX = if (!isHorizontalSwap && swapArcProgress.value > 0.01f) {
        arcSin * arcSign * 0.36f * cellPx
    } else 0f
    val arcOffsetY = if (isHorizontalSwap && swapArcProgress.value > 0.01f) {
        -arcSin * arcSign * 0.36f * cellPx
    } else 0f

    // Squash & stretch + swap pop scale calculation
    val squash = landingSquashAnim.value
    val swapBoost = if (swapArcProgress.value > 0.01f) {
        if (arcSign > 0f) 0.36f * arcSin else 0.16f * arcSin
    } else 0f

    val baseScale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.15f
            isHinted -> 1.07f + (hintWave * 0.05f)
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "tile_base_scale"
    )

    val finalScaleX = when {
        tile.isCrushing -> crushScaleAnim.value
        isFallingMidAir -> 0.87f
        squash > 0.01f -> 1f + (squash * 0.24f)
        else -> baseScale + swapBoost
    }
    val finalScaleY = when {
        tile.isCrushing -> crushScaleAnim.value
        isFallingMidAir -> 1.17f
        squash > 0.01f -> 1f - (squash * 0.20f)
        else -> baseScale + swapBoost
    }
    val finalRotation = when {
        tile.isCrushing -> crushRotAnim.value
        swapArcProgress.value > 0.01f -> arcSin * arcSign * 16f
        isHinted -> hintWave * 7f
        isSelected -> -5f
        else -> 0f
    }

    var dragAccum by remember { mutableStateOf(Offset.Zero) }
    var swipeTriggered by remember { mutableStateOf(false) }

    val cellShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(
                when {
                    swapArcProgress.value > 0.01f && arcSign > 0f -> 12f
                    swapArcProgress.value > 0.01f || isFallingMidAir || isSelected -> 6f
                    else -> 0f
                }
            )
            .pointerInput(pos) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(pos) {
                detectDragGestures(
                    onDragStart = {
                        dragAccum = Offset.Zero
                        swipeTriggered = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (!swipeTriggered) {
                            dragAccum += dragAmount
                            val threshold = 24f
                            if (abs(dragAccum.x) > threshold || abs(dragAccum.y) > threshold) {
                                swipeTriggered = true
                                val target = if (abs(dragAccum.x) > abs(dragAccum.y)) {
                                    if (dragAccum.x > 0) GridPos(pos.row, pos.col + 1)
                                    else GridPos(pos.row, pos.col - 1)
                                } else {
                                    if (dragAccum.y > 0) GridPos(pos.row + 1, pos.col)
                                    else GridPos(pos.row - 1, pos.col)
                                }
                                onSwipe(target)
                            }
                        }
                    }
                )
            }
            .testTag("board_tile_${pos.row}_${pos.col}"),
        contentAlignment = Alignment.Center
    ) {
        // Stationary paper cell card background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cellShape)
                .background(
                    when {
                        isSelected -> StickyNoteYellow
                        isHinted -> StickyNoteYellow.copy(alpha = 0.65f)
                        else -> cellPaperColor
                    },
                    cellShape
                )
                .border(
                    width = if (isSelected || isHinted) 1.8.dp else 0.5.dp,
                    color = when {
                        isSelected -> PeppermintRed
                        isHinted -> OrigamiFoxOrange
                        else -> MaskingTapeBorder.copy(alpha = 0.6f)
                    },
                    shape = cellShape
                )
        )

        // Die-cut paper game piece with 3D arced swap, squash-and-stretch drop, and crush pop
        PaperPieceVector(
            pieceType = tile.type,
            modifier = Modifier
                .fillMaxSize(0.82f)
                .offset {
                    IntOffset(
                        x = (offsetColAnim.value * cellPx + arcOffsetX + shakeAnim.value).roundToInt(),
                        y = (offsetRowAnim.value * cellPx + arcOffsetY).roundToInt()
                    )
                }
                .graphicsLayer {
                    scaleX = finalScaleX
                    scaleY = finalScaleY
                    rotationZ = finalRotation
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
