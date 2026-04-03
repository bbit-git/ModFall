package com.bigbangit.blockdrop.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.core.GameState
import com.bigbangit.blockdrop.ui.model.GameUiModel
import com.bigbangit.blockdrop.ui.theme.AppBackgroundCenter
import com.bigbangit.blockdrop.ui.theme.AppBackgroundEdge
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme
import com.bigbangit.blockdrop.ui.theme.BoardBackground
import com.bigbangit.blockdrop.ui.theme.TextWhite
import com.bigbangit.blockdrop.ui.viewmodel.GameViewModel

private const val HardDropButtonTag = "hard-drop-button"

@Composable
fun BlockDropApp(viewModel: GameViewModel) {
    val uiModel by viewModel.uiModel.collectAsState()

    BlockDropScreen(
        uiModel = uiModel,
        onStartGame = viewModel::startGame,
        onPause = viewModel::pauseGame,
        onResume = viewModel::resumeGame,
        onQuit = viewModel::quitGame,
        onMuteToggle = viewModel::toggleMute,
        onShowTutorial = viewModel::showTutorial,
        onDismissTutorial = viewModel::dismissTutorial,
        onMoveLeft = viewModel::moveLeft,
        onMoveRight = viewModel::moveRight,
        onRotateClockwise = viewModel::rotateClockwise,
        onRotateCounterClockwise = viewModel::rotateCounterClockwise,
        onSoftDrop = viewModel::softDrop,
        onHardDrop = viewModel::hardDrop,
        onHold = viewModel::hold,
        onDropDelay = viewModel::activateDropDelay,
        onNicknameChanged = viewModel::updateNickname,
        onSubmitScore = viewModel::submitScore,
        onShowScoreboard = viewModel::showScoreboard,
        onDismissScoreboard = viewModel::dismissScoreboard,
    )
}

@Composable
fun BlockDropScreen(
    uiModel: GameUiModel,
    onStartGame: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onMuteToggle: () -> Unit,
    onShowTutorial: () -> Unit,
    onDismissTutorial: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onDropDelay: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSubmitScore: () -> Unit,
    onShowScoreboard: () -> Unit,
    onDismissScoreboard: () -> Unit,
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
                TextButton(
                    onClick = {
                        showExitConfirm = false
                        onQuit()
                    },
                ) {
                    Text(stringResource(R.string.quit_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(AppBackgroundCenter, AppBackgroundEdge),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TopBar(
                score = uiModel.score,
                level = uiModel.level,
                lines = uiModel.lines,
                isMuted = uiModel.isMuted,
                onTutorialToggle = onShowTutorial,
                onMuteToggle = onMuteToggle,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                GameBoardStage(
                    uiModel = uiModel,
                    onStartGame = onStartGame,
                    onResume = onResume,
                    onQuit = onQuit,
                    onMoveLeft = onMoveLeft,
                    onMoveRight = onMoveRight,
                    onRotateClockwise = onRotateClockwise,
                    onRotateCounterClockwise = onRotateCounterClockwise,
                    onSoftDrop = onSoftDrop,
                    onHardDrop = onHardDrop,
                    onHold = onHold,
                    onDropDelay = onDropDelay,
                    onNicknameChanged = onNicknameChanged,
                    onSubmitScore = onSubmitScore,
                    onShowScoreboard = onShowScoreboard,
                )
            }

            BottomControlsRow(
                enabled = uiModel.state == GameState.Running,
                onMoveLeft = onMoveLeft,
                onMoveRight = onMoveRight,
                onRotateClockwise = onRotateClockwise,
                onSoftDrop = onSoftDrop,
                onHardDrop = onHardDrop,
            )
        }

        if (uiModel.showTutorial) {
            TutorialOverlay(onDismiss = onDismissTutorial)
        }

        if (uiModel.isScoreboardVisible) {
            ScoreboardOverlay(
                entries = uiModel.scoreboardEntries,
                onDismiss = onDismissScoreboard,
            )
        }
    }
}

@Composable
private fun TopBar(
    score: Int,
    level: Int,
    lines: Int,
    isMuted: Boolean,
    onTutorialToggle: () -> Unit,
    onMuteToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactChromeButton(
            onClick = onTutorialToggle,
            contentDescription = stringResource(R.string.help_description),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = stringResource(R.string.help_description),
                tint = TextWhite,
            )
        }

        CompactStatsBar(
            score = score,
            level = level,
            lines = lines,
            modifier = Modifier.weight(1f),
        )

        CompactChromeButton(
            onClick = onMuteToggle,
            contentDescription = stringResource(R.string.mute_description),
        ) {
            Icon(
                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = stringResource(R.string.mute_description),
                tint = TextWhite,
            )
        }
    }
}

