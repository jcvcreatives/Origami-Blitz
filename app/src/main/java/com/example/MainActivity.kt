package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PaperProfileRepository
import com.example.ui.components.ConfettiShredOverlay
import com.example.ui.components.GlobalPaperLightingOverlay
import com.example.ui.components.PaperBoosterBar
import com.example.ui.components.PaperCardboardBoard
import com.example.ui.components.PaperDioramaBackdrop
import com.example.ui.components.PaperGmControlModal
import com.example.ui.components.PaperHomeAuthScreen
import com.example.ui.components.PaperStatusModal
import com.example.ui.components.PaperTopHud
import com.example.ui.components.PaperWorldMapOverlay
import com.example.ui.components.attemptAutoSignIn
import com.example.ui.components.rememberDeviceTilt
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PaperCrushViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContext = applicationContext
        val prefs = getSharedPreferences(PaperCrushViewModel.PREFS_NAME, Context.MODE_PRIVATE)
        val databaseId = appContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(databaseId)
        val profileRepository = PaperProfileRepository(firestore)

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PaperCrushViewModel(
                    prefs = prefs,
                    profileRepository = profileRepository
                ) as T
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
    val quotaStats by viewModel.quotaStats.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentUser by remember {
        mutableStateOf(
            try {
                Firebase.auth.currentUser
            } catch (_: Exception) {
                null
            }
        )
    }

    DisposableEffect(Unit) {
        val auth = try {
            Firebase.auth
        } catch (_: Exception) {
            null
        }
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            currentUser = user
            if (user != null) {
                viewModel.onGoogleAuthSuccess(user.email ?: user.displayName)
            }
        }
        auth?.addAuthStateListener(listener)
        onDispose {
            auth?.removeAuthStateListener(listener)
        }
    }

    // Attempt silent Google Auto-Sign-In on startup
    LaunchedEffect(Unit) {
        val credentialManager = try {
            CredentialManager.create(context)
        } catch (_: Exception) {
            null
        }
        if (credentialManager != null && currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    val u = try {
                        Firebase.auth.currentUser
                    } catch (_: Exception) {
                        null
                    }
                    currentUser = u
                    if (u != null) {
                        viewModel.onGoogleAuthSuccess(u.email ?: u.displayName)
                    }
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isHomeAuthVisible = state.showHomeScreen

    BackHandler(enabled = state.showGmPanel || state.showWorldMap || !isHomeAuthVisible) {
        when {
            state.showGmPanel -> viewModel.closeGmPanel()
            state.showWorldMap -> viewModel.closeWorldMap()
            !isHomeAuthVisible -> viewModel.openHomeScreen()
        }
    }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var pointerTilt by remember { mutableStateOf(Offset.Zero) }

    // Combines hardware gravity/accelerometer tilt with deadbanded pointer-position tilt
    val deviceTilt = rememberDeviceTilt(pointerTiltOffset = pointerTilt)

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
                                if (abs(nx - pointerTilt.x) > 0.035f || abs(ny - pointerTilt.y) > 0.035f) {
                                    pointerTilt = Offset(nx, ny)
                                }
                            } else if (pointerTilt != Offset.Zero) {
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
            scraps = confettiScraps,
            stageNumber = state.currentStage.stageNumber
        )

        // 2. Auth Gate: Only mount active gameplay board when authenticated (Google Sign-In or Account/GM Login)
        if (!isHomeAuthVisible) {
            val feverScreenShake = remember { androidx.compose.animation.core.Animatable(0f) }
            LaunchedEffect(state.feverShakeSeq) {
                if (state.feverShakeSeq > 0L) {
                    feverScreenShake.snapTo(1f)
                    feverScreenShake.animateTo(
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 360,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    )
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .graphicsLayer {
                        val shakeDecay = feverScreenShake.value
                        if (shakeDecay > 0.001f) {
                            val phase = (1f - shakeDecay) * (kotlin.math.PI.toFloat() * 10f)
                            val ampPx = shakeDecay * state.feverShakeIntensity.coerceIn(0.8f, 1.75f) * 6.5f.dp.toPx()
                            translationX = kotlin.math.sin(phase * 1.35f) * ampPx
                            translationY = kotlin.math.cos(phase * 1.75f) * ampPx * 0.85f
                            rotationZ = kotlin.math.sin(phase) * shakeDecay * state.feverShakeIntensity * 0.75f
                        }
                    }
            ) {
                val hudParallaxModifier = Modifier
                val isLandscape = maxWidth > maxHeight * 1.12f && maxHeight < 560.dp

                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
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
                                onDismissTutorial = viewModel::dismissTutorial,
                                onOpenHomeScreen = viewModel::openHomeScreen,
                                onOpenGmPanel = viewModel::openGmPanel
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
                            onOpenHomeScreen = viewModel::openHomeScreen,
                            onOpenGmPanel = viewModel::openGmPanel,
                            modifier = hudParallaxModifier
                        )

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
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
        }

        // 3. Full-Screen Confetti Shred Particle Overlay (only composed when particles are active)
        if (confettiScraps.isNotEmpty()) {
            ConfettiShredOverlay(
                scraps = confettiScraps,
                tilt = deviceTilt
            )
        }

        // 4. Subtle Global Tilt-Reactive Lighting & Paper Specular Sheen Overlay
        GlobalPaperLightingOverlay(tilt = deviceTilt)

        // 5. Level Complete / Out of Moves Paper Modal
        if (!isHomeAuthVisible) {
            PaperStatusModal(
                state = state,
                onNextLevel = viewModel::advanceToNextLevel,
                onRetryLevel = viewModel::retryOrResetLevel,
                onOpenWorldMap = viewModel::openWorldMap
            )
        }

        // 6. Candy Crush-style Paper Craft Saga World Map Overlay
        PaperWorldMapOverlay(
            state = state,
            onSelectLevel = viewModel::selectLevelOnMap,
            onPlayLevel = { level ->
                viewModel.closeHomeScreenAndPlay()
                viewModel.playLevelFromMap(level)
            },
            onCloseMap = viewModel::closeWorldMap
        )

        // 7. Home & Login Portal Screen (Google Gmail Sign-In, Username/Password & GM jcv/jan27 Login)
        if (isHomeAuthVisible && !state.showWorldMap) {
            PaperHomeAuthScreen(
                state = state,
                quotaStats = quotaStats,
                isGoogleSignedIn = currentUser != null,
                googleEmailLabel = currentUser?.email ?: currentUser?.displayName,
                onGoogleAuthSuccess = {
                    val u = try {
                        Firebase.auth.currentUser
                    } catch (_: Exception) {
                        null
                    }
                    currentUser = u
                    viewModel.onGoogleAuthSuccess(u?.email ?: u?.displayName)
                },
                onLoginWithCredentials = viewModel::loginWithUsernameAndPassword,
                onSignOutAll = {
                    currentUser = null
                    viewModel.signOutSession()
                },
                onManualCloudSync = viewModel::triggerManualCloudSync,
                onStartPlaying = viewModel::closeHomeScreenAndPlay,
                onOpenWorldMap = viewModel::openWorldMap,
                onOpenGmPanel = viewModel::openGmPanel
            )
        }

        // 8. GM (Game Master) Control Modal (jcv / jan27)
        if (state.showGmPanel && state.isGmAccount) {
            PaperGmControlModal(
                state = state,
                quotaStats = quotaStats,
                onUnlockAllLevels = viewModel::gmUnlockAllLevels,
                onGrant99Boosters = viewModel::gmGrant99Boosters,
                onInstantWinLevel = viewModel::gmInstantWinLevel,
                onAdd15Moves = viewModel::gmAdd15Moves,
                onJumpToLevel = viewModel::gmJumpToLevel,
                onManualCloudSync = viewModel::triggerManualCloudSync,
                onDismiss = viewModel::closeGmPanel
            )
        }
    }
}
