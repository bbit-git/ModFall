package com.bigbangit.blockdrop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bigbangit.blockdrop.di.AppContainer

class ViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameViewModel::class.java)) {
            "Unsupported ViewModel class: ${modelClass.name}"
        }
        return GameViewModel(
            gameLoop = appContainer.gameLoop,
            effectBridge = appContainer.effectBridge,
            settingsRepository = appContainer.settingsRepository,
            scoreboardRepository = appContainer.scoreboardRepository,
        ) as T
    }
}
