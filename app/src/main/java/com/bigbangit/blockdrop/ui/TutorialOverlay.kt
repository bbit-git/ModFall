package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.ui.theme.BlockDropTheme

@Composable
fun TutorialOverlay(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f)),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tutorial_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(24.dp))

                TutorialSection(title = stringResource(R.string.tutorial_section_movement)) {
                    TutorialItem(
                        stringResource(R.string.tutorial_movement_grab_label),
                        stringResource(R.string.tutorial_movement_grab_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_movement_swipe_label),
                        stringResource(R.string.tutorial_movement_swipe_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_movement_rotation_label),
                        stringResource(R.string.tutorial_movement_rotation_desc)
                    )
                }

                TutorialSection(title = stringResource(R.string.tutorial_section_dropping)) {
                    TutorialItem(
                        stringResource(R.string.tutorial_dropping_soft_label),
                        stringResource(R.string.tutorial_dropping_soft_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_dropping_hard_label),
                        stringResource(R.string.tutorial_dropping_hard_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_dropping_delay_label),
                        stringResource(R.string.tutorial_dropping_delay_desc)
                    )
                }

                TutorialSection(title = stringResource(R.string.tutorial_section_strategy)) {
                    TutorialItem(
                        stringResource(R.string.tutorial_strategy_hold_label),
                        stringResource(R.string.tutorial_strategy_hold_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_strategy_ghost_label),
                        stringResource(R.string.tutorial_strategy_ghost_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_strategy_queue_label),
                        stringResource(R.string.tutorial_strategy_queue_desc)
                    )
                }

                TutorialSection(title = stringResource(R.string.tutorial_section_scoring)) {
                    TutorialItem(
                        stringResource(R.string.tutorial_scoring_lines_label),
                        stringResource(R.string.tutorial_scoring_lines_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_scoring_tspin_label),
                        stringResource(R.string.tutorial_scoring_tspin_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_scoring_combos_label),
                        stringResource(R.string.tutorial_scoring_combos_desc)
                    )
                    TutorialItem(
                        stringResource(R.string.tutorial_scoring_allclear_label),
                        stringResource(R.string.tutorial_scoring_allclear_desc)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.tutorial_got_it))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun TutorialSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = Color.Yellow,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun TutorialItem(
    label: String,
    description: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "en")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "ar")
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp", locale = "zh-rCN")
@Composable
fun TutorialOverlayPreview() {
    BlockDropTheme {
        TutorialOverlay(onDismiss = {})
    }
}
