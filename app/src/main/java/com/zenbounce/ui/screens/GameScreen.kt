package com.zenbounce.ui.screens

import androidx.compose.runtime.withFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenbounce.game.GameViewModel
import com.zenbounce.theme.AppTheme
import com.zenbounce.ui.components.AmbientParticle
import com.zenbounce.ui.components.AmbientParticleState
import com.zenbounce.ui.components.BallCanvas
import com.zenbounce.ui.components.FlashState
import com.zenbounce.ui.components.TRAIL_LENGTH
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.zenbounce.game.GameState

/**
 * Full-screen game screen.
 *
 * Responsibilities:
 *  - Runs the Choreographer-synced game loop via `withFrameMillis`
 *  - Maintains trail ring-buffer
 *  - Drives ambient particle animation
 *  - Fires the edge-flash animation on collision
 *  - Hosts the theme-picker FAB and bottom sheet
 *  - Pauses/resumes the game on lifecycle events
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    theme: AppTheme,
    onMainMenu: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()

    // ---- Trail ring-buffer --------------------------------------------------
    val trailPositions = remember { mutableStateListOf<Offset>() }

    // ---- Ambient particles --------------------------------------------------
    val particleState = remember { AmbientParticleState(count = 14) }
    val particles = remember { mutableStateListOf<AmbientParticle>() }

    // ---- Wall-hit flash -----------------------------------------------------
    val flashState = remember { FlashState() }
    val flashScope = rememberCoroutineScope()

    // ---- Theme picker -------------------------------------------------------
    var showThemePicker by remember { mutableStateOf(false) }

    // ---- Pause overlay ------------------------------------------------------
    var showPauseOverlay by remember { mutableStateOf(false) }

    // ---- Canvas size (set via callback from BallCanvas) --------------------
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // ---- Lifecycle pause/resume --------------------------------------------
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> viewModel.pauseGame()
                Lifecycle.Event.ON_RESUME -> viewModel.resumeGame()
                else                      -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- Main game loop (Choreographer-synced) ------------------------------
    LaunchedEffect(Unit) {
        viewModel.resumeGame()   // ensure game is running when screen appears
        var lastFrameMs = withFrameMillis { it }

        while (true) {
            val frameMs = withFrameMillis { it }
            val deltaMs = (frameMs - lastFrameMs).coerceAtLeast(0L)
            lastFrameMs = frameMs

            val paused = gameState?.status == GameState.Status.Paused

            // Advance physics (no-op when paused)
            viewModel.tick(deltaMs)

            if (!paused) {
                // Update particles
                if (canvasW > 0f && canvasH > 0f) {
                    particleState.initialise(canvasW, canvasH)
                    particleState.update(deltaMs, canvasW, canvasH)
                    particles.clear()
                    particles.addAll(particleState.particles)
                }

                // Update trail from current ball positions
                gameState?.balls?.firstOrNull()?.let { ball ->
                    if (trailPositions.size >= TRAIL_LENGTH) {
                        trailPositions.removeFirst()
                    }
                    trailPositions.add(ball.position)
                }
            }
        }
    }

    // ---- Flash trigger on collision ----------------------------------------
    LaunchedEffect(viewModel) {
        viewModel.collisionEvents.collect {
            // Launch in a separate scope so animation doesn't block the collect loop
            flashScope.launch { flashState.flash() }
        }
    }

    // ---- UI -----------------------------------------------------------------
    Box(modifier = Modifier.fillMaxSize()) {

        BallCanvas(
            gameState = gameState,
            theme = theme,
            trailPositions = trailPositions.toList(),
            flashAlpha = flashState.alpha.value,
            particles = particles.toList(),
            onSize = { w, h ->
                if (w != canvasW || h != canvasH) {
                    canvasW = w
                    canvasH = h
                    viewModel.onCanvasSize(w, h)
                }
            }
        )

        // Pause button — top-right
        IconButton(
            onClick = {
                showPauseOverlay = true
                viewModel.pauseGame()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        // Theme picker FAB — bottom-right
        FloatingActionButton(
            onClick = { showThemePicker = true },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp),
            containerColor = theme.accentColor.copy(alpha = 0.85f),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Choose theme",
                tint = Color.White
            )
        }

        // Theme picker bottom sheet
        if (showThemePicker) {
            ThemePicker(
                currentTheme = theme,
                onSelectTheme = { viewModel.selectTheme(it) },
                onDismiss = { showThemePicker = false }
            )
        }

        // Pause overlay
        if (showPauseOverlay) {
            PauseOverlay(
                onResume = {
                    showPauseOverlay = false
                    viewModel.resumeGame()
                },
                onMainMenu = onMainMenu
            )
        }
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Paused",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00EFFF),
                    contentColor   = Color(0xFF0A0E27)
                )
            ) {
                Text("Resume", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onMainMenu,
                modifier = Modifier.width(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1F3C),
                    contentColor   = Color(0xFF00EFFF)
                )
            ) {
                Text("Main Menu", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
