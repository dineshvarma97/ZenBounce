package com.zenbounce.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.zenbounce.haptics.HapticManager
import com.zenbounce.objects.BounceObject
import com.zenbounce.objects.BounceObjectCatalog
import com.zenbounce.objects.BounceObjectManager
import com.zenbounce.sensors.GyroscopeManager
import com.zenbounce.sound.SoundManager
import com.zenbounce.theme.AppTheme
import com.zenbounce.theme.ThemeManager
import com.zenbounce.theme.ThemePresets
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.zenbounce.prefs.SensitivityManager

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
    private val sensitivityManager = SensitivityManager(appContext)
    private val objectManager = BounceObjectManager(appContext)
    private val soundManager = SoundManager(appContext)

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

    // ---- Sensitivity --------------------------------------------------------

    private val _sensitivity = MutableStateFlow(SensitivityManager.DEFAULT_SENSITIVITY)
    val sensitivity: StateFlow<Int> = _sensitivity.asStateFlow()

    private var sensitivityJob: Job? = null

    /**
     * Update sensitivity immediately in-memory and debounce the DataStore write
     * so rapid slider drags don't flood disk I/O.
     */
    fun setSensitivity(value: Int) {
        _sensitivity.value = value
        sensitivityJob?.cancel()
        sensitivityJob = viewModelScope.launch {
            delay(300L)
            sensitivityManager.setSensitivity(value)
        }
    }

    // ---- Bounce Object ------------------------------------------------------

    private val _currentObject = MutableStateFlow<BounceObject>(BounceObjectCatalog.DEFAULT)

    /** The currently active bounce object, updated immediately and persisted to DataStore. */
    val currentObject: StateFlow<BounceObject> = _currentObject.asStateFlow()

    /**
     * Switch to [obj] immediately (instant physics + visual change) and persist
     * the choice to DataStore in the background.
     */
    fun selectObject(obj: BounceObject) {
        _currentObject.value = obj
        // Update radius on all existing balls so physics and rendering stay in sync
        _gameState.update { state ->
            state?.copy(balls = state.balls.map { it.copy(radius = obj.visuals.radius) })
        }
        viewModelScope.launch { objectManager.selectObject(obj) }
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
        // Restore saved sensitivity
        viewModelScope.launch {
            _sensitivity.value = sensitivityManager.sensitivity.first()
        }
        // Restore saved object selection
        viewModelScope.launch {
            val savedObj = objectManager.currentObject.first()
            _currentObject.value = savedObj
        }
        // Collect sensor gravity on a coroutine scoped to the ViewModel lifetime
        viewModelScope.launch {
            gyroscopeManager.gravityFlow().collect { vector ->
                _gravity.value = vector
            }
        }
        // Pre-load all object collision sounds (only those with a resource ID assigned)
        soundManager.preload(BounceObjectCatalog.ALL.mapNotNull { it.soundResId })
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

        // Initialise or reset ball when size is known, using the current object's radius
        _gameState.value = GameState.initial(width, height, ballRadius = _currentObject.value.visuals.radius)
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

        val obj = _currentObject.value

        // Scale gravity by sensitivity (0=still, 50=normal 1×, 100=fast 2×)
        // then divide by mass so heavier objects respond more sluggishly
        val raw = _gravity.value
        val sensitivityFactor = (_sensitivity.value / 50f).coerceIn(0f, 4f)
        val massFactor = 1f / obj.physics.mass
        val gravity = GravityVector(
            raw.x * sensitivityFactor * massFactor,
            raw.y * sensitivityFactor * massFactor
        )

        val updatedBalls = state.balls.map { ball ->
            val result = PhysicsEngine.update(
                ball = ball,
                gravity = gravity,
                deltaMs = deltaMs,
                boundsW = canvasWidth,
                boundsH = canvasHeight,
                restitution = obj.physics.restitution,
                airDamping = obj.physics.airDamping
            )

            result.collision?.let { info ->
                viewModelScope.launch {
                    hapticManager.vibrate(info.speed)
                }
                _collisionEvents.trySend(info.speed)
                soundManager.playCollisionSound(obj.soundResId, info.speed)
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
     * The game intentionally stays paused so the PauseOverlay is presented and the
     * user can explicitly tap Resume rather than having the game snap back immediately.
     */
    fun onLifecycleResume() {
        // No-op: game remains paused; user resumes via the PauseOverlay.
    }

    /** Pause triggered by an explicit user action (tap-to-pause). */
    fun pauseGame() {
        _gameState.update { it?.copy(status = GameState.Status.Paused) }
    }

    /** Resume triggered by an explicit user action (tap-to-resume). */
    fun resumeGame() {
        _gameState.update { it?.copy(status = GameState.Status.Playing) }
    }

    /** Reset the ball and start fresh. Safe to call before canvas size is known. */
    fun startNewGame() {
        if (canvasWidth > 0f && canvasHeight > 0f) {
            _gameState.value = GameState.initial(canvasWidth, canvasHeight, ballRadius = _currentObject.value.visuals.radius)
        } else {
            resumeGame()
        }
    }

    // ---- Lifecycle cleanup --------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
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
