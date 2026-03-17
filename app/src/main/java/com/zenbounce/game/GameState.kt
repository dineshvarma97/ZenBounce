package com.zenbounce.game

import androidx.compose.ui.geometry.Offset

/**
 * Represents the complete snapshot of the game world at a point in time.
 * Wrapping balls in a List makes multi-ball expansion a one-liner.
 */
data class GameState(
    val balls: List<Ball>,
    val status: Status = Status.Playing
) {
    enum class Status { Playing, Paused }

    companion object {
        /**
         * Create an initial state with a single ball centred in the given bounds,
         * with a gentle random nudge so it starts moving immediately.
         */
        fun initial(
            canvasWidth: Float, canvasHeight: Float,
            ballRadius: Float = DEFAULT_BALL_RADIUS
        ): GameState {
            val ball = Ball(
                position = Offset(canvasWidth / 2f, canvasHeight / 2f),
                velocity = Offset(80f, -120f),    // gentle kick so it moves from the start
                radius = ballRadius
            )
            return GameState(balls = listOf(ball))
        }

        const val DEFAULT_BALL_RADIUS = 36f
    }
}
