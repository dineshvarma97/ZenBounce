package com.zenbounce.theme

import androidx.compose.ui.graphics.Color

/**
 * The six built-in theme presets.
 * To add a new theme: append an [AppTheme] to this list — no other changes needed.
 */
object ThemePresets {

    val all: List<AppTheme> = listOf(

        // 0 — Dark Neon -------------------------------------------------------
        AppTheme(
            id = 0,
            name = "Dark Neon",
            backgroundTop = Color(0xFF0A0E27),
            backgroundBot = Color(0xFF0D1B4B),
            ballColor = Color(0xFF00EFFF),
            glowColor = Color(0xFF00EFFF),
            glowRadius = 32f,
            trailEnabled = true,
            particleColor = Color(0x3300EFFF),
            accentColor = Color(0xFF00EFFF)
        ),

        // 1 — Amoled Night ----------------------------------------------------
        AppTheme(
            id = 1,
            name = "Amoled Night",
            backgroundTop = Color(0xFF000000),
            backgroundBot = Color(0xFF0D0D0D),
            ballColor = Color(0xFFFF80AB),
            glowColor = Color(0xFFFF80AB),
            glowRadius = 26f,
            trailEnabled = true,
            particleColor = Color(0x22FF80AB),
            accentColor = Color(0xFFFF80AB)
        ),

        // 2 — Ocean Deep ------------------------------------------------------
        AppTheme(
            id = 2,
            name = "Ocean Deep",
            backgroundTop = Color(0xFF0C2340),
            backgroundBot = Color(0xFF0A4A6E),
            ballColor = Color(0xFF00FFC8),
            glowColor = Color(0xFF00FFC8),
            glowRadius = 30f,
            trailEnabled = true,
            particleColor = Color(0x2200FFC8),
            accentColor = Color(0xFF00FFC8)
        ),

        // 3 — Aurora ----------------------------------------------------------
        AppTheme(
            id = 3,
            name = "Aurora",
            backgroundTop = Color(0xFF0B0022),
            backgroundBot = Color(0xFF0E2E1A),
            ballColor = Color(0xFFB47FFF),
            glowColor = Color(0xFF4DFFB4),
            glowRadius = 34f,
            trailEnabled = true,
            particleColor = Color(0x33B47FFF),
            accentColor = Color(0xFF4DFFB4)
        ),

        // 4 — Sunset ----------------------------------------------------------
        AppTheme(
            id = 4,
            name = "Sunset",
            backgroundTop = Color(0xFF1A0A00),
            backgroundBot = Color(0xFF3D1234),
            ballColor = Color(0xFFFFAB40),
            glowColor = Color(0xFFFF6D00),
            glowRadius = 28f,
            trailEnabled = true,
            particleColor = Color(0x33FFAB40),
            accentColor = Color(0xFFFFAB40)
        ),

        // 5 — Zen White -------------------------------------------------------
        AppTheme(
            id = 5,
            name = "Zen White",
            backgroundTop = Color(0xFFF0F4F8),
            backgroundBot = Color(0xFFDCE7F0),
            ballColor = Color(0xFF7C9CBF),
            glowColor = Color(0xFFA8C5E0),
            glowRadius = 20f,
            trailEnabled = false,
            particleColor = Color(0x227C9CBF),
            accentColor = Color(0xFF7C9CBF)
        )
    )

    val default: AppTheme get() = all[0]

    fun byId(id: Int): AppTheme = all.firstOrNull { it.id == id } ?: default
}
