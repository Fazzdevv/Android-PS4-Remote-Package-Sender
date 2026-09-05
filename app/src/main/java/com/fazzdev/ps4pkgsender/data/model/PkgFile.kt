package com.fazzdev.ps4pkgsender.data.model

import android.net.Uri

data class PkgFile(
    val id: String,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val isSelected: Boolean = false,
    val titleId: String? = null
)
