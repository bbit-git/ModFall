package com.bigbangit.blockdrop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.theme.AppBackgroundCenter
import com.bigbangit.blockdrop.ui.theme.AppBackgroundEdge
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import com.bigbangit.blockdrop.ui.viewmodel.GameViewModel

@Composable
fun BlockDropApp(viewModel: GameViewModel) {
    val uiModel by viewModel.uiModel.collectAsState()
    
    // TODO: Connect isMuted to DataStore in M4
    var isMuted by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    BlockDropScreen(
        uiModel = uiModel,
        isMuted = isMuted,
        showTutorial = showTutorial,
        onStartGame = viewModel::startGame,
        onPause = viewModel::pauseGame,
        onResume = viewModel::resumeGame,
        onQuit = viewModel::quitGame,
        onMuteToggle = { isMuted = !isMuted },
        onTutorialToggle = { showTutorial = !showTutorial },
        onMoveLeft = viewModel::moveLeft,
        onMoveRight = viewModel::moveRight,
        onRotateClockwise = viewModel::rotateClockwise,
        onRotateCounterClockwise = viewModel::rotateCounterClockwise,
        onSoftDrop = viewModel::softDrop,
        onHardDrop = viewModel::hardDrop,
        onHold = viewModel::hold,
        onDropDelay = viewModel::activateDropDelay,
    )
}

@Composable
fun BlockDropScreen(
    uiModel: GameUiModel,
    isMuted: Boolean,
    showTutorial: Boolean,
    onStartGame: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onMuteToggle: () -> Unit,
    onTutorialToggle: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onDropDelay: () -> Unit,
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    if (uiModel.state == GameState.Running) {
        BackHandler { onPause() }
    } else if (uiModel.state == GameState.Paused) {
        BackHandler { showExitConfirm = true }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.exit_confirm_title)) },
            text = { Text(stringResource(R.string.exit_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onQuit()
                }) {
                    Text(stringResource(R.string.quit_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AppBackgroundCenter, AppBackgroundEdge)
                )
            )
    ) {
        // Main Game Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            // Left margin or future items
            Spacer(modifier = Modifier.width(16.dp))

            // Board (Center)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                ControlSurface(
                    uiModel = uiModel,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onRotateClockwise = onRotateClockwise,
                    onRotateCounterClockwise = onRotateCounterClockwise,
                    onSoftDrop = onSoftDrop,
                    onHardDrop = onHardDrop,
                    onHold = onHold,
                    onDropDelay = onDropDelay,
                )

                // Overlays
                if (uiModel.state == GameState.Idle || uiModel.state == GameState.GameOver) {
                    Column(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                if (uiModel.state == GameState.Idle) R.string.block_drop_title else R.string.game_over_title
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        if (uiModel.state == GameState.GameOver) {
                            Text(
                                text = stringResource(R.string.score_display, uiModel.score),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onStartGame) {
                            Text(text = stringResource(R.string.start_game))
                        }
                    }
                } else if (uiModel.state == GameState.Paused) {
                    Column(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.7f), MaterialTheme.shapes.medium)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.paused_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onResume) {
                            Text(text = stringResource(R.string.resume_button))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onStartGame) {
                            Text(text = stringResource(R.string.restart_button))
                        }
                    }
                }
            }

            // HUD (Right)
            ScorePanel(uiModel = uiModel)
        }

        // Top Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onTutorialToggle) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = stringResource(R.string.help_description),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }

            IconButton(onClick = onMuteToggle) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = stringResource(R.string.mute_description),
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        if (showTutorial) {
            TutorialOverlay(onDismiss = onTutorialToggle)
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun BlockDropScreenPreview() {
    BlockDropTheme {
        BlockDropScreen(
            uiModel = GameUiModel(state = GameState.Running),
            isMuted = false,
            showTutorial = false,
            onStartGame = {},
            onPause = {},
            onResume = {},
            onQuit = {},
            onMuteToggle = {},
            onTutorialToggle = {},
            onMoveLeft = {},
            onMoveRight = {},
            onRotateClockwise = {},
            onRotateCounterClockwise = {},
            onSoftDrop = {},
            onHardDrop = {},
            onHold = {},
            onDropDelay = {},
        )
    }
}