@Composable
private fun GameBoardStage(
    uiModel: GameUiModel,
    onStartGame: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
    onHold: () -> Unit,
    onDropDelay: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSubmitScore: () -> Unit,
    onShowScoreboard: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val boardWidth = if (maxWidth - 24.dp < maxHeight / 2) maxWidth - 24.dp else maxHeight / 2
        val boardHeight = (boardWidth.value * 2f).dp
        val nextPieces = uiModel.nextPieces.take(com.bigbangit.blockdrop.core.GameConstants.getVisibleNextCount(uiModel.level))

        Box(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight),
            contentAlignment = Alignment.Center,
        ) {
            ControlSurface(
                uiModel = uiModel,
                onMoveLeft = onMoveLeft,
                onMoveRight = onMoveRight,
                onRotateClockwise = onRotateClockwise,
                onRotateCounterClockwise = onRotateCounterClockwise,
                onSoftDrop = onSoftDrop,
                onHardDrop = onHardDrop,
                onDropDelay = onDropDelay,
                modifier = Modifier.fillMaxSize(),
            )

            BoardSidePanel(
                label = stringResource(R.string.hold_label),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 6.dp, top = 6.dp)
                    .pointerInput(onHold, uiModel.state, uiModel.canHold) {
                        detectTapGestures {
                            if (uiModel.state == GameState.Running && uiModel.canHold) {
                                onHold()
                            }
                        }
                    },
            ) {
                HoldPreview(
                    type = uiModel.heldPiece,
                    enabled = uiModel.canHold,
                )
            }

            BoardSidePanel(
                label = stringResource(R.string.next_label_caps),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    // Consume pointer events to prevent them leaking to ControlSurface (rotation) below
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown().consume()
                        }
                    },
            ) {
                NextPreviewColumn(pieces = nextPieces)
            }

            when (uiModel.state) {
                GameState.Idle -> CompactOverlayCard(
                    title = stringResource(R.string.block_drop_title),
                    subtitle = null,
                    width = (boardWidth.value * 0.78f).dp,
                    primaryLabel = stringResource(R.string.start_game),
                    onPrimaryClick = onStartGame,
                )

                GameState.GameOver -> GameOverOverlayCard(
                    uiModel = uiModel,
                    width = (boardWidth.value * 0.84f).dp,
                    onPrimaryClick = onStartGame,
                    onSecondaryClick = onQuit,
                    onNicknameChanged = onNicknameChanged,
                    onSubmitScore = onSubmitScore,
                    onShowScoreboard = onShowScoreboard,
                )

                GameState.Paused -> CompactOverlayCard(
                    title = stringResource(R.string.paused_title),
                    subtitle = stringResource(R.string.pause_menu_subtitle),
                    width = (boardWidth.value * 0.8f).dp,
                    primaryLabel = stringResource(R.string.resume_button),
                    onPrimaryClick = onResume,
                    secondaryLabel = stringResource(R.string.restart_button),
                    onSecondaryClick = onStartGame,
                    tertiaryLabel = stringResource(R.string.menu_button),
                    onTertiaryClick = onQuit,
                )

                else -> Unit
            }
        }
    }
}

@Composable
private fun BottomControlsRow(
    enabled: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameplayControlButton(
            onClick = onMoveLeft,
            enabled = enabled,
            contentDescription = stringResource(R.string.move_left),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.move_left),
            )
        }

        GameplayControlButton(
            onClick = onRotateClockwise,
            enabled = enabled,
            contentDescription = stringResource(R.string.rotate_cw),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = stringResource(R.string.rotate_cw),
            )
        }

        GameplayControlButton(
            onClick = onMoveRight,
            enabled = enabled,
            contentDescription = stringResource(R.string.move_right),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.move_right),
            )
        }

        GameplayControlButton(
            onClick = onSoftDrop,
            enabled = enabled,
            contentDescription = stringResource(R.string.soft_drop),
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = stringResource(R.string.soft_drop),
            )
        }

        GameplayControlButton(
            onClick = onHardDrop,
            enabled = enabled,
            contentDescription = stringResource(R.string.hard_drop),
            modifier = Modifier
                .weight(1.45f)
                .testTag(HardDropButtonTag),
            label = stringResource(R.string.hard_drop_short),
            emphasize = true,
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardDoubleArrowDown,
                contentDescription = stringResource(R.string.hard_drop),
            )
        }
    }
}

