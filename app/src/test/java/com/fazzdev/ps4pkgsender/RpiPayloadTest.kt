package com.fazzdev.ps4pkgsender

import com.fazzdev.ps4pkgsender.data.model.RpiInstallRequest
import com.fazzdev.ps4pkgsender.data.model.RpiInstallResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RpiPayloadTest {

    private val gson = Gson()

    @Test
    fun testInstallRequestJsonSerialization() {
        val request = RpiInstallRequest(
            packages = listOf("http://192.168.1.50:8080/pkg/game.pkg", "http://192.168.1.50:8080/pkg/patch.pkg")
        )
        val json = gson.toJson(request)

        assertTrue(json.contains("\"type\":\"direct\""))
        assertTrue(json.contains("http://192.168.1.50:8080/pkg/game.pkg"))
        assertTrue(json.contains("http://192.168.1.50:8080/pkg/patch.pkg"))
    }

    @Test
    fun testInstantFailureTaskIdMinusOne() {
        val responseJson = """{"status":"fail","task_id":-1,"error":"Invalid value"}"""
        val response = gson.fromJson(responseJson, RpiInstallResponse::class.java)

        assertEquals(-1, response.taskId)
        assertEquals("fail", response.status)
        // Verify that -1 is detected as invalid
        assertTrue(response.taskId == -1)
    }
}
