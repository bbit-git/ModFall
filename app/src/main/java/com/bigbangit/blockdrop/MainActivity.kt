package com.bigbangit.blockdrop

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.bigbangit.blockdrop.di.AppContainer
import com.bigbangit.blockdrop.ui.BlockDropApp
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import com.bigbangit.blockdrop.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(applicationContext) }
    private val gameViewModel by viewModels<com.bigbangit.blockdrop.ui.viewmodel.GameViewModel> {
        ViewModelFactory(appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            BlockDropTheme {
                BlockDropApp(gameViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appContainer.soundManager.start()
        appContainer.vibrationManager.start()
        gameViewModel.onForegrounded()
    }

    override fun onStop() {
        gameViewModel.onBackgrounded()
        appContainer.soundManager.stop()
        appContainer.vibrationManager.stop()
        super.onStop()
    }
}
