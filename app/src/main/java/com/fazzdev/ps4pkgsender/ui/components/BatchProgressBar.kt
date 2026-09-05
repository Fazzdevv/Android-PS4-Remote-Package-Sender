package com.fazzdev.ps4pkgsender.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.data.model.BatchStatus
import com.fazzdev.ps4pkgsender.data.model.BatchTask
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.StatusError
import com.fazzdev.ps4pkgsender.ui.theme.StatusSuccess
import com.fazzdev.ps4pkgsender.ui.theme.StatusWarning
import com.fazzdev.ps4pkgsender.ui.theme.Typography
import com.fazzdev.ps4pkgsender.util.ByteFormatter

@Composable
fun BatchProgressBar(
    batchTask: BatchTask,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    val animatedProgress by animateFloatAsState(
        targetValue = (batchTask.progressPercentage / 100f).coerceIn(0f, 1f),
        label = "BatchProgress"
    )

    val (statusColor, statusBg) = when (batchTask.status) {
        BatchStatus.SENT_TO_PS4 -> Pair(StatusSuccess, StatusSuccess.copy(alpha = 0.15f))
        BatchStatus.IN_PROGRESS -> Pair(colors.cyanAccent, colors.cyanAccent.copy(alpha = 0.15f))
        BatchStatus.SUBMITTING -> Pair(StatusWarning, StatusWarning.copy(alpha = 0.15f))
        BatchStatus.PAUSED -> Pair(StatusWarning, StatusWarning.copy(alpha = 0.15f))
        BatchStatus.FAILED, BatchStatus.FAILED_INVALID_TASK_ID -> Pair(StatusError, StatusError.copy(alpha = 0.15f))
        else -> Pair(colors.textSecondary, colors.border)
    }

    val statusLabel = when (batchTask.status) {
        BatchStatus.PENDING -> strings.statusQueued
        BatchStatus.SUBMITTING -> strings.statusSending
        BatchStatus.IN_PROGRESS -> strings.statusInstalling
        BatchStatus.SENT_TO_PS4 -> strings.statusSuccess
        BatchStatus.PAUSED -> strings.statusPaused
        BatchStatus.STOPPED -> strings.statusCancelled
        BatchStatus.FAILED, BatchStatus.FAILED_INVALID_TASK_ID -> strings.statusFailed
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status row & Task ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = Typography.titleMedium,
                    color = statusColor
                )
            }

            if (batchTask.taskId != null && batchTask.taskId > 0) {
                Text(
                    text = "Task ID: #${batchTask.taskId}",
                    style = Typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = if (batchTask.status == BatchStatus.SENT_TO_PS4) StatusSuccess else colors.cyanAccent,
            trackColor = colors.surfaceVariant
        )

        // Transfer statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${"%.1f".format(batchTask.progressPercentage)}% ${strings.batchProgress}",
                    style = Typography.titleMedium,
                    color = colors.textPrimary
                )
                if (batchTask.speedBytesPerSec > 0 && batchTask.status == BatchStatus.IN_PROGRESS) {
                    Text(
                        text = "${ByteFormatter.formatSpeed(batchTask.speedBytesPerSec)} • ${strings.eta}: ${ByteFormatter.formatEta(batchTask.totalBytes - batchTask.transferredBytes, batchTask.speedBytesPerSec)}",
                        style = Typography.labelSmall,
                        color = colors.cyanAccent
                    )
                }
            }
            Text(
                text = "${ByteFormatter.formatBytes(batchTask.transferredBytes)} / ${ByteFormatter.formatBytes(batchTask.totalBytes)}",
                style = Typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        // Error message if any
        if (batchTask.errorMessage != null) {
            Text(
                text = batchTask.errorMessage,
                style = Typography.bodyMedium,
                color = StatusError
            )
        }

        // Action controls (Pause, Resume, Stop)
        if (batchTask.status == BatchStatus.IN_PROGRESS || batchTask.status == BatchStatus.PAUSED) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (batchTask.status == BatchStatus.IN_PROGRESS) {
                    OutlinedButton(
                        onClick = onPauseClick,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = strings.pause, modifier = Modifier.size(16.dp))
                        Text(" ${strings.pause}", style = Typography.labelSmall)
                    }
                } else if (batchTask.status == BatchStatus.PAUSED) {
                    Button(
                        onClick = onResumeClick,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = strings.resume, modifier = Modifier.size(16.dp), tint = colors.onPrimary)
                        Text(" ${strings.resume}", style = Typography.labelSmall, color = colors.onPrimary)
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                OutlinedButton(
                    onClick = onStopClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = strings.cancel, modifier = Modifier.size(16.dp))
                    Text(" ${strings.cancel}", style = Typography.labelSmall)
                }
            }
        }
    }
}
