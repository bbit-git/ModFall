package com.bigbangit.blockdrop.music

data class ModTrackInfo(
    val title: String?,
    val fileName: String,
    val format: String?,
    val pathOrUri: String,
) {
    fun displayString(): String {
        val label = if (!title.isNullOrBlank()) title else fileName
        return if (!format.isNullOrBlank()) "\u266B $label [$format]" else "\u266B $label"
    }
}
