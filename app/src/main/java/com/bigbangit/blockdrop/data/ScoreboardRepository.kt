package com.bigbangit.blockdrop.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ScoreboardEntry(
    val nickname: String,
    val score: Int,
    val level: Int,
    val lines: Int,
)

data class RankedScoreboardEntry(
    val rank: Int,
    val entry: ScoreboardEntry,
)

data class ScoreSubmissionResult(
    val accepted: Boolean,
    val rankedEntries: List<RankedScoreboardEntry>,
)

class ScoreboardRepository(
    private val context: Context,
) {
    val entries: Flow<List<RankedScoreboardEntry>> = context.blockDropPreferences.data.map { preferences ->
        rankEntries(decodeEntries(preferences[ScoreboardEntriesKey].orEmpty()))
    }

    suspend fun submit(entry: ScoreboardEntry): ScoreSubmissionResult {
        val normalizedEntry = entry.copy(nickname = sanitizeNickname(entry.nickname))
        val currentEntries = entries.first().map { it.entry }
        val updatedEntries = buildUpdatedEntries(currentEntries, normalizedEntry)
        val accepted = updatedEntries.any { it == normalizedEntry }

        if (accepted) {
            context.blockDropPreferences.edit { preferences ->
                preferences[ScoreboardEntriesKey] = encodeEntries(updatedEntries)
            }
        }

        return ScoreSubmissionResult(
            accepted = accepted,
            rankedEntries = rankEntries(if (accepted) updatedEntries else currentEntries),
        )
    }

    internal companion object {
        private const val MaxEntries = 10
        private val ScoreboardEntriesKey = stringPreferencesKey("scoreboard_entries")

        fun sanitizeNickname(rawNickname: String): String {
            val filtered = buildString {
                rawNickname.forEach { character ->
                    if (character.isLetterOrDigit() || character == ' ') {
                        append(character)
                    }
                }
            }
            return filtered.trim().replace(Regex("\\s+"), " ").take(12)
        }

        fun buildUpdatedEntries(
            existingEntries: List<ScoreboardEntry>,
            candidate: ScoreboardEntry,
        ): List<ScoreboardEntry> {
            if (candidate.nickname.isBlank()) return sortEntries(existingEntries).take(MaxEntries)

            val sortedExistingEntries = sortEntries(existingEntries)
            if (sortedExistingEntries.size < MaxEntries) {
                return sortEntries(sortedExistingEntries + candidate).take(MaxEntries)
            }

            val lowestEntry = sortedExistingEntries.last()
            return if (entryComparator.compare(candidate, lowestEntry) < 0) {
                sortEntries(sortedExistingEntries.dropLast(1) + candidate).take(MaxEntries)
            } else {
                sortedExistingEntries
            }
        }

        fun rankEntries(entries: List<ScoreboardEntry>): List<RankedScoreboardEntry> {
            val sortedEntries = sortEntries(entries)
            var currentRank = 0
            var previousEntry: ScoreboardEntry? = null

            return sortedEntries.mapIndexed { index, entry ->
                if (previousEntry == null || !isSharedRank(previousEntry!!, entry)) {
                    currentRank = index + 1
                }
                previousEntry = entry
                RankedScoreboardEntry(rank = currentRank, entry = entry)
            }
        }

        private fun sortEntries(entries: List<ScoreboardEntry>): List<ScoreboardEntry> {
            return entries.sortedWith(entryComparator)
        }

        private fun encodeEntries(entries: List<ScoreboardEntry>): String {
            return entries.joinToString(separator = "\n") { entry ->
                listOf(
                    entry.nickname,
                    entry.score.toString(),
                    entry.level.toString(),
                    entry.lines.toString(),
                ).joinToString(separator = "|")
            }
        }

        private fun decodeEntries(encoded: String): List<ScoreboardEntry> {
            return encoded
                .lineSequence()
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size != 4) return@mapNotNull null

                    val score = parts[1].toIntOrNull() ?: return@mapNotNull null
                    val level = parts[2].toIntOrNull() ?: return@mapNotNull null
                    val lines = parts[3].toIntOrNull() ?: return@mapNotNull null
                    val nickname = sanitizeNickname(parts[0])
                    if (nickname.isBlank()) return@mapNotNull null

                    ScoreboardEntry(
                        nickname = nickname,
                        score = score,
                        level = level,
                        lines = lines,
                    )
                }
                .toList()
        }

        private fun isSharedRank(left: ScoreboardEntry, right: ScoreboardEntry): Boolean {
            return left.score == right.score &&
                left.level == right.level &&
                left.lines == right.lines
        }

        private val entryComparator = compareByDescending<ScoreboardEntry> { it.score }
            .thenBy { it.level }
            .thenBy { it.lines }
            .thenBy { it.nickname.lowercase() }
    }
}
