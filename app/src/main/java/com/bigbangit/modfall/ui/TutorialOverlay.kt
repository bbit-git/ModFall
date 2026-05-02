package com.bigbangit.modfall.ui

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bigbangit.modfall.R
import com.bigbangit.modfall.ui.theme.BlockDropTheme

@Composable
fun TutorialScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GameUiTokens.BackgroundCenter.copy(alpha = 0.98f),
                        GameUiTokens.BackgroundDark.copy(alpha = 0.99f),
                    ),
                ),
            )
            .safeDrawingPadding(),
    ) {
        Text(
            text = stringResource(R.string.tutorial_title),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 84.dp, bottom = 110.dp, start = 24.dp, end = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TutorialSection(title = stringResource(R.string.tutorial_section_movement)) {
                TutorialItem(
                    stringResource(R.string.tutorial_movement_grab_label),
                    stringResource(R.string.tutorial_movement_grab_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_movement_swipe_label),
                    stringResource(R.string.tutorial_movement_swipe_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_movement_rotation_label),
                    stringResource(R.string.tutorial_movement_rotation_desc),
                )
            }

            TutorialSection(title = stringResource(R.string.tutorial_section_dropping)) {
                TutorialItem(
                    stringResource(R.string.tutorial_dropping_soft_label),
                    stringResource(R.string.tutorial_dropping_soft_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_dropping_hard_label),
                    stringResource(R.string.tutorial_dropping_hard_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_dropping_delay_label),
                    stringResource(R.string.tutorial_dropping_delay_desc),
                )
            }

            TutorialSection(title = stringResource(R.string.tutorial_section_strategy)) {
                TutorialItem(
                    stringResource(R.string.tutorial_strategy_hold_label),
                    stringResource(R.string.tutorial_strategy_hold_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_strategy_ghost_label),
                    stringResource(R.string.tutorial_strategy_ghost_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_strategy_queue_label),
                    stringResource(R.string.tutorial_strategy_queue_desc),
                )
            }

            TutorialSection(title = stringResource(R.string.tutorial_section_scoring)) {
                TutorialItem(
                    stringResource(R.string.tutorial_scoring_lines_label),
                    stringResource(R.string.tutorial_scoring_lines_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_scoring_tspin_label),
                    stringResource(R.string.tutorial_scoring_tspin_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_scoring_combos_label),
                    stringResource(R.string.tutorial_scoring_combos_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_scoring_allclear_label),
                    stringResource(R.string.tutorial_scoring_allclear_desc),
                )
            }

            TutorialSection(title = stringResource(R.string.tutorial_section_music)) {
                TutorialItem(
                    stringResource(R.string.tutorial_music_set_folder_label),
                    stringResource(R.string.tutorial_music_set_folder_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_music_set_main_label),
                    stringResource(R.string.tutorial_music_set_main_desc),
                )
                TutorialItem(
                    stringResource(R.string.tutorial_music_download_label),
                    stringResource(R.string.tutorial_music_download_desc),
                )
            }
        }

        PanelControlButton(
            text = stringResource(R.string.tutorial_got_it),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun TutorialSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = GameUiTokens.FrameElectricGlow,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun TutorialItem(
    label: String,
    description: String,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
    }
}

@Composable
fun PanelControlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .drawBehind {
                val cr = CornerRadius(size.height * 0.48f)
                drawIntoCanvas { canvas ->
                    val glowPaint = Paint().asFrameworkPaint().apply {
                        color = GameUiTokens.ControlGlow.copy(alpha = 0.16f).toArgb()
                        maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        4f, 4f, size.width - 4f, size.height - 4f,
                        cr.x, cr.y, glowPaint,
                    )
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            GameUiTokens.ControlBg.copy(alpha = 0.16f),
                            GameUiTokens.ControlBg.copy(alpha = 0.08f),
                        ),
                    ),
                    cornerRadius = cr,
                )
                drawRoundRect(
                    color = GameUiTokens.ControlBorder.copy(alpha = 0.28f),
                    cornerRadius = cr,
                    style = Stroke(width = 1.6f),
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.95f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "en")
@Composable
fun TutorialOverlayPreview() {
    BlockDropTheme {
        TutorialScreen(onDismiss = {})
    }
}
