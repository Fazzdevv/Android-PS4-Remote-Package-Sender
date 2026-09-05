package com.fazzdev.ps4pkgsender.data.network

import android.util.Log
import com.fazzdev.ps4pkgsender.data.model.ConnectionTestResult
import com.fazzdev.ps4pkgsender.data.model.RpiInstallRequest
import com.fazzdev.ps4pkgsender.data.model.RpiInstallResponse
import com.fazzdev.ps4pkgsender.data.model.RpiIsExistsRequest
import com.fazzdev.ps4pkgsender.data.model.RpiProgressResponse
import com.fazzdev.ps4pkgsender.data.model.RpiTaskActionRequest
import com.fazzdev.ps4pkgsender.data.model.RpiUninstallRequest
import com.fazzdev.ps4pkgsender.data.model.TestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class RpiClient {

    companion object {
        private const val TAG = "RpiClient"
        private const val DUMMY_CHECK_TITLE = "XXXX00000_0000000000000000"
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun getApiService(ip: String, port: Int): RpiApiService {
        val baseUrl = "http://$ip:$port/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RpiApiService::class.java)
    }

    /**
     * Executes 2-layer connection test per PRD Section 6.6:
     * Layer 1: Check Local HTTP Server via loopback http://127.0.0.1:<localPort>/health
     * Layer 2: Check PS4 RPI via GET /api/is_exists?title_id=XXXX... with socket fallback
     */
    suspend fun testConnection(
        localIp: String?,
        localPort: Int,
        ps4Ip: String,
        ps4Port: Int
    ): ConnectionTestResult = withContext(Dispatchers.IO) {
        // Layer 1: Local HTTP server check
        val localState = try {
            val loopbackUrl = "http://127.0.0.1:$localPort/health"
            val request = Request.Builder().url(loopbackUrl).build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                TestState.Success("Server lokal aktif di port $localPort (IP: ${localIp ?: "127.0.0.1"})")
            } else {
                TestState.Error("Server lokal mengembalikan status ${response.code}")
            }
        } catch (e: Exception) {
            TestState.Error(
                message = "Server HTTP lokal belum aktif atau port $localPort diblokir: ${e.message}",
                troubleshootingHelp = "Pastikan foreground service berjalan dan tidak ada aplikasi lain memakai port $localPort."
            )
        }

        // Layer 2: PS4 RPI connectivity check
        val ps4State = if (ps4Ip.isBlank()) {
            TestState.Error("IP PS4 belum diisi")
        } else {
            try {
                val api = getApiService(ps4Ip, ps4Port)
                val response = try {
                    api.checkIsExists(RpiIsExistsRequest(DUMMY_CHECK_TITLE))
                } catch (_: Exception) {
                    api.checkIsExistsGet(DUMMY_CHECK_TITLE)
                }
                if (response.isSuccessful) {
                    TestState.Success("RPI terdeteksi & merespons aktif pada $ps4Ip:$ps4Port")
                } else {
                    // Fallback to raw TCP socket connect
                    val socketSuccess = checkRawTcpSocket(ps4Ip, ps4Port, timeoutMs = 3000)
                    if (socketSuccess) {
                        TestState.Success("Port $ps4Port terbuka pada $ps4Ip (HTTP status ${response.code()})")
                    } else {
                        TestState.Error("RPI merespons dengan kode error HTTP ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                // Try fallback raw TCP connection
                val socketConnected = checkRawTcpSocket(ps4Ip, ps4Port, timeoutMs = 3000)
                if (socketConnected) {
                    TestState.Success("Port $ps4Port terbuka pada $ps4Ip (socket connect berhasil)")
                } else {
                    TestState.Error(
                        message = "Gagal terhubung ke PS4 di $ps4Ip:$ps4Port (${e.message ?: "Connection Refused / Timeout"})",
                        troubleshootingHelp = """
                            1. Pastikan PS4 & HP berada dalam satu jaringan Wi-Fi router yang sama.
                            2. Pastikan RPI (Remote Package Installer) sedang FOREGROUND di PS4 (tidak diminimize).
                            3. Periksa kembali IP PS4 di Settings -> Network -> View Connection Status.
                            4. Pastikan fitur 'AP Isolation' / 'Client Isolation' di router Anda dalam kondisi OFF.
                        """.trimIndent()
                    )
                }
            }
        }

        ConnectionTestResult(
            localServerState = localState,
            ps4RpiState = ps4State,
            localIpAddress = localIp,
            localPort = localPort,
            ps4IpAddress = ps4Ip,
            ps4Port = ps4Port
        )
    }

    private fun checkRawTcpSocket(ip: String, port: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sends a batch installation command to RPI.
     * Per PRD 6.1 & 6.8: All URLs are included in one `packages` array in a single call.
     * Instant failure is triggered if task_id == -1.
     */
    suspend fun sendBatchInstall(
        ps4Ip: String,
        ps4Port: Int,
        packageUrls: List<String>
    ): Result<RpiInstallResponse> = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(ps4Ip, ps4Port)
            val request = RpiInstallRequest(packages = packageUrls)
            val response = api.installPackages(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // PRD 6.8: Check instant failure for task_id: -1
                if (body.taskId != null && body.taskId == -1) {
                    Result.failure(Exception("RPI mengembalikan task_id: -1 (kegagalan instan: PKG tidak kompatibel/invalid)"))
                } else if (body.status == "fail") {
                    Result.failure(Exception("RPI gagal membuat task: ${body.error ?: "Unknown error"}"))
                } else {
                    Result.success(body)
                }
            } else {
                Result.failure(Exception("HTTP Error ${response.code()}: ${response.errorBody()?.string() ?: response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "sendBatchInstall failed", e)
            Result.failure(e)
        }
    }

    /**
     * Polls the progress of the active batch task.
     */
    suspend fun getTaskProgress(
        ps4Ip: String,
        ps4Port: Int,
        taskId: Int
    ): Result<RpiProgressResponse> = withContext(Dispatchers.IO) {
        try {
            val api = getApiService(ps4Ip, ps4Port)
            // Flatz RPI and OpenOrbis RPI standard is POST /api/get_task_progress {"task_id": 123}
            val response = try {
                val postResp = api.getTaskProgress(RpiTaskActionRequest(taskId))
                if (postResp.isSuccessful && postResp.body() != null) {
                    postResp
                } else {
                    // Fallback to GET if POST was rejected
                    api.getTaskProgressGet(taskId)
                }
            } catch (e: Exception) {
                // Fallback to GET if POST threw exception
                try {
                    api.getTaskProgressGet(taskId)
                } catch (_: Exception) {
                    throw e
                }
            }

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: Gagal membaca progres task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pauseTask(ps4Ip: String, ps4Port: Int, taskId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService(ps4Ip, ps4Port).pauseTask(RpiTaskActionRequest(taskId))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resumeTask(ps4Ip: String, ps4Port: Int, taskId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService(ps4Ip, ps4Port).resumeTask(RpiTaskActionRequest(taskId))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopTask(ps4Ip: String, ps4Port: Int, taskId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService(ps4Ip, ps4Port).stopTask(RpiTaskActionRequest(taskId))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallGame(ps4Ip: String, ps4Port: Int, titleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService(ps4Ip, ps4Port).uninstallGame(RpiUninstallRequest(titleId))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallPatch(ps4Ip: String, ps4Port: Int, titleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService(ps4Ip, ps4Port).uninstallPatch(RpiUninstallRequest(titleId))
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
