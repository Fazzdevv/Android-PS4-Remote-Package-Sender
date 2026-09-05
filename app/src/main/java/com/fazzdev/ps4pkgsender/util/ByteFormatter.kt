package com.fazzdev.ps4pkgsender.util

import java.text.DecimalFormat
import java.util.Locale

object ByteFormatter {

    private val UNITS = arrayOf("B", "KB", "MB", "GB", "TB")
    private val DECIMAL_FORMAT = DecimalFormat("#,##0.00")

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, UNITS.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return "${DECIMAL_FORMAT.format(value)} ${UNITS[index]}"
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun formatEta(remainingBytes: Long, speedBytesPerSecond: Long): String {
        if (speedBytesPerSecond <= 0 || remainingBytes <= 0) return "--:--"
        val seconds = remainingBytes / speedBytesPerSecond
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d jam %02d mnt", hours, minutes % 60)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds % 60)
        }
    }
}
