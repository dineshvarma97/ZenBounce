package com.zenbounce.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Landing screen shown when the app launches.
 * Provides access to Start, Settings, and Exit.
 */
@Composable
fun MainMenuScreen(
    onStart: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E27), Color(0xFF0D1B4B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ZenBounce",
                color = Color(0xFF00EFFF),
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tilt · Bounce · Relax",
                color = Color(0xFF00EFFF).copy(alpha = 0.55f),
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            MenuButton(text = "Start",    onClick = onStart,    primary = true)
            MenuButton(text = "Settings", onClick = onSettings, primary = false)
            MenuButton(text = "Exit",     onClick = onExit,     primary = false)
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit, primary: Boolean) {
    Button(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) Color(0xFF00EFFF) else Color(0xFF1A1F3C),
            contentColor   = if (primary) Color(0xFF0A0E27) else Color(0xFF00EFFF)
        )
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}
