package com.fazzdev.ps4pkgsender

import com.fazzdev.ps4pkgsender.data.server.ContentRangeStreamer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentRangeTest {

    @Test
    fun testParseExactRange() {
        val totalLength = 5000L
        val range = ContentRangeStreamer.parseRangeHeader("bytes=0-1023", totalLength)

        assertNotNull(range)
        assertEquals(0L, range!!.start)
        assertEquals(1023L, range.end)
        assertEquals(1024L, range.length)
        assertEquals(5000L, range.total)
    }

    @Test
    fun testParsePrefixRange() {
        val totalLength = 10000L
        val range = ContentRangeStreamer.parseRangeHeader("bytes=2000-", totalLength)

        assertNotNull(range)
        assertEquals(2000L, range!!.start)
        assertEquals(9999L, range.end)
        assertEquals(8000L, range.length)
        assertEquals(10000L, range.total)
    }

    @Test
    fun testParseSuffixRange() {
        val totalLength = 4000L
        val range = ContentRangeStreamer.parseRangeHeader("bytes=-500", totalLength)

        assertNotNull(range)
        assertEquals(3500L, range!!.start)
        assertEquals(3999L, range.end)
        assertEquals(500L, range.length)
    }

    @Test
    fun testInvalidRanges() {
        val totalLength = 1000L
        // Start exceeds end
        assertNull(ContentRangeStreamer.parseRangeHeader("bytes=500-200", totalLength))
        // Start out of bounds
        assertNull(ContentRangeStreamer.parseRangeHeader("bytes=1500-2000", totalLength))
        // Null or empty
        assertNull(ContentRangeStreamer.parseRangeHeader(null, totalLength))
        assertNull(ContentRangeStreamer.parseRangeHeader("", totalLength))
        assertNull(ContentRangeStreamer.parseRangeHeader("invalid_header", totalLength))
    }
}
