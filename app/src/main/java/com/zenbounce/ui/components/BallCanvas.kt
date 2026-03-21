package com.zenbounce.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import com.zenbounce.game.GameState
import com.zenbounce.theme.AppTheme

/**
 * Full-screen Canvas responsible for rendering the game world.
 *
 * Drawing order (back to front):
 *  1. Gradient background
 *  2. Ambient particles (provided by [AmbientParticleState])
 *  3. Ball trail (last [TRAIL_LENGTH] positions, decreasing opacity)
 *  4. Ball glow (BlurMaskFilter painted in a separate pass)
 *  5. Ball fill (solid circle on top)
 *  6. Wall-hit flash (edge radial gradient on collision)
 *
 * @param gameState      Current simulation state; null = not yet initialised.
 * @param theme          Active visual theme.
 * @param trailPositions Ring buffer of recent ball centres from game loop.
 * @param flashAlpha     0..1 flash intensity from [FlashState]; drives edge glow on collision.
 * @param particles      Ambient background particles.
 * @param onSize         Callback invoked when the canvas is first measured (or resized).
 */
@Composable
fun BallCanvas(
    gameState: GameState?,
    theme: AppTheme,
    trailPositions: List<Offset>,
    flashAlpha: Float,
    particles: List<AmbientParticle>,
    onSize: (width: Float, height: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height
        onSize(w, h)

        // 1 — Background gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(theme.backgroundTop, theme.backgroundBot),
                startY = 0f,
                endY = h
            )
        )

        // 2 — Ambient particles
        particles.forEach { p ->
            drawCircle(
                color = theme.particleColor,
                radius = p.radius,
                center = Offset(p.x, p.y)
            )
        }

        // 3 + 4 + 5 — Balls
        gameState?.balls?.forEach { ball ->

            // 3 — Trail
            if (theme.trailEnabled && trailPositions.isNotEmpty()) {
                trailPositions.forEachIndexed { index, pos ->
                    val alpha = (index.toFloat() / trailPositions.size) * 0.4f
                    drawCircle(
                        color = theme.ballColor.copy(alpha = alpha),
                        radius = ball.radius * (0.5f + 0.5f * (index.toFloat() / trailPositions.size)),
                        center = pos
                    )
                }
            }

            // 4 — Glow (BlurMaskFilter)
            drawGlowingBall(
                center = ball.position,
                radius = ball.radius,
                glowColor = theme.glowColor,
                glowRadius = theme.glowRadius
            )

            // 5 — Ball fill (solid)
            drawCircle(
                color = theme.ballColor,
                radius = ball.radius,
                center = ball.position
            )
        }

        // 6 — Wall-hit edge flash
        if (flashAlpha > 0.01f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        theme.accentColor.copy(alpha = flashAlpha * 0.45f)
                    ),
                    center = Offset(w / 2f, h / 2f),
                    radius = maxOf(w, h) * 0.8f
                )
            )
        }
    }
}

/**
 * Draws a soft glow halo around a ball using Android's [android.graphics.BlurMaskFilter].
 *
 * This is done via [drawIntoCanvas] to access the underlying [android.graphics.Canvas]
 * because BlurMaskFilter is not exposed through Compose's DrawScope API.
 */
private fun DrawScope.drawGlowingBall(
    center: Offset,
    radius: Float,
    glowColor: Color,
    glowRadius: Float
) {
    // BlurMaskFilter produces a real hardware-compatible blur halo.
    // We paint a slightly larger circle in the glow colour using the blur mask,
    // then the solid ball is drawn on top by the caller.
    val glowPaint = Paint().apply {
        asFrameworkPaint().apply {
            isAntiAlias = true
            color = glowColor.copy(alpha = 0.75f).toArgb()
            maskFilter = BlurMaskFilter(
                glowRadius,
                BlurMaskFilter.Blur.NORMAL
            )
        }
    }

    drawIntoCanvas { canvas ->
        canvas.drawCircle(center, radius, glowPaint)
    }
}

// ---- Ambient Particle -------------------------------------------------------

/** Represents a single floating ambient background particle. */
data class AmbientParticle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speedX: Float,
    val speedY: Float
)

/** Holds and updates the list of ambient particles over time. */
class AmbientParticleState(
    private val count: Int = 14
) {
    val particles: MutableList<AmbientParticle> = mutableListOf()

    /** Seed particle positions once canvas dimensions are known. */
    fun initialise(w: Float, h: Float) {
        if (particles.isNotEmpty()) return
        repeat(count) {
            particles.add(randomParticle(w, h))
        }
    }

    /** Advance each particle by [deltaMs] ms, wrapping at canvas edges. */
    fun update(deltaMs: Long, w: Float, h: Float) {
        val dt = deltaMs / 1000f
        for (i in particles.indices) {
            val p = particles[i]
            var nx = p.x + p.speedX * dt
            var ny = p.y + p.speedY * dt
            // Wrap around
            if (nx < -p.radius) nx = w + p.radius
            if (nx > w + p.radius) nx = -p.radius
            if (ny < -p.radius) ny = h + p.radius
            if (ny > h + p.radius) ny = -p.radius
            particles[i] = p.copy(x = nx, y = ny)
        }
    }

    private fun randomParticle(w: Float, h: Float): AmbientParticle {
        val angle = Math.random() * 2 * Math.PI
        val speed = (8 + Math.random() * 18).toFloat()
        return AmbientParticle(
            x = (Math.random() * w).toFloat(),
            y = (Math.random() * h).toFloat(),
            radius = (2f + Math.random() * 4f).toFloat(),
            speedX = (Math.cos(angle) * speed).toFloat(),
            speedY = (Math.sin(angle) * speed).toFloat()
        )
    }
}

// ---- Flash State ------------------------------------------------------------

/**
 * Manages the edge-flash animation triggered on wall collision.
 * Exposes [alpha] as an [Animatable] that decays to 0 over [FLASH_DURATION_MS].
 */
class FlashState {
    val alpha = Animatable(0f)

    suspend fun flash() {
        alpha.snapTo(1f)
        alpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = FLASH_DURATION_MS)
        )
    }

    companion object {
        private const val FLASH_DURATION_MS = 350
    }
}

const val TRAIL_LENGTH = 18
