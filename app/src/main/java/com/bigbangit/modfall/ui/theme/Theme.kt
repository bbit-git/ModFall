package com.bigbangit.modfall.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BlockDropColorScheme = darkColorScheme(
    primary = AccentBlue,
    background = BoardBackground,
    surface = BoardBackground,
)

@Composable
fun BlockDropTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BlockDropColorScheme,
        content = content,
    )
}
