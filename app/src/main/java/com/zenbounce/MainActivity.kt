package com.zenbounce

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenbounce.game.GameViewModel
import com.zenbounce.game.GameViewModelFactory
import com.zenbounce.ui.screens.GameScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while app is active — essential for gyroscope play
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Edge-to-edge immersive experience
        enableEdgeToEdge()

        setContent {
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModelFactory(applicationContext)
            )
            val theme by viewModel.currentTheme.collectAsState()

            GameScreen(
                viewModel = viewModel,
                theme = theme
            )
        }
    }
}
