package com.personal.mangarock.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.mangarock.ui.theme.Primary
import com.personal.mangarock.ui.theme.Surface
import com.personal.mangarock.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            // Theme section
            item {
                SettingsSectionHeader("Appearance")
                SettingsRow(
                    label = "Theme",
                    value = uiState.theme.replaceFirstChar { it.uppercase() }
                ) {
                    DropdownSetting(
                        current = uiState.theme,
                        options = listOf("system", "dark", "light"),
                        labels = listOf("System", "Dark", "Light"),
                        onSelect = viewModel::setTheme
                    )
                }
            }

            // Reader section
            item {
                SettingsSectionHeader("Reading")
                SettingsRow(
                    label = "Reading Mode",
                    value = when (uiState.readingDirection) {
                        "VERTICAL" -> "Webtoon"
                        "RIGHT_TO_LEFT" -> "Right to Left"
                        else -> "Left to Right"
                    }
                ) {
                    DropdownSetting(
                        current = uiState.readingDirection,
                        options = listOf("VERTICAL", "LEFT_TO_RIGHT", "RIGHT_TO_LEFT"),
                        labels = listOf("Webtoon", "Left to Right", "Right to Left"),
                        onSelect = viewModel::setReadingDirection
                    )
                }
                SettingsRow(
                    label = "Manga Titles",
                    value = if (uiState.titleLanguage == "en") "English" else "Romanji"
                ) {
                    DropdownSetting(
                        current = uiState.titleLanguage,
                        options = listOf("en", "romanji"),
                        labels = listOf("English", "Romanji"),
                        onSelect = viewModel::setTitleLanguage
                    )
                }
                SettingsSwitchRow(
                    label = "Data Saver",
                    description = "Load lower-resolution images",
                    checked = uiState.dataSaver,
                    onCheckedChange = viewModel::setDataSaver
                )
            }

            // Notifications section
            item {
                SettingsSectionHeader("Notifications")
                SettingsRow(
                    label = "Check for updates",
                    value = "${uiState.updateIntervalHours}h"
                ) {
                    DropdownSetting(
                        current = uiState.updateIntervalHours.toString(),
                        options = listOf("1", "3", "6", "12"),
                        labels = listOf("Every 1h", "Every 3h", "Every 6h", "Every 12h"),
                        onSelect = { viewModel.setUpdateInterval(it.toLong()) }
                    )
                }
            }

            // Storage section
            item {
                SettingsSectionHeader("Storage")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.clearCache() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Clear Cache", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Current size: ${uiState.cacheSize.toReadableSize()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Text("Clear", color = Primary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Server section
            item {
                var showServerDialog by remember { mutableStateOf(false) }

                SettingsSectionHeader("Server")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showServerDialog = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Server URL", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            uiState.serverUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Text("Change", color = Primary, style = MaterialTheme.typography.bodyMedium)
                }

                if (showServerDialog) {
                    ServerUrlDialog(
                        current = uiState.serverUrl,
                        onSave = { viewModel.setServerUrl(it); showServerDialog = false },
                        onDismiss = { showServerDialog = false }
                    )
                }
            }

            // About
            item {
                SettingsSectionHeader("About")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Content provided by", style = MaterialTheme.typography.bodyMedium)
                    Text("MangaKakalot", color = Primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = Primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun DropdownSetting(
    current: String,
    options: List<String>,
    labels: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            text = labels[options.indexOf(current).coerceAtLeast(0)],
            color = Primary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(labels[index]) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ServerUrlDialog(
    current: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Server URL") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter your server's IP and port.\nExample: 192.168.1.65:3000",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("192.168.1.65:3000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text("Save", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun Long.toReadableSize(): String {
    val kb = this / 1024
    val mb = kb / 1024
    return if (mb > 0) "${mb}MB" else if (kb > 0) "${kb}KB" else "${this}B"
}
