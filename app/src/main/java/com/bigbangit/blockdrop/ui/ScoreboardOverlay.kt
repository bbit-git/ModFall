package com.bigbangit.blockdrop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bigbangit.blockdrop.R
import com.bigbangit.blockdrop.data.RankedScoreboardEntry
import com.bigbangit.blockdrop.ui.theme.TextWhite

@Composable
fun ScoreboardOverlay(
    entries: List<RankedScoreboardEntry>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.scoreboard_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            if (entries.isEmpty()) {
                Text(text = stringResource(R.string.scoreboard_empty))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScoreboardHeader()
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(entries) { rankedEntry ->
                            ScoreboardRow(rankedEntry)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        },
    )
}

@Composable
private fun ScoreboardHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreboardCell(text = stringResource(R.string.scoreboard_rank_header), weight = 0.8f, emphasized = true)
        ScoreboardCell(text = stringResource(R.string.scoreboard_name_header), weight = 2f, emphasized = true)
        ScoreboardCell(text = stringResource(R.string.scoreboard_score_header), weight = 1.6f, emphasized = true)
        ScoreboardCell(text = stringResource(R.string.scoreboard_level_header), weight = 1f, emphasized = true)
        ScoreboardCell(text = stringResource(R.string.scoreboard_lines_header), weight = 1f, emphasized = true)
    }
}

@Composable
private fun ScoreboardRow(
    rankedEntry: RankedScoreboardEntry,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScoreboardCell(text = rankedEntry.rank.toString(), weight = 0.8f)
        ScoreboardCell(text = rankedEntry.entry.nickname, weight = 2f)
        ScoreboardCell(text = rankedEntry.entry.score.toString(), weight = 1.6f)
        ScoreboardCell(text = rankedEntry.entry.level.toString(), weight = 1f)
        ScoreboardCell(text = rankedEntry.entry.lines.toString(), weight = 1f)
    }
}

@Composable
private fun RowScope.ScoreboardCell(
    text: String,
    weight: Float,
    emphasized: Boolean = false,
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (emphasized) TextWhite else TextWhite.copy(alpha = 0.9f),
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
    )
}
