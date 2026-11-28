/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.twelve.ext.MUSIC_DIRECTORIES_DEFAULT
import org.lineageos.twelve.ext.THEME_MODE_AMOLED
import org.lineageos.twelve.ext.THEME_MODE_DARK
import org.lineageos.twelve.ext.THEME_MODE_LIGHT
import org.lineageos.twelve.ext.THEME_MODE_SYSTEM
import org.lineageos.twelve.ext.dynamicBackground
import org.lineageos.twelve.ext.dynamicColor
import org.lineageos.twelve.ext.enableDirectOutput
import org.lineageos.twelve.ext.enableFloatOutput
import org.lineageos.twelve.ext.enableOffload
import org.lineageos.twelve.ext.fullscreenMode
import org.lineageos.twelve.ext.musicDirectories
import org.lineageos.twelve.ext.skipSilence
import org.lineageos.twelve.ext.splitLocalDevices
import org.lineageos.twelve.ext.stopPlaybackOnTaskRemoved
import org.lineageos.twelve.ext.themeMode
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.ui.theme.TwelveExpressiveTheme
import org.lineageos.twelve.utils.AudioFolderInfo
import org.lineageos.twelve.utils.LocalMusicFilter
import org.lineageos.twelve.viewmodels.SettingsViewModel

/**
 * Material 3 Expressive Compose settings screen replacing legacy XML PreferenceFragmentCompat.
 */
class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TwelveExpressiveTheme {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateUp = { onBackPressedDispatcher.onBackPressed() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateUp: () -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val prefs = remember(context) {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val haptics = rememberTwelveHaptics()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var dynamicColor by remember { mutableStateOf(prefs.dynamicColor) }
    var dynamicBackground by remember { mutableStateOf(prefs.dynamicBackground) }
    var fullscreenMode by remember { mutableStateOf(prefs.fullscreenMode) }

    var enableOffload by remember { mutableStateOf(prefs.enableOffload) }
    var enableFloatOutput by remember { mutableStateOf(prefs.enableFloatOutput) }
    var enableDirectOutput by remember { mutableStateOf(prefs.enableDirectOutput) }
    var skipSilence by remember { mutableStateOf(prefs.skipSilence) }
    var stopOnTaskRemoved by remember { mutableStateOf(prefs.stopPlaybackOnTaskRemoved) }
    var splitLocalDevices by remember { mutableStateOf(prefs.splitLocalDevices) }
    var musicDirectories by remember { mutableStateOf(prefs.musicDirectories) }

