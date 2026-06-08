package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import xyz.ludothegreat.audiobooktv.R
import xyz.ludothegreat.audiobooktv.playback.formatSleepLabel
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import xyz.ludothegreat.audiobooktv.ui.player.PlayerUiState
import xyz.ludothegreat.audiobooktv.ui.player.PlayerViewModel

/**
 * Touch player. Shares PlayerViewModel with the TV surface (decision: one
 * VM, two presentations) -- every cross-device-resume invariant is upheld
 * because the underlying VM/repository code is identical.
 *
 * Scrubber UX detail: while the user is dragging, local state takes the
 * slider value so the thumb doesn't fight the 1-Hz ticker. On release the
 * value is committed via viewModel.seekToAbsoluteSec, which clamps, seeks,
 * and pushes the new position to ABS in one step.
 */
@Composable
fun TouchPlayerScreen(
    itemId: String?,
    coverUrl: String?,
    onOpenLibrary: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            viewModel.load(itemId, coverUrl)
        }
    }

    if (itemId.isNullOrBlank()) {
        EmptyPlayer(onOpenLibrary)
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AsyncImage(
                model = state.coverUrl ?: coverUrl,
                contentDescription = state.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface),
            )

            MetadataBlock(state = state)

            Spacer(modifier = Modifier.weight(1f))

            ScrubberRow(
                positionSec = state.positionSec,
                durationSec = state.durationSec,
                onScrub = viewModel::seekToAbsoluteSec,
            )

            PrimaryControls(
                isPlaying = state.isPlaying,
                onSkipBack = viewModel::skipBack30,
                onPlayPause = viewModel::togglePlayPause,
                onSkipForward = viewModel::skipForward30,
            )

            SecondaryChips(
                speed = state.speed,
                sleepTimerMinutes = state.sleepTimerMinutes,
                sleepTimerRemainingSec = state.sleepTimerRemainingSec,
                onSpeedClick = viewModel::openSpeedPanel,
                onSleepClick = viewModel::openSleepTimerPanel,
                onBookmarkClick = viewModel::openBookmarkPanel,
            )
        }
    }

    if (state.speedPanelVisible) {
        TouchSpeedSheet(
            currentSpeed = state.speed,
            onPick = { picked ->
                viewModel.setSpeed(picked)
                viewModel.closeSpeedPanel()
            },
            onDismiss = viewModel::closeSpeedPanel,
        )
    }
    if (state.sleepTimerPanelVisible) {
        TouchSleepSheet(
            currentMinutes = state.sleepTimerMinutes,
            onPick = { minutes ->
                viewModel.setSleepTimerMinutes(minutes)
                viewModel.closeSleepTimerPanel()
            },
            onDismiss = viewModel::closeSleepTimerPanel,
        )
    }
    if (state.bookmarkPanelVisible) {
        TouchBookmarkSheet(
            bookmarks = state.bookmarks,
            loading = state.bookmarksLoading,
            currentPositionSec = state.positionSec,
            onAddHere = viewModel::addBookmarkHere,
            onJump = { bookmark ->
                viewModel.jumpToBookmark(bookmark)
                viewModel.closeBookmarkPanel()
            },
            onDismiss = viewModel::closeBookmarkPanel,
        )
    }
}

@Composable
private fun EmptyPlayer(onOpenLibrary: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.placeholder_now_playing),
                color = colors.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            AssistChip(onClick = onOpenLibrary, label = { Text(text = stringResource(R.string.nav_library)) })
        }
    }
}

@Composable
private fun MetadataBlock(state: PlayerUiState) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            text = state.title.ifEmpty { "Loading..." },
            color = colors.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.author.isNotBlank()) {
            Text(
                text = state.author,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        if (state.chapterTitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.chapterTitle,
                color = colors.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.isReconnecting) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Reconnecting...",
                color = colors.error,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ScrubberRow(positionSec: Long, durationSec: Long, onScrub: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var dragging by remember { mutableStateOf(false) }
    var dragValueSec by remember { mutableStateOf(positionSec) }
    // Reset the local drag value to the live position whenever the ticker
    // advances and the user is NOT currently dragging. Without this, the
    // slider would freeze at the last drag value forever.
    LaunchedEffect(positionSec, dragging) {
        if (!dragging) dragValueSec = positionSec
    }
    val displaySec = if (dragging) dragValueSec else positionSec
    val maxSec = durationSec.coerceAtLeast(1)
    Column {
        Slider(
            value = displaySec.toFloat().coerceIn(0f, maxSec.toFloat()),
            valueRange = 0f..maxSec.toFloat(),
            onValueChange = { v ->
                dragging = true
                dragValueSec = v.toLong()
            },
            onValueChangeFinished = {
                onScrub(dragValueSec)
                dragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.primary,
                activeTrackColor = colors.primary,
                inactiveTrackColor = colors.surfaceVariant,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTimestampHms(displaySec),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatTimestampHms(durationSec),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PrimaryControls(
    isPlaying: Boolean,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onSkipBack,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Replay30,
                contentDescription = "Skip back 30 seconds",
                tint = colors.onBackground,
                modifier = Modifier.size(36.dp),
            )
        }
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(40.dp),
            )
        }
        IconButton(
            onClick = onSkipForward,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Forward30,
                contentDescription = "Skip forward 30 seconds",
                tint = colors.onBackground,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun SecondaryChips(
    speed: Float,
    sleepTimerMinutes: Int,
    sleepTimerRemainingSec: Long?,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    onBookmarkClick: () -> Unit,
) {
    val sleepLabel = formatSleepLabel(
        selectedMinutes = sleepTimerMinutes,
        remainingSec = sleepTimerRemainingSec,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        AssistChip(
            onClick = onSpeedClick,
            label = { Text(text = "%.2fx".format(speed)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        AssistChip(
            onClick = onSleepClick,
            label = { Text(text = sleepLabel) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.NightsStay,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
        AssistChip(
            onClick = onBookmarkClick,
            label = { Text(text = "Bookmark") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    }
}
