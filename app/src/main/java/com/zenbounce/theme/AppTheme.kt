package com.zenbounce.theme

import androidx.compose.ui.graphics.Color

/**
 * Immutable description of a visual theme.
 *
 * @param id            Stable identifier (used for DataStore persistence).
 * @param name          Human-readable display name shown in the picker.
 * @param backgroundTop Gradient start colour (top of screen).
 * @param backgroundBot Gradient end colour (bottom of screen).
 * @param ballColor     Primary fill colour of the ball.
 * @param glowColor     Colour of the blur-mask glow around the ball.
 * @param glowRadius    Blur radius of the glow in dp (scaled to px at runtime).
 * @param trailEnabled  Whether the ball should render a fading position trail.
 * @param particleColor Colour of ambient background particles.
 * @param accentColor   Highlight used for wall-hit flash and UI accents.
 */
data class AppTheme(
    val id: Int,
    val name: String,
    val backgroundTop: Color,
    val backgroundBot: Color,
    val ballColor: Color,
    val glowColor: Color,
    val glowRadius: Float = 28f,
    val trailEnabled: Boolean = true,
    val particleColor: Color,
    val accentColor: Color
)
