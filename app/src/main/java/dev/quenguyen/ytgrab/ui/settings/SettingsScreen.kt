package dev.quenguyen.ytgrab.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quenguyen.ytgrab.model.FIXED_MP4_PRESET_HEIGHTS
import dev.quenguyen.ytgrab.model.MP3_QUALITY_PRESETS
import dev.quenguyen.ytgrab.model.MediaFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Default format", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.defaultFormat == MediaFormat.MP4,
                    onClick = { viewModel.setDefaultFormat(MediaFormat.MP4) },
                    label = { Text("MP4") },
                )
                FilterChip(
                    selected = settings.defaultFormat == MediaFormat.MP3,
                    onClick = { viewModel.setDefaultFormat(MediaFormat.MP3) },
                    label = { Text("MP3") },
                )
            }

            if (settings.defaultFormat == MediaFormat.MP4) {
                Text("Default video quality", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    FIXED_MP4_PRESET_HEIGHTS.sorted().forEach { h ->
                        FilterChip(
                            selected = settings.defaultHeightCap == h,
                            onClick = { viewModel.setDefaultHeightCap(h) },
                            label = { Text("${h}p") },
                        )
                    }
                    FilterChip(
                        selected = settings.defaultHeightCap == null,
                        onClick = { viewModel.setDefaultHeightCap(null) },
                        label = { Text("Best") },
                    )
                }
            } else {
                Text("Default audio quality", style = MaterialTheme.typography.titleSmall)
                Column {
                    MP3_QUALITY_PRESETS.forEach { preset ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings.defaultAudioQualityArg == preset.audioQualityArg,
                                onClick = { viewModel.setDefaultAudioQualityArg(preset.audioQualityArg) },
                            )
                            Text(preset.label)
                        }
                    }
                }
            }

            Text("Concurrent downloads", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..3).forEach { n ->
                    FilterChip(
                        selected = settings.concurrency == n,
                        onClick = { viewModel.setConcurrency(n) },
                        label = { Text(n.toString()) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Wi-Fi only (warn on mobile data)", style = MaterialTheme.typography.titleSmall)
                Switch(checked = settings.wifiOnly, onCheckedChange = viewModel::setWifiOnly)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("yt-dlp", style = MaterialTheme.typography.titleSmall)
                Text(
                    "YouTube changes frequently and can break extraction. If downloads start failing, update yt-dlp.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = viewModel::checkForYtDlpUpdate, enabled = !viewModel.isUpdating) {
                        Text("Check for update")
                    }
                    if (viewModel.isUpdating) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    viewModel.updateStatusText?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