    var showMusicDirectoriesDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var showRescanDialog by remember { mutableStateOf(false) }

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        treeUri ?: return@rememberLauncherForActivityResult
        val relPath = LocalMusicFilter.extractRelativePathFromTreeUri(treeUri)
        if (!relPath.isNullOrBlank()) {
            val updated = (musicDirectories + relPath).toSet()
            musicDirectories = updated
            prefs.musicDirectories = updated
            haptics.click()
            Toast.makeText(
                context,
                resources.getString(R.string.music_directories_added, relPath),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_activity_settings),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptics.click()
                            onNavigateUp()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                            contentDescription = null,
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsSection(title = stringResource(R.string.settings_appearance_header)) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.theme_mode),
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val themeOptions = listOf(
                        THEME_MODE_SYSTEM to stringResource(R.string.theme_mode_system),
                        THEME_MODE_LIGHT to stringResource(R.string.theme_mode_light),
                        THEME_MODE_DARK to stringResource(R.string.theme_mode_dark),
                        THEME_MODE_AMOLED to stringResource(R.string.theme_mode_amoled),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        themeOptions.forEach { (value, label) ->
                            val selected = themeMode == value
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptics.click()
                                        themeMode = value
                                        prefs.themeMode = value
                                    },
                                shape = CircleShape,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                            ) {
                                Text(
                                    text = label,
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    style = MaterialTheme.typography.labelMediumEmphasized,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SectionDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.dynamic_color),
                        summary = stringResource(R.string.dynamic_color_summary),
                        checked = dynamicColor,
                        onCheckedChange = {
                            haptics.click()
                            dynamicColor = it
                            prefs.dynamicColor = it
                        },
                    )
                }

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.dynamic_background),
                    summary = stringResource(R.string.dynamic_background_summary),
                    checked = dynamicBackground,
                    onCheckedChange = {
                        haptics.click()
                        dynamicBackground = it
                        prefs.dynamicBackground = it
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.fullscreen_mode),
                    summary = stringResource(R.string.fullscreen_mode_summary),
                    checked = fullscreenMode,
                    onCheckedChange = {
                        haptics.click()
                        fullscreenMode = it
                        prefs.fullscreenMode = it
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_audio_header)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.enable_direct_output),
                    summary = stringResource(R.string.enable_direct_output_summary),
                    checked = enableDirectOutput,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        enableDirectOutput = enabled
                        prefs.enableDirectOutput = enabled
                        scope.launch { viewModel.toggleDirectOutput(enabled) }
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.enable_float_output),
                    summary = stringResource(R.string.enable_float_output_summary),
                    checked = enableFloatOutput,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        enableFloatOutput = enabled
                        prefs.enableFloatOutput = enabled
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.enable_offload),
                    summary = stringResource(R.string.enable_offload_summary),
                    checked = enableOffload,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        enableOffload = enabled
                        prefs.enableOffload = enabled
                        scope.launch { viewModel.toggleOffload(enabled) }
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.skip_silence),
                    summary = stringResource(R.string.skip_silence_summary),
                    checked = skipSilence,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        skipSilence = enabled
                        prefs.skipSilence = enabled
                        scope.launch { viewModel.toggleSkipSilence(enabled) }
                    },
                )
            }

            SettingsSection(title = stringResource(R.string.settings_library_header)) {
                val directoriesSummary = if (musicDirectories.isEmpty()) {
                    stringResource(R.string.music_directories_summary_all)
                } else {
                    musicDirectories.sorted().joinToString(", ")
                }

                SettingsActionRow(
                    title = stringResource(R.string.music_directories),
                    summary = directoriesSummary,
                    iconRes = R.drawable.ic_lineage_library,
                    onClick = {
                        haptics.click()
                        showMusicDirectoriesDialog = true
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.stop_playback_on_task_removed),
                    summary = stringResource(R.string.stop_playback_on_task_removed_summary),
                    checked = stopOnTaskRemoved,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        stopOnTaskRemoved = enabled
                        prefs.stopPlaybackOnTaskRemoved = enabled
                    },
                )

                SectionDivider()
                SettingsSwitchRow(
                    title = stringResource(R.string.split_local_devices),
                    summary = stringResource(R.string.split_local_devices_summary),
                    checked = splitLocalDevices,
                    onCheckedChange = { enabled ->
                        haptics.click()
                        splitLocalDevices = enabled
                        prefs.splitLocalDevices = enabled
                    },
                )

                SectionDivider()
                SettingsActionRow(
                    title = stringResource(R.string.rescan_media_store),
                    iconRes = R.drawable.ic_sync,
                    onClick = {
                        haptics.click()
                        showRescanDialog = true
                    },
                )

                SectionDivider()
                SettingsActionRow(
                    title = stringResource(R.string.reset_local_stats),
                    iconRes = R.drawable.ic_delete,
                    onClick = {
                        haptics.click()
                        showResetStatsDialog = true
                    },
                )
            }
        }
    }

    if (showMusicDirectoriesDialog) {
        MusicDirectoriesDialog(
            selectedDirectories = musicDirectories,
            onUpdateDirectories = { updated ->
                musicDirectories = updated
                prefs.musicDirectories = updated
            },
            onAddFolderClick = {
                haptics.click()
                directoryPickerLauncher.launch(null)
            },
            onResetToMusicOnly = {
                haptics.click()
                musicDirectories = MUSIC_DIRECTORIES_DEFAULT
                prefs.musicDirectories = MUSIC_DIRECTORIES_DEFAULT
            },
            onDismiss = { showMusicDirectoriesDialog = false },
        )
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text(stringResource(R.string.reset_local_stats_confirm_title)) },
            text = { Text(stringResource(R.string.reset_local_stats_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetStatsDialog = false
                        haptics.click()
                        scope.launch {
                            viewModel.resetLocalStats()
                            Toast.makeText(
                                context,
                                R.string.reset_local_stats_success,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    },
                ) {
                    Text(stringResource(R.string.reset_local_stats_confirm_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }

    if (showRescanDialog) {
        AlertDialog(
            onDismissRequest = { showRescanDialog = false },
            title = { Text(stringResource(R.string.rescan_media_store_confirm_title)) },
            text = { Text(stringResource(R.string.rescan_media_store_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRescanDialog = false
                        haptics.click()
                        viewModel.rescanMediaStore()
                        Toast.makeText(
                            context,
                            R.string.rescan_media_store_started,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                ) {
                    Text(stringResource(R.string.rescan_media_store_confirm_positive))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsActionRow(
    title: String,
    @DrawableRes iconRes: Int,
    summary: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!summary.isNullOrBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MusicDirectoriesDialog(
    selectedDirectories: Set<String>,
    onUpdateDirectories: (Set<String>) -> Unit,
    onAddFolderClick: () -> Unit,
    onResetToMusicOnly: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberTwelveHaptics()
    var detectedFolders by remember { mutableStateOf<List<AudioFolderInfo>>(emptyList()) }

    LaunchedEffect(selectedDirectories) {
        detectedFolders = withContext(Dispatchers.IO) {
            LocalMusicFilter.queryAvailableAudioFolders(context.contentResolver)
        }
    }

    val folderEntries = remember(detectedFolders, selectedDirectories) {
        val map = LinkedHashMap<String, Int?>()
        detectedFolders.forEach { map[it.relativePath] = it.trackCount }
        selectedDirectories.sorted().forEach { customDir ->
            if (!map.containsKey(customDir)) {
                map[customDir] = null
            }
        }
        map.entries.map { (path, count) -> path to count }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.music_directories_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.music_directories_dialog_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = folderEntries,
                        key = { (path, _) -> path },
                    ) { (path, count) ->
                        val isChecked = selectedDirectories.any {
                            it.equals(path, ignoreCase = true)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptics.click()
                                    val updated = if (isChecked) {
                                        selectedDirectories.filterNot {
                                            it.equals(path, ignoreCase = true)
                                        }.toSet()
                                    } else {
                                        (selectedDirectories + path).toSet()
                                    }
                                    onUpdateDirectories(updated)
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    haptics.click()
                                    val updated = if (!checked) {
                                        selectedDirectories.filterNot {
                                            it.equals(path, ignoreCase = true)
                                        }.toSet()
                                    } else {
                                        (selectedDirectories + path).toSet()
                                    }
                                    onUpdateDirectories(updated)
                                },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (count != null) {
                                    Text(
                                        text = stringResource(
                                            R.string.music_directories_tracks_count,
                                            count,
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onAddFolderClick) {
                        Text(stringResource(R.string.music_directories_add_folder))
                    }
                    TextButton(onClick = onResetToMusicOnly) {
                        Text(stringResource(R.string.music_directories_reset_music))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.ok))
            }
        },
    )
}
