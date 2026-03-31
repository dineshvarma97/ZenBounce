package com.zenbounce.game

import androidx.compose.ui.geometry.Offset

/**
 * Gravity vector emitted by the sensor layer.
 * Values are in m/s² and represent the force of gravity on each axis
 * as returned by the TYPE_GRAVITY sensor.
 *
 * x — lateral (roll left/right)
 * y — longitudinal (tilt forward/backward)
 */
data class GravityVector(val x: Float, val y: Float)

/**
 * Information about a wall collision that occurred during a physics step.
 * Used by the ViewModel to schedule haptic feedback.
 *
 * @param speed Magnitude of velocity component normal to the wall (pixels/s),
 *              used to scale haptic intensity.
 */
data class CollisionInfo(val speed: Float)

/**
 * Result of a single physics update step.
 *
 * @param ball      The updated ball (new position + velocity)
 * @param collision Non-null if a wall collision occurred this step
 */
data class PhysicsResult(val ball: Ball, val collision: CollisionInfo?)

/**
 * Pure, stateless physics engine — no Android dependencies.
 *
 * All methods are pure functions: same inputs always yield the same output.
 * The ViewModel calls [update] once per Choreographer frame.
 *
 * Coordinate system matches Compose Canvas:
 *   x increases rightward, y increases downward.
 * Gravity from sensors therefore needs its y-component negated before being
 * passed here (sensor +y = device tilted away from user = ball goes down visually).
 */
object PhysicsEngine {

    /** Default restitution coefficient (1.0 = perfectly elastic, 0.0 = completely inelastic). */
    const val RESTITUTION = 0.75f

    /** Default velocity damping applied every frame (simulates air resistance). */
    const val AIR_DAMPING = 0.995f

    /**
     * Advance the simulation by [deltaMs] milliseconds.
     *
     * Per-object physics can be supplied via [restitution] and [airDamping].
     * Defaults match the legacy constants so existing callers are unaffected.
     *
     * @param ball        Current ball state
     * @param gravity     Gravity vector in pixels/s² (already scaled from sensor m/s²)
     * @param deltaMs     Frame delta in milliseconds; clamped to [MAX_DELTA_MS] to prevent
     *                    tunnelling after the app is resumed from background
     * @param boundsW     Width of the playfield in pixels (from canvas size)
     * @param boundsH     Height of the playfield in pixels
     * @param restitution Bounce elasticity for this object (0..1); defaults to [RESTITUTION]
     * @param airDamping  Per-frame velocity multiplier for this object (0..1); defaults to [AIR_DAMPING]
     */
    fun update(
        ball: Ball,
        gravity: GravityVector,
        deltaMs: Long,
        boundsW: Float,
        boundsH: Float,
        restitution: Float = RESTITUTION,
        airDamping: Float = AIR_DAMPING
    ): PhysicsResult {
        // Cap delta to avoid tunnelling on resume
        val dt = minOf(deltaMs, MAX_DELTA_MS) / 1000f   // seconds

        // --- Integrate velocity ---
        val vx = (ball.velocity.x + gravity.x * dt) * airDamping
        val vy = (ball.velocity.y + gravity.y * dt) * airDamping

        // --- Integrate position ---
        var newX = ball.position.x + vx * dt
        var newY = ball.position.y + vy * dt

        var finalVx = vx
        var finalVy = vy
        var collision: CollisionInfo? = null

        val r = ball.radius

        // --- Wall collision resolution ---
        // Each wall: clamp position + reflect velocity + record highest-speed collision
        var collisionSpeed = 0f

        if (newX - r < 0f) {
            newX = r
            if (finalVx < 0f) {
                collisionSpeed = maxOf(collisionSpeed, -finalVx)
                finalVx = -finalVx * restitution
            }
        }
        if (newX + r > boundsW) {
            newX = boundsW - r
            if (finalVx > 0f) {
                collisionSpeed = maxOf(collisionSpeed, finalVx)
                finalVx = -finalVx * restitution
            }
        }
        if (newY - r < 0f) {
            newY = r
            if (finalVy < 0f) {
                collisionSpeed = maxOf(collisionSpeed, -finalVy)
                finalVy = -finalVy * restitution
            }
        }
        if (newY + r > boundsH) {
            newY = boundsH - r
            if (finalVy > 0f) {
                collisionSpeed = maxOf(collisionSpeed, finalVy)
                finalVy = -finalVy * restitution
            }
        }

        if (collisionSpeed > MIN_COLLISION_SPEED) {
            collision = CollisionInfo(speed = collisionSpeed)
        }

        val updatedBall = ball.copy(
            position = Offset(newX, newY),
            velocity = Offset(finalVx, finalVy)
        )

        return PhysicsResult(ball = updatedBall, collision = collision)
    }

    // ---- Constants --------------------------------------------------------

    /** Max allowed delta in ms to prevent ball tunnelling on resume. */
    private const val MAX_DELTA_MS = 50L

    /**
     * Minimum normal-component speed (px/s) for a collision to trigger haptics.
     * Filters out micro-jitter bounces when the ball rests against a wall.
     */
    private const val MIN_COLLISION_SPEED = 80f
}
