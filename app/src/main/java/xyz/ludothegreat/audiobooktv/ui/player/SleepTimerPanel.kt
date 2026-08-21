package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/**
 * Sleep-timer preset picker. Same control idiom as SpeedPanel: each
 * preset is a focusable Surface row, the currently-selected preset is
 * filled with primary (green) and outlined with secondary (orange) when
 * focused. No on-screen keyboard, no numeric entry - D-pad picks a
 * preset from a fixed list (decision: ship with the locked grain).
 */
@Composable
fun SleepTimerPanel(
    currentMinutes: Int,
    remainingSec: Long?,
    endOfChapter: Boolean,
    showEndOfChapter: Boolean,
    onSelect: (Int) -> Unit,
    onToggleEndOfChapter: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .focusable(false),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().focusGroup(),
            ) {
                Text(
                    text = "Sleep timer",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (remainingSec != null && remainingSec > 0) {
                    Text(
                        text = "Stops in ${formatRemaining(remainingSec)}",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                val initialFocus = remember { FocusRequester() }
                val currentIndex = SLEEP_TIMER_PRESETS_MINUTES.indexOf(currentMinutes)
                    .let { if (it == -1) 0 else it }

                SLEEP_TIMER_PRESETS_MINUTES.forEachIndexed { index, minutes ->
                    val isCurrent = minutes == currentMinutes
                    val mod = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .let { if (index == currentIndex) it.focusRequester(initialFocus) else it }

                    Surface(
                        onClick = {
                            onSelect(minutes)
                            onDismiss()
                        },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) colors.primary else colors.background,
                            contentColor = if (isCurrent) colors.onPrimary else colors.onSurface,
                            focusedContainerColor = if (isCurrent) colors.primary else colors.background,
                            focusedContentColor = if (isCurrent) colors.onPrimary else colors.onSurface,
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, colors.secondary),
                                shape = RoundedCornerShape(8.dp),
                            ),
                        ),
                        modifier = mod,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = formatPresetLabel(minutes),
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 20.dp),
                            )
                        }
                    }
                }

                // Only offered when the loaded book has chapter data: a
                // chapter stop can never fire on a chapterless book, so
                // showing the switch there would arm a promise the player
                // cannot keep. Toggling does NOT dismiss -- the point is
                // combining it with a minutes preset in one panel visit.
                if (showEndOfChapter) {
                    Spacer(modifier = Modifier.height(8.dp))
                    EndOfChapterRow(
                        endOfChapter = endOfChapter,
                        onToggle = { onToggleEndOfChapter(!endOfChapter) },
                        colors = colors,
                    )
                    if (endOfChapter && currentMinutes > 0) {
                        Text(
                            text = "Counts down $currentMinutes min, then stops at the chapter end",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }

                LaunchedEffect(Unit) { initialFocus.requestFocus() }
            }
        }
    }
}

/**
 * The end-of-chapter switch row. Same Surface idiom as the preset rows;
 * the trailing On/Off word makes the state readable at 10 feet without
 * relying on fill alone.
 */
@Composable
private fun EndOfChapterRow(
    endOfChapter: Boolean,
    onToggle: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Surface(
        onClick = onToggle,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (endOfChapter) colors.primary else colors.background,
            contentColor = if (endOfChapter) colors.onPrimary else colors.onSurface,
            focusedContainerColor = if (endOfChapter) colors.primary else colors.background,
            focusedContentColor = if (endOfChapter) colors.onPrimary else colors.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.secondary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "End of chapter", fontSize = 18.sp)
            Text(text = if (endOfChapter) "On" else "Off", fontSize = 14.sp)
        }
    }
}

private fun formatPresetLabel(minutes: Int): String = when (minutes) {
    0 -> "Off"
    1 -> "1 minute"
    else -> "$minutes minutes"
}

private fun formatRemaining(remainingSec: Long): String {
    val total = remainingSec.coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
