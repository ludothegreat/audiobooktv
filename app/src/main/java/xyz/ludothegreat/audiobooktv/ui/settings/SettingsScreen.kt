package xyz.ludothegreat.audiobooktv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(colors.background).padding(48.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(text = "Settings", color = colors.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Medium)

            InfoRow(label = "Server", value = state.serverUrl, colors = colors)
            InfoRow(label = "User", value = state.username, colors = colors)

            ToggleRow(
                label = "Stop playback when app closes",
                description = "If off, audio keeps playing in the background after you exit the app.",
                checked = state.stopOnAppClose,
                onChange = viewModel::setStopOnAppClose,
                colors = colors,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.colors(
                    containerColor = colors.surface,
                    contentColor = colors.error,
                    focusedContainerColor = colors.error,
                    focusedContentColor = colors.onError,
                ),
                modifier = Modifier.width(220.dp).height(52.dp),
            ) {
                Text(text = "Log out", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.fillMaxWidth().weight(1f))
            Text(
                text = "audiobooktv ${state.versionName}",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, colors: androidx.tv.material3.ColorScheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, color = colors.onSurfaceVariant, fontSize = 16.sp, modifier = Modifier.width(160.dp))
        Text(text = value, color = colors.onBackground, fontSize = 16.sp)
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = colors.onBackground, fontSize = 16.sp)
            Text(text = description, color = colors.onSurfaceVariant, fontSize = 13.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.primary,
                checkedThumbColor = colors.onPrimary,
                uncheckedTrackColor = colors.surface,
                uncheckedThumbColor = colors.onSurfaceVariant,
            ),
        )
    }
}
