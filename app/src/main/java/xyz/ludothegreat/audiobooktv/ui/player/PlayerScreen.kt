package xyz.ludothegreat.audiobooktv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import xyz.ludothegreat.audiobooktv.R

@Composable
fun PlayerScreen(
    itemId: String?,
    coverUrl: String?,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            viewModel.load(itemId, coverUrl)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background).padding(48.dp)) {
        if (itemId.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.placeholder_now_playing),
                    color = colors.onBackground,
                    fontSize = 20.sp,
                )
            }
            return@Box
        }

        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            AsyncImage(
                model = state.coverUrl ?: coverUrl,
                contentDescription = state.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface),
            )

            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        text = state.title.ifEmpty { "Loading..." },
                        color = colors.onBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.author,
                        color = colors.onSurfaceVariant,
                        fontSize = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = state.chapterTitle.ifBlank { "" },
                        color = colors.primary,
                        fontSize = 18.sp,
                    )
                }

                Column {
                    ProgressRow(positionSec = state.positionSec, durationSec = state.durationSec, colors = colors)
                    Spacer(modifier = Modifier.height(24.dp))
                    ControlRow(
                        isPlaying = state.isPlaying,
                        speed = state.speed,
                        onSkipBack = viewModel::skipBack30,
                        onPlayPause = viewModel::togglePlayPause,
                        onSkipForward = viewModel::skipForward30,
                        onCycleSpeed = viewModel::openSpeedPanel,
                        onBookmark = { /* Phase 8 */ },
                        colors = colors,
                    )
                    state.error?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = msg, color = colors.error, fontSize = 14.sp)
                    }
                }
            }
        }

        if (state.speedPanelVisible) {
            SpeedPanel(
                currentSpeed = state.speed,
                onSelect = viewModel::setSpeed,
                onDismiss = viewModel::closeSpeedPanel,
            )
        }
    }
}

@Composable
private fun ProgressRow(positionSec: Long, durationSec: Long, colors: androidx.tv.material3.ColorScheme) {
    val fraction = if (durationSec > 0) positionSec.toFloat() / durationSec.toFloat() else 0f
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = formatTime(positionSec), color = colors.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .height(6.dp)
                .fillMaxWidth(0.7f)
                .background(colors.surface, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .background(colors.primary, RoundedCornerShape(3.dp)),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = formatTime(durationSec), color = colors.onSurfaceVariant, fontSize = 14.sp)
    }
}

@Composable
private fun ControlRow(
    isPlaying: Boolean,
    speed: Float,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onBookmark: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(label = "« 30", onClick = onSkipBack, colors = colors)
        ControlButton(label = if (isPlaying) "Pause" else "Play", onClick = onPlayPause, emphasised = true, colors = colors)
        ControlButton(label = "30 »", onClick = onSkipForward, colors = colors)
        ControlButton(label = formatSpeed(speed), onClick = onCycleSpeed, colors = colors)
        ControlButton(label = "Mark", onClick = onBookmark, colors = colors)
    }
}

@Composable
private fun ControlButton(
    label: String,
    onClick: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
    emphasised: Boolean = false,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.colors(
            containerColor = if (emphasised) colors.primary else colors.surface,
            contentColor = if (emphasised) colors.onPrimary else colors.onSurface,
            focusedContainerColor = colors.primary,
            focusedContentColor = colors.onPrimary,
        ),
        modifier = Modifier.height(52.dp),
    ) {
        Text(text = label, fontSize = 16.sp, color = Color.Unspecified)
    }
}

private fun formatTime(seconds: Long): String {
    if (seconds < 0) return "0:00"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) "%.0f".format(speed) else "%.2f".format(speed).trimEnd('0').trimEnd('.')
    return "${s}x"
}

