package com.bigbangit.blockdrop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.ui.theme.TextWhite
import kotlinx.coroutines.delay

/**
 * Transient overlay that displays the current track name.
 *
 * Appears on [trackDisplayKey] change, stays for ~4 seconds, then fades out.
 * Hidden when [trackDisplay] is null (e.g. muted / no tracks).
 */
@Composable
fun TrackInfoOverlay(
    trackDisplay: String?,
    trackDisplayKey: Int,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(trackDisplayKey) {
        if (trackDisplay != null) {
            visible = true
            delay(4_000L)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible && trackDisplay != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(600)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = trackDisplay.orEmpty(),
                style = MaterialTheme.typography.bodySmall.copy(
                    shadow = Shadow(
                        color = androidx.compose.ui.graphics.Color.Black,
                        offset = Offset(1f, 1f),
                        blurRadius = 4f,
                    ),
                ),
                color = TextWhite.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
    }
}
