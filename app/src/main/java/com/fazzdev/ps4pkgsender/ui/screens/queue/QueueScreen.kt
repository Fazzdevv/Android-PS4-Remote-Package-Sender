package com.fazzdev.ps4pkgsender.ui.screens.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.ui.components.BatchProgressBar
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.StatusError
import com.fazzdev.ps4pkgsender.ui.theme.Typography
import com.fazzdev.ps4pkgsender.util.ByteFormatter

@Composable
fun QueueScreen(
    viewModel: QueueViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    val pendingTotalSize = uiState.pendingFiles.sumOf { it.sizeBytes }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active batch section
        if (uiState.activeBatch != null) {
            item {
                Text(
                    text = strings.tabActive,
                    style = Typography.titleLarge,
                    color = colors.cyanAccent
                )
                Spacer(modifier = Modifier.height(8.dp))
                BatchProgressBar(
                    batchTask = uiState.activeBatch!!,
                    onPauseClick = { viewModel.pauseBatch() },
                    onResumeClick = { viewModel.resumeBatch() },
                    onStopClick = { viewModel.stopBatch() }
                )
            }
        }

        // Pending queue header & actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.queueTitle,
                        style = Typography.titleLarge,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${uiState.pendingFiles.size} files • Total ${ByteFormatter.formatBytes(pendingTotalSize)}",
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                    Text(
                        text = "Target PS4: ${uiState.ps4Ip}:${uiState.ps4Port}",
                        style = Typography.labelSmall,
                        color = colors.cyanAccent
                    )
                }

                if (uiState.pendingFiles.isNotEmpty() && !uiState.isSendingActive) {
                    TextButton(onClick = { viewModel.clearPendingQueue() }) {
                        Text(strings.clearQueue, color = StatusError)
                    }
                }
            }
        }

        // Send Button
        item {
            Button(
                onClick = { viewModel.startBatchTransfer() },
                enabled = uiState.pendingFiles.isNotEmpty() && !uiState.isSendingActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                    disabledContainerColor = colors.border,
                    disabledContentColor = colors.textMuted
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (uiState.pendingFiles.isNotEmpty() && !uiState.isSendingActive) colors.onPrimary else colors.textMuted)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (uiState.isSendingActive) strings.statusSending else strings.sendFiles(uiState.pendingFiles.size),
                    style = Typography.titleMedium,
                    color = if (uiState.pendingFiles.isNotEmpty() && !uiState.isSendingActive) colors.onPrimary else colors.textMuted
                )
            }
        }

        // List of pending items
        if (uiState.pendingFiles.isEmpty() && uiState.activeBatch == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.emptyActiveQueue,
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            }
        } else {
            items(uiState.pendingFiles, key = { it.id }) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = Typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = file.formattedSize,
                                style = Typography.bodyMedium,
                                color = colors.cyanAccent
                            )
                        }

                        if (!uiState.isSendingActive) {
                            IconButton(onClick = { viewModel.removePendingFile(file) }) {
                                Icon(Icons.Default.Delete, contentDescription = strings.cancel, tint = colors.textMuted)
                            }
                        }
                    }
                }
            }
        }

        // History of completed batches
        if (uiState.completedBatches.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = strings.tabCompleted,
                    style = Typography.titleLarge,
                    color = colors.textSecondary
                )
            }

            items(uiState.completedBatches) { batch ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = batch.status.displayLabel,
                                style = Typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "${batch.files.size} files",
                                style = Typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        Text(
                            text = ByteFormatter.formatBytes(batch.totalBytes),
                            style = Typography.bodyMedium,
                            color = colors.cyanAccent
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
