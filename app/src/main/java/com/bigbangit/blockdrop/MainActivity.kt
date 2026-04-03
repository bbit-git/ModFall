package com.bigbangit.blockdrop

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.bigbangit.blockdrop.di.AppContainer
import com.bigbangit.blockdrop.ui.BlockDropApp
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import com.bigbangit.blockdrop.ui.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer() }
    private val gameViewModel by viewModels<com.bigbangit.blockdrop.ui.viewmodel.GameViewModel> {
        ViewModelFactory(appContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val launchTime = System.currentTimeMillis()
        installSplashScreen().setKeepOnScreenCondition {
            System.currentTimeMillis() - launchTime < 2_000L
        }
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        setContent {
            BlockDropTheme {
                BlockDropApp(gameViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gameViewModel.onForegrounded()
    }

    override fun onStop() {
        gameViewModel.onBackgrounded()
        super.onStop()
    }
}
