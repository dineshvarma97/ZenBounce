package com.zenbounce.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import com.zenbounce.game.GameState
import com.zenbounce.objects.BounceObject
import com.zenbounce.objects.RenderType
import com.zenbounce.theme.AppTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen Canvas responsible for rendering the game world.
 *
 * Drawing order (back to front):
 *  1. Gradient background
 *  2. Ambient particles (provided by [AmbientParticleState])
 *  3. Ball trail (last [TRAIL_LENGTH] positions, decreasing opacity)
 *  4. Ball glow (BlurMaskFilter painted in a separate pass)
 *  5. Ball fill (dispatched per [RenderType])
 *  6. Wall-hit flash (edge radial gradient on collision)
 *
 * @param gameState      Current simulation state; null = not yet initialised.
 * @param theme          Active visual theme.
 * @param currentObject  The active bounce object (affects drawing style).
 * @param trailPositions Ring buffer of recent ball centres from game loop.
 * @param flashAlpha     0..1 flash intensity from [FlashState]; drives edge glow on collision.
 * @param particles      Ambient background particles.
 * @param onSize         Callback invoked when the canvas is first measured (or resized).
 */
@Composable
fun BallCanvas(
    gameState: GameState?,
    theme: AppTheme,
    currentObject: BounceObject,
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
        val isDefault = currentObject.visuals.renderType == RenderType.DEFAULT
        val trailColor = if (isDefault) theme.ballColor else currentObject.visuals.primaryColor
        val glowColor = if (isDefault) theme.glowColor else currentObject.visuals.glowColor
        val glowRadius = if (isDefault) theme.glowRadius else 28f

        gameState?.balls?.forEach { ball ->

            // 3 — Trail
            if (theme.trailEnabled && trailPositions.isNotEmpty()) {
                trailPositions.forEachIndexed { index, pos ->
                    val alpha = (index.toFloat() / trailPositions.size) * 0.4f
                    drawCircle(
                        color = trailColor.copy(alpha = alpha),
                        radius = ball.radius * (0.5f + 0.5f * (index.toFloat() / trailPositions.size)),
                        center = pos
                    )
                }
            }

            // 4 — Glow (BlurMaskFilter)
            drawGlowingBall(
                center = ball.position,
                radius = ball.radius,
                glowColor = glowColor,
                glowRadius = glowRadius
            )

            // 5 — Ball fill (dispatched by render type)
            when (currentObject.visuals.renderType) {
                RenderType.DEFAULT    -> drawCircle(color = theme.ballColor, radius = ball.radius, center = ball.position)
                RenderType.FOOTBALL   -> drawFootball(ball.position, ball.radius, currentObject)
                RenderType.TENNIS_BALL -> drawTennisBall(ball.position, ball.radius, currentObject)
                RenderType.PING_PONG  -> drawPingPong(ball.position, ball.radius, currentObject)
            }
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

// ---- Glow ---------------------------------------------------------------

/**
 * Draws a soft glow halo around a ball using Android's [BlurMaskFilter].
 * Accessible internally so [BallPickerScreen] can reuse it for previews.
 */
internal fun DrawScope.drawGlowingBall(
    center: Offset,
    radius: Float,
    glowColor: Color,
    glowRadius: Float
) {
    val glowPaint = Paint().apply {
        asFrameworkPaint().apply {
            isAntiAlias = true
            color = glowColor.copy(alpha = 0.75f).toArgb()
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
    }
    drawIntoCanvas { canvas ->
        canvas.drawCircle(center, radius, glowPaint)
    }
}

// ---- Ball-specific draw functions ----------------------------------------

/**
 * Draws a soccer-style football: white base with black pentagon patches.
 * Patches are clipped to the circle boundary via save/restore.
 */
internal fun DrawScope.drawFootball(center: Offset, radius: Float, obj: BounceObject) {
    // White base
    drawCircle(color = obj.visuals.primaryColor, radius = radius, center = center)

    val patchColor = obj.visuals.secondaryColor

    drawIntoCanvas { canvas ->
        canvas.save()

        // Clip to circle so edge patches don't bleed outside
        val clipPath = Path().apply {
            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }
        canvas.clipPath(clipPath)

        val fillPaint = Paint().apply {
            color = patchColor
            style = PaintingStyle.Fill
            asFrameworkPaint().isAntiAlias = true
        }

        // Central pentagon
        canvas.drawPath(pentagonPath(center.x, center.y, radius * 0.28f, -90f), fillPaint)

        // 5 surrounding pentagons at 72° intervals
        for (i in 0 until 5) {
            val angleDeg = (-90f + i * 72f)
            val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
            val px = center.x + radius * 0.64f * cos(angleRad)
            val py = center.y + radius * 0.64f * sin(angleRad)
            canvas.drawPath(pentagonPath(px, py, radius * 0.25f, angleDeg + 18f), fillPaint)
        }

        canvas.restore()
    }
}

/**
 * Draws a tennis ball: yellow-green base with two opposing white seam curves.
 */
internal fun DrawScope.drawTennisBall(center: Offset, radius: Float, obj: BounceObject) {
    // Yellow-green base
    drawCircle(color = obj.visuals.primaryColor, radius = radius, center = center)

    val seamWidth = (radius * 0.1f).coerceAtLeast(3f)

    // Seam 1: S-curve from top to bottom (tilts right)
    val seam1 = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(
            center.x + radius * 0.65f, center.y - radius * 0.35f,
            center.x - radius * 0.65f, center.y + radius * 0.35f,
            center.x, center.y + radius
        )
    }
    // Seam 2: S-curve from top to bottom (tilts left — mirrored)
    val seam2 = Path().apply {
        moveTo(center.x, center.y - radius)
        cubicTo(
            center.x - radius * 0.65f, center.y - radius * 0.35f,
            center.x + radius * 0.65f, center.y + radius * 0.35f,
            center.x, center.y + radius
        )
    }

    // Clip seams to the ball circle
    drawIntoCanvas { canvas ->
        canvas.save()
        val clipPath = Path().apply {
            addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
        }
        canvas.clipPath(clipPath)

        val seamPaint = Paint().apply {
            color = obj.visuals.secondaryColor.copy(alpha = 0.9f)
            style = PaintingStyle.Stroke
            strokeWidth = seamWidth
            asFrameworkPaint().apply {
                isAntiAlias = true
                strokeCap = android.graphics.Paint.Cap.ROUND
            }
        }
        canvas.drawPath(seam1, seamPaint)
        canvas.drawPath(seam2, seamPaint)

        canvas.restore()
    }
}

/**
 * Draws a ping pong ball: smooth white sphere with a specular highlight.
 */
internal fun DrawScope.drawPingPong(center: Offset, radius: Float, obj: BounceObject) {
    // Base fill
    drawCircle(color = obj.visuals.primaryColor, radius = radius, center = center)

    // Subtle warm tint on the lower-right to hint at spherical shading
    drawCircle(
        color = obj.visuals.secondaryColor.copy(alpha = 0.12f),
        radius = radius * 0.90f,
        center = Offset(center.x + radius * 0.07f, center.y + radius * 0.10f)
    )

    // Primary specular highlight (bright, top-left)
    drawOval(
        color = Color.White.copy(alpha = 0.88f),
        topLeft = Offset(center.x - radius * 0.52f, center.y - radius * 0.50f),
        size = Size(radius * 0.42f, radius * 0.30f)
    )

    // Secondary micro-highlight (smaller, sharper)
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(center.x - radius * 0.30f, center.y - radius * 0.28f),
        size = Size(radius * 0.18f, radius * 0.13f)
    )
}

// ---- Path helpers --------------------------------------------------------

/** Build a closed regular 5-gon centred at (cx, cy) with given radius and rotation. */
private fun pentagonPath(cx: Float, cy: Float, r: Float, rotDeg: Float): Path {
    val path = Path()
    for (i in 0 until 5) {
        val angle = Math.toRadians((rotDeg + i * 72.0)).toFloat()
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
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
