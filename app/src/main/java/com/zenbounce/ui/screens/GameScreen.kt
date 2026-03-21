package com.zenbounce.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.zenbounce.game.GameState
import com.zenbounce.game.GameViewModel
import com.zenbounce.theme.AppTheme
import com.zenbounce.ui.components.AmbientParticle
import com.zenbounce.ui.components.AmbientParticleState
import com.zenbounce.ui.components.BallCanvas
import com.zenbounce.ui.components.FlashState
import com.zenbounce.ui.components.TRAIL_LENGTH
import kotlinx.coroutines.launch

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
    theme: AppTheme
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

    // ---- Canvas size (set via callback from BallCanvas) --------------------
    var canvasW by remember { mutableStateOf(0f) }
    var canvasH by remember { mutableStateOf(0f) }

    // ---- Lifecycle pause/resume --------------------------------------------
    // Use lifecycle-aware variants so that a user-initiated pause is NOT
    // overridden when the app returns to the foreground.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE  -> viewModel.onLifecyclePause()
                Lifecycle.Event.ON_RESUME -> viewModel.onLifecycleResume()
                else                      -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- Back handler: dismiss ThemePicker instead of exiting the app ------
    BackHandler(enabled = showThemePicker) {
        showThemePicker = false
    }

    // ---- Main game loop (Choreographer-synced) ------------------------------
    LaunchedEffect(Unit) {
        var lastFrameMs = withFrameMillis { it }

        while (true) {
            val frameMs = withFrameMillis { it }
            val deltaMs = (frameMs - lastFrameMs).coerceAtLeast(0L)
            lastFrameMs = frameMs

            // Advance physics
            viewModel.tick(deltaMs)

            val isPaused = gameState?.status == GameState.Status.Paused

            // Only update visual elements while the game is running
            if (!isPaused) {
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
    val isPaused = gameState?.status == GameState.Status.Paused

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

        // Tap-to-pause / tap-to-resume overlay (covers game area, under FAB)
        // Disabled while the theme picker is open so the scrim handles taps there.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!showThemePicker) {
                        Modifier.clickable {
                            if (isPaused) viewModel.resumeGame() else viewModel.pauseGame()
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isPaused) {
                // Dim the background and show a pause prompt
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PAUSED",
                        color = theme.accentColor,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to Resume",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Theme picker FAB — bottom-right (on top of tap overlay)
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
    }
}
