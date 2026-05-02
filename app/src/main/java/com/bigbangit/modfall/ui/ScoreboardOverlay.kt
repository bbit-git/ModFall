package com.bigbangit.modfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bigbangit.modfall.R
import com.bigbangit.modfall.data.RankedScoreboardEntry
import com.bigbangit.modfall.data.ScoreboardEntry
import com.bigbangit.modfall.ui.theme.BlockDropTheme
import com.bigbangit.modfall.ui.theme.TextWhite

@Composable
fun ScoreboardScreen(
    entries: List<RankedScoreboardEntry>,
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
            text = stringResource(R.string.scoreboard_title),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Black,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 84.dp, bottom = 110.dp, start = 18.dp, end = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.scoreboard_empty),
                        color = TextWhite.copy(alpha = 0.78f),
                    )
                }
            } else {
                ScoreboardHeader()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    itemsIndexed(entries) { index, rankedEntry ->
                        ScoreboardRow(rankedEntry)
                        if (index < entries.lastIndex) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.14f),
                                thickness = 1.dp,
                            )
                        }
                    }
                }
            }
        }

        PanelControlButton(
            text = stringResource(R.string.close_button),
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 22.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ScoreboardHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScoreboardCell(text = stringResource(R.string.scoreboard_name_header), weight = 1.5f, emphasized = true)
            ScoreboardCell(
                text = stringResource(R.string.scoreboard_score_header),
                weight = 1f,
                emphasized = true,
                textAlign = TextAlign.End,
            )
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.18f),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun ScoreboardRow(
    rankedEntry: RankedScoreboardEntry,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreboardCell(
            text = "${rankedEntry.rank}. ${rankedEntry.entry.nickname}",
            weight = 1.5f,
        )
        ScoreboardCell(
            text = "${rankedEntry.entry.level} / ${rankedEntry.entry.lines} / ${rankedEntry.entry.score}",
            weight = 1f,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun RowScope.ScoreboardCell(
    text: String,
    weight: Float,
    emphasized: Boolean = false,
    textAlign: TextAlign = TextAlign.Start,
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (emphasized) TextWhite else TextWhite.copy(alpha = 0.9f),
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
        textAlign = textAlign,
    )
}

@Preview(showBackground = true, locale = "en")
@Composable
fun ScoreboardOverlayPreview() {
    BlockDropTheme {
        ScoreboardScreen(
            entries = listOf(
                RankedScoreboardEntry(1, ScoreboardEntry("Player1", 10000, 10, 100)),
                RankedScoreboardEntry(2, ScoreboardEntry("Player2", 8000, 8, 80)),
            ),
            onDismiss = {},
        )
    }
}
