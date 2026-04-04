package com.bigbangit.blockdrop.music

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Method
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.`when`

class ModLibraryTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    @Test
    fun `scan returns empty list when directory does not exist`() {
        val library = ModLibrary(baseDir = File(tempDir.root, "nonexistent"))
        val result = library.scan()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `scan returns empty list when directory is empty`() {
        val dir = tempDir.newFolder("Mods")
        val library = ModLibrary(baseDir = dir)
        val result = library.scan()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `scan finds MOD and XM files`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "track1.mod").writeBytes(byteArrayOf(1, 2, 3))
        File(dir, "track2.xm").writeBytes(byteArrayOf(4, 5, 6))
        File(dir, "readme.txt").writeText("ignore me")

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertEquals(2, result.size)
        assertEquals(setOf("track1.mod", "track2.xm"), result.map { it.fileName }.toSet())
    }

    @Test
    fun `scan finds S3M and IT files`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "song.s3m").writeBytes(byteArrayOf(1))
        File(dir, "music.it").writeBytes(byteArrayOf(2))

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertEquals(2, result.size)
        assertEquals(setOf("S3M", "IT"), result.map { it.format }.toSet())
    }

    @Test
    fun `scan ignores zero-length files`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "empty.mod").createNewFile() // 0 bytes
        File(dir, "valid.xm").writeBytes(byteArrayOf(1))

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertEquals(1, result.size)
        assertEquals("valid.xm", result[0].fileName)
    }

    @Test
    fun `scan ignores unsupported extensions`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "song.mp3").writeBytes(byteArrayOf(1))
        File(dir, "pic.png").writeBytes(byteArrayOf(2))
        File(dir, "noext").writeBytes(byteArrayOf(3))

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scan ignores subdirectories`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "subdir.mod").mkdirs() // directory, not file
        File(dir, "real.mod").writeBytes(byteArrayOf(1))

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertEquals(1, result.size)
        assertEquals("real.mod", result[0].fileName)
    }

    @Test
    fun `format detection is case insensitive`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "UPPER.MOD").writeBytes(byteArrayOf(1))
        File(dir, "Mixed.Xm").writeBytes(byteArrayOf(2))

        val library = ModLibrary(baseDir = dir)
        val result = library.scan()

        assertEquals(2, result.size)
        assertEquals(setOf("MOD", "XM"), result.map { it.format }.toSet())
    }

    @Test
    fun `tracks returns last scan result`() {
        val dir = tempDir.newFolder("Mods")
        File(dir, "a.mod").writeBytes(byteArrayOf(1))

        val library = ModLibrary(baseDir = dir)
        assertTrue(library.tracks().isEmpty()) // before scan

        library.scan()
        assertEquals(1, library.tracks().size)
    }

    @Test
    fun `formatOf returns correct format strings`() {
        assertEquals("MOD", ModLibrary.formatOf("test.mod"))
        assertEquals("XM", ModLibrary.formatOf("test.xm"))
        assertEquals("S3M", ModLibrary.formatOf("test.s3m"))
        assertEquals("IT", ModLibrary.formatOf("test.it"))
        assertNull(ModLibrary.formatOf("test.mp3"))
        assertNull(ModLibrary.formatOf("noext"))
    }

    @Test
    fun `scanSafEntries returns empty list when given no files`() {
        val library = ModLibrary()

        val result = library.scanSafEntries(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scanSafEntries filters zero-length files`() {
        val library = ModLibrary()

        val result = library.scanSafEntries(
            listOf(
                fakeSafEntry("empty.mod", "content://mods/empty", entryLength = 0L),
                fakeSafEntry("valid.xm", "content://mods/valid", entryLength = 1L),
            ),
        )

        assertEquals(1, result.size)
        assertEquals("valid.xm", result.single().fileName)
    }

    @Test
    fun `scanSafEntries ignores unsupported extensions`() {
        val library = ModLibrary()

        val result = library.scanSafEntries(
            listOf(
                fakeSafEntry("song.mp3", "content://mods/song", entryLength = 1L),
                fakeSafEntry("cover.png", "content://mods/cover", entryLength = 1L),
            ),
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scanSafEntries returns tracks for valid files`() {
        val library = ModLibrary()

        val result = library.scanSafEntries(
            listOf(
                fakeSafEntry("track1.mod", "content://mods/track1", entryLength = 3L),
                fakeSafEntry("track2.it", "content://mods/track2", entryLength = 7L),
            ),
        )

        assertEquals(2, result.size)
        assertEquals(setOf("track1.mod", "track2.it"), result.map { it.fileName }.toSet())
        assertEquals(setOf("content://mods/track1", "content://mods/track2"), result.map { it.pathOrUri }.toSet())
    }

    @Test
    fun `scanSaf returns empty list when context is null`() {
        val library = ModLibrary()

        val result = invokeScanSaf(library, "content://mods/tree")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `scanSaf returns empty list when fromTreeUri returns null root`() {
        val context = mock(Context::class.java)
        val library = ModLibrary(context = context)

        mockStatic(DocumentFile::class.java).use { documentFileMock ->
            documentFileMock.`when`<DocumentFile?> {
                DocumentFile.fromTreeUri(
                    org.mockito.ArgumentMatchers.eq(context),
                    org.mockito.ArgumentMatchers.any(android.net.Uri::class.java),
                )
            }.thenReturn(null)

            val result = invokeScanSaf(library, "content://mods/tree")

            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `scanSaf returns empty list when listFiles returns null`() {
        val context = mock(Context::class.java)
        val root = mock(DocumentFile::class.java)
        val library = ModLibrary(context = context)

        doReturn(true).`when`(root).isDirectory
        doReturn(null as Array<DocumentFile>?).`when`(root).listFiles()

        mockStatic(DocumentFile::class.java).use { documentFileMock ->
            documentFileMock.`when`<DocumentFile?> {
                DocumentFile.fromTreeUri(
                    org.mockito.ArgumentMatchers.eq(context),
                    org.mockito.ArgumentMatchers.any(android.net.Uri::class.java),
                )
            }.thenReturn(root)

            val result = invokeScanSaf(library, "content://mods/tree")

            assertTrue(result.isEmpty())
        }
    }

    @Test
    fun `scanSaf returns empty list when listFiles succeeds with no entries`() {
        val context = mock(Context::class.java)
        val root = mock(DocumentFile::class.java)
        val library = ModLibrary(context = context)

        doReturn(true).`when`(root).isDirectory
        doReturn(emptyArray<DocumentFile>()).`when`(root).listFiles()

        mockStatic(DocumentFile::class.java).use { documentFileMock ->
            documentFileMock.`when`<DocumentFile?> {
                DocumentFile.fromTreeUri(
                    org.mockito.ArgumentMatchers.eq(context),
                    org.mockito.ArgumentMatchers.any(android.net.Uri::class.java),
                )
            }.thenReturn(root)

            val result = invokeScanSaf(library, "content://mods/tree")

            assertTrue(result.isEmpty())
        }
    }

    private fun fakeSafEntry(
        entryName: String?,
        entryUri: String,
        entryLength: Long,
        entryIsFile: Boolean = true,
        entryCanRead: Boolean = true,
    ): SafEntry = object : SafEntry {
        override val isFile: Boolean = entryIsFile
        override val canRead: Boolean = entryCanRead
        override val length: Long = entryLength
        override val name: String? = entryName
        override val pathOrUri: String = entryUri
    }

    private fun invokeScanSaf(library: ModLibrary, treeUri: String): List<ModTrackInfo> {
        val method: Method = ModLibrary::class.java.getDeclaredMethod("scanSaf", String::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(library, treeUri) as List<ModTrackInfo>
    }
}
