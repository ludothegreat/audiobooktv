package xyz.ludothegreat.audiobooktv.ui.touch.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.ludothegreat.audiobooktv.ui.settings.SettingsViewModel
import xyz.ludothegreat.audiobooktv.ui.theme.AppTheme

@Composable
fun TouchSettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Settings",
                color = colors.onBackground,
                style = MaterialTheme.typography.headlineSmall,
            )
            InfoRow(label = "Server", value = state.serverUrl)
            InfoRow(label = "User", value = state.username)

            HorizontalDivider()

            ToggleRow(
                label = "Stop playback when app closes",
                description = "If off, audio keeps playing after you leave the app.",
                checked = state.stopOnAppClose,
                onChange = viewModel::setStopOnAppClose,
            )
            ToggleRow(
                label = "Diagnostic log",
                description = "Off by default. When on, writes a rolling debug log file.",
                checked = state.diagnosticLogEnabled,
                onChange = viewModel::setDiagnosticLogEnabled,
            )
            ThemeRow(selected = state.selectedTheme, onSelect = viewModel::setTheme)

            HorizontalDivider()

            Button(
                onClick = viewModel::refreshLibrary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Refresh library")
            }
            state.refreshFeedback?.let { msg ->
                Text(
                    text = msg,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = viewModel::logout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Log out")
            }
            Text(
                text = "audiobooktv ${state.versionName}",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(text = label, color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value.ifBlank { "-" },
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.onBackground, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ThemeRow(selected: AppTheme, onSelect: (AppTheme) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(text = "Theme", color = colors.onBackground, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTheme.entries.forEach { theme ->
                FilterChip(
                    selected = theme == selected,
                    onClick = { onSelect(theme) },
                    label = { Text(text = theme.displayName) },
                )
            }
        }
    }
}
