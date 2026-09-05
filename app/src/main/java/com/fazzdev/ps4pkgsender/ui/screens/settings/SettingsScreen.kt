package com.fazzdev.ps4pkgsender.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Smartphone
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fazzdev.ps4pkgsender.data.model.TestState
import com.fazzdev.ps4pkgsender.ui.i18n.LocalAppStrings
import com.fazzdev.ps4pkgsender.ui.theme.AppTheme
import com.fazzdev.ps4pkgsender.ui.theme.StatusError
import com.fazzdev.ps4pkgsender.ui.theme.StatusSuccess
import com.fazzdev.ps4pkgsender.ui.theme.StatusWarning
import com.fazzdev.ps4pkgsender.ui.theme.Typography
import com.fazzdev.ps4pkgsender.util.OemBatteryHelper
import com.fazzdev.ps4pkgsender.util.OemVendor

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = strings.settingsTitle,
            style = Typography.headlineMedium,
            color = colors.textPrimary
        )

        // 1. Language Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = colors.cyanAccent)
                    Text(strings.languageTitle, style = Typography.titleMedium, color = colors.textPrimary)
                }

                OptionSelectRow(
                    title = strings.languageEnglish,
                    subtitle = strings.languageEnglishDesc,
                    icon = Icons.Default.Language,
                    isSelected = uiState.appLanguage == "en",
                    onClick = { viewModel.onLanguageChanged("en") }
                )

                OptionSelectRow(
                    title = strings.languageIndonesian,
                    subtitle = strings.languageIndonesianDesc,
                    icon = Icons.Default.Language,
                    isSelected = uiState.appLanguage == "id",
                    onClick = { viewModel.onLanguageChanged("id") }
                )
            }
        }

        // 2. Theme Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = colors.cyanAccent)
                    Text(strings.themeTitle, style = Typography.titleMedium, color = colors.textPrimary)
                }

                OptionSelectRow(
                    title = strings.themeDark,
                    subtitle = strings.themeDarkDesc,
                    icon = Icons.Default.DarkMode,
                    isSelected = uiState.appTheme == "dark",
                    onClick = { viewModel.onThemeChanged("dark") }
                )

                OptionSelectRow(
                    title = strings.themeLight,
                    subtitle = strings.themeLightDesc,
                    icon = Icons.Default.LightMode,
                    isSelected = uiState.appTheme == "light",
                    onClick = { viewModel.onThemeChanged("light") }
                )

                OptionSelectRow(
                    title = strings.themeSystem,
                    subtitle = strings.themeSystemDesc,
                    icon = Icons.Default.SettingsBrightness,
                    isSelected = uiState.appTheme == "system",
                    onClick = { viewModel.onThemeChanged("system") }
                )
            }
        }

        // 3. PS4 Destination Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Router, contentDescription = null, tint = colors.cyanAccent)
                    Text(strings.ps4Destination, style = Typography.titleMedium, color = colors.textPrimary)
                }

                OutlinedTextField(
                    value = uiState.ps4Ip,
                    onValueChange = { viewModel.onPs4IpChanged(it) },
                    label = { Text(strings.ps4IpLabel, color = colors.textSecondary) },
                    placeholder = { Text("192.168.1.xxx", color = colors.textMuted) },
                    singleLine = true,
                    textStyle = Typography.bodyLarge.copy(color = colors.textPrimary),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyanAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.cyanAccent,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.cyanAccent,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )

                OutlinedTextField(
                    value = uiState.ps4Port,
                    onValueChange = { viewModel.onPs4PortChanged(it) },
                    label = { Text(strings.ps4PortLabel, color = colors.textSecondary) },
                    singleLine = true,
                    textStyle = Typography.bodyLarge.copy(color = colors.textPrimary),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyanAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.cyanAccent,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.cyanAccent,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
            }
        }

        // 4. Local Server Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = colors.primary)
                        Text(strings.localServer, style = Typography.titleMedium, color = colors.textPrimary)
                    }

                    IconButton(
                        onClick = { viewModel.refreshLocalIp() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh IP", tint = colors.cyanAccent)
                    }
                }

                Text(
                    text = strings.localIpLabel(uiState.localIp ?: "..."),
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.cyanAccent
                )

                OutlinedTextField(
                    value = uiState.localPort,
                    onValueChange = { viewModel.onLocalPortChanged(it) },
                    label = { Text(strings.localPortLabel, color = colors.textSecondary) },
                    singleLine = true,
                    textStyle = Typography.bodyLarge.copy(color = colors.textPrimary),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.cyanAccent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.cyanAccent,
                        focusedContainerColor = colors.surface,
                        unfocusedContainerColor = colors.surface,
                        focusedLabelColor = colors.cyanAccent,
                        unfocusedLabelColor = colors.textSecondary
                    )
                )
            }
        }

        // 5. Test Connection Button
        Button(
            onClick = { viewModel.runConnectionTest() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = !uiState.isTesting
        ) {
            if (uiState.isTesting) {
                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.size(8.dp))
                Text(strings.testingConnection, style = Typography.titleMedium, color = colors.onPrimary)
            } else {
                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(20.dp), tint = colors.onPrimary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(strings.testConnection, style = Typography.titleMedium, color = colors.onPrimary)
            }
        }

        // 6. Test Results Display
        if (uiState.testResult.localServerState !is TestState.Idle || uiState.testResult.ps4RpiState !is TestState.Idle) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(strings.testResultTitle, style = Typography.titleMedium, color = colors.textPrimary)

                    TestItemRow(
                        title = strings.testLayerServer,
                        state = uiState.testResult.localServerState,
                        notTestedStr = strings.testNotTested,
                        checkingStr = strings.testChecking
                    )

                    TestItemRow(
                        title = strings.testLayerRpi,
                        state = uiState.testResult.ps4RpiState,
                        notTestedStr = strings.testNotTested,
                        checkingStr = strings.testChecking
                    )

                    if (uiState.testResult.ps4RpiState is TestState.Error) {
                        val errorState = uiState.testResult.ps4RpiState as TestState.Error
                        if (errorState.troubleshootingHelp != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.navyDark)
                                    .border(1.dp, StatusWarning.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                                        Text(strings.troubleshootingTips, style = Typography.titleMedium, color = StatusWarning)
                                    }
                                    Text(errorState.troubleshootingHelp, style = Typography.bodyMedium, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Background & Battery Optimization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            val context = LocalContext.current
            val oemVendor = remember { OemBatteryHelper.getCurrentOem() }
            val explanation = when (oemVendor) {
                OemVendor.XIAOMI -> strings.oemExplanationXiaomi
                OemVendor.OPPO_REALME -> strings.oemExplanationOppo
                OemVendor.HUAWEI -> strings.oemExplanationHuawei
                OemVendor.SAMSUNG -> strings.oemExplanationSamsung
                OemVendor.GENERIC -> strings.oemExplanationGeneric
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = colors.cyanAccent
                    )
                    Text(
                        text = strings.batteryOptTitle,
                        style = Typography.titleMedium,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = "${strings.oemDetected(oemVendor.displayName)}. $explanation",
                    style = Typography.bodyMedium,
                    color = colors.textSecondary
                )

                Button(
                    onClick = { OemBatteryHelper.openOemBatterySettings(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.textPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(strings.openSystemSettings, style = Typography.labelSmall)
                }
            }
        }

        // 8. Credits & About Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = colors.cyanAccent)
                    Text(strings.creditsTitle, style = Typography.titleMedium, color = colors.textPrimary)
                }

                Text(
                    text = "PS4 PKG Sender Android",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = colors.cyanAccent
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${strings.creditsDeveloper}: ${strings.creditsDeveloperName}",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = colors.primary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.creditsVersion,
                            style = Typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }

                Text(
                    text = strings.creditsApi,
                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = colors.textPrimary
                )

                Text(
                    text = strings.creditsNote,
                    style = Typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun TestItemRow(title: String, state: TestState, notTestedStr: String, checkingStr: String) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        when (state) {
            is TestState.Loading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = StatusWarning)
            is TestState.Success -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(20.dp))
            is TestState.Error -> Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(20.dp))
            TestState.Idle -> Icon(Icons.Default.Info, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = Typography.titleMedium, color = colors.textPrimary)
            when (state) {
                is TestState.Success -> Text(state.message, style = Typography.bodyMedium, color = StatusSuccess)
                is TestState.Error -> Text(state.message, style = Typography.bodyMedium, color = StatusError)
                is TestState.Loading -> Text(checkingStr, style = Typography.bodyMedium, color = StatusWarning)
                TestState.Idle -> Text(notTestedStr, style = Typography.bodyMedium, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun OptionSelectRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(12.dp)

    val borderStroke = if (isSelected) {
        BorderStroke(1.5.dp, colors.cyanAccent)
    } else {
        BorderStroke(1.dp, colors.border.copy(alpha = 0.7f))
    }

    val containerColor = if (isSelected) {
        if (colors.isDark) colors.primary.copy(alpha = 0.18f) else colors.primaryContainer.copy(alpha = 0.6f)
    } else {
        if (colors.isDark) colors.surfaceVariant.copy(alpha = 0.35f) else colors.surfaceVariant.copy(alpha = 0.45f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(borderStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) colors.primary.copy(alpha = 0.22f)
                    else colors.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) colors.cyanAccent else colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = Typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = Typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.cyanAccent,
                unselectedColor = colors.textMuted
            )
        )
    }
}
