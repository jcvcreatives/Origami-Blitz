package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ConfettiShredOverlay
import com.example.ui.components.GlobalPaperLightingOverlay
import com.example.ui.components.PaperBoosterBar
import com.example.ui.components.PaperCardboardBoard
import com.example.ui.components.PaperDioramaBackdrop
import com.example.ui.components.PaperStatusModal
import com.example.ui.components.PaperTopHud
import com.example.ui.components.PaperWorldMapOverlay
import com.example.ui.components.rememberDeviceTilt
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PaperCrushViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(PaperCrushViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaperCrushViewModel(prefs = prefs) as T
            }
        }

        setContent {
            MyApplicationTheme {
                val vm: PaperCrushViewModel = viewModel(factory = factory)
                PaperCraftCrushApp(viewModel = vm)
            }
        }
    }
}

@Composable
fun PaperCraftCrushApp(
    viewModel: PaperCrushViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val confettiScraps by viewModel.confettiScraps.collectAsStateWithLifecycle()

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var pointerTilt by remember { mutableStateOf(Offset.Zero) }

    // Combines hardware gravity/accelerometer tilt with subtle pointer-position tilt
    val deviceTilt = rememberDeviceTilt(pointerTiltOffset = pointerTilt)
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull()
                        if (change != null && containerSize.width > 0 && containerSize.height > 0) {
                            if (change.pressed) {
                                val nx = ((change.position.x / containerSize.width) - 0.5f) * 0.22f
                                val ny = ((change.position.y / containerSize.height) - 0.5f) * 0.22f
                                pointerTilt = Offset(nx, ny)
                            } else {
                                pointerTilt = Offset.Zero
                            }
                        }
                    }
                }
            }
    ) {
        // 1. Multi-layer Paper & Origami Diorama Backdrop with gentle tilt parallax & landed depth confetti
        PaperDioramaBackdrop(
            tilt = deviceTilt,
            scraps = confettiScraps
        )

        // 2. Adaptive Layout compatible with all phones, foldables, tablets & landscape screens
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val isLandscape = maxWidth > maxHeight * 1.12f
            val hudParallaxModifier = Modifier

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = hudParallaxModifier
                            .weight(1f)
                            .widthIn(max = 380.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PaperTopHud(
                            state = state,
                            onToggleSound = viewModel::toggleSound,
                            onResetProgress = viewModel::retryOrResetLevel,
                            onOpenWorldMap = viewModel::openWorldMap,
                            onOpenTutorial = viewModel::reopenTutorial,
                            onNextTutorialStep = viewModel::advanceTutorialStep,
                            onDismissTutorial = viewModel::dismissTutorial
                        )
                        PaperBoosterBar(
                            state = state,
                            onSelectBooster = viewModel::selectBooster
                        )
                    }

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxBoardSide = minOf(maxWidth, maxHeight, 480.dp)
                        PaperCardboardBoard(
                            state = state,
                            onTileTapped = viewModel::onTileTapped,
                            onTileSwiped = viewModel::onTileSwiped,
                            tilt = deviceTilt,
                            modifier = Modifier.sizeIn(
                                maxWidth = maxBoardSide,
                                maxHeight = maxBoardSide
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 520.dp)
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PaperTopHud(
                        state = state,
                        onToggleSound = viewModel::toggleSound,
                        onResetProgress = viewModel::retryOrResetLevel,
                        onOpenWorldMap = viewModel::openWorldMap,
                        onOpenTutorial = viewModel::reopenTutorial,
                        onNextTutorialStep = viewModel::advanceTutorialStep,
                        onDismissTutorial = viewModel::dismissTutorial,
                        modifier = hudParallaxModifier
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxBoardSide = minOf(maxWidth, maxHeight, 440.dp)
                        PaperCardboardBoard(
                            state = state,
                            onTileTapped = viewModel::onTileTapped,
                            onTileSwiped = viewModel::onTileSwiped,
                            tilt = deviceTilt,
                            modifier = Modifier.sizeIn(
                                maxWidth = maxBoardSide,
                                maxHeight = maxBoardSide
                            )
                        )
                    }

                    PaperBoosterBar(
                        state = state,
                        onSelectBooster = viewModel::selectBooster,
                        modifier = hudParallaxModifier
                    )
                }
            }
        }

        // 3. Full-Screen Confetti Shred Particle Overlay (airborne depth-sorted particles)
        ConfettiShredOverlay(
            scraps = confettiScraps,
            tilt = deviceTilt
        )

        // 4. Subtle Global Tilt-Reactive Lighting & Paper Specular Sheen Overlay
        GlobalPaperLightingOverlay(tilt = deviceTilt)

        // 5. Level Complete / Out of Moves Paper Modal
        PaperStatusModal(
            state = state,
            onNextLevel = viewModel::advanceToNextLevel,
            onRetryLevel = viewModel::retryOrResetLevel,
            onOpenWorldMap = viewModel::openWorldMap
        )

        // 6. Candy Crush-style Paper Craft Saga World Map Overlay
        PaperWorldMapOverlay(
            state = state,
            onSelectLevel = viewModel::selectLevelOnMap,
            onPlayLevel = viewModel::playLevelFromMap,
            onCloseMap = viewModel::closeWorldMap
        )
    }
}
