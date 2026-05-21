package com.personal.mangastone.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.Surface
import com.personal.mangastone.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context     = LocalContext.current
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.let(viewModel::importLibrary) }
    }

    // Launch share sheet when export is ready
    val exportIntent = (backupState as? BackupState.ExportReady)?.intent
    LaunchedEffect(exportIntent) {
        exportIntent?.let {
            context.startActivity(Intent.createChooser(it, "Save backup to…"))
            viewModel.dismissBackupState()
        }
    }

    // Result dialog for import
    val backupMessage = when (val s = backupState) {
        is BackupState.ImportDone -> s.message
        is BackupState.Error      -> "Error: ${s.message}"
        else                      -> null
    }
    if (backupMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissBackupState,
            title = { Text(if (backupState is BackupState.ImportDone) "Import complete" else "Backup error") },
            text  = { Text(backupMessage) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissBackupState) { Text("OK") }
            }
        )
    }

    // Dialog when update is available
    val availableInfo = (updateState as? AppUpdateState.Available)?.info
    if (availableInfo != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUpdate,
            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = Primary) },
            title = { Text("Update available — v${availableInfo.latestVersion}") },
            text = {
                Text(
                    availableInfo.releaseNotes.ifBlank { "A new version is ready to install." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.downloadUpdate(availableInfo) },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Download & Install") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUpdate) { Text("Later") }
            }
        )
    }

    // Trigger install as soon as download finishes
    val readyFile = (updateState as? AppUpdateState.ReadyToInstall)?.file
    LaunchedEffect(readyFile) {
        readyFile?.let { viewModel.installUpdate(it) }
    }

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

            // Library backup
            item {
                SettingsSectionHeader("Library")

                // Export
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = backupState !is BackupState.Exporting) {
                            viewModel.exportLibrary()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Export Library", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Save favorites & reading progress to a file",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    if (backupState is BackupState.Exporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                    } else {
                        Text("Export", color = Primary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = backupState !is BackupState.Importing) {
                            importLauncher.launch("application/json")
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Import Library", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Restore from a previous backup file",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    if (backupState is BackupState.Importing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                    } else {
                        Text("Import", color = Primary, style = MaterialTheme.typography.bodyMedium)
                    }
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

            // App Updates
            item {
                SettingsSectionHeader("App")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = updateState !is AppUpdateState.Checking &&
                                      updateState !is AppUpdateState.Downloading
                        ) { viewModel.checkForUpdate() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Check for Updates", style = MaterialTheme.typography.bodyLarge)
                        Text("Current version: 1.1.1", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        when (val s = updateState) {
                            is AppUpdateState.Checking ->
                                Text("Checking…", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            is AppUpdateState.UpToDate ->
                                Text("You're on the latest version", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            is AppUpdateState.Downloading ->
                                Text("Downloading… ${s.progress}%", style = MaterialTheme.typography.bodySmall, color = Primary)
                            is AppUpdateState.ReadyToInstall ->
                                Text("Installing…", style = MaterialTheme.typography.bodySmall, color = Primary)
                            is AppUpdateState.Error ->
                                Text("Error: ${s.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            else -> {}
                        }
                        if (updateState is AppUpdateState.Downloading) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (updateState as AppUpdateState.Downloading).progress / 100f },
                                modifier = Modifier.fillMaxWidth(),
                                color = Primary,
                                trackColor = Primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                    if (updateState is AppUpdateState.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    }
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
                    Text("MangaHere", color = Primary, style = MaterialTheme.typography.bodyMedium)
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

private fun Long.toReadableSize(): String {
    val kb = this / 1024
    val mb = kb / 1024
    return if (mb > 0) "${mb}MB" else if (kb > 0) "${kb}KB" else "${this}B"
}
