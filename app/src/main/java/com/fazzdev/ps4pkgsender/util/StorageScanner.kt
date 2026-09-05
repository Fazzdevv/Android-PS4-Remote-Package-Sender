package com.fazzdev.ps4pkgsender.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.fazzdev.ps4pkgsender.data.local.PkgEntity
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

object StorageScanner {

    private fun generateId(uriString: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(uriString.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Scans a SAF picked tree URI recursively for .pkg files.
     */
    suspend fun scanDocumentTree(
        context: Context,
        treeUri: Uri,
        onFileFound: ((PkgEntity) -> Unit)? = null
    ): List<PkgEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PkgEntity>()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext results

        scanDocumentFileRecursive(rootDoc, results, onFileFound)
        results
    }

    private fun scanDocumentFileRecursive(
        directory: DocumentFile,
        results: MutableList<PkgEntity>,
        onFileFound: ((PkgEntity) -> Unit)?
    ) {
        val children = try {
            directory.listFiles()
        } catch (_: Exception) {
            emptyArray<DocumentFile>()
        }

        for (child in children) {
            if (child.isDirectory) {
                scanDocumentFileRecursive(child, results, onFileFound)
            } else {
                val name = child.name ?: continue
                if (name.endsWith(".pkg", ignoreCase = true)) {
                    val size = child.length()
                    val lastModified = child.lastModified()
                    val uri = child.uri
                    val entity = PkgEntity(
                        id = generateId(uri.toString()),
                        uriString = uri.toString(),
                        name = name,
                        sizeBytes = size,
                        lastModified = lastModified
                    )
                    results.add(entity)
                    onFileFound?.invoke(entity)
                }
            }
        }
    }

    /**
     * Scans storage directly using File API (Used when MANAGE_EXTERNAL_STORAGE is granted).
     */
    suspend fun scanStorageDirect(
        directory: File = Environment.getExternalStorageDirectory(),
        onFileFound: ((PkgEntity) -> Unit)? = null
    ): List<PkgEntity> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PkgEntity>()
        scanFileRecursive(directory, results, onFileFound)
        results
    }

    private fun scanFileRecursive(
        dir: File,
        results: MutableList<PkgEntity>,
        onFileFound: ((PkgEntity) -> Unit)?
    ) {
        val files = try {
            dir.listFiles()
        } catch (_: Exception) {
            null
        } ?: return

        for (file in files) {
            if (file.isDirectory) {
                // Skip system folders
                if (!file.name.startsWith(".") && file.name != "Android") {
                    scanFileRecursive(file, results, onFileFound)
                }
            } else if (file.name.endsWith(".pkg", ignoreCase = true)) {
                val uri = Uri.fromFile(file)
                val entity = PkgEntity(
                    id = generateId(uri.toString()),
                    uriString = uri.toString(),
                    name = file.name,
                    sizeBytes = file.length(),
                    lastModified = file.lastModified()
                )
                results.add(entity)
                onFileFound?.invoke(entity)
            }
        }
    }

    fun entityToModel(entity: PkgEntity): PkgFile {
        return PkgFile(
            id = entity.id,
            uri = Uri.parse(entity.uriString),
            name = entity.name,
            sizeBytes = entity.sizeBytes,
            formattedSize = ByteFormatter.formatBytes(entity.sizeBytes),
            lastModified = entity.lastModified,
            isSelected = false,
            titleId = entity.titleId
        )
    }
}
