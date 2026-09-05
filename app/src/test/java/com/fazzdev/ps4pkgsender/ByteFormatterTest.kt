package com.fazzdev.ps4pkgsender

import com.fazzdev.ps4pkgsender.util.ByteFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ByteFormatterTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", ByteFormatter.formatBytes(0))
        assertTrue(ByteFormatter.formatBytes(1024).contains("KB"))
        assertTrue(ByteFormatter.formatBytes(1024 * 1024 * 1024L).contains("GB"))
    }

    @Test
    fun testFormatEta() {
        val eta = ByteFormatter.formatEta(100 * 1024 * 1024L, 10 * 1024 * 1024L)
        // 10 seconds -> "00:10"
        assertEquals("00:10", eta)
    }
}
