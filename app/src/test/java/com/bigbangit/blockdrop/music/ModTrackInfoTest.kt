package com.bigbangit.blockdrop.music

import org.junit.Assert.assertEquals
import org.junit.Test

class ModTrackInfoTest {

    @Test
    fun `displayString uses title and format when both available`() {
        val info = ModTrackInfo(
            title = "Space Drift",
            fileName = "spacedrift.xm",
            format = "XM",
            pathOrUri = "/test/spacedrift.xm",
        )
        assertEquals("\u266B Space Drift [XM]", info.displayString())
    }

    @Test
    fun `displayString falls back to fileName when title is null`() {
        val info = ModTrackInfo(
            title = null,
            fileName = "acid_loop_02.mod",
            format = "MOD",
            pathOrUri = "/test/acid_loop_02.mod",
        )
        assertEquals("\u266B acid_loop_02.mod [MOD]", info.displayString())
    }

    @Test
    fun `displayString falls back to fileName when title is blank`() {
        val info = ModTrackInfo(
            title = "   ",
            fileName = "track.s3m",
            format = "S3M",
            pathOrUri = "/test/track.s3m",
        )
        assertEquals("\u266B track.s3m [S3M]", info.displayString())
    }

    @Test
    fun `displayString omits format bracket when format is null`() {
        val info = ModTrackInfo(
            title = "Untitled",
            fileName = "mystery.bin",
            format = null,
            pathOrUri = "/test/mystery.bin",
        )
        assertEquals("\u266B Untitled", info.displayString())
    }

    @Test
    fun `displayString with filename only and no format`() {
        val info = ModTrackInfo(
            title = null,
            fileName = "file.mod",
            format = null,
            pathOrUri = "/test/file.mod",
        )
        assertEquals("\u266B file.mod", info.displayString())
    }
}
