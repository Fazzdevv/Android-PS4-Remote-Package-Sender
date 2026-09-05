package com.fazzdev.ps4pkgsender.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pkg_files")
data class PkgEntity(
    @PrimaryKey
    val id: String,
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val titleId: String? = null
)
