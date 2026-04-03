package com.bigbangit.blockdrop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreboardRepositoryTest {
    @Test
    fun sanitizeNicknameStripsUnsupportedCharactersAndClampsLength() {
        val sanitized = ScoreboardRepository.sanitizeNickname("  A*Very-Long_Name!! 42  ")

        assertEquals("AVeryLongNam", sanitized)
    }

    @Test
    fun buildUpdatedEntriesEvictsLowestEntryWhenCandidateQualifies() {
        val existingEntries = (1..10).map { index ->
            ScoreboardEntry(
                nickname = "P$index",
                score = 1_100 - (index * 10),
                level = index,
                lines = index * 2,
            )
        }

        val updatedEntries = ScoreboardRepository.buildUpdatedEntries(
            existingEntries = existingEntries,
            candidate = ScoreboardEntry(
                nickname = "ACE",
                score = 1_500,
                level = 2,
                lines = 20,
            ),
        )

        assertEquals(10, updatedEntries.size)
        assertEquals("ACE", updatedEntries.first().nickname)
        assertFalse(updatedEntries.any { it.nickname == "P10" })
    }

    @Test
    fun buildUpdatedEntriesRejectsCandidateThatDoesNotQualify() {
        val existingEntries = (1..10).map { index ->
            ScoreboardEntry(
                nickname = "P$index",
                score = 1_100 - (index * 10),
                level = index,
                lines = index * 2,
            )
        }

        val updatedEntries = ScoreboardRepository.buildUpdatedEntries(
            existingEntries = existingEntries,
            candidate = ScoreboardEntry(
                nickname = "LATE",
                score = 500,
                level = 20,
                lines = 120,
            ),
        )

        assertEquals(existingEntries.sortedByDescending { it.score }.size, updatedEntries.size)
        assertFalse(updatedEntries.any { it.nickname == "LATE" })
    }

    @Test
    fun rankEntriesShareRankAndSkipFollowingOrdinal() {
        val rankedEntries = ScoreboardRepository.rankEntries(
            listOf(
                ScoreboardEntry("AAA", 2_000, 4, 30),
                ScoreboardEntry("BBB", 2_000, 4, 30),
                ScoreboardEntry("CCC", 1_500, 5, 40),
            ),
        )

        assertEquals(listOf(1, 1, 3), rankedEntries.map { it.rank })
        assertTrue(rankedEntries[0].entry.score >= rankedEntries[2].entry.score)
    }
}
