package com.bigbangit.blockdrop.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R

@Composable
fun ControlPadOverlay(
    enabled: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onRotateClockwise: () -> Unit,
    onRotateCounterClockwise: () -> Unit,
    onSoftDrop: () -> Unit,
    onHardDrop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Top row: rotate-left / rotate-right (narrower, spread apart)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WideControlButton(
                icon = Icons.AutoMirrored.Filled.RotateLeft,
                contentDescription = stringResource(R.string.rotate_ccw),
                enabled = enabled,
                onClick = onRotateCounterClockwise,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(64.dp))
            WideControlButton(
                icon = Icons.AutoMirrored.Filled.RotateRight,
                contentDescription = stringResource(R.string.rotate_cw),
                enabled = enabled,
                onClick = onRotateClockwise,
                modifier = Modifier.weight(1f),
            )
        }

        // Bottom row: left / down / right (full-width spread)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WideControlButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.move_left),
                enabled = enabled,
                onClick = onMoveLeft,
                modifier = Modifier.weight(1f),
            )
            WideControlButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.soft_drop),
                enabled = enabled,
                onClick = onSoftDrop,
                onLongClick = onHardDrop,
                modifier = Modifier.weight(1f),
            )
            WideControlButton(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.move_right),
                enabled = enabled,
                onClick = onMoveRight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Wide rounded-rect pill button matching the concept image:
 * dark fill, subtle blue border, slight glow, full-width layout.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun WideControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) GameUiTokens.ControlPressedScale else 1f,
        label = "ctrl-scale",
    )

    val bgAlpha = if (pressed) 0.18f else 0.10f
    val borderAlpha = if (pressed) 0.50f else 0.30f
    val iconAlpha = if (pressed) 0.95f else 0.78f

    val clickModifier = if (onLongClick != null) {
        Modifier.pointerInput(enabled, onClick, onLongClick) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                val up = withTimeoutOrNull(200L) {
                    waitForUpOrCancellation()
                }
                if (up != null) {
                    // Released before 200ms — short tap
                    up.consume()
                    interactionSource.tryEmit(PressInteraction.Release(press))
                    onClick()
                } else {
                    // Held 200ms+ — hard drop
                    onLongClick()
                    // Wait for actual release to clean up interaction
                    val finalUp = waitForUpOrCancellation()
                    finalUp?.consume()
                    interactionSource.tryEmit(PressInteraction.Release(press))
                }
            }
        }
    } else {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick,
        )
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val cr = CornerRadius(size.height * 0.32f)

                // Subtle outer glow
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = GameUiTokens.ControlGlow.copy(alpha = 0.10f).toArgb()
                        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        cr.x, cr.y, paint,
                    )
                }

                // Dark fill
                drawRoundRect(
                    color = GameUiTokens.BackgroundCenter.copy(alpha = bgAlpha),
                    cornerRadius = cr,
                )

                // Border
                drawRoundRect(
                    color = GameUiTokens.ControlBorder.copy(alpha = borderAlpha),
                    cornerRadius = cr,
                    style = Stroke(width = 1.5f),
                )
            }
            .then(clickModifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = GameUiTokens.ControlIcon.copy(alpha = iconAlpha),
            modifier = Modifier.size(26.dp),
        )
    }
}
