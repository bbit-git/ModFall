package com.bigbangit.blockdrop.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

class ModLibrary(
    private val context: Context? = null,
    private val baseDir: File? = null,
) {
    private var entries: List<ModTrackInfo> = emptyList()

    fun scan(treeUri: String? = null): List<ModTrackInfo> {
        entries = when {
            !treeUri.isNullOrBlank() && context != null -> scanSaf(treeUri)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                Log.i(TAG, "No SAF tree configured on API 33+; music library scan returns empty")
                emptyList()
            }
            else -> scanFileSystem()
        }
        return entries
    }

    fun tracks(): List<ModTrackInfo> = entries

    private fun scanFileSystem(): List<ModTrackInfo> {
        val downloadsModsDir = baseDir ?: defaultDownloadsModsDir()

        return buildList {
            if (!downloadsModsDir.isDirectory) {
                Log.i(TAG, "Music directory does not exist: ${downloadsModsDir.absolutePath}")
                return@buildList
            }
            if (!downloadsModsDir.canRead()) {
                Log.w(TAG, "Music directory not readable: ${downloadsModsDir.absolutePath}")
                return@buildList
            }

            val files = downloadsModsDir.listFiles() ?: run {
                Log.w(TAG, "Could not list files in: ${downloadsModsDir.absolutePath}")
                return@buildList
            }

            for (file in files) {
                if (!file.isFile || !file.canRead()) continue
                val format = formatOf(file.name) ?: continue
                if (file.length() == 0L) continue

                add(
                    ModTrackInfo(
                        title = null, // title extracted at load time by libopenmpt
                        fileName = file.name,
                        format = format,
                        pathOrUri = file.absolutePath,
                    ),
                )
            }

            if (isEmpty()) {
                Log.i(TAG, "No valid module files found in: ${downloadsModsDir.absolutePath}")
            } else {
                Log.i(TAG, "Found ${size} module file(s) in: ${downloadsModsDir.absolutePath}")
            }
        }
    }

    private fun scanMediaStore(): List<ModTrackInfo> {
        val appContext = context ?: return emptyList()
        val downloadsModsSelection = buildString {
            append("${MediaStore.Downloads.RELATIVE_PATH} LIKE ?")
            append(" AND (")
            SUPPORTED_EXTENSIONS.forEachIndexed { index, extension ->
                if (index > 0) append(" OR ")
                append("${MediaStore.Downloads.DISPLAY_NAME} LIKE ?")
            }
            append(")")
        }
        val selectionArgs = buildList {
            add("Download/Mods/%")
            SUPPORTED_EXTENSIONS.forEach { extension ->
                add("%.${extension}")
            }
        }.toTypedArray()
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
        )

        val cursor = appContext.contentResolver.query(
            collection,
            projection,
            downloadsModsSelection,
            selectionArgs,
            "${MediaStore.Downloads.DISPLAY_NAME} COLLATE NOCASE ASC",
        ) ?: run {
            Log.w(TAG, "Could not query Downloads collection")
            return emptyList()
        }

        return buildList {
            cursor.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameColumn)?.takeIf { it.isNotBlank() } ?: continue
                    val format = formatOf(displayName) ?: continue
                    val id = cursor.getLong(idColumn)
                    val contentUri = ContentUris.withAppendedId(collection, id)

                    add(
                        ModTrackInfo(
                            title = null, // title extracted at load time by libopenmpt
                            fileName = displayName,
                            format = format,
                            pathOrUri = contentUri.toString(),
                        ),
                    )
                }
            }

            if (isEmpty()) {
                Log.i(TAG, "No valid module files found in Downloads/Mods via MediaStore")
            } else {
                Log.i(TAG, "Found ${size} module file(s) in Downloads/Mods via MediaStore")
            }
        }
    }

    private fun scanSaf(treeUri: String): List<ModTrackInfo> {
        val appContext = context ?: return emptyList()
        val root = DocumentFile.fromTreeUri(appContext, Uri.parse(treeUri))
        if (root == null || !root.isDirectory) {
            Log.w(TAG, "Invalid SAF root URI: $treeUri")
            return emptyList()
        }

        val files = root.listFiles() ?: run {
            Log.w(TAG, "Could not list files in SAF tree: $treeUri")
            return emptyList()
        }

        return scanSafEntries(files.map { it.asSafEntry() })
    }

    internal fun scanSafEntries(entries: List<SafEntry>): List<ModTrackInfo> {
        return buildList {
            for (file in entries) {
                if (!file.isFile || !file.canRead) continue
                if (file.length == 0L) continue
                val displayName = file.name?.takeIf { it.isNotBlank() } ?: continue
                val format = formatOf(displayName) ?: continue

                add(
                    ModTrackInfo(
                        title = null,
                        fileName = displayName,
                        format = format,
                        pathOrUri = file.pathOrUri,
                    ),
                )
            }

            if (isEmpty()) {
                Log.i(TAG, "No valid module files found in SAF tree")
            } else {
                Log.i(TAG, "Found ${size} module file(s) in SAF tree")
            }
        }
    }

    private fun defaultDownloadsModsDir(): File {
        @Suppress("DEPRECATION")
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Mods")
        return downloadsDir
    }

    companion object {
        private const val TAG = "ModLibrary"
        private val SUPPORTED_EXTENSIONS = listOf("mod", "xm", "s3m", "it")

        fun formatOf(fileName: String): String? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return if (ext in SUPPORTED_EXTENSIONS) ext.uppercase() else null
        }
    }
}

internal interface SafEntry {
    val isFile: Boolean
    val canRead: Boolean
    val length: Long
    val name: String?
    val pathOrUri: String
}

private fun DocumentFile.asSafEntry(): SafEntry = object : SafEntry {
    override val isFile: Boolean get() = this@asSafEntry.isFile
    override val canRead: Boolean get() = this@asSafEntry.canRead()
    override val length: Long get() = this@asSafEntry.length()
    override val name: String? get() = this@asSafEntry.name
    override val pathOrUri: String get() = this@asSafEntry.uri.toString()
}
