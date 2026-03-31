package com.zenbounce.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.zenbounce.objects.BounceObject
import com.zenbounce.objects.BounceObjectCatalog
import com.zenbounce.objects.RenderType
import com.zenbounce.ui.components.drawFootball
import com.zenbounce.ui.components.drawGlowingBall
import com.zenbounce.ui.components.drawPingPong
import com.zenbounce.ui.components.drawTennisBall

/**
 * Full-screen ball picker reached from the Main Menu.
 *
 * Shows all objects from [BounceObjectCatalog.ALL] in a 2-column grid.
 * Each card displays a canvas preview, display name, and a mass tier pill.
 * Tapping a card selects the object and returns to the menu.
 */
@Composable
fun BallPickerScreen(
    currentObjectId: Int,
    onSelectObject: (BounceObject) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A0E27), Color(0xFF0D1B4B))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ---- Top bar ------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF00EFFF)
                    )
                }
                Text(
                    text = "Choose Your Ball",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // ---- Grid ---------------------------------------------------
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(BounceObjectCatalog.ALL) { obj ->
                    BallCard(
                        obj = obj,
                        isSelected = obj.id == currentObjectId,
                        onClick = { onSelectObject(obj) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BallCard(
    obj: BounceObject,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF00EFFF) else Color.Transparent,
        animationSpec = tween(250),
        label = "border_${obj.id}"
    )
    val massLabel = when {
        obj.physics.mass < 0.5f -> "Light"
        obj.physics.mass < 2.5f -> "Medium"
        else                    -> "Heavy"
    }
    val massTagColor = when (massLabel) {
        "Light"  -> Color(0xFF00EFFF).copy(alpha = 0.25f)
        "Medium" -> Color(0xFFFFAB40).copy(alpha = 0.25f)
        else     -> Color(0xFFFF5252).copy(alpha = 0.25f)
    }
    val massTextColor = when (massLabel) {
        "Light"  -> Color(0xFF00EFFF)
        "Medium" -> Color(0xFFFFAB40)
        else     -> Color(0xFFFF5252)
    }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, borderColor, RoundedCornerShape(20.dp))
            .background(Color(0xFF12131A))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Ball preview canvas
        Canvas(modifier = Modifier.size(100.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val previewRadius = size.width * 0.38f

            when (obj.visuals.renderType) {
                RenderType.DEFAULT -> {
                    // Use neon cyan matching the default theme
                    val previewColor = Color(0xFF00EFFF)
                    drawGlowingBall(center, previewRadius, previewColor, 22f)
                    drawCircle(color = previewColor, radius = previewRadius, center = center)
                }
                RenderType.FOOTBALL   -> {
                    drawGlowingBall(center, previewRadius, obj.visuals.glowColor, 20f)
                    drawFootball(center, previewRadius, obj)
                }
                RenderType.TENNIS_BALL -> {
                    drawGlowingBall(center, previewRadius, obj.visuals.glowColor, 20f)
                    drawTennisBall(center, previewRadius, obj)
                }
                RenderType.PING_PONG  -> {
                    drawGlowingBall(center, previewRadius, obj.visuals.glowColor, 18f)
                    drawPingPong(center, previewRadius, obj)
                }
            }
        }

        // Display name
        Text(
            text = obj.displayName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        // Mass tier pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(massTagColor)
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                text = massLabel,
                color = massTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Selection indicator dot
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF00EFFF), shape = RoundedCornerShape(50))
            )
        }
    }
}
