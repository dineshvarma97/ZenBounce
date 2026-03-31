package com.zenbounce.game

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [PhysicsEngine].
 *
 * All tests run on the JVM (zero Android dependencies in PhysicsEngine).
 */
class PhysicsEngineTest {

    companion object {
        private const val W = 1080f
        private const val H = 1920f
        private const val RADIUS = 36f
        private const val DELTA_MS = 16L    // ~60 fps frame
    }

    private fun makeBall(
        x: Float = W / 2f,
        y: Float = H / 2f,
        vx: Float = 0f,
        vy: Float = 0f
    ) = Ball(position = Offset(x, y), velocity = Offset(vx, vy), radius = RADIUS)

    private fun noGravity() = GravityVector(0f, 0f)

    // ---- Basic motion -------------------------------------------------------

    @Test
    fun `ball moves in direction of velocity with no gravity`() {
        val ball = makeBall(vx = 300f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertTrue(
            "Ball should have moved right",
            result.ball.position.x > ball.position.x
        )
    }

    @Test
    fun `gravity accelerates ball in positive y direction`() {
        val ball = makeBall(vy = 0f)
        val gravity = GravityVector(0f, 980f)   // downward gravity in px/s²
        val result = PhysicsEngine.update(ball, gravity, DELTA_MS, W, H)

        assertTrue(
            "Vertical velocity should increase with downward gravity",
            result.ball.velocity.y > 0f
        )
    }

    // ---- Wall collisions ----------------------------------------------------

    @Test
    fun `ball bounces off right wall`() {
        // Place ball near right wall, moving right
        val ball = makeBall(x = W - RADIUS - 1f, vx = 500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertNotNull("Collision should be reported", result.collision)
        assertTrue(
            "Ball x velocity should be negative after right-wall bounce",
            result.ball.velocity.x < 0f
        )
        assertTrue(
            "Ball should be clamped inside right boundary",
            result.ball.position.x + RADIUS <= W
        )
    }

    @Test
    fun `ball bounces off left wall`() {
        val ball = makeBall(x = RADIUS + 1f, vx = -500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertNotNull("Collision should be reported", result.collision)
        assertTrue(
            "Ball x velocity should be positive after left-wall bounce",
            result.ball.velocity.x > 0f
        )
        assertTrue(
            "Ball should be clamped inside left boundary",
            result.ball.position.x - RADIUS >= 0f
        )
    }

    @Test
    fun `ball bounces off top wall`() {
        val ball = makeBall(y = RADIUS + 1f, vy = -500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertNotNull("Collision should be reported", result.collision)
        assertTrue(
            "Ball y velocity should be positive after top-wall bounce",
            result.ball.velocity.y > 0f
        )
    }

    @Test
    fun `ball bounces off bottom wall`() {
        val ball = makeBall(y = H - RADIUS - 1f, vy = 500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertNotNull("Collision should be reported", result.collision)
        assertTrue(
            "Ball y velocity should be negative after bottom-wall bounce",
            result.ball.velocity.y < 0f
        )
    }

    @Test
    fun `no collision reported for ball moving away from wall`() {
        // Ball very close to right wall but moving LEFT (away from it)
        val ball = makeBall(x = W - RADIUS - 1f, vx = -200f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        assertNull("No collision when moving away from wall", result.collision)
    }

    // ---- Restitution --------------------------------------------------------

    @Test
    fun `velocity after bounce is reduced by restitution factor`() {
        val speed = 500f
        val ball = makeBall(x = W - RADIUS - 1f, vx = speed)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)

        val expectedSpeed = speed * PhysicsEngine.RESTITUTION
        assertEquals(
            "Reflected speed should equal input * RESTITUTION",
            expectedSpeed,
            -result.ball.velocity.x,    // vx negated after right-wall bounce
            20f                          // tolerance for damping and integration
        )
    }

    // ---- Delta clamping (anti-tunnelling) -----------------------------------

    @Test
    fun `large delta is clamped so ball does not tunnel through wall`() {
        // With no cap, a 2 s delta and 10 000 px/s velocity would place ball far outside bounds.
        // With the 50 ms cap it should still stay inside.
        val ball = makeBall(x = W / 2f, vx = 10_000f)
        val result = PhysicsEngine.update(ball, noGravity(), 2000L, W, H)

        assertTrue(
            "Ball should remain within right boundary even with huge delta",
            result.ball.position.x <= W
        )
    }

    // ---- Ball stays in bounds -----------------------------------------------

    @Test
    fun `ball position is always within bounds after update`() {
        // Run 200 frames starting near the centre with high velocity and fall-gravity
        var ball = makeBall(vx = 800f, vy = -1200f)
        val gravity = GravityVector(200f, 600f)

        repeat(200) {
            val result = PhysicsEngine.update(ball, gravity, DELTA_MS, W, H)
            ball = result.ball
            assertTrue("x should be >= radius",  ball.position.x >= RADIUS)
            assertTrue("x should be <= W-radius", ball.position.x <= W - RADIUS)
            assertTrue("y should be >= radius",  ball.position.y >= RADIUS)
            assertTrue("y should be <= H-radius", ball.position.y <= H - RADIUS)
        }
    }

    // ---- Per-object physics parameters --------------------------------------

    @Test
    fun `custom restitution is applied instead of default`() {
        val highBounce = 0.95f
        val ball = makeBall(x = W - RADIUS - 1f, vx = 500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H, restitution = highBounce)

        // Reflected speed should approximate input speed × highBounce
        val expected = 500f * highBounce
        assertEquals("High restitution should be used", expected, -result.ball.velocity.x, 30f)
    }

    @Test
    fun `low restitution produces nearly zero bounce`() {
        val deadBounce = 0.10f
        val ball = makeBall(x = W - RADIUS - 1f, vx = 500f)
        val result = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H, restitution = deadBounce)

        assertTrue(
            "Very low restitution should result in nearly no reflected speed",
            -result.ball.velocity.x < 100f
        )
    }

    @Test
    fun `heavy object (high mass factor applied externally) moves slower than light object`() {
        // Simulate the ViewModel applying massFactor before calling update:
        //   gravity_effective = gravity_raw * (1 / mass)
        val rawGravity = GravityVector(0f, 960f)
        val pingPongMass = 0.27f  // lighter  → gets more effective gravity
        val footballMass  = 4.5f  // heavier → gets less effective gravity

        val gravityPingPong = GravityVector(rawGravity.x / pingPongMass, rawGravity.y / pingPongMass)
        val gravityFootball  = GravityVector(rawGravity.x / footballMass,  rawGravity.y / footballMass)

        val startBall = makeBall(vy = 0f)
        val ppResult  = PhysicsEngine.update(startBall, gravityPingPong, DELTA_MS, W, H)
        val fbResult  = PhysicsEngine.update(startBall, gravityFootball, DELTA_MS, W, H)

        assertTrue(
            "Ping pong (light) should accelerate faster than football (heavy)",
            ppResult.ball.velocity.y > fbResult.ball.velocity.y
        )
    }

    @Test
    fun `custom airDamping reduces velocity more aggressively than default`() {
        val highDamping = 0.50f  // much stronger air resistance
        val ball = makeBall(vx = 1000f)
        val resultDefault = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H)
        val resultDamped  = PhysicsEngine.update(ball, noGravity(), DELTA_MS, W, H, airDamping = highDamping)

        assertTrue(
            "Custom high damping should produce lower speed than default damping",
            resultDamped.ball.velocity.x < resultDefault.ball.velocity.x
        )
    }
}
