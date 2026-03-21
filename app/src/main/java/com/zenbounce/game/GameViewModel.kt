package com.zenbounce.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenbounce.haptics.HapticManager
import com.zenbounce.sensors.GyroscopeManager
import com.zenbounce.theme.AppTheme
import com.zenbounce.theme.ThemeManager
import com.zenbounce.theme.ThemePresets
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Central orchestrator connecting:
 *  - [GyroscopeManager] → sensor input
 *  - [PhysicsEngine]    → stateless physics computation
 *  - [HapticManager]    → collision feedback
 *  - [ThemeManager]     → persistent theme selection
 *
 * The game loop lives in the UI layer ([GameScreen]) using `withFrameMillis`
 * and calls [tick] on each Choreographer callback. This keeps the ViewModel
 * concerned only with *what* the state is, not *when* to render.
 */
class GameViewModel(
    private val appContext: Context
) : ViewModel() {

    // ---- Dependencies -------------------------------------------------------

    private val gyroscopeManager = GyroscopeManager(appContext)
    private val hapticManager = HapticManager(appContext)
    private val themeManager = ThemeManager(appContext)

    // ---- Theme --------------------------------------------------------------

    val currentTheme: StateFlow<AppTheme> = themeManager.currentTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemePresets.default
        )

    fun selectTheme(theme: AppTheme) {
        viewModelScope.launch { themeManager.selectTheme(theme) }
    }

    // ---- Collision events (for UI flash) ------------------------------------

    private val _collisionEvents = Channel<Float>(capacity = Channel.UNLIMITED)

    /**
     * Emits the collision speed (px/s) each time the ball hits a wall.
     * Collect in the UI layer to drive the edge-flash animation.
     */
    val collisionEvents = _collisionEvents.receiveAsFlow()

    // ---- Game State ---------------------------------------------------------

    /** Mutable game state — updated every frame via [tick]. */
    private val _gameState = MutableStateFlow<GameState?>(null)

    /** Exposed as read-only. Null until canvas size is first known. */
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    // ---- Gravity (from sensor) ----------------------------------------------

    /** Gravity vector in px/s² — updated by the sensor flow. */
    private val _gravity = MutableStateFlow(GravityVector(0f, 0f))

    init {
        // Collect sensor gravity on a coroutine scoped to the ViewModel lifetime
        viewModelScope.launch {
            gyroscopeManager.gravityFlow().collect { vector ->
                _gravity.value = vector
            }
        }
    }

    // ---- Canvas size --------------------------------------------------------

    private var canvasWidth: Float = 0f
    private var canvasHeight: Float = 0f

    /**
     * Called by the UI once the Canvas is measured.
     * Sets canvas dimensions and (re)initialises the game state if needed.
     */
    fun onCanvasSize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        if (canvasWidth == width && canvasHeight == height) return

        canvasWidth = width
        canvasHeight = height

        // Initialise or reset ball when size is known
        _gameState.value = GameState.initial(width, height)
    }

    // ---- Game Loop ----------------------------------------------------------

    /**
     * Advance the simulation by [deltaMs] milliseconds.
     * Called on every Choreographer frame from the Compose game loop.
     *
     * @param deltaMs Time since the last frame in milliseconds.
     */
    fun tick(deltaMs: Long) {
        val state = _gameState.value ?: return
        if (state.status == GameState.Status.Paused) return
        if (canvasWidth <= 0f || canvasHeight <= 0f) return

        val gravity = _gravity.value

        val updatedBalls = state.balls.map { ball ->
            val result = PhysicsEngine.update(
                ball = ball,
                gravity = gravity,
                deltaMs = deltaMs,
                boundsW = canvasWidth,
                boundsH = canvasHeight
            )

            // Fire haptic on collision (non-blocking, uses internal cooldown)
            result.collision?.let { info ->
                viewModelScope.launch {
                    hapticManager.vibrate(info.speed)
                }
                _collisionEvents.trySend(info.speed)
            }

            result.ball
        }

        _gameState.update { it?.copy(balls = updatedBalls) }
    }

    // ---- Lifecycle ----------------------------------------------------------

    /**
     * True when the game was actively [Playing] at the moment the app went to
     * background.  Used by [onLifecycleResume] to decide whether to auto-resume.
     */
    private var wasPlayingBeforeBackground = false

    /**
     * Called by the lifecycle observer when the app goes to background (ON_PAUSE).
     * Records the current play-state so we can restore it correctly on return.
     */
    fun onLifecyclePause() {
        wasPlayingBeforeBackground = _gameState.value?.status == GameState.Status.Playing
        _gameState.update { it?.copy(status = GameState.Status.Paused) }
    }

    /**
     * Called by the lifecycle observer when the app returns to foreground (ON_RESUME).
     * Only auto-resumes if the game was playing when we left — a user-initiated pause
     * (e.g. tapping the screen) is preserved across background/foreground cycles.
     */
    fun onLifecycleResume() {
        if (wasPlayingBeforeBackground) {
            _gameState.update { it?.copy(status = GameState.Status.Playing) }
        }
    }

    /** Pause triggered by an explicit user action (tap-to-pause). */
    fun pauseGame() {
        _gameState.update { it?.copy(status = GameState.Status.Paused) }
    }

    /** Resume triggered by an explicit user action (tap-to-resume). */
    fun resumeGame() {
        _gameState.update { it?.copy(status = GameState.Status.Playing) }
    }
}

// ---- Factory ----------------------------------------------------------------

/**
 * Factory required because [GameViewModel] takes a constructor argument ([Context]).
 */
class GameViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GameViewModel(context.applicationContext) as T
    }
}
