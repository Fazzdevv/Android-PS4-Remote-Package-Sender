package com.fazzdev.ps4pkgsender.data.model

enum class BatchStatus(val displayLabel: String) {
    PENDING("Menunggu"),
    SUBMITTING("Mengirim perintah install..."),
    FAILED_INVALID_TASK_ID("Gagal: task_id tidak valid (-1)"),
    IN_PROGRESS("Mengunduh ke PS4"),
    SENT_TO_PS4("Terkirim ke PS4 ✅"),
    PAUSED("Dijeda"),
    STOPPED("Dibatalkan"),
    FAILED("Gagal transfer")
}

data class BatchTask(
    val batchId: String,
    val files: List<PkgFile>,
    val packageUrls: List<String>,
    val taskId: Int? = null,
    val status: BatchStatus = BatchStatus.PENDING,
    val progressPercentage: Float = 0f,
    val totalBytes: Long = 0L,
    val transferredBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val errorMessage: String? = null,
    val startTime: Long = System.currentTimeMillis()
)
