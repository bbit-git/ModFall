package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.music.ModTrackInfo
import com.bigbangit.blockdrop.ui.theme.AppBackgroundCenter
import com.bigbangit.blockdrop.ui.theme.TextWhite

@Composable
fun MusicLibraryScreen(
    isMuted: Boolean,
    musicEnabled: Boolean,
    availableTracks: List<ModTrackInfo>,
    currentTrack: ModTrackInfo?,
    mainTrackPathOrUri: String?,
    isMusicPlaying: Boolean,
    musicFolderUri: String?,
    trackLoadError: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPickMusicFolder: () -> Unit,
    onSelectTrack: (ModTrackInfo) -> Unit,
    onSelectMainTrack: (ModTrackInfo) -> Unit,
    onPauseMusic: () -> Unit,
    onResumeMusic: () -> Unit,
    onStopMusic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackgroundCenter.copy(alpha = 0.98f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.music_library_back_description),
                    tint = TextWhite,
                )
            }
            Text(
                text = stringResource(R.string.music_library_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
            )
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.music_library_refresh),
                    tint = TextWhite,
                )
            }
            IconButton(onClick = onPickMusicFolder) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = stringResource(R.string.music_library_pick_folder),
                    tint = TextWhite,
                )
            }
        }

        CurrentTrackCard(
            isMuted = isMuted,
            musicEnabled = musicEnabled,
            currentTrack = currentTrack,
            isMusicPlaying = isMusicPlaying,
            onPauseMusic = onPauseMusic,
            onResumeMusic = onResumeMusic,
            onStopMusic = onStopMusic,
        )

        if (!trackLoadError.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.music_library_track_load_error, trackLoadError),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFFB4A8),
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (availableTracks.isEmpty()) {
                EmptyMusicLibraryState(
                    hasMusicFolder = !musicFolderUri.isNullOrBlank(),
                    onPickMusicFolder = onPickMusicFolder,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    itemsIndexed(
                        items = availableTracks,
                        key = { _, track -> track.pathOrUri },
                    ) { index, track ->
                        MusicTrackRow(
                            track = track,
                            isCurrentTrack = currentTrack?.pathOrUri == track.pathOrUri,
                            isMainTrack = mainTrackPathOrUri == track.pathOrUri,
                            musicEnabled = musicEnabled && !isMuted,
                            onPlay = { onSelectTrack(track) },
                            onSelectMainTrack = { onSelectMainTrack(track) },
                        )
                        if (index < availableTracks.lastIndex) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.12f),
                                thickness = 1.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentTrackCard(
    isMuted: Boolean,
    musicEnabled: Boolean,
    currentTrack: ModTrackInfo?,
    isMusicPlaying: Boolean,
    onPauseMusic: () -> Unit,
    onResumeMusic: () -> Unit,
    onStopMusic: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
        )

        Text(
            text = currentTrackLabel(
                isMuted = isMuted,
                musicEnabled = musicEnabled,
                currentTrack = currentTrack,
            ),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!isMuted && musicEnabled && currentTrack != null) {
            if (isMusicPlaying) {
                IconButton(onClick = onPauseMusic) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = stringResource(R.string.music_library_pause),
                        tint = TextWhite,
                    )
                }
            }

            IconButton(onClick = onStopMusic) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.music_library_stop),
                    tint = TextWhite,
                )
            }

            if (!isMusicPlaying) {
                IconButton(onClick = onResumeMusic) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.music_library_play),
                        tint = TextWhite,
                    )
                }
            }
        }
    }
}

@Composable
fun MusicTrackRow(
    track: ModTrackInfo,
    isCurrentTrack: Boolean,
    isMainTrack: Boolean,
    musicEnabled: Boolean,
    onPlay: () -> Unit,
    onSelectMainTrack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isCurrentTrack) Color(0xFF97AEFF).copy(alpha = 0.12f) else Color.Transparent,
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.86f),
        )
        Text(
            text = track.displayString(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = TextWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onPlay, enabled = musicEnabled) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.music_library_play_track),
                tint = TextWhite,
            )
        }
        IconButton(onClick = onSelectMainTrack) {
            Icon(
                imageVector = if (isMainTrack) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = stringResource(R.string.music_library_set_main_track),
                tint = TextWhite,
            )
        }
    }
}

@Composable
private fun EmptyMusicLibraryState(
    hasMusicFolder: Boolean,
    onPickMusicFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.music_library_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite,
        )
        Text(
            text = stringResource(R.string.music_library_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite.copy(alpha = 0.78f),
        )
        Text(
            text = if (hasMusicFolder) {
                stringResource(R.string.music_library_empty_selected_folder_hint)
            } else {
                stringResource(R.string.music_library_empty_api33_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextWhite.copy(alpha = 0.66f),
        )
        Button(onClick = onPickMusicFolder) {
            Text(stringResource(R.string.music_library_pick_folder))
        }
    }
}

@Composable
private fun currentTrackLabel(
    isMuted: Boolean,
    musicEnabled: Boolean,
    currentTrack: ModTrackInfo?,
): String {
    return when {
        isMuted -> stringResource(R.string.music_library_muted_placeholder)
        !musicEnabled -> stringResource(R.string.music_library_disabled_placeholder)
        currentTrack == null -> stringResource(R.string.music_library_no_track_placeholder)
        else -> currentTrack.displayString()
    }
}
