package com.zenbounce.game

import androidx.compose.ui.geometry.Offset

/**
 * Immutable value object representing a single ball in the simulation.
 *
 * position — centre of the ball in pixels, in Compose canvas coordinates
 * velocity — pixels per second, x/y components
 * radius   — rendering and collision radius in pixels
 */
data class Ball(
    val position: Offset,
    val velocity: Offset,
    val radius: Float
)
