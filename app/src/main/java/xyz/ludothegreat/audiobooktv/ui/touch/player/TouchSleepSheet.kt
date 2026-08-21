package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ludothegreat.audiobooktv.ui.player.SLEEP_TIMER_PRESETS_MINUTES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchSleepSheet(
    currentMinutes: Int,
    endOfChapter: Boolean,
    showEndOfChapter: Boolean,
    onPick: (Int) -> Unit,
    onToggleEndOfChapter: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SLEEP_TIMER_PRESETS_MINUTES.forEach { minutes ->
                val selected = minutes == currentMinutes
                val label = if (minutes == 0) "Off" else "${minutes}m"
                FilterChip(
                    selected = selected,
                    onClick = { onPick(minutes) },
                    label = { Text(text = label) },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
            }
            // Hidden for chapterless books: a chapter stop can never fire
            // there. Toggling keeps the sheet open so it can be combined
            // with a minutes preset in one visit; picking a preset (or Off,
            // which also clears this flag) is what dismisses.
            if (showEndOfChapter) {
                FilterChip(
                    selected = endOfChapter,
                    onClick = { onToggleEndOfChapter(!endOfChapter) },
                    label = { Text(text = "End of chapter") },
                    leadingIcon = if (endOfChapter) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else {
                        null
                    },
                )
                if (endOfChapter && currentMinutes > 0) {
                    Text(
                        text = "Counts down $currentMinutes min, then stops at the chapter end",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
