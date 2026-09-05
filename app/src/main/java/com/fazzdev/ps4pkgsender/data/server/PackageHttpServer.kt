package com.fazzdev.ps4pkgsender.data.server

import android.content.Context
import android.util.Log
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class PackageHttpServer(
    private val context: Context,
    val port: Int = 8080
) {
    companion object {
        private const val TAG = "PackageHttpServer"
        private const val THREAD_POOL_SIZE = 16 // Multi-threaded per PRD 6.7
        private val registeredFiles = ConcurrentHashMap<String, PkgFile>()

        val totalBytesServed = AtomicLong(0L)
        val activeConnectionsCount = AtomicLong(0L)
        val lastDataServedTimestamp = AtomicLong(System.currentTimeMillis())

        fun registerFiles(files: List<PkgFile>) {
            for (file in files) {
                registeredFiles[file.id] = file
            }
        }

        fun registerFile(file: PkgFile) {
            registeredFiles[file.id] = file
        }

        fun getFile(id: String): PkgFile? = registeredFiles[id]

        fun clearFiles() {
            registeredFiles.clear()
        }

        fun resetStats() {
            totalBytesServed.set(0L)
            lastDataServedTimestamp.set(System.currentTimeMillis())
        }
    }

    private var serverSocket: ServerSocket? = null
    private val isRunning = AtomicBoolean(false)
    private var threadPool: ThreadPoolExecutor? = null
    
    // Diagnostic stats (access companion properties)
    val totalBytesServed: AtomicLong get() = Companion.totalBytesServed
    val activeConnectionsCount: AtomicLong get() = Companion.activeConnectionsCount

    var onLogMessage: ((String) -> Unit)? = null

    fun registerFiles(files: List<PkgFile>) {
        Companion.registerFiles(files)
    }

    fun registerFile(file: PkgFile) {
        Companion.registerFile(file)
    }

    fun isServerRunning(): Boolean = isRunning.get()

    fun start() {
        if (isRunning.get()) {
            log("Server already running on port $port")
            return
        }

        try {
            serverSocket = ServerSocket(port).apply {
                reuseAddress = true
            }
            isRunning.set(true)
            threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE) as ThreadPoolExecutor

            log("Server started on port $port with pool size $THREAD_POOL_SIZE")

            // Accept loop thread
            Thread({
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        threadPool?.execute {
                            handleClient(clientSocket)
                        }
                    } catch (e: SocketException) {
                        if (!isRunning.get()) break
                        Log.e(TAG, "SocketException in accept loop: ${e.message}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception in accept loop", e)
                    }
                }
            }, "PackageHttpServer-Acceptor").start()

        } catch (e: Exception) {
            isRunning.set(false)
            log("Failed to start server on port $port: ${e.message}")
            throw e
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) return

        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null

        try {
            threadPool?.shutdownNow()
        } catch (_: Exception) {}
        threadPool = null

        log("Server stopped")
    }

    private fun handleClient(socket: Socket) {
        activeConnectionsCount.incrementAndGet()
        try {
            socket.tcpNoDelay = true
            try { socket.sendBufferSize = 1024 * 1024 } catch (_: Exception) {}
            try { socket.receiveBufferSize = 256 * 1024 } catch (_: Exception) {}
            val inputStream = socket.getInputStream()
            val outputStream = socket.getOutputStream()
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.US_ASCII))

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val rawPath = parts[1]

            // Parse headers
            val headers = mutableMapOf<String, String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrEmpty()) break
                val colonIdx = line!!.indexOf(':')
                if (colonIdx != -1) {
                    val key = line!!.substring(0, colonIdx).trim().lowercase()
                    val value = line!!.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            val rangeHeader = headers["range"]
            val clientIp = socket.inetAddress?.hostAddress ?: "unknown"

            log("Incoming $method $rawPath from $clientIp (Range: $rangeHeader)")

            // Health check / Loopback test
            if (rawPath == "/health" || rawPath == "/") {
                val body = "PS4 Package Sender HTTP Server OK"
                val response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: ${body.length}\r\n\r\n$body"
                outputStream.write(response.toByteArray(Charsets.US_ASCII))
                outputStream.flush()
                return
            }

            // Path format: /pkg/{fileId} or /pkg/{fileId}/name.pkg
            val decodedPath = try {
                java.net.URLDecoder.decode(rawPath, "UTF-8")
            } catch (_: Exception) {
                rawPath
            }

            if (decodedPath.startsWith("/pkg/")) {
                val pathWithoutPrefix = decodedPath.removePrefix("/pkg/")
                val fileId = pathWithoutPrefix.substringBefore("/").substringBefore("?")
                var file = getFile(fileId)

                if (file == null) {
                    // Fallback to Room Database!
                    try {
                        val db = com.fazzdev.ps4pkgsender.PkgSenderApp.instance.database
                        val entity = kotlinx.coroutines.runBlocking { db.pkgDao().getPkgById(fileId) }
                        if (entity != null) {
                            file = com.fazzdev.ps4pkgsender.util.StorageScanner.entityToModel(entity)
                            registerFile(file)
                            log("Recovered file from database: ${file.name} (ID: $fileId)")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error looking up file in database: ${e.message}")
                    }
                }

                if (file == null) {
                    log("File not registered or found: ID $fileId (Path: $rawPath)")
                    val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\n\r\nNot Found"
                    outputStream.write(notFound.toByteArray(Charsets.US_ASCII))
                    outputStream.flush()
                    return
                }

                ContentRangeStreamer.streamContent(
                    contentResolver = context.contentResolver,
                    uri = file.uri,
                    totalLength = file.sizeBytes,
                    rangeHeader = rangeHeader,
                    outputStream = outputStream,
                    isHeadRequest = (method == "HEAD"),
                    onProgress = { bytesDelta, _, _ ->
                        totalBytesServed.addAndGet(bytesDelta)
                        lastDataServedTimestamp.set(System.currentTimeMillis())
                    }
                )
            } else {
                val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\n\r\nNot Found"
                outputStream.write(notFound.toByteArray(Charsets.US_ASCII))
                outputStream.flush()
            }

        } catch (e: Exception) {
            Log.d(TAG, "Client connection ended: ${e.message}")
        } finally {
            activeConnectionsCount.decrementAndGet()
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun log(message: String) {
        Log.i(TAG, message)
        onLogMessage?.invoke(message)
    }
}
