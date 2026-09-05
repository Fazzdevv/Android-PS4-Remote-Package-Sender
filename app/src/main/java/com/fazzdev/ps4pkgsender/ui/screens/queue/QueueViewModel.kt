package com.fazzdev.ps4pkgsender.ui.screens.queue

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fazzdev.ps4pkgsender.PkgSenderApp
import com.fazzdev.ps4pkgsender.data.model.BatchStatus
import com.fazzdev.ps4pkgsender.data.model.BatchTask
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import com.fazzdev.ps4pkgsender.data.network.NetworkUtils
import com.fazzdev.ps4pkgsender.service.TransferForegroundService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class QueueUiState(
    val pendingFiles: List<PkgFile> = emptyList(),
    val activeBatch: BatchTask? = null,
    val completedBatches: List<BatchTask> = emptyList(),
    val ps4Ip: String = "192.168.1.100",
    val ps4Port: Int = 12800,
    val localPort: Int = 8080,
    val localIp: String? = null,
    val isSendingActive: Boolean = false
)

class QueueViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PkgSenderApp
    private val settingsManager = app.settingsManager
    private val rpiClient = app.rpiClient

    private val _uiState = MutableStateFlow(
        QueueUiState(
            ps4Ip = settingsManager.getPs4Ip(),
            ps4Port = settingsManager.getPs4Port(),
            localPort = settingsManager.getLocalPort()
        )
    )
    val uiState: StateFlow<QueueUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        refreshLocalIp()
        viewModelScope.launch {
            settingsManager.ps4Ip.collect { ip ->
                _uiState.update { it.copy(ps4Ip = ip) }
            }
        }
        viewModelScope.launch {
            settingsManager.ps4Port.collect { port ->
                _uiState.update { it.copy(ps4Port = port) }
            }
        }
        viewModelScope.launch {
            settingsManager.localPort.collect { port ->
                _uiState.update { it.copy(localPort = port) }
            }
        }
    }

    fun refreshLocalIp() {
        val ip = NetworkUtils.getLocalIpAddress(getApplication())
        _uiState.update { it.copy(localIp = ip) }
    }

    fun addFilesToQueue(files: List<PkgFile>) {
        _uiState.update { current ->
            val existingIds = current.pendingFiles.map { it.id }.toSet()
            val newUniqueFiles = files.filterNot { existingIds.contains(it.id) }
            current.copy(pendingFiles = current.pendingFiles + newUniqueFiles)
        }
    }

    fun removePendingFile(file: PkgFile) {
        _uiState.update { current ->
            current.copy(pendingFiles = current.pendingFiles.filterNot { it.id == file.id })
        }
    }

    fun clearPendingQueue() {
        _uiState.update { it.copy(pendingFiles = emptyList()) }
    }

    /**
     * Executes single-task per batch install per PRD 6.1 & 6.8.
     * All pending files are sent in one POST /api/install request as a single batch.
     */
    fun startBatchTransfer() {
        val currentState = _uiState.value
        if (currentState.isSendingActive || currentState.pendingFiles.isEmpty()) return

        val filesToTransfer = currentState.pendingFiles.toList()
        val localIp = NetworkUtils.getLocalIpAddress(getApplication()) ?: currentState.localIp ?: "127.0.0.1"
        val localPort = settingsManager.getLocalPort()
        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()

        // Register files with server immediately and reset transfer counters
        com.fazzdev.ps4pkgsender.data.server.PackageHttpServer.registerFiles(filesToTransfer)
        com.fazzdev.ps4pkgsender.data.server.PackageHttpServer.resetStats()

        // Start Foreground Service to hold WifiLock and HTTP server
        TransferForegroundService.start(getApplication(), localPort)

        viewModelScope.launch {
            delay(500)
            // Build URLs with filename ending in .pkg (mandatory for PS4 RPI package header inspection)
            val packageUrls = filesToTransfer.map { file ->
                val filename = if (file.name.endsWith(".pkg", ignoreCase = true)) file.name else "${file.name}.pkg"
                "http://$localIp:$localPort/pkg/${file.id}/${Uri.encode(filename)}"
            }

            val totalBytes = filesToTransfer.sumOf { it.sizeBytes }
            val batchId = UUID.randomUUID().toString()

            val initialBatch = BatchTask(
                batchId = batchId,
                files = filesToTransfer,
                packageUrls = packageUrls,
                status = BatchStatus.SUBMITTING,
                totalBytes = totalBytes
            )

            _uiState.update {
                it.copy(
                    pendingFiles = emptyList(),
                    activeBatch = initialBatch,
                    isSendingActive = true
                )
            }

            // Send single install request
            val result = rpiClient.sendBatchInstall(
                ps4Ip = ps4Ip,
                ps4Port = ps4Port,
                packageUrls = packageUrls
            )

            result.onSuccess { response ->
                val taskId = response.taskId
                if (taskId == null || taskId <= 0) {
                    // Instant failure per PRD 6.8
                    _uiState.update { state ->
                        state.copy(
                            activeBatch = state.activeBatch?.copy(
                                status = BatchStatus.FAILED_INVALID_TASK_ID,
                                errorMessage = "RPI mengembalikan task_id: -1. File PKG tidak didukung atau terjadi kesalahan parameter."
                            ),
                            isSendingActive = false
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            activeBatch = state.activeBatch?.copy(
                                taskId = taskId,
                                status = BatchStatus.IN_PROGRESS
                            )
                        )
                    }
                    startPollingProgress(taskId, ps4Ip, ps4Port)
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        activeBatch = state.activeBatch?.copy(
                            status = BatchStatus.FAILED,
                            errorMessage = error.message ?: "Gagal mengirim perintah install ke PS4 ($ps4Ip:$ps4Port)"
                        ),
                        isSendingActive = false
                    )
                }
            }
        }
    }

    private fun startPollingProgress(taskId: Int, ps4Ip: String, ps4Port: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var consecutiveErrors = 0
            var lastTransferredBytes = 0L
            var lastSpeedTimestamp = System.currentTimeMillis()

            while (isActive) {
                delay(1500) // 1.5 seconds polling interval per PRD 6.8
                val now = System.currentTimeMillis()

                val result = rpiClient.getTaskProgress(
                    ps4Ip = ps4Ip,
                    ps4Port = ps4Port,
                    taskId = taskId
                )

                var currentTransferred: Long? = null
                var currentTotal: Long? = null

                if (result.isSuccess && result.getOrNull() != null) {
                    val progress = result.getOrNull()!!
                    consecutiveErrors = 0

                    val rpiTransferred = when {
                        (progress.transferredTotal ?: 0L) > 0L -> progress.transferredTotal
                        (progress.transferred ?: 0L) > 0L -> progress.transferred
                        else -> null
                    }
                    val rpiLength = when {
                        (progress.lengthTotal ?: 0L) > 0L -> progress.lengthTotal
                        (progress.length ?: 0L) > 0L -> progress.length
                        else -> null
                    }

                    if (rpiTransferred != null) {
                        currentTransferred = rpiTransferred
                    }
                    if (rpiLength != null) {
                        currentTotal = rpiLength
                    }
                }

                // If RPI polling failed or RPI is suspended in background on PS4:
                // Fallback to local server byte count per PRD Section 6.8 cross-check!
                if (currentTransferred == null) {
                    val localBytesServed = com.fazzdev.ps4pkgsender.data.server.PackageHttpServer.totalBytesServed.get()
                    val activeConns = com.fazzdev.ps4pkgsender.data.server.PackageHttpServer.activeConnectionsCount.get()
                    val lastServedTime = com.fazzdev.ps4pkgsender.data.server.PackageHttpServer.lastDataServedTimestamp.get()
                    val msSinceLastServed = now - lastServedTime

                    val isActivelyStreaming = activeConns > 0 || msSinceLastServed < 15000

                    if (isActivelyStreaming && localBytesServed > 0) {
                        consecutiveErrors = 0
                        currentTransferred = localBytesServed
                    } else {
                        consecutiveErrors++
                        if (consecutiveErrors >= 30) { // ~45 seconds without RPI response AND without local data transfer
                            _uiState.update { state ->
                                state.copy(
                                    activeBatch = state.activeBatch?.copy(
                                        status = BatchStatus.FAILED,
                                        errorMessage = "Polling timeout: RPI pada $ps4Ip:$ps4Port tidak merespons dan transfer data terhenti."
                                    ),
                                    isSendingActive = false
                                )
                            }
                            break
                        }
                    }
                }

                if (currentTransferred != null) {
                    val total = currentTotal ?: _uiState.value.activeBatch?.totalBytes ?: 1L
                    val percentage = if (total > 0) {
                        ((currentTransferred.toDouble() / total.toDouble()) * 100.0).toFloat().coerceIn(0f, 100f)
                    } else 0f

                    // Calculate speed and ETA
                    val timeDeltaSec = (now - lastSpeedTimestamp).toDouble() / 1000.0
                    val speed = if (timeDeltaSec >= 1.0 && currentTransferred >= lastTransferredBytes) {
                        ((currentTransferred - lastTransferredBytes) / timeDeltaSec).toLong()
                    } else {
                        _uiState.value.activeBatch?.speedBytesPerSec ?: 0L
                    }

                    if (timeDeltaSec >= 1.0) {
                        lastTransferredBytes = currentTransferred
                        lastSpeedTimestamp = now
                    }

                    val remainingBytes = (total - currentTransferred).coerceAtLeast(0L)
                    val etaSec = if (speed > 0) remainingBytes / speed else 0L

                    // CRITICAL FIX:
                    // NEVER mark completed just because progress.status == "success"!
                    // In RPI, status: "success" only means the API call was processed.
                    // A task is only completed when transferred >= total (or percentage >= 100).
                    val isCompleted = percentage >= 100f || (total > 0 && currentTransferred >= total)

                    _uiState.update { state ->
                        val updated = state.activeBatch?.copy(
                            transferredBytes = currentTransferred,
                            totalBytes = if (total > 0) total else state.activeBatch.totalBytes,
                            progressPercentage = percentage,
                            speedBytesPerSec = speed,
                            etaSeconds = etaSec,
                            status = if (isCompleted) BatchStatus.SENT_TO_PS4 else BatchStatus.IN_PROGRESS,
                            errorMessage = null
                        )
                        state.copy(
                            activeBatch = updated,
                            isSendingActive = !isCompleted
                        )
                    }

                    if (isCompleted) {
                        _uiState.value.activeBatch?.let { completed ->
                            _uiState.update { it.copy(completedBatches = listOf(completed) + it.completedBatches) }
                        }
                        break
                    }
                }
            }
        }
    }

    fun pauseBatch() {
        val taskId = _uiState.value.activeBatch?.taskId ?: return
        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()
        viewModelScope.launch {
            rpiClient.pauseTask(ps4Ip, ps4Port, taskId)
            _uiState.update { state ->
                state.copy(activeBatch = state.activeBatch?.copy(status = BatchStatus.PAUSED))
            }
        }
    }

    fun resumeBatch() {
        val taskId = _uiState.value.activeBatch?.taskId ?: return
        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()
        viewModelScope.launch {
            rpiClient.resumeTask(ps4Ip, ps4Port, taskId)
            _uiState.update { state ->
                state.copy(activeBatch = state.activeBatch?.copy(status = BatchStatus.IN_PROGRESS))
            }
            startPollingProgress(taskId, ps4Ip, ps4Port)
        }
    }

    fun stopBatch() {
        val taskId = _uiState.value.activeBatch?.taskId
        val ps4Ip = settingsManager.getPs4Ip()
        val ps4Port = settingsManager.getPs4Port()
        viewModelScope.launch {
            if (taskId != null && taskId > 0) {
                rpiClient.stopTask(ps4Ip, ps4Port, taskId)
            }
            pollingJob?.cancel()
            _uiState.update { state ->
                state.copy(
                    activeBatch = state.activeBatch?.copy(status = BatchStatus.STOPPED),
                    isSendingActive = false
                )
            }
        }
    }
}
