package com.zenbounce.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
 * Settings screen exposing the ball sensitivity slider.
 *
 * @param sensitivity     Current value 0–100.
 * @param onSensitivityChange  Called on every slider move (debounced in ViewModel before persisting).
 * @param onBack          Navigate back to the main menu.
 */
@Composable
fun SettingsScreen(
    sensitivity: Int,
    onSensitivityChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E27), Color(0xFF0D1B4B))
                )
            )
            .padding(horizontal = 24.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF00EFFF)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(48.dp))

        // ── Sensitivity control ────────────────────────────────────────
        Text(
            text = "Ball Sensitivity",
            color = Color(0xFF00EFFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Controls how strongly the ball responds to tilting",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "0",   color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
            Slider(
                value = sensitivity.toFloat(),
                onValueChange = { onSensitivityChange(it.toInt()) },
                valueRange = 0f..100f,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(
                    thumbColor         = Color(0xFF00EFFF),
                    activeTrackColor   = Color(0xFF00EFFF),
                    inactiveTrackColor = Color(0xFF1A1F3C)
                )
            )
            Text(text = "100", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        }

        Text(
            text = "$sensitivity",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
