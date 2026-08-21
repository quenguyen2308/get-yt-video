package dev.quenguyen.ytgrab.ui.selection

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.quenguyen.ytgrab.model.FIXED_MP4_PRESET_HEIGHTS
import dev.quenguyen.ytgrab.model.MP3_QUALITY_PRESETS
import dev.quenguyen.ytgrab.model.MediaFormat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SelectionScreen(
    viewModel: SelectionViewModel,
    onBack: () -> Unit,
    onConfirmed: () -> Unit,
) {
    val context = LocalContext.current
    val metadata = viewModel.metadata

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(metadata?.title ?: "Choose format") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (metadata == null) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Nothing to show — go back and paste a link.")
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (metadata.isPlaylist) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${viewModel.selectedEntryIds.size} / ${metadata.entries.size} selected",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row {
                        Button(onClick = { viewModel.selectAll(true) }) { Text("All") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { viewModel.selectAll(false) }) { Text("None") }
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    items(metadata.entries) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = entry.id in viewModel.selectedEntryIds,
                                onCheckedChange = { viewModel.toggleEntry(entry.id) },
                            )
                            Text(entry.title, maxLines = 2, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Format", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = viewModel.format == MediaFormat.MP4,
                        onClick = { viewModel.onFormatSelected(MediaFormat.MP4) },
                        label = { Text("MP4 (video)") },
                    )
                    FilterChip(
                        selected = viewModel.format == MediaFormat.MP3,
                        onClick = { viewModel.onFormatSelected(MediaFormat.MP3) },
                        label = { Text("MP3 (audio)") },
                    )
                }

                Text("Quality", style = MaterialTheme.typography.titleSmall)
                if (viewModel.format == MediaFormat.MP4) {
                    val heights = metadata.availableHeights.ifEmpty { FIXED_MP4_PRESET_HEIGHTS }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        heights.sorted().forEach { h ->
                            FilterChip(
                                selected = viewModel.videoHeightCap == h,
                                onClick = { viewModel.onVideoHeightCapSelected(h) },
                                label = { Text("${h}p") },
                            )
                        }
                        FilterChip(
                            selected = viewModel.videoHeightCap == null,
                            onClick = { viewModel.onVideoHeightCapSelected(null) },
                            label = { Text("Best") },
                        )
                    }
                } else {
                    Column {
                        MP3_QUALITY_PRESETS.forEach { preset ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = viewModel.audioPreset == preset,
                                    onClick = { viewModel.onAudioPresetSelected(preset) },
                                )
                                Text(preset.label)
                            }
                        }
                    }
                }

                if (viewModel.audioLanguageOptions.isNotEmpty()) {
                    Text("Audio language", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        FilterChip(
                            selected = viewModel.audioLanguage == null,
                            onClick = { viewModel.onAudioLanguageSelected(null) },
                            label = { Text("Original") },
                        )
                        viewModel.audioLanguageOptions.forEach { option ->
                            FilterChip(
                                selected = viewModel.audioLanguage == option.code,
                                onClick = { viewModel.onAudioLanguageSelected(option.code) },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }

                val itemCount = if (metadata.isPlaylist) viewModel.selectedEntryIds.size else 1
                Button(
                    onClick = { if (viewModel.confirm(context)) onConfirmed() },
                    enabled = itemCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (itemCount > 1) "Download $itemCount items" else "Download")
                }
            }
        }
    }
}
