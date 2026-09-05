package com.fazzdev.ps4pkgsender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.data.model.ConnectionTestResult
import com.fazzdev.ps4pkgsender.data.model.TestState
import com.fazzdev.ps4pkgsender.ui.i18n.IndonesianStrings
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.StatusError
import com.fazzdev.ps4pkgsender.ui.theme.StatusSuccess
import com.fazzdev.ps4pkgsender.ui.theme.StatusWarning
import com.fazzdev.ps4pkgsender.ui.theme.Typography

@Composable
fun ConnectionStatusBadge(
    connectionResult: ConnectionTestResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    val isIndo = strings is IndonesianStrings

    val (statusColor, statusText) = when {
        connectionResult.isAllConnected -> Pair(StatusSuccess, if (isIndo) "PS4 Siap" else "PS4 Ready")
        connectionResult.ps4RpiState is TestState.Loading || connectionResult.localServerState is TestState.Loading ->
            Pair(StatusWarning, if (isIndo) "Mengecek..." else "Checking...")
        connectionResult.ps4RpiState is TestState.Error || connectionResult.localServerState is TestState.Error ->
            Pair(StatusError, if (isIndo) "PS4 Terputus" else "PS4 Offline")
        else -> Pair(StatusWarning, if (isIndo) "Belum Dites" else "Not Tested")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Text(
            text = statusText,
            style = Typography.labelSmall,
            color = colors.textPrimary
        )
    }
}
