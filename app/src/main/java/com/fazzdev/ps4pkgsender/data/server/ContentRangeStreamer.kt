package com.fazzdev.ps4pkgsender.data.server

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileInputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

object ContentRangeStreamer {

    private const val TAG = "ContentRangeStreamer"
    private const val BUFFER_SIZE = 256 * 1024 // 256 KB buffer for high-throughput streaming

    data class Range(val start: Long, val end: Long, val total: Long) {
        val length: Long get() = end - start + 1
    }

    /**
     * Parses the Range header according to RFC 7233.
     * Examples: "bytes=0-1023", "bytes=1024-", "bytes=-500"
     */
    fun parseRangeHeader(rangeHeader: String?, totalLength: Long): Range? {
        if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) {
            return null
        }

        try {
            val rangeSpec = rangeHeader.removePrefix("bytes=").trim()
            if (rangeSpec.contains(",")) {
                // Multi-range is rarely used by PS4 RPI; pick first range
                val firstRange = rangeSpec.split(",")[0].trim()
                return parseSingleRange(firstRange, totalLength)
            }
            return parseSingleRange(rangeSpec, totalLength)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Range header: '$rangeHeader'", e)
            return null
        }
    }

    private fun parseSingleRange(spec: String, totalLength: Long): Range? {
        val dashIndex = spec.indexOf('-')
        if (dashIndex == -1) return null

        val startStr = spec.substring(0, dashIndex).trim()
        val endStr = spec.substring(dashIndex + 1).trim()

        var start: Long
        var end: Long

        if (startStr.isEmpty()) {
            // Suffix range: bytes=-500 (last 500 bytes)
            val suffixLength = endStr.toLongOrNull() ?: return null
            start = (totalLength - suffixLength).coerceAtLeast(0L)
            end = totalLength - 1
        } else if (endStr.isEmpty()) {
            // Prefix range: bytes=1000- (from byte 1000 to end)
            start = startStr.toLongOrNull() ?: return null
            end = totalLength - 1
        } else {
            // Range: bytes=100-200
            start = startStr.toLongOrNull() ?: return null
            end = endStr.toLongOrNull() ?: return null
        }

        if (start > end || start >= totalLength || start < 0) {
            return null
        }

        end = end.coerceAtMost(totalLength - 1)
        return Range(start, end, totalLength)
    }

    /**
     * Streams file content from a SAF ContentResolver Uri or File Uri with full Range support.
     */
    fun streamContent(
        contentResolver: ContentResolver,
        uri: Uri,
        totalLength: Long,
        rangeHeader: String?,
        outputStream: OutputStream,
        isHeadRequest: Boolean = false,
        onProgress: ((bytesDelta: Long, streamTotalSent: Long, totalRangeBytes: Long) -> Unit)? = null
    ) {
        val (fileChannel, closeAction) = try {
            if (uri.scheme == "file") {
                val file = java.io.File(uri.path ?: "")
                if (!file.exists() || !file.canRead()) {
                    Log.e(TAG, "File not found or unreadable: ${file.absolutePath}")
                    writeErrorResponse(outputStream, 404, "File Not Found")
                    return
                }
                val fis = FileInputStream(file)
                Pair(fis.channel) {
                    try { fis.close() } catch (_: Exception) {}
                }
            } else {
                val pfd = contentResolver.openFileDescriptor(uri, "r")
                if (pfd == null) {
                    Log.e(TAG, "Cannot open ParcelFileDescriptor for URI: $uri")
                    writeErrorResponse(outputStream, 404, "Not Found")
                    return
                }
                val fis = FileInputStream(pfd.fileDescriptor)
                Pair(fis.channel) {
                    try { fis.close() } catch (_: Exception) {}
                    try { pfd.close() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open file stream for URI: $uri", e)
            writeErrorResponse(outputStream, 404, "Cannot Open File")
            return
        }

        try {
            val actualTotal = if (totalLength > 0) totalLength else fileChannel.size()
            val range = parseRangeHeader(rangeHeader, actualTotal)

            if (range != null) {
                // HTTP 206 Partial Content
                val headers = buildString {
                    append("HTTP/1.1 206 Partial Content\r\n")
                    append("Content-Type: application/octet-stream\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Content-Range: bytes ${range.start}-${range.end}/${range.total}\r\n")
                    append("Content-Length: ${range.length}\r\n")
                    append("Connection: keep-alive\r\n")
                    append("\r\n")
                }
                outputStream.write(headers.toByteArray(Charsets.US_ASCII))
                outputStream.flush()

                if (!isHeadRequest) {
                    fileChannel.position(range.start)
                    val channel: WritableByteChannel = Channels.newChannel(outputStream)
                    val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

                    var remaining = range.length
                    var totalSent = 0L

                    while (remaining > 0) {
                        val toRead = remaining.coerceAtMost(BUFFER_SIZE.toLong()).toInt()
                        buffer.limit(toRead)
                        val read = fileChannel.read(buffer)
                        if (read <= 0) break

                        buffer.flip()
                        while (buffer.hasRemaining()) {
                            channel.write(buffer)
                        }
                        buffer.clear()

                        remaining -= read
                        totalSent += read
                        onProgress?.invoke(read.toLong(), totalSent, range.length)
                    }
                }
            } else {
                // HTTP 200 OK
                val headers = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: application/octet-stream\r\n")
                    append("Accept-Ranges: bytes\r\n")
                    append("Content-Length: $actualTotal\r\n")
                    append("Connection: keep-alive\r\n")
                    append("\r\n")
                }
                outputStream.write(headers.toByteArray(Charsets.US_ASCII))
                outputStream.flush()

                if (!isHeadRequest) {
                    fileChannel.position(0L)
                    val channel: WritableByteChannel = Channels.newChannel(outputStream)
                    val buffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

                    var totalSent = 0L
                    while (true) {
                        val read = fileChannel.read(buffer)
                        if (read <= 0) break

                        buffer.flip()
                        while (buffer.hasRemaining()) {
                            channel.write(buffer)
                        }
                        buffer.clear()

                        totalSent += read
                        onProgress?.invoke(read.toLong(), totalSent, actualTotal)
                    }
                }
            }
        } catch (e: Exception) {
            // Client disconnect or socket closed by PS4
            Log.d(TAG, "Stream transfer ended or interrupted: ${e.message}")
        } finally {
            closeAction()
        }
    }

    private fun writeErrorResponse(outputStream: OutputStream, statusCode: Int, message: String) {
        val response = "HTTP/1.1 $statusCode $message\r\nContent-Type: text/plain\r\nContent-Length: ${message.length}\r\n\r\n$message"
        try {
            outputStream.write(response.toByteArray(Charsets.US_ASCII))
            outputStream.flush()
        } catch (_: Exception) {}
    }
}
