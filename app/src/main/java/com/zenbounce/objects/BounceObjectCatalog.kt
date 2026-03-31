package com.zenbounce.objects

import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for all available bounce objects.
 *
 * To add a new object:
 *  1. Create a new [BounceObject] constant here.
 *  2. Add it to [ALL].
 *  3. Optionally add a sound file to res/raw/ and set [BounceObject.soundResId].
 *  No other changes are required — the picker UI and physics are fully data-driven.
 *
 * Physics notes:
 *  effectiveGravity = rawGravity × sensitivityFactor × (1 / mass)
 *  Lighter objects (low mass) react faster; heavier objects (high mass) react slower.
 */
object BounceObjectCatalog {

    /** Default ZenBall — uses the active AppTheme colours, matches legacy behaviour. */
    val DEFAULT = BounceObject(
        id = 0,
        name = "default",
        displayName = "ZenBall",
        physics = ObjectPhysics(mass = 1.0f, restitution = 0.75f, airDamping = 0.995f),
        visuals = ObjectVisuals(
            renderType = RenderType.DEFAULT,
            primaryColor = Color.Transparent,   // resolved from AppTheme at render time
            secondaryColor = Color.Transparent,
            glowColor = Color.Transparent,      // resolved from AppTheme at render time
            radius = 36f                        // matches GameState.DEFAULT_BALL_RADIUS
        )
        // soundResId = R.raw.bounce_default    — add after placing OGG in res/raw/
    )

    /** Ping pong ball — very light, fast, high bounce. */
    val PING_PONG = BounceObject(
        id = 1,
        name = "ping_pong",
        displayName = "Ping Pong",
        physics = ObjectPhysics(mass = 0.27f, restitution = 0.90f, airDamping = 0.992f),
        visuals = ObjectVisuals(
            renderType = RenderType.PING_PONG,
            primaryColor = Color(0xFFF5F5F5),
            secondaryColor = Color(0xFFFFAD80),
            glowColor = Color(0xFF80C8FF),
            radius = 25f
        )
        // soundResId = R.raw.bounce_pingpong
    )

    /** Tennis ball — medium-light, good bounce, yellow-green. */
    val TENNIS_BALL = BounceObject(
        id = 2,
        name = "tennis_ball",
        displayName = "Tennis Ball",
        physics = ObjectPhysics(mass = 1.5f, restitution = 0.82f, airDamping = 0.994f),
        visuals = ObjectVisuals(
            renderType = RenderType.TENNIS_BALL,
            primaryColor = Color(0xFFCCE83D),
            secondaryColor = Color.White,
            glowColor = Color(0xFFE8FF60),
            radius = 42f
        )
        // soundResId = R.raw.bounce_tennis
    )

    /** Football (soccer ball) — heavy, low bounce, slow response. */
    val FOOTBALL = BounceObject(
        id = 3,
        name = "football",
        displayName = "Football",
        physics = ObjectPhysics(mass = 4.5f, restitution = 0.65f, airDamping = 0.997f),
        visuals = ObjectVisuals(
            renderType = RenderType.FOOTBALL,
            primaryColor = Color.White,
            secondaryColor = Color(0xFF1A1A1A),
            glowColor = Color(0xFFAAFF88),
            radius = 60f
        )
        // soundResId = R.raw.bounce_football
    )

    /**
     * All objects in picker display order.
     * Adding an entry here is sufficient to surface a new object throughout the app.
     */
    val ALL: List<BounceObject> = listOf(DEFAULT, PING_PONG, TENNIS_BALL, FOOTBALL)

    /** Resolve by persisted ID; falls back to [DEFAULT] for unknown IDs. */
    fun byId(id: Int): BounceObject = ALL.find { it.id == id } ?: DEFAULT
}
