package com.zenbounce

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zenbounce.game.GameViewModel
import com.zenbounce.game.GameViewModelFactory
import com.zenbounce.ui.screens.BallPickerScreen
import com.zenbounce.ui.screens.GameScreen
import com.zenbounce.ui.screens.MainMenuScreen
import com.zenbounce.ui.screens.SettingsScreen

enum class AppScreen { MainMenu, Game, Settings, BallPicker }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            val viewModel: GameViewModel = viewModel(
                factory = GameViewModelFactory(applicationContext)
            )
            val theme by viewModel.currentTheme.collectAsState()
            val sensitivity by viewModel.sensitivity.collectAsState()
            val currentObject by viewModel.currentObject.collectAsState()

            var currentScreen by remember { mutableStateOf(AppScreen.MainMenu) }

            when (currentScreen) {
                AppScreen.MainMenu -> MainMenuScreen(
                    onStart = {
                        viewModel.startNewGame()
                        currentScreen = AppScreen.Game
                    },
                    onSettings = { currentScreen = AppScreen.Settings },
                    onBalls = { currentScreen = AppScreen.BallPicker },
                    onExit = { finish() }
                )
                AppScreen.Game -> GameScreen(
                    viewModel = viewModel,
                    theme = theme,
                    onMainMenu = { currentScreen = AppScreen.MainMenu }
                )
                AppScreen.Settings -> SettingsScreen(
                    sensitivity = sensitivity,
                    onSensitivityChange = { viewModel.setSensitivity(it) },
                    onBack = { currentScreen = AppScreen.MainMenu }
                )
                AppScreen.BallPicker -> BallPickerScreen(
                    currentObjectId = currentObject.id,
                    onSelectObject = { obj ->
                        viewModel.selectObject(obj)
                        currentScreen = AppScreen.MainMenu
                    },
                    onBack = { currentScreen = AppScreen.MainMenu }
                )
            }
        }
    }
}

