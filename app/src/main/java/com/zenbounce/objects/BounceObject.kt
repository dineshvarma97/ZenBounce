package com.zenbounce.objects

import androidx.compose.ui.graphics.Color

/**
 * The visual render style for a bounce object.
 *
 * To add a new style:
 *  1. Add a new entry here
 *  2. Add a `draw*` extension function in BallCanvas
 *  3. Create a [BounceObject] entry in [BounceObjectCatalog]
 */
enum class RenderType {
    DEFAULT,
    FOOTBALL,
    TENNIS_BALL,
    PING_PONG
}

/**
 * Physics parameters for a bounce object.
 *
 * @param mass        Normalised mass (> 0). Higher = slower response to tilt / sensor input.
 * @param restitution Bounce elasticity (0 = dead stop, 1 = perfectly elastic).
 * @param airDamping  Per-frame velocity multiplier for air resistance (0..1).
 */
data class ObjectPhysics(
    val mass: Float,
    val restitution: Float,
    val airDamping: Float
) {
    init {
        require(mass > 0f) { "mass must be positive, got $mass" }
        require(restitution in 0f..1f) { "restitution must be in 0..1, got $restitution" }
        require(airDamping in 0f..1f) { "airDamping must be in 0..1, got $airDamping" }
    }
}

/**
 * Visual appearance of a bounce object.
 *
 * @param renderType    How the object is drawn on the canvas.
 * @param primaryColor  Main fill colour of the object.
 * @param secondaryColor Secondary detail colour (patches, seam, shading).
 * @param glowColor     Halo glow colour used in the BlurMaskFilter pass.
 * @param radius        Collision and rendering radius in canvas pixels (same units as PhysicsEngine).
 */
data class ObjectVisuals(
    val renderType: RenderType,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val radius: Float
)

/**
 * A fully described bounceable object.
 *
 * Adding a new object only requires a new entry in [BounceObjectCatalog] —
 * no other code changes are needed.
 *
 * @param id          Unique, stable integer (persisted to DataStore).
 * @param name        Internal key (snake_case).
 * @param displayName Human-readable name shown in the picker UI.
 * @param physics     Mass / restitution / air-damping coefficients.
 * @param visuals     Colours, render type, and radius.
 * @param soundResId  R.raw.* resource ID for collision sound; null = silent.
 */
data class BounceObject(
    val id: Int,
    val name: String,
    val displayName: String,
    val physics: ObjectPhysics,
    val visuals: ObjectVisuals,
    val soundResId: Int? = null
)
