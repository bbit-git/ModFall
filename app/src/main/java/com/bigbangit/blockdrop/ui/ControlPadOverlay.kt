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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = GameUiTokens.ControlHorizontalInset),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietControlButton(
                    icon = Icons.AutoMirrored.Filled.RotateLeft,
                    contentDescription = stringResource(R.string.rotate_ccw),
                    enabled = enabled,
                    onClick = onRotateCounterClockwise,
                    modifier = Modifier
                        .size(GameUiTokens.ControlButtonSize)
                        .offset(x = 8.dp),
                )
                QuietControlButton(
                    icon = Icons.AutoMirrored.Filled.RotateRight,
                    contentDescription = stringResource(R.string.rotate_cw),
                    enabled = enabled,
                    onClick = onRotateClockwise,
                    modifier = Modifier
                        .size(GameUiTokens.ControlButtonSize)
                        .offset(x = (-8).dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietControlButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.move_left),
                    enabled = enabled,
                    onClick = onMoveLeft,
                    modifier = Modifier.size(width = 96.dp, height = GameUiTokens.ControlButtonSize),
                )
                QuietControlButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.soft_drop),
                    enabled = enabled,
                    onClick = onSoftDrop,
                    onLongClick = onHardDrop,
                    modifier = Modifier.size(GameUiTokens.ControlButtonSize),
                )
                QuietControlButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.move_right),
                    enabled = enabled,
                    onClick = onMoveRight,
                    modifier = Modifier.size(width = 96.dp, height = GameUiTokens.ControlButtonSize),
                )
            }
        }
    }
}

/**
 * Wide rounded-rect pill button matching the concept image:
 * dark fill, subtle blue border, slight glow, full-width layout.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun QuietControlButton(
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
    val borderAlpha = if (pressed) GameUiTokens.ControlPressedBorderAlpha else GameUiTokens.ControlBorderAlpha
    val iconAlpha = if (pressed) 0.96f else GameUiTokens.ControlIconAlpha

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
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val cr = CornerRadius(size.height * 0.48f)

                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = GameUiTokens.ControlGlow.copy(
                            alpha = if (pressed) 0.18f else GameUiTokens.ControlGlowAlpha,
                        ).toArgb()
                        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        4f, 4f, size.width - 4f, size.height - 4f,
                        cr.x, cr.y, paint,
                    )
                }

                drawRoundRect(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            GameUiTokens.ControlBg.copy(
                                alpha = if (pressed) GameUiTokens.ControlPressedBgAlpha else GameUiTokens.ControlBgAlpha,
                            ),
                            Color.Transparent,
                        ),
                    ),
                    cornerRadius = cr,
                )

                drawRoundRect(
                    color = GameUiTokens.ControlBorder.copy(alpha = borderAlpha),
                    cornerRadius = cr,
                    style = Stroke(width = 1.2f),
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
