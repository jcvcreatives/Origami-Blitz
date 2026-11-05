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
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Minimalist 7x7 Washi Paper Board with subtle two-tone neutral linen cells
 * that let the origami pieces stand out calmly without eye strain.
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

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (tilt.x * 10f).roundToInt(),
                    y = (tilt.y * 8f).roundToInt()
                )
            }
            .aspectRatio(1f)
            .shadow(10.dp, outerShape)
            .clip(outerShape)
            .background(CardboardMedium, outerShape)
            .border(3.dp, CardboardDark, outerShape)
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
                    val rowHasMotion = state.board.getOrNull(r)?.any {
                        abs(it.slideFromColDelta) > 0.01f || abs(it.slideFromRowDelta) > 0.01f
                    } == true || state.selectedPos?.row == r || state.glueFirstPos?.row == r

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .zIndex(if (rowHasMotion) 3f else 0f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (c in 0 until BOARD_SIZE) {
                            val pos = GridPos(r, c)
                            val tile = state.board.getOrNull(r)?.getOrNull(c)
                            // Gentle 2-tone Washi checkerboard
                            val cellColor = BoardCellColors[(r + c) % BoardCellColors.size]
                            val isSelected = state.selectedPos == pos || state.glueFirstPos == pos
                            val isInvalidShake = state.invalidSwapPair?.let {
                                it.first == pos || it.second == pos
                            } == true
                            val tileHasMotion = tile != null && (
                                abs(tile.slideFromColDelta) > 0.01f || abs(tile.slideFromRowDelta) > 0.01f
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .zIndex(if (tileHasMotion || isSelected) 4f else 0f)
                            ) {
                                if (tile != null) {
                                    key(tile.id) {
                                        PaperBoardCell(
                                            tile = tile,
                                            pos = pos,
                                            cellSize = cellSize,
                                            cellPaperColor = cellColor,
                                            isSelected = isSelected,
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

            // Minimalist floating combo pill
            AnimatedVisibility(
                visible = state.comboBannerText != null,
                enter = scaleIn(spring(dampingRatio = 0.65f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaskingTapeCream,
                    shadowElevation = 4.dp,
                    modifier = Modifier.border(1.5.dp, CardboardDark, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = state.comboBannerText.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = InkBrown,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PaperBoardCell(
    tile: BoardTile,
    pos: GridPos,
    cellSize: Dp,
    cellPaperColor: Color,
    isSelected: Boolean,
    isInvalidShake: Boolean,
    onTap: () -> Unit,
    onSwipe: (GridPos) -> Unit
) {
    val density = LocalDensity.current
    val cellPx = with(density) { cellSize.toPx() }

    val offsetColAnim = remember(tile.id) { Animatable(tile.slideFromColDelta) }
    val offsetRowAnim = remember(tile.id) { Animatable(tile.slideFromRowDelta) }

    LaunchedEffect(tile.animSeq, tile.slideFromColDelta, tile.slideFromRowDelta) {
        val hasColSlide = abs(tile.slideFromColDelta) > 0.001f
        val hasRowSlide = abs(tile.slideFromRowDelta) > 0.001f
        val isGravityDrop = tile.spawnDistance > 0.001f

        if (hasColSlide || hasRowSlide) {
            offsetColAnim.snapTo(tile.slideFromColDelta)
            offsetRowAnim.snapTo(tile.slideFromRowDelta)

            if (isGravityDrop) {
                // Falling block animation: bouncy gravity drop
                offsetRowAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = 0.68f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            } else {
                // Changing/swapping block animation: smooth tactile slide across cells
                launch {
                    offsetColAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
                launch {
                    offsetRowAnim.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.78f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        } else {
            offsetColAnim.snapTo(0f)
            offsetRowAnim.snapTo(0f)
        }
    }

    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(isInvalidShake) {
        if (isInvalidShake) {
            for (i in 0..3) {
                shakeAnim.animateTo(if (i % 2 == 0) 8f else -8f, tween(40))
            }
            shakeAnim.animateTo(0f, tween(35))
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    val isMoving = abs(offsetColAnim.value) > 0.02f || abs(offsetRowAnim.value) > 0.02f
    val isSwappingMove = isMoving && tile.spawnDistance <= 0.001f

    val scale by animateFloatAsState(
        targetValue = when {
            tile.isCrushing -> 0.12f
            isSwappingMove -> 1.15f
            isSelected -> 1.12f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
        label = "tile_scale"
    )

    var dragAccum by remember { mutableStateOf(Offset.Zero) }
    var swipeTriggered by remember { mutableStateOf(false) }

    val cellShape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isMoving || isSelected) 5f else 0f)
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
                .background(if (isSelected) StickyNoteYellow else cellPaperColor, cellShape)
                .border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) CardboardDark else MaskingTapeBorder.copy(alpha = 0.6f),
                    shape = cellShape
                )
        )

        // Die-cut paper game piece (unclipped by cell so it glides smoothly across cell borders when moved or dropped)
        PaperPieceVector(
            pieceType = tile.type,
            modifier = Modifier
                .fillMaxSize(0.80f)
                .offset {
                    IntOffset(
                        x = (offsetColAnim.value * cellPx + shakeAnim.value).roundToInt(),
                        y = (offsetRowAnim.value * cellPx).roundToInt()
                    )
                }
                .scale(scale)
        )
    }
}

@Composable
fun PaperStatusModal(
    state: PaperGameState,
    onNextLevel: () -> Unit,
    onRetryLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.status != GameStatus.PLAYING,
        enter = scaleIn(spring(dampingRatio = 0.7f)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        val isWin = state.status == GameStatus.WON
        val cardShape = RoundedCornerShape(16.dp)

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isWin) {
                        stringResource(R.string.dialog_win_title)
                    } else {
                        stringResource(R.string.dialog_lose_title)
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    color = InkBrown
                )

                PaperPieceVector(
                    pieceType = state.goal.targetPiece,
                    modifier = Modifier.size(60.dp)
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
            }
        }
    }
}
