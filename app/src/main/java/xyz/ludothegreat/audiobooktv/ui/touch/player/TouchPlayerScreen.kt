package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Toc
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            viewModel.load(itemId, coverUrl)
        }
    }

    NoticeSnackbars(state = state, hostState = snackbarHostState, onUndo = viewModel::undoSeek)

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

            val chapterIndex = ChapterMath.indexAt(state.positionSec.toDouble(), state.chapters)
            if (chapterIndex != null) {
                ChapterRow(
                    title = state.chapterTitle.ifBlank { "Chapter ${chapterIndex + 1}" },
                    positionSec = state.positionSec,
                    chapter = state.chapters[chapterIndex],
                    speed = state.speed,
                )
            }

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
                showChapters = state.chapters.isNotEmpty(),
                undoAvailable = state.undoSeekTargetSec != null,
                onSpeedClick = viewModel::openSpeedPanel,
                onSleepClick = viewModel::openSleepTimerPanel,
                onBookmarkClick = viewModel::openBookmarkPanel,
                onChaptersClick = viewModel::openChapterPanel,
                onUndoClick = viewModel::undoSeek,
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    PlayerSheets(state = state, viewModel = viewModel)
}

@Composable
private fun PlayerSheets(state: PlayerUiState, viewModel: PlayerViewModel) {
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
    if (state.chapterPanelVisible) {
        TouchChapterSheet(
            chapters = state.chapters,
            currentIndex = ChapterMath.indexAt(state.positionSec.toDouble(), state.chapters),
            onPick = { chapter ->
                viewModel.jumpToChapter(chapter)
                viewModel.closeChapterPanel()
            },
            onDismiss = viewModel::closeChapterPanel,
        )
    }
    if (state.bookmarkPanelVisible) {
        TouchBookmarkSheet(
            bookmarks = state.bookmarks,
            loading = state.bookmarksLoading,
            currentPositionSec = state.positionSec,
            notice = state.bookmarkNotice,
            onAddHere = viewModel::addBookmarkHere,
            onRename = viewModel::renameBookmark,
            onDelete = viewModel::deleteBookmark,
            onJump = { bookmark ->
                viewModel.jumpToBookmark(bookmark)
                viewModel.closeBookmarkPanel()
            },
            onDismiss = viewModel::closeBookmarkPanel,
        )
    }
}

/**
 * Keyed on seq so every seek re-fires; the freshness check stops a stale
 * notice from replaying when a tab switch re-enters this composition with
 * the same state. Undo runs through the ViewModel so it is a normal
 * clamp-seek-push seek, which in turn raises the next notice (redo).
 */
@Composable
private fun NoticeSnackbars(
    state: PlayerUiState,
    hostState: SnackbarHostState,
    onUndo: () -> Unit,
) {
    LaunchedEffect(state.seekNotice?.seq) {
        val notice = state.seekNotice ?: return@LaunchedEffect
        if (System.currentTimeMillis() - notice.atMs > NOTICE_FRESH_MS) return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = "Jumped to ${formatTimestampHms(notice.toSec)}",
            actionLabel = "Undo",
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) onUndo()
    }
    LaunchedEffect(state.bookmarkNotice?.seq) {
        val notice = state.bookmarkNotice ?: return@LaunchedEffect
        if (System.currentTimeMillis() - notice.atMs > NOTICE_FRESH_MS) return@LaunchedEffect
        hostState.showSnackbar(message = notice.text, duration = SnackbarDuration.Short)
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

/**
 * Chapter half of the dual position display: the current chapter title over
 * a thin bar in the dim primary token (the scrubber's quieter sibling),
 * elapsed-in-chapter on the left and the "-12:34" until-next-chapter
 * countdown (at the playback rate) on the right. Only rendered while the
 * head is inside a chapter, so chapterless books keep the plain
 * scrubber-only layout.
 */
@Composable
private fun ChapterRow(title: String, positionSec: Long, chapter: AbsChapter, speed: Float) {
    val colors = MaterialTheme.colorScheme
    val absSec = positionSec.toDouble()
    Column {
        Text(
            text = title,
            color = colors.primary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(4.dp)
                .fillMaxWidth()
                .background(colors.surfaceVariant, RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(ChapterMath.progressFraction(absSec, chapter))
                    .background(colors.primaryContainer, RoundedCornerShape(2.dp)),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTimestampHms(ChapterMath.elapsedSec(absSec, chapter).toLong()),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = ChapterMath.remainingLabel(ChapterMath.remainingSecAtSpeed(absSec, chapter, speed)),
                color = colors.onSurfaceVariant,
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
    // coerceAtLeast(1) only keeps the range legal for Slider; it does NOT make
    // a 0-duration book scrubbable. Gate on the real duration instead, or a
    // drag during load reaches seekToAbsoluteSec with a meaningless target.
    val durationKnown = durationSec > 0
    val maxSec = durationSec.coerceAtLeast(1)
    Column {
        Slider(
            enabled = durationKnown,
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
    showChapters: Boolean,
    undoAvailable: Boolean,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onUndoClick: () -> Unit,
) {
    val sleepLabel = formatSleepLabel(
        selectedMinutes = sleepTimerMinutes,
        remainingSec = sleepTimerRemainingSec,
    )
    // Four chips overflow a narrow phone (the foldable cover screen is ~370dp),
    // so the row scrolls sideways. horizontalScroll only relaxes the max
    // constraint: with three chips the layout is untouched and stays
    // centered, exactly as it was before the Chapters chip existed.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        if (showChapters) {
            AssistChip(
                onClick = onChaptersClick,
                label = { Text(text = "Chapters") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Toc,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
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
        // Persistent undo entry point; the post-seek snackbar is transient
        // and easy to miss mid-listen.
        if (undoAvailable) {
            AssistChip(
                onClick = onUndoClick,
                label = { Text(text = "Undo") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo last seek",
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}

private const val NOTICE_FRESH_MS = 5_000L
