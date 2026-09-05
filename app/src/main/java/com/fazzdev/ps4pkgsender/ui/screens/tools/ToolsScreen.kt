package com.fazzdev.ps4pkgsender.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.ui.theme.PsBackground
import com.fazzdev.ps4pkgsender.ui.theme.PsBluePrimary
import com.fazzdev.ps4pkgsender.ui.theme.PsBorder
import com.fazzdev.ps4pkgsender.ui.theme.PsCyanAccent
import com.fazzdev.ps4pkgsender.ui.theme.PsNavyDark
import com.fazzdev.ps4pkgsender.ui.theme.PsSurface
import com.fazzdev.ps4pkgsender.ui.theme.StatusError
import com.fazzdev.ps4pkgsender.ui.theme.StatusSuccess
import com.fazzdev.ps4pkgsender.ui.theme.TextMuted
import com.fazzdev.ps4pkgsender.ui.theme.TextPrimary
import com.fazzdev.ps4pkgsender.ui.theme.Typography

@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PsBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Alat & Log PS4",
            style = Typography.headlineMedium,
            color = TextPrimary
        )

        // Uninstall & Title Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PsSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Manajemen Title ID PS4",
                    style = Typography.titleMedium,
                    color = TextPrimary
                )

                OutlinedTextField(
                    value = uiState.titleIdInput,
                    onValueChange = { viewModel.onTitleIdChanged(it) },
                    label = { Text("Title ID (Contoh: CUSA02299)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsCyanAccent,
                        unfocusedBorderColor = PsBorder
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.checkTitleInstalled() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cek Terpasang", style = Typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.uninstallPatch() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PsBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hapus Patch", style = Typography.labelSmall)
                    }

                    Button(
                        onClick = { viewModel.uninstallGame() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StatusError),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hapus Game", style = Typography.labelSmall)
                    }
                }

                if (uiState.isExecuting) {
                    CircularProgressIndicator(color = PsCyanAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
                }

                if (uiState.resultMessage != null) {
                    Text(
                        text = uiState.resultMessage!!,
                        style = Typography.bodyMedium,
                        color = StatusSuccess
                    )
                }
            }
        }

        // Live Log Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = PsNavyDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = PsCyanAccent)
                        Text("Log Komunikasi", style = Typography.titleMedium, color = TextPrimary)
                    }

                    TextButton(onClick = { viewModel.clearLogs() }) {
                        Text("Bersihkan", color = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PsBackground)
                        .padding(8.dp)
                ) {
                    items(uiState.logs) { logLine ->
                        Text(
                            text = logLine,
                            style = Typography.labelSmall,
                            color = if (logLine.contains("Error", ignoreCase = true) || logLine.contains("gagal", ignoreCase = true)) StatusError else PsCyanAccent
                        )
                    }
                }
            }
        }
    }
}
