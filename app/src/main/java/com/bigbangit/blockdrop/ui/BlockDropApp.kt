package com.bigbangit.blockdrop.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
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
fun BlockDropApp(
    viewModel: GameViewModel,
    splashDurationMs: Long = 2_000L,
) {
    val context = LocalContext.current
    val uiModel by viewModel.uiModel.collectAsState()
    var showSplash by remember(splashDurationMs) { mutableStateOf(splashDurationMs > 0L) }
    var hasRequestedMusicPermission by remember { mutableStateOf(false) }
    val musicPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.refreshMusicLibrary()
        }
    }
    val musicFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            viewModel.setMusicFolderUri(uri.toString())
            viewModel.refreshMusicLibrary()
        }
    }
    val musicFolderInitialUri = remember {
        @Suppress("DEPRECATION")
        Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
    }
    val needsMusicPermission = Build.VERSION.SDK_INT <= 32 &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        ) != PackageManager.PERMISSION_GRANTED

    LaunchedEffect(splashDurationMs) {
        if (splashDurationMs <= 0L) return@LaunchedEffect
        delay(splashDurationMs)
        showSplash = false
    }

    LaunchedEffect(needsMusicPermission) {
        if (!needsMusicPermission) {
            viewModel.refreshMusicLibrary()
        }
    }

    LaunchedEffect(needsMusicPermission, uiModel.isMuted, hasRequestedMusicPermission) {
        if (needsMusicPermission && !uiModel.isMuted && !hasRequestedMusicPermission) {
            hasRequestedMusicPermission = true
            musicPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BlockDropScreen(
            uiModel = uiModel,
            onStartGame = viewModel::startGame,
            onPause = viewModel::pauseGame,
            onResume = viewModel::resumeGame,
            onQuit = viewModel::quitGame,
            onMuteToggle = viewModel::toggleMute,
            onOpenMusicLibrary = viewModel::openMusicLibrary,
            onCloseMusicLibrary = viewModel::closeMusicLibrary,
            onRefreshMusicLibrary = viewModel::refreshMusicLibrary,
            onPickMusicFolder = { musicFolderLauncher.launch(musicFolderInitialUri) },
            onSelectTrack = viewModel::selectTrack,
            onPauseMusic = viewModel::pauseMusic,
            onResumeMusic = viewModel::resumeMusic,
            onStopMusic = viewModel::stopMusic,
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
            onOpenSettings = viewModel::openSettings,
            onCloseSettings = viewModel::closeSettings,
            onToggleButtonsEnabled = viewModel::toggleButtonsEnabled,
            onToggleGesturesEnabled = viewModel::toggleGesturesEnabled,
            onToggleMusicEnabled = viewModel::toggleMusicEnabled,
            onSetMainTrack = viewModel::setMainTrack,
        )

        if (!uiModel.isMuted && uiModel.musicEnabled && !uiModel.showMusicLibrary) {
            TrackInfoOverlay(
                trackDisplay = uiModel.trackDisplay,
                trackDisplayKey = uiModel.trackDisplayKey,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(bottom = 4.dp),
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = EnterTransition.None,
            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
        ) {
            Image(
                painter = painterResource(R.drawable.splashscreen),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun BlockDropScreen(
    uiModel: GameUiModel,
    onStartGame: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onMuteToggle: () -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onCloseMusicLibrary: () -> Unit,
    onRefreshMusicLibrary: () -> Unit,
    onPickMusicFolder: () -> Unit,
    onSelectTrack: (com.bigbangit.blockdrop.music.ModTrackInfo) -> Unit,
    onPauseMusic: () -> Unit,
    onResumeMusic: () -> Unit,
    onStopMusic: () -> Unit,
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
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onToggleButtonsEnabled: () -> Unit,
    onToggleGesturesEnabled: () -> Unit,
    onToggleMusicEnabled: () -> Unit,
    onSetMainTrack: (com.bigbangit.blockdrop.music.ModTrackInfo) -> Unit,
) {
    var showExitConfirm by remember { mutableStateOf(false) }

    if (uiModel.showMusicLibrary) {
        BackHandler { onCloseMusicLibrary() }
    } else if (uiModel.showSettings) {
        BackHandler { onCloseSettings() }
    } else if (uiModel.state == GameState.Running) {
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
        AnimatedContent(
            targetState = when {
                uiModel.showMusicLibrary -> 2
                uiModel.showSettings -> 1
                else -> 0
            },
            transitionSpec = {
                if (targetState > initialState) {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            animationSpec = tween(260),
                            initialOffsetX = { fullWidth -> fullWidth },
                        ) + fadeIn(animationSpec = tween(260)),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { fullWidth -> -fullWidth / 4 },
                        ) + fadeOut(animationSpec = tween(220)),
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = tween(180)),
                        initialContentExit = slideOutHorizontally(
                            animationSpec = tween(260),
                            targetOffsetX = { fullWidth -> fullWidth },
                        ) + fadeOut(animationSpec = tween(220)),
                    )
                }
            },
            label = "panel-screen",
        ) { panelState ->
            when (panelState) {
                2 -> MusicLibraryScreen(
                    isMuted = uiModel.isMuted,
                    musicEnabled = uiModel.musicEnabled,
                    availableTracks = uiModel.availableTracks,
                    currentTrack = uiModel.currentTrack,
                    isMusicPlaying = uiModel.isMusicPlaying,
                    musicFolderUri = uiModel.musicFolderUri,
                    trackLoadError = uiModel.trackLoadError,
                    mainTrackPathOrUri = uiModel.mainTrackPathOrUri,
                    onBack = onCloseMusicLibrary,
                    onRefresh = onRefreshMusicLibrary,
                    onPickMusicFolder = onPickMusicFolder,
                    onSelectTrack = onSelectTrack,
                    onSelectMainTrack = onSetMainTrack,
                    onPauseMusic = onPauseMusic,
                    onResumeMusic = onResumeMusic,
                    onStopMusic = onStopMusic,
                    modifier = Modifier.safeDrawingPadding(),
                )
                1 -> SettingsPanel(
                    uiModel = uiModel,
                    onBack = onCloseSettings,
                    onOpenMusicLibrary = onOpenMusicLibrary,
                    onToggleButtonsEnabled = onToggleButtonsEnabled,
                    onToggleGesturesEnabled = onToggleGesturesEnabled,
                    onToggleMusicEnabled = onToggleMusicEnabled,
                )
                else -> GameScreenContent(
                    uiModel = uiModel,
                    onStartGame = onStartGame,
                    onResume = onResume,
                    onQuit = onQuit,
                    onMuteToggle = onMuteToggle,
                    onOpenMusicLibrary = onOpenMusicLibrary,
                    onOpenSettings = onOpenSettings,
                    onShowTutorial = onShowTutorial,
                    onDismissTutorial = onDismissTutorial,
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
                    onDismissScoreboard = onDismissScoreboard,
                )
            }
        }
    }
}

@Composable
private fun GameScreenContent(
    uiModel: GameUiModel,
    onStartGame: () -> Unit,
    onResume: () -> Unit,
    onQuit: () -> Unit,
    onMuteToggle: () -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopBar(
            score = uiModel.score,
            level = uiModel.level,
            lines = uiModel.lines,
            isMuted = uiModel.isMuted,
            onTutorialToggle = onShowTutorial,
            onMuteToggle = onMuteToggle,
            onOpenMusicLibrary = onOpenMusicLibrary,
            onOpenSettings = onOpenSettings,
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

@Composable
private fun TopBar(
    score: Int,
    level: Int,
    lines: Int,
    isMuted: Boolean,
    onTutorialToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween,
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

            CompactChromeButton(
                onClick = onOpenSettings,
                contentDescription = stringResource(R.string.settings_description),
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_description),
                    tint = TextWhite,
                )
            }

            CompactChromeButton(
                onClick = onMuteToggle,
                onLongClick = onOpenMusicLibrary,
                contentDescription = stringResource(R.string.mute_description),
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.mute_description),
                    tint = TextWhite,
                )
            }
        }

        CompactStatsBar(
            score = score,
            level = level,
            lines = lines,
            modifier = Modifier.fillMaxWidth(),
        )
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
        val layoutDirection = LocalLayoutDirection.current
        val safePadding = WindowInsets.safeDrawing.asPaddingValues()
        val leftUnsafeRail = safePadding.calculateLeftPadding(layoutDirection) + 16.dp
        val rightUnsafeRail = safePadding.calculateRightPadding(layoutDirection) + 16.dp
        val gutterGap = 0.dp
        val reservedControlButtonHeight = 48.dp
        val horizontalGaps = gutterGap * 2
        val verticalGaps = 0.dp
        val boardWidthFromWidth = maxWidth - horizontalGaps - leftUnsafeRail - rightUnsafeRail
        val boardWidthFromHeight = (maxHeight - reservedControlButtonHeight - verticalGaps) / 2f
        val boardWidth = (
            if (boardWidthFromWidth < boardWidthFromHeight) boardWidthFromWidth else boardWidthFromHeight
            ).coerceAtLeast(166.dp)
        val boardHeight = boardWidth * 2f
        val buttonEnabled = uiModel.state == GameState.Running
        val stageHeight = boardHeight + reservedControlButtonHeight + verticalGaps
        val nextPieces = uiModel.nextPieces.take(com.bigbangit.blockdrop.core.GameConstants.getVisibleNextCount(uiModel.level))

        Column(
            modifier = Modifier
                .width((boardWidth + leftUnsafeRail + rightUnsafeRail + horizontalGaps).coerceAtMost(maxWidth))
                .height(stageHeight),
            verticalArrangement = Arrangement.spacedBy(verticalGaps),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (uiModel.buttonsEnabled) {
                    SideRailControlButton(
                        onClick = onMoveLeft,
                        enabled = buttonEnabled,
                        contentDescription = stringResource(R.string.move_left),
                        modifier = Modifier
                            .width(leftUnsafeRail)
                            .fillMaxHeight(),
                        attachedEdge = AttachedEdge.End,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                            )
                        },
                    )
                }

                Spacer(modifier = Modifier.width(if (uiModel.buttonsEnabled) gutterGap else 0.dp))

                Box(
                    modifier = Modifier
                        .width(boardWidth)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    ControlSurface(
                        uiModel = uiModel,
                        enabled = uiModel.gesturesEnabled,
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
                            .padding(start = 3.dp, top = 3.dp)
                            .width(72.dp)
                            .pointerInput(onHold, uiModel.state, uiModel.canHold) {
                                detectTapGestures {
                                    if (uiModel.state == GameState.Running && uiModel.canHold) onHold()
                                }
                            },
                    ) {
                        HoldPreview(type = uiModel.heldPiece, enabled = uiModel.canHold)
                    }

                    BoardSidePanel(
                        label = stringResource(R.string.next_label_caps),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 3.dp, end = 3.dp)
                            .width(78.dp)
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

                Spacer(modifier = Modifier.width(if (uiModel.buttonsEnabled) gutterGap else 0.dp))

                if (uiModel.buttonsEnabled) {
                    SideRailControlButton(
                        onClick = onMoveRight,
                        enabled = buttonEnabled,
                        contentDescription = stringResource(R.string.move_right),
                        modifier = Modifier
                            .width(rightUnsafeRail)
                            .fillMaxHeight(),
                        attachedEdge = AttachedEdge.Start,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(boardWidth + leftUnsafeRail + rightUnsafeRail)
                    .height(reservedControlButtonHeight),
            ) {
                if (uiModel.buttonsEnabled) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameplayControlButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onClick = onRotateClockwise,
                            enabled = buttonEnabled,
                            contentDescription = stringResource(R.string.rotate_cw),
                            attachedEdge = AttachedEdge.Top,
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = null,
                                )
                            },
                        )
                        GameplayControlButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onClick = onHardDrop,
                            enabled = buttonEnabled,
                            contentDescription = stringResource(R.string.hard_drop),
                            emphasize = true,
                            attachedEdge = AttachedEdge.Top,
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardDoubleArrowDown,
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CompactChromeButton(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(36.dp, 30.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.96f else 1f
                scaleY = if (pressed) 0.96f else 1f
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun SideRailControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    attachedEdge: AttachedEdge,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.985f else 1f, label = "side-rail-scale")
    val railColor = Color(0xFF4C6580)
    val iconColor = BoardBackground

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .sideRailSurface(railColor, attachedEdge)
            .combinedClickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides iconColor) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun GameplayControlButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    emphasize: Boolean = false,
    attachedEdge: AttachedEdge = AttachedEdge.None,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (pressed) 0.96f else 1f, label = "control-scale")
    val taggedModifier = if (contentDescription == stringResource(R.string.hard_drop)) {
        modifier.testTag(HardDropButtonTag)
    } else {
        modifier
    }

    val containerColor = if (emphasize) Color(0xFF5B7695) else Color(0xFF4B6583)
    val iconColor = BoardBackground
    val controlPadding = when (attachedEdge) {
        AttachedEdge.Start, AttachedEdge.End -> 0.dp
        else -> 8.dp
    }

    Box(
        modifier = taggedModifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .requiredHeightIn(min = 46.dp)
            .retroGameButtonSurface(containerColor, attachedEdge)
            .combinedClickable(
                onClick = onClick,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            )
            .padding(horizontal = controlPadding, vertical = controlPadding),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides iconColor) {
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
                        color = iconColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private enum class AttachedEdge {
    None,
    Start,
    End,
    Top,
}

private fun Modifier.sideRailSurface(
    containerColor: Color,
    attachedEdge: AttachedEdge,
): Modifier = drawBehind {
    val fillBrush = when (attachedEdge) {
        AttachedEdge.Start -> Brush.horizontalGradient(
            colors = listOf(
                containerColor.copy(alpha = 0.55f),
                containerColor.copy(alpha = 0.9f),
            ),
        )
        AttachedEdge.End -> Brush.horizontalGradient(
            colors = listOf(
                containerColor.copy(alpha = 0.9f),
                containerColor.copy(alpha = 0.55f),
            ),
        )
        else -> Brush.horizontalGradient(
            colors = listOf(containerColor, containerColor),
        )
    }

    drawRect(color = containerColor.copy(alpha = 0.38f))
    drawRect(
        brush = fillBrush,
        topLeft = Offset.Zero,
        size = size,
    )

    val innerEdgeX = if (attachedEdge == AttachedEdge.End) size.width - 1f else 1f
    drawLine(
        color = Color.White.copy(alpha = 0.18f),
        start = Offset(innerEdgeX, 0f),
        end = Offset(innerEdgeX, size.height),
        strokeWidth = 2f,
    )
    val edgeGlowX = if (attachedEdge == AttachedEdge.End) size.width - 3f else 3f
    drawLine(
        color = Color(0xFFB9CBE0).copy(alpha = 0.14f),
        start = Offset(edgeGlowX, 0f),
        end = Offset(edgeGlowX, size.height),
        strokeWidth = 4f,
    )
}

private fun Modifier.retroGameButtonSurface(
    containerColor: Color,
    attachedEdge: AttachedEdge,
): Modifier = drawBehind {
    val outerRadius = CornerRadius(size.minDimension * 0.03f)
    val innerRadius = CornerRadius(size.minDimension * 0.02f)
    drawRoundRect(
        color = containerColor.copy(alpha = 0.42f),
        cornerRadius = outerRadius,
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(containerColor, containerColor.copy(alpha = 0.78f)),
        ),
        topLeft = Offset(2f, 2f),
        size = Size(size.width - 4f, size.height - 4f),
        cornerRadius = innerRadius,
    )

    if (attachedEdge != AttachedEdge.Top) {
        drawLine(
            color = Color.White.copy(alpha = 0.08f),
            start = Offset(3f, 3f),
            end = Offset(size.width - 3f, 3f),
            strokeWidth = 2f,
        )
    }
    if (attachedEdge != AttachedEdge.Start) {
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(3f, 3f),
            end = Offset(3f, size.height - 3f),
            strokeWidth = 2f,
        )
    }
    drawLine(
        color = Color(0xFF171C22),
        start = Offset(3f, size.height - 3f),
        end = Offset(size.width - 3f, size.height - 3f),
        strokeWidth = 2f,
    )
    if (attachedEdge != AttachedEdge.End) {
        drawLine(
            color = Color(0xFF171C22),
            start = Offset(size.width - 3f, 3f),
            end = Offset(size.width - 3f, size.height - 3f),
            strokeWidth = 2f,
        )
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
            .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom))
            .background(AppBackgroundCenter.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
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
            .background(AppBackgroundCenter.copy(alpha = 0.95f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
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

@Composable
private fun SettingsPanel(
    uiModel: GameUiModel,
    onBack: () -> Unit,
    onOpenMusicLibrary: () -> Unit,
    onToggleButtonsEnabled: () -> Unit,
    onToggleGesturesEnabled: () -> Unit,
    onToggleMusicEnabled: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .background(AppBackgroundCenter.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactChromeButton(onClick = onBack, contentDescription = stringResource(R.string.close_button)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = TextWhite)
            }
            Text(
                text = stringResource(R.string.settings_title),
                color = TextWhite,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
        }
        Text(
            text = stringResource(R.string.settings_input_method_rule),
            color = TextWhite.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodySmall,
        )
        RetroSettingRow(
            label = stringResource(R.string.show_buttons_setting),
            checked = uiModel.buttonsEnabled,
            onToggle = onToggleButtonsEnabled,
            enabled = !uiModel.buttonsEnabled || uiModel.gesturesEnabled,
        )
        RetroSettingRow(
            label = stringResource(R.string.enable_gestures_setting),
            checked = uiModel.gesturesEnabled,
            onToggle = onToggleGesturesEnabled,
            enabled = !uiModel.gesturesEnabled || uiModel.buttonsEnabled,
        )
        RetroSettingRow(
            label = stringResource(R.string.enable_music_setting),
            checked = uiModel.musicEnabled,
            onToggle = onToggleMusicEnabled,
            enabled = true,
        )
        RetroActionRow(
            label = stringResource(R.string.main_tune_setting),
            value = uiModel.availableTracks
                .firstOrNull { it.pathOrUri == uiModel.mainTrackPathOrUri }
                ?.displayString()
                ?: uiModel.mainTrackPathOrUri?.substringAfterLast('/')
                ?: stringResource(R.string.none_label),
            onClick = onOpenMusicLibrary,
        )
    }
}

@Composable
private fun RetroSettingRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = TextWhite, fontWeight = FontWeight.SemiBold)
        Checkbox(checked = checked, onCheckedChange = { if (enabled) onToggle() }, enabled = enabled)
    }
}

@Composable
private fun RetroActionRow(
    label: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = TextWhite, fontWeight = FontWeight.SemiBold)
            Text(value, color = TextWhite.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onClick) { Text(stringResource(R.string.music_library_title)) }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "en")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "ar")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "zh-rCN")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "de")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "ru")
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
            onOpenMusicLibrary = {},
            onCloseMusicLibrary = {},
            onRefreshMusicLibrary = {},
            onPickMusicFolder = {},
            onSelectTrack = {},
            onPauseMusic = {},
            onResumeMusic = {},
            onStopMusic = {},
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
            onOpenSettings = {},
            onCloseSettings = {},
            onToggleButtonsEnabled = {},
            onToggleGesturesEnabled = {},
            onToggleMusicEnabled = {},
            onSetMainTrack = {},
        )
    }
}
