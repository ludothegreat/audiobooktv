package xyz.ludothegreat.audiobooktv.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import xyz.ludothegreat.audiobooktv.ui.theme.AppTheme

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(modifier = Modifier.fillMaxSize().background(colors.background).padding(horizontal = 32.dp, vertical = 24.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

            ToggleRow(
                label = "Diagnostic log",
                description = "Write a rolling log to the app's external files directory. Off by default.",
                checked = state.diagnosticLogEnabled,
                onChange = viewModel::setDiagnosticLogEnabled,
                colors = colors,
            )

            ThemeRow(
                selected = state.selectedTheme,
                onSelect = viewModel::setTheme,
                colors = colors,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = { viewModel.refreshLibrary() },
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = colors.surface,
                        contentColor = colors.onSurface,
                        focusedContainerColor = colors.surface,
                        focusedContentColor = colors.onSurface,
                    ),
                    border = ButtonDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, colors.secondary),
                            shape = RoundedCornerShape(8.dp),
                        ),
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(text = "Refresh library", fontSize = 16.sp)
                }
                state.refreshFeedback?.let { msg ->
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = msg, color = colors.primary, fontSize = 14.sp)
                }
            }

            Button(
                onClick = { viewModel.logout() },
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.colors(
                    containerColor = colors.surface,
                    contentColor = colors.error,
                    focusedContainerColor = colors.surface,
                    focusedContentColor = colors.error,
                ),
                border = ButtonDefaults.border(
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, colors.error),
                        shape = RoundedCornerShape(8.dp),
                    ),
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Text(text = "Log out", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))
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
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        Box(
            modifier = Modifier
                .border(
                    width = 2.dp,
                    color = if (isFocused) colors.secondary else Color.Transparent,
                    shape = RoundedCornerShape(50),
                )
                .padding(2.dp),
        ) {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                interactionSource = interactionSource,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = colors.primary,
                    checkedThumbColor = colors.onPrimary,
                    uncheckedTrackColor = colors.surface,
                    uncheckedThumbColor = colors.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun ThemeRow(
    selected: AppTheme,
    onSelect: (AppTheme) -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Theme", color = colors.onBackground, fontSize = 16.sp)
            Text(
                text = "Color scheme used across the app.",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTheme.entries.forEach { theme ->
                val isSelected = theme == selected
                Button(
                    onClick = { onSelect(theme) },
                    shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) colors.primary else colors.surface,
                        contentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                        focusedContainerColor = if (isSelected) colors.primary else colors.surface,
                        focusedContentColor = if (isSelected) colors.onPrimary else colors.onSurface,
                    ),
                    border = ButtonDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, colors.secondary),
                            shape = RoundedCornerShape(8.dp),
                        ),
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Text(text = theme.displayName, fontSize = 14.sp)
                }
            }
        }
    }
}
