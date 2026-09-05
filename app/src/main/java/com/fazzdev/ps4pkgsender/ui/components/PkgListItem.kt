package com.fazzdev.ps4pkgsender.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.data.model.PkgFile
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.Typography

@Composable
fun PkgListItem(
    pkgFile: PkgFile,
    isSelected: Boolean,
    onToggleSelect: (PkgFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val borderColor = if (isSelected) colors.cyanAccent else colors.border
    val backgroundColor = if (isSelected) colors.surfaceVariant else colors.surface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onToggleSelect(pkgFile) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) colors.primary.copy(alpha = 0.25f) else colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = if (isSelected) colors.cyanAccent else colors.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // File details
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = pkgFile.name,
                style = Typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pkgFile.formattedSize,
                    style = Typography.bodyMedium,
                    color = colors.cyanAccent
                )
                if (pkgFile.titleId != null) {
                    Text(
                        text = "• ${pkgFile.titleId}",
                        style = Typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            }
        }

        // Checkbox
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelect(pkgFile) },
            colors = CheckboxDefaults.colors(
                checkedColor = colors.cyanAccent,
                checkmarkColor = colors.navyDark,
                uncheckedColor = colors.border
            )
        )
    }
}
