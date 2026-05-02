package com.bigbangit.modfall.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TrackSelectionTest {

    private fun track(name: String) = ModTrackInfo(
        title = null,
        fileName = name,
        format = "MOD",
        pathOrUri = "/test/$name",
    )

    @Test
    fun `pickNext returns null for empty list`() {
        assertNull(DefaultModMusicService.pickNext(emptyList(), null, Random(0)))
    }

    @Test
    fun `pickNext returns the only track when single track available`() {
        val single = track("only.mod")
        val result = DefaultModMusicService.pickNext(listOf(single), null, Random(0))
        assertEquals(single, result)
    }

    @Test
    fun `pickNext replays single track even if it was last played`() {
        val single = track("only.mod")
        val result = DefaultModMusicService.pickNext(listOf(single), single.pathOrUri, Random(0))
        assertEquals(single, result)
    }

    @Test
    fun `pickNext avoids immediate repeat with two tracks`() {
        val a = track("a.mod")
        val b = track("b.mod")
        val tracks = listOf(a, b)

        // When last played was 'a', should always pick 'b'
        repeat(20) {
            val result = DefaultModMusicService.pickNext(tracks, a.pathOrUri, Random(it))
            assertEquals("Should avoid repeating 'a'", b, result)
        }
    }

    @Test
    fun `pickNext avoids immediate repeat with many tracks`() {
        val tracks = (1..10).map { track("track$it.mod") }
        val lastPlayed = tracks[3].pathOrUri

        repeat(50) {
            val result = DefaultModMusicService.pickNext(tracks, lastPlayed, Random(it))
            assertNotNull(result)
            assertNotEquals(
                "Should not repeat track4",
                lastPlayed,
                result!!.pathOrUri,
            )
        }
    }

    @Test
    fun `pickNext with null lastPlayed picks from all tracks`() {
        val tracks = (1..5).map { track("track$it.mod") }
        val result = DefaultModMusicService.pickNext(tracks, null, Random(42))
        assertNotNull(result)
    }

    @Test
    fun `pickNext distributes across available tracks`() {
        val tracks = (1..5).map { track("track$it.mod") }
        val lastPlayed = tracks[0].pathOrUri

        val picked = mutableSetOf<String>()
        repeat(100) {
            val result = DefaultModMusicService.pickNext(tracks, lastPlayed, Random(it))!!
            picked += result.pathOrUri
        }

        // Should have picked from at least 2 different non-last tracks
        assertEquals("Should not contain the last played", false, picked.contains(lastPlayed))
        assertTrue("Should pick from multiple tracks", picked.size > 1)
    }

    @Test
    fun `findNextPlayableTrack skips unplayable tracks without recursion`() {
        val a = track("a.mod")
        val b = track("b.mod")
        val c = track("c.mod")
        val attempted = mutableListOf<String>()
        val alwaysFirst = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val result = DefaultModMusicService.findNextPlayableTrack(
            tracks = listOf(a, b, c),
            lastPlayedPath = a.pathOrUri,
            random = alwaysFirst,
        ) { track ->
            attempted += track.fileName
            if (track == b) null else track.copy(title = "Loaded ${track.fileName}")
        }

        assertEquals(listOf("b.mod", "c.mod"), attempted)
        assertEquals("Loaded c.mod", result?.title)
        assertEquals(c.pathOrUri, result?.pathOrUri)
    }

    @Test
    fun `findNextPlayableTrack returns null when every track fails`() {
        val tracks = listOf(track("a.mod"), track("b.mod"), track("c.mod"))
        val attempted = mutableSetOf<String>()

        val result = DefaultModMusicService.findNextPlayableTrack(
            tracks = tracks,
            lastPlayedPath = null,
            random = Random(1),
        ) { track ->
            attempted += track.pathOrUri
            null
        }

        assertNull(result)
        assertEquals(tracks.map { it.pathOrUri }.toSet(), attempted)
    }
}
