package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.CloudQuotaStats
import com.example.model.PaperGameState
import com.example.ui.theme.CardboardDark
import com.example.ui.theme.CardboardLight
import com.example.ui.theme.CardboardMedium
import com.example.ui.theme.InkBrown
import com.example.ui.theme.InkSoftBrown
import com.example.ui.theme.MaskingTapeBorder
import com.example.ui.theme.MaskingTapeCream
import com.example.ui.theme.OrigamiCraneBlue
import com.example.ui.theme.OrigamiFoxGreen
import com.example.ui.theme.OrigamiFoxOrange
import com.example.ui.theme.PeppermintRed
import com.example.ui.theme.PeppermintWhite
import com.example.ui.theme.StarCardboardGold
import com.example.ui.theme.StickyNoteShadow
import com.example.ui.theme.StickyNoteYellow
import com.example.ui.theme.WrappedCandyPurple
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Silent Auto-Sign-In on startup using GetGoogleIdOption in its own single-option request.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption in its own single-option request.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

/**
 * Sign-Out flow clearing CredentialManager state inside NonCancellable before Firebase.auth.signOut().
 */
fun signOutFromGoogle(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

/**
 * Draws authentic Origami paper valley/mountain fold creases and a 3D folded dog-ear corner flap
 * on top of any paper card container.
 */
private fun Modifier.origamiFoldedSheet(
    flapColor: Color = StickyNoteYellow,
    creaseColor: Color = InkBrown.copy(alpha = 0.14f),
    flapSize: Dp = 18.dp,
    bottomLeftFlap: Boolean = false
): Modifier = this.drawWithContent {
    drawContent()
    val w = size.width
    val h = size.height
    val flapPx = flapSize.toPx()
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

    // Subtle diagonal origami valley-fold crease lines
    drawLine(
        color = creaseColor,
        start = Offset(0f, h * 0.28f),
        end = Offset(w * 0.36f, 0f),
        strokeWidth = 1.4f,
        pathEffect = dashEffect
    )
    drawLine(
        color = creaseColor,
        start = Offset(w * 0.68f, h),
        end = Offset(w, h * 0.64f),
        strokeWidth = 1.4f,
        pathEffect = dashEffect
    )

    // Top-Right 3D Folded Origami Triangular Dog-Ear Flap
    val trShadow = Path().apply {
        moveTo(w - flapPx - 3f, 0f)
        lineTo(w, flapPx + 3f)
        lineTo(w - flapPx - 2f, flapPx + 2f)
        close()
    }
    drawPath(trShadow, Color(0x26000000))

    val trFlap = Path().apply {
        moveTo(w - flapPx, 0f)
        lineTo(w, flapPx)
        lineTo(w - flapPx, flapPx)
        close()
    }
    drawPath(trFlap, flapColor)
    drawPath(trFlap, InkBrown.copy(alpha = 0.45f), style = Stroke(width = 1.5f))

    if (bottomLeftFlap) {
        val blFlap = Path().apply {
            moveTo(0f, h - flapPx)
            lineTo(flapPx, h)
            lineTo(flapPx, h - flapPx)
            close()
        }
        drawPath(blFlap, flapColor)
        drawPath(blFlap, InkBrown.copy(alpha = 0.45f), style = Stroke(width = 1.5f))
    }
}

/**
 * Animated Origami Sun Emblem for the Login Page Header:
 * Features a faceted origami sun that moves in a gentle circular orbit while two concentric
 * rings of folded triangular paper rays and origami diamond sparks continuously circle around it.
 */
@Composable
private fun OrigamiCirclingSunHero(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "login_origami_sun_transition")

    // Outer 12 folded origami rays circling clockwise (0 -> 360 deg)
    val outerRayDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_origami_rays_deg"
    )

    // Inner 8 folded origami rays circling counter-clockwise (360 -> 0 deg)
    val innerRayDeg by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_origami_rays_deg"
    )

    // Orbital circular path for the sun body & orbiting paper sparks
    val orbitDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_circle_orbit_deg"
    )

    Canvas(
        modifier = modifier
            .size(104.dp)
            .testTag("origami_circling_sun_hero")
    ) {
        val orbitRad = (orbitDeg * (Math.PI / 180.0)).toFloat()
        val orbitRadius = size.minDimension * 0.045f
        // The sun itself gently circles around the emblem center while its rays spin around it
        val sunCenter = Offset(
            x = size.width / 2f + cos(orbitRad) * orbitRadius,
            y = size.height / 2f + sin(orbitRad) * orbitRadius
        )
        val coreRadius = size.minDimension * 0.22f

        // 1. Soft paper cutout shadow beneath the sun
        drawCircle(
            color = Color(0x24000000),
            radius = coreRadius * 1.65f,
            center = sunCenter + Offset(3f, 4f)
        )

        // 2. Outer Ring: 12 Two-Tone Folded Origami Triangular Rays Circling Clockwise
        val outerRayCount = 12
        rotate(degrees = outerRayDeg, pivot = sunCenter) {
            for (i in 0 until outerRayCount) {
                rotate(degrees = i * (360f / outerRayCount), pivot = sunCenter) {
                    val baseR = coreRadius * 0.90f
                    val tipR = coreRadius * 1.92f
                    val halfW = coreRadius * 0.28f

                    // Left sunlit folded triangle facet
                    val leftHalf = Path().apply {
                        moveTo(sunCenter.x - halfW, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(leftHalf, StickyNoteYellow)

                    // Right shaded folded triangle facet
                    val rightHalf = Path().apply {
                        moveTo(sunCenter.x, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x + halfW, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(rightHalf, OrigamiFoxOrange)

                    // Crisp ink border & center fold crease
                    val fullRay = Path().apply {
                        moveTo(sunCenter.x - halfW, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x + halfW, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(fullRay, InkBrown, style = Stroke(width = 2f))
                    drawLine(
                        color = InkBrown.copy(alpha = 0.65f),
                        start = Offset(sunCenter.x, sunCenter.y - baseR),
                        end = Offset(sunCenter.x, sunCenter.y - tipR),
                        strokeWidth = 1.6f
                    )
                }
            }
        }

        // 3. Inner Ring: 8 Secondary Folded Paper Rays Circling Counter-Clockwise + Orbiting Origami Sparks
        rotate(degrees = innerRayDeg, pivot = sunCenter) {
            for (i in 0 until 8) {
                rotate(degrees = i * 45f + 22.5f, pivot = sunCenter) {
                    val baseR = coreRadius * 0.92f
                    val tipR = coreRadius * 1.54f
                    val halfW = coreRadius * 0.22f

                    val innerLeft = Path().apply {
                        moveTo(sunCenter.x - halfW, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(innerLeft, PeppermintWhite)

                    val innerRight = Path().apply {
                        moveTo(sunCenter.x, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x + halfW, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(innerRight, StarCardboardGold)

                    val innerOutline = Path().apply {
                        moveTo(sunCenter.x - halfW, sunCenter.y - baseR)
                        lineTo(sunCenter.x, sunCenter.y - tipR)
                        lineTo(sunCenter.x + halfW, sunCenter.y - baseR)
                        close()
                    }
                    drawPath(innerOutline, InkBrown, style = Stroke(width = 1.6f))

                    // Orbiting miniature origami diamond sun-spark circling outside the ray tips
                    val sparkY = sunCenter.y - coreRadius * 2.12f
                    val sparkSize = coreRadius * 0.14f
                    val diamond = Path().apply {
                        moveTo(sunCenter.x, sparkY - sparkSize)
                        lineTo(sunCenter.x + sparkSize, sparkY)
                        lineTo(sunCenter.x, sparkY + sparkSize)
                        lineTo(sunCenter.x - sparkSize, sparkY)
                        close()
                    }
                    drawPath(diamond, if (i % 2 == 0) StarCardboardGold else PeppermintRed)
                    drawPath(diamond, InkBrown, style = Stroke(width = 1.3f))
                }
            }
        }

        // 4. Faceted Octagonal Origami Sun Core with Geometric Valley/Mountain Creases
        val octagonOuter = Path().apply {
            for (i in 0 until 8) {
                val a = (i * 45f + outerRayDeg * 0.5f) * (Math.PI / 180.0)
                val px = sunCenter.x + cos(a).toFloat() * (coreRadius * 1.05f)
                val py = sunCenter.y + sin(a).toFloat() * (coreRadius * 1.05f)
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(octagonOuter, StarCardboardGold)
        drawPath(octagonOuter, InkBrown, style = Stroke(width = 2.6f))

        // Alternating folded paper facets inside the sun core
        for (i in 0 until 8) {
            val a1 = (i * 45f + outerRayDeg * 0.5f) * (Math.PI / 180.0)
            val a2 = ((i + 1) * 45f + outerRayDeg * 0.5f) * (Math.PI / 180.0)
            val facet = Path().apply {
                moveTo(sunCenter.x, sunCenter.y)
                lineTo(
                    sunCenter.x + cos(a1).toFloat() * coreRadius,
                    sunCenter.y + sin(a1).toFloat() * coreRadius
                )
                lineTo(
                    sunCenter.x + cos(a2).toFloat() * coreRadius,
                    sunCenter.y + sin(a2).toFloat() * coreRadius
                )
                close()
            }
            drawPath(
                path = facet,
                color = if (i % 2 == 0) StickyNoteYellow else OrigamiFoxOrange.copy(alpha = 0.85f)
            )
            drawPath(facet, InkBrown.copy(alpha = 0.45f), style = Stroke(width = 1.2f))
        }

        // Inner warm origami heart-disc
        drawCircle(
            color = PeppermintRed,
            radius = coreRadius * 0.36f,
            center = sunCenter
        )
        drawCircle(
            color = StickyNoteYellow,
            radius = coreRadius * 0.16f,
            center = sunCenter
        )
    }
}

@Composable
fun PaperHomeAuthScreen(
    state: PaperGameState,
    quotaStats: CloudQuotaStats,
    isGoogleSignedIn: Boolean,
    googleEmailLabel: String?,
    onGoogleAuthSuccess: () -> Unit,
    onLoginWithCredentials: (username: String, password: String, isRegister: Boolean) -> String?,
    onSignOutAll: () -> Unit,
    onManualCloudSync: () -> Unit,
    onStartPlaying: () -> Unit,
    onOpenWorldMap: () -> Unit,
    onOpenGmPanel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isRegisterTab by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isErrorFeedback by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val outerOrigamiShape = CutCornerShape(topStart = 20.dp, topEnd = 28.dp, bottomEnd = 20.dp, bottomStart = 28.dp)
    val sectionOrigamiShape = CutCornerShape(topStart = 12.dp, topEnd = 18.dp, bottomEnd = 12.dp, bottomStart = 12.dp)
    val buttonOrigamiShape = CutCornerShape(10.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x781E1108))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .shadow(16.dp, outerOrigamiShape)
                .clip(outerOrigamiShape)
                .background(CardboardLight, outerOrigamiShape)
                .border(3.dp, InkBrown, outerOrigamiShape)
                .origamiFoldedSheet(
                    flapColor = StickyNoteYellow,
                    creaseColor = InkBrown.copy(alpha = 0.16f),
                    flapSize = 26.dp,
                    bottomLeftFlap = true
                )
                .verticalScroll(scrollState)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Origami Sun Hero + Folded Origami Ribbon Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(5.dp, sectionOrigamiShape)
                    .clip(sectionOrigamiShape)
                    .background(StickyNoteYellow, sectionOrigamiShape)
                    .border(2.5.dp, InkBrown, sectionOrigamiShape)
                    .origamiFoldedSheet(
                        flapColor = OrigamiFoxOrange,
                        creaseColor = InkBrown.copy(alpha = 0.14f),
                        flapSize = 18.dp
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Animated Origami Sun with Circling Folded Paper Rays
                    OrigamiCirclingSunHero()

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "ORIGAMI BLITZ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 23.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.0.sp
                        ),
                        color = PeppermintRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Sign in to play and save your progress",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = InkSoftBrown,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Current Account Status & Continue Playing Card (if logged in)
            if (state.isAuthenticatedSession || isGoogleSignedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, sectionOrigamiShape)
                        .clip(sectionOrigamiShape)
                        .background(MaskingTapeCream, sectionOrigamiShape)
                        .border(
                            2.dp,
                            if (state.isGmAccount) StarCardboardGold else OrigamiFoxGreen,
                            sectionOrigamiShape
                        )
                        .origamiFoldedSheet(
                            flapColor = if (state.isGmAccount) StarCardboardGold else OrigamiFoxGreen,
                            flapSize = 15.dp
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isGmAccount) Icons.Default.Star else Icons.Default.CheckCircle,
                                contentDescription = "Active Account",
                                tint = if (state.isGmAccount) StarCardboardGold else OrigamiFoxGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "Player: ${state.accountUsername}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = InkBrown
                                )
                                Text(
                                    text = buildString {
                                        append("Level ${state.level}/${com.example.model.TOTAL_LEVELS} • Best: ${state.highScore}")
                                        if (isGoogleSignedIn) {
                                            append(" • Gmail Connected")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = InkSoftBrown
                                )
                            }
                        }

                        if (state.isGmAccount) {
                            Box(
                                modifier = Modifier
                                    .clip(CutCornerShape(8.dp))
                                    .background(WrappedCandyPurple)
                                    .border(1.5.dp, InkBrown, CutCornerShape(8.dp))
                                    .clickable(onClick = onOpenGmPanel)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("home_open_gm_panel_button")
                            ) {
                                Text(
                                    text = "★ GM Tools",
                                    color = PeppermintWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onStartPlaying,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                                .testTag("home_play_game_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrigamiFoxGreen,
                                contentColor = PeppermintWhite
                            ),
                            shape = buttonOrigamiShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Play Level ${state.level}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = onOpenWorldMap,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(48.dp)
                                .testTag("home_world_map_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrigamiFoxOrange,
                                contentColor = PeppermintWhite
                            ),
                            shape = buttonOrigamiShape
                        ) {
                            Text(
                                text = "World Map",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // 3. Username & Password Login / Create Account (Folded Origami Sheet)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, sectionOrigamiShape)
                    .clip(sectionOrigamiShape)
                    .background(MaskingTapeCream, sectionOrigamiShape)
                    .border(2.dp, InkBrown, sectionOrigamiShape)
                    .origamiFoldedSheet(
                        flapColor = PeppermintRed.copy(alpha = 0.85f),
                        flapSize = 16.dp
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode Tabs: Log In vs Create Account (Chamfered Origami Tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CutCornerShape(8.dp))
                        .background(CardboardLight)
                        .border(1.5.dp, InkBrown, CutCornerShape(8.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CutCornerShape(6.dp))
                            .background(if (!isRegisterTab) StickyNoteYellow else Color.Transparent)
                            .then(
                                if (!isRegisterTab) Modifier.border(1.5.dp, InkBrown, CutCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable {
                                isRegisterTab = false
                                feedbackMessage = null
                            }
                            .padding(vertical = 7.dp)
                            .testTag("tab_login_mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log In",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = InkBrown
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CutCornerShape(6.dp))
                            .background(if (isRegisterTab) StickyNoteYellow else Color.Transparent)
                            .then(
                                if (isRegisterTab) Modifier.border(1.5.dp, InkBrown, CutCornerShape(6.dp))
                                else Modifier
                            )
                            .clickable {
                                isRegisterTab = true
                                feedbackMessage = null
                            }
                            .padding(vertical = 7.dp)
                            .testTag("tab_register_mode"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = InkBrown
                        )
                    }
                }

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Username", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Username",
                            tint = InkBrown
                        )
                    },
                    singleLine = true,
                    shape = CutCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InkBrown,
                        unfocusedTextColor = InkBrown,
                        focusedBorderColor = PeppermintRed,
                        unfocusedBorderColor = InkBrown,
                        focusedContainerColor = PeppermintWhite,
                        unfocusedContainerColor = PeppermintWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input")
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password",
                            tint = InkBrown
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = if (showPassword) "Hide" else "Show",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PeppermintRed,
                            modifier = Modifier
                                .clickable { showPassword = !showPassword }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    singleLine = true,
                    shape = CutCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = InkBrown,
                        unfocusedTextColor = InkBrown,
                        focusedBorderColor = PeppermintRed,
                        unfocusedBorderColor = InkBrown,
                        focusedContainerColor = PeppermintWhite,
                        unfocusedContainerColor = PeppermintWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input")
                )

                Button(
                    onClick = {
                        val err = onLoginWithCredentials(
                            usernameInput.trim(),
                            passwordInput,
                            isRegisterTab
                        )
                        if (err != null) {
                            isErrorFeedback = true
                            feedbackMessage = err
                        } else {
                            isErrorFeedback = false
                            feedbackMessage = if (isRegisterTab) {
                                "Account '${usernameInput.trim()}' created & signed in!"
                            } else {
                                "Welcome back, '${usernameInput.trim()}'!"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .border(2.dp, InkBrown, buttonOrigamiShape)
                        .testTag("account_login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeppermintRed,
                        contentColor = PeppermintWhite
                    ),
                    shape = buttonOrigamiShape
                ) {
                    Text(
                        text = if (isRegisterTab) "Create Account" else "Log In",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                if (state.isAuthenticatedSession && !isGoogleSignedIn) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Log Out of ${state.accountUsername}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PeppermintRed,
                            modifier = Modifier
                                .clickable {
                                    onSignOutAll()
                                    usernameInput = ""
                                    passwordInput = ""
                                    feedbackMessage = "Logged out."
                                }
                                .padding(4.dp)
                                .testTag("local_account_logout_button")
                        )
                    }
                }
            }

            // 4. Google Sign-In (Gmail Account) Origami Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, sectionOrigamiShape)
                    .clip(sectionOrigamiShape)
                    .background(MaskingTapeCream, sectionOrigamiShape)
                    .border(2.dp, InkBrown, sectionOrigamiShape)
                    .origamiFoldedSheet(
                        flapColor = OrigamiCraneBlue,
                        flapSize = 15.dp
                    )
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isGoogleSignedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Connected",
                            tint = OrigamiCraneBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Gmail: ${googleEmailLabel ?: "Connected"}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = InkBrown
                        )
                    }
                }

                if (!isGoogleSignedIn) {
                    Button(
                        onClick = {
                            isGoogleLoading = true
                            feedbackMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isGoogleLoading = false
                                    isErrorFeedback = false
                                    feedbackMessage = "Signed in with Gmail!"
                                    onGoogleAuthSuccess()
                                },
                                onAuthError = { err ->
                                    isGoogleLoading = false
                                    isErrorFeedback = true
                                    feedbackMessage = err
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isGoogleLoading = false
                                }
                            )
                        },
                        enabled = !isGoogleLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(2.dp, InkBrown, buttonOrigamiShape)
                            .testTag("google_sign_in_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrigamiCraneBlue,
                            contentColor = PeppermintWhite
                        ),
                        shape = buttonOrigamiShape
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = PeppermintWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Google Sign-In",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Sign in with Gmail",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                onManualCloudSync()
                                isErrorFeedback = false
                                feedbackMessage = "Progress saved!"
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(2.dp, InkBrown, buttonOrigamiShape)
                                .testTag("cloud_sync_now_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrigamiCraneBlue,
                                contentColor = PeppermintWhite
                            ),
                            shape = buttonOrigamiShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Save Progress",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Save Progress", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                signOutFromGoogle(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    onSignOutComplete = {
                                        onSignOutAll()
                                        isErrorFeedback = false
                                        feedbackMessage = "Signed out of session."
                                    }
                                )
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(48.dp)
                                .border(2.dp, InkBrown, buttonOrigamiShape)
                                .testTag("sign_out_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PeppermintRed,
                                contentColor = PeppermintWhite
                            ),
                            shape = buttonOrigamiShape
                        ) {
                            Text("Sign Out", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            if (feedbackMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isErrorFeedback) Color(0xFFFFE3E3) else StickyNoteYellow,
                            CutCornerShape(8.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isErrorFeedback) PeppermintRed else OrigamiFoxGreen,
                            CutCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = feedbackMessage!!,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isErrorFeedback) PeppermintRed else InkBrown,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 5. Play as Guest Origami Card (when not logged into an account)
            if (!state.isAuthenticatedSession && !isGoogleSignedIn) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, sectionOrigamiShape)
                        .clip(sectionOrigamiShape)
                        .background(MaskingTapeCream, sectionOrigamiShape)
                        .border(2.dp, InkBrown, sectionOrigamiShape)
                        .origamiFoldedSheet(
                            flapColor = OrigamiFoxGreen,
                            flapSize = 15.dp
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onStartPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(2.dp, InkBrown, buttonOrigamiShape)
                            .testTag("play_as_guest_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrigamiFoxGreen,
                            contentColor = PeppermintWhite
                        ),
                        shape = buttonOrigamiShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play as Guest",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "Play as Guest",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "Play right away without logging in. Create an account anytime to save your progress!",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = InkSoftBrown,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun PaperGmControlModal(
    state: PaperGameState,
    quotaStats: CloudQuotaStats,
    onUnlockAllLevels: () -> Unit,
    onGrant99Boosters: () -> Unit,
    onInstantWinLevel: () -> Unit,
    onAdd15Moves: () -> Unit,
    onJumpToLevel: (Int) -> Unit,
    onManualCloudSync: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x991A0E06))
            .clickable(onClick = onDismiss)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .background(CardboardLight, RoundedCornerShape(22.dp))
                .border(3.dp, StarCardboardGold, RoundedCornerShape(22.dp))
                .clickable(enabled = false) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "GM Mode",
                        tint = StarCardboardGold,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "★ GM CONTROL PANEL (${state.accountUsername.uppercase()})",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = InkBrown
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(36.dp)
                        .background(MaskingTapeCream, CircleShape)
                        .border(1.dp, CardboardDark, CircleShape)
                        .testTag("close_gm_panel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close GM Panel",
                        tint = InkBrown
                    )
                }
            }

            Text(
                text = "Game Master privileges active. Instant stage unlocks, infinite boosters, and quota-guarded cloud sync.",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = InkSoftBrown,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUnlockAllLevels,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gm_unlock_all_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrigamiFoxGreen,
                        contentColor = PeppermintWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Unlock All 50 Lvls", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Button(
                    onClick = onGrant99Boosters,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gm_grant_boosters_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WrappedCandyPurple,
                        contentColor = PeppermintWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+99 All Boosters", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onInstantWinLevel()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gm_instant_win_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PeppermintRed,
                        contentColor = PeppermintWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Instant Win (3★)", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                Button(
                    onClick = onAdd15Moves,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("gm_add_moves_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrigamiFoxOrange,
                        contentColor = PeppermintWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("+15 Extra Moves", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            Text(
                text = "Quick Jump to Any Stage (1–10):",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = InkBrown
            )

            // 2 rows of 5 stage jump pills
            for (rowIdx in 0..1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (colIdx in 1..5) {
                        val stageNum = rowIdx * 5 + colIdx
                        val targetLevel = (stageNum - 1) * 5 + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (state.currentStage.stageNumber == stageNum) StickyNoteYellow else MaskingTapeCream
                                )
                                .border(
                                    1.5.dp,
                                    if (state.currentStage.stageNumber == stageNum) PeppermintRed else CardboardMedium,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onJumpToLevel(targetLevel)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Stg $stageNum",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = InkBrown
                            )
                        }
                    }
                }
            }

            // Quota Status & Manual Sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaskingTapeCream, RoundedCornerShape(10.dp))
                    .border(1.dp, MaskingTapeBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quotaStats.lastSyncStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkBrown
                    )
                    Text(
                        text = "Reads: ${quotaStats.sessionReads} | Writes: ${quotaStats.sessionWrites} | Skipped: ${quotaStats.skippedDuplicateWrites}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = InkSoftBrown
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(OrigamiCraneBlue)
                        .clickable(onClick = onManualCloudSync)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Sync Now",
                        color = PeppermintWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