@Composable
private fun CompactChromeButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                content()
            }
        }
    }
}

@Composable
private fun GameplayControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    emphasize: Boolean = false,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "control-scale")
    val containerColor = if (emphasize) {
        if (pressed) Color(0xFFBFCBFF) else Color(0xFFE6ECFF)
    } else {
        if (pressed) Color.White.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.14f)
    }
    val borderColor = if (emphasize) Color(0xFF93A7FF) else Color.White.copy(alpha = 0.18f)
    val contentColor = if (emphasize) BoardBackground else TextWhite

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .requiredHeightIn(min = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOverOverlayCard(
    uiModel: GameUiModel,
    width: Dp,
    onPrimaryClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    onNicknameChanged: (String) -> Unit,
    onSubmitScore: () -> Unit,
    onShowScoreboard: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(width)
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.game_over_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = TextWhite,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.score_display, uiModel.score),
            style = MaterialTheme.typography.titleMedium,
            color = TextWhite.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
        )

        if (!uiModel.hasSubmittedScore) {
            OutlinedTextField(
                value = uiModel.pendingNickname,
                onValueChange = onNicknameChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.nickname_label)) },
                placeholder = { Text(stringResource(R.string.nickname_placeholder)) },
                isError = uiModel.nicknameError != null,
                supportingText = {
                    if (uiModel.nicknameError != null) {
                        Text(stringResource(R.string.nickname_required))
                    }
                },
            )

            Button(
                onClick = onSubmitScore,
                enabled = !uiModel.isSubmittingScore,
            ) {
                Text(stringResource(R.string.save_score_button))
            }
        } else {
            val submissionText = when {
                uiModel.didQualifyForScoreboard == true && uiModel.qualifiedRank != null ->
                    stringResource(R.string.score_saved_ranked, uiModel.qualifiedRank)
                uiModel.didQualifyForScoreboard == true ->
                    stringResource(R.string.score_saved)
                else ->
                    stringResource(R.string.not_in_top_ten)
            }
            Text(
                text = submissionText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
            FilledTonalButton(onClick = onShowScoreboard) {
                Text(stringResource(R.string.scoreboard_button))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onPrimaryClick) {
                Text(stringResource(R.string.restart_button))
            }
            FilledTonalButton(onClick = onSecondaryClick) {
                Text(stringResource(R.string.menu_button))
            }
        }
    }
}

@Composable
private fun CompactOverlayCard(
    title: String,
    subtitle: String?,
    width: Dp,
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiaryClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .width(width)
            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = TextWhite,
            textAlign = TextAlign.Center,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onPrimaryClick) {
                Text(primaryLabel)
            }
            if (secondaryLabel != null && onSecondaryClick != null) {
                FilledTonalButton(onClick = onSecondaryClick) {
                    Text(secondaryLabel)
                }
            }
            if (tertiaryLabel != null && onTertiaryClick != null) {
                TextButton(onClick = onTertiaryClick) {
                    Text(tertiaryLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun BlockDropScreenPreview() {
    BlockDropTheme {
        BlockDropScreen(
            uiModel = GameUiModel(state = GameState.Running),
            onStartGame = {},
            onPause = {},
            onResume = {},
            onQuit = {},
            onMuteToggle = {},
            onShowTutorial = {},
            onDismissTutorial = {},
            onMoveLeft = {},
            onMoveRight = {},
            onRotateClockwise = {},
            onRotateCounterClockwise = {},
            onSoftDrop = {},
            onHardDrop = {},
            onHold = {},
            onDropDelay = {},
            onNicknameChanged = {},
            onSubmitScore = {},
            onShowScoreboard = {},
            onDismissScoreboard = {},
        )
    }
}
