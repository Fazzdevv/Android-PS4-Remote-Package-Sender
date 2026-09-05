package com.fazzdev.ps4pkgsender.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.StatusWarning
import com.fazzdev.ps4pkgsender.ui.theme.Typography
import com.fazzdev.ps4pkgsender.util.OemVendor

@Composable
fun OemWarningBanner(
    oemVendor: OemVendor,
    onConfigureClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (oemVendor == OemVendor.GENERIC) return

    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    val explanation = when (oemVendor) {
        OemVendor.XIAOMI -> strings.oemExplanationXiaomi
        OemVendor.OPPO_REALME -> strings.oemExplanationOppo
        OemVendor.HUAWEI -> strings.oemExplanationHuawei
        OemVendor.SAMSUNG -> strings.oemExplanationSamsung
        OemVendor.GENERIC -> strings.oemExplanationGeneric
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = StatusWarning
                )
                Text(
                    text = strings.oemDetected(oemVendor.displayName),
                    style = Typography.titleMedium,
                    color = colors.textPrimary
                )
            }

            IconButton(
                onClick = onDismissClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = strings.oemClose,
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = explanation,
            style = Typography.bodyMedium,
            color = colors.textSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onDismissClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusWarning),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(strings.oemDismiss, style = Typography.labelSmall)
            }

            Button(
                onClick = onConfigureClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StatusWarning,
                    contentColor = colors.navyDark
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(strings.oemConfigure, style = Typography.labelSmall)
            }
        }
    }
}
