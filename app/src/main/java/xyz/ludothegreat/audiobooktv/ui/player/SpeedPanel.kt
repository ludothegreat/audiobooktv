package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
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
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

@Composable
fun SpeedPanel(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
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
                .width(380.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Playback speed",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val initialFocus = remember { FocusRequester() }
                val currentIndex = SPEED_PRESETS.indexOfFirst { it == currentSpeed }
                    .let { if (it == -1) 1 else it }

                SPEED_PRESETS.forEachIndexed { index, preset ->
                    val isCurrent = preset == currentSpeed
                    val mod = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .let { if (index == currentIndex) it.focusRequester(initialFocus) else it }

                    Surface(
                        onClick = {
                            onSelect(preset)
                            onDismiss()
                        },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) colors.primaryContainer else colors.background,
                            contentColor = if (isCurrent) colors.onPrimaryContainer else colors.onSurface,
                            focusedContainerColor = colors.primary,
                            focusedContentColor = colors.onPrimary,
                        ),
                        modifier = mod,
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(
                                text = formatSpeedLabel(preset),
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

private fun formatSpeedLabel(speed: Float): String {
    val s = if (speed % 1f == 0f) "%.0f".format(speed) else "%.2f".format(speed).trimEnd('0').trimEnd('.')
    return "${s}x"
}
