package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * focused. No on-screen keyboard, no numeric entry — D-pad picks a
 * preset from a fixed list (decision: ship with the locked grain).
 */
@Composable
fun SleepTimerPanel(
    currentMinutes: Int,
    remainingSec: Long?,
    onSelect: (Int) -> Unit,
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
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
                        .height(52.dp)
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

                LaunchedEffect(Unit) { initialFocus.requestFocus() }
            }
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
