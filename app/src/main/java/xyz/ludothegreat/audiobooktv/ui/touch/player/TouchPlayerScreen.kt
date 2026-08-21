package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Toc
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import xyz.ludothegreat.audiobooktv.R
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.playback.BookProgress
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
import xyz.ludothegreat.audiobooktv.playback.SeekTargets
import xyz.ludothegreat.audiobooktv.playback.formatSleepLabel
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import xyz.ludothegreat.audiobooktv.ui.common.CoverArt
import xyz.ludothegreat.audiobooktv.ui.player.PlayerUiState
import xyz.ludothegreat.audiobooktv.ui.player.PlayerViewModel
import xyz.ludothegreat.audiobooktv.ui.player.SkipLabels

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
            // The cover is the ONLY flexible element: it gets whatever height
            // is left after the fixed stack below (metadata, chapter bar,
            // scrubber, transport, chips) measures, because weighted children
            // size last. The old chain sized the cover first and its
            // fillMaxWidth().widthIn(max) cap was dead code (fillMaxWidth pins
            // min = max, so widthIn cannot shrink it): on the foldable inner
            // display that produced a full-width square cover which
            // pushed the transport row and the chips clean off the bottom
            // edge, measuring them at zero height.
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CoverPlayToggle(
                    state = state,
                    fallbackCoverUrl = coverUrl,
                    onToggle = viewModel::togglePlayPause,
                )
            }

            MetadataBlock(state = state)

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
                speed = state.speed,
                labeled = chapterIndex != null,
                onScrub = viewModel::seekToAbsoluteSec,
            )

            PrimaryControls(
                isPlaying = state.isPlaying,
                onLongSkipBack = viewModel::skipBackLong,
                onSkipBack = viewModel::skipBack30,
                onPlayPause = viewModel::togglePlayPause,
                onSkipForward = viewModel::skipForward30,
                onLongSkipForward = viewModel::skipForwardLong,
            )

            SecondaryChips(
                speed = state.speed,
                sleepLabel = formatSleepLabel(
                    selectedMinutes = state.sleepTimerMinutes,
                    remainingSec = state.sleepTimerRemainingSec,
                    endOfChapter = state.sleepEndOfChapter && state.chapters.isNotEmpty(),
                    eocWaiting = state.sleepEocWaiting,
                ),
                showChapters = state.chapters.isNotEmpty(),
                chaptersLabel = ChapterMath.counterLabel(chapterIndex, state.chapters.size),
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
            endOfChapter = state.sleepEndOfChapter,
            showEndOfChapter = state.chapters.isNotEmpty(),
            onPick = { minutes ->
                viewModel.setSleepTimerMinutes(minutes)
                viewModel.closeSleepTimerPanel()
            },
            onToggleEndOfChapter = viewModel::setSleepEndOfChapter,
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

/**
 * The cover doubles as the biggest play/pause target on the screen -- the
 * eyes-free tap the category's reference apps are loved for. A tap toggles
 * playback through the same togglePlayPause path as the transport button
 * (pre-play server refresh included) and flashes a short scrim-circle
 * acknowledgement showing the action taken, because the audible result can
 * lag the tap by a beat while the position refresh runs. The scrubber and
 * chip targets around it are untouched.
 */
@Composable
private fun CoverPlayToggle(
    state: PlayerUiState,
    fallbackCoverUrl: String?,
    onToggle: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // ackPlaying records the action ISSUED (true = play requested), not the
    // eventual player state: togglePlayPause resolves asynchronously after
    // its pre-play server check, and the acknowledgement must not wait.
    var ackPlaying by remember { mutableStateOf<Boolean?>(null) }
    var ackSeq by remember { mutableStateOf(0) }
    LaunchedEffect(ackSeq) {
        if (ackSeq > 0) {
            delay(COVER_ACK_MS)
            ackPlaying = null
        }
    }
    Box(
        modifier = Modifier
            .sizeIn(maxWidth = 360.dp, maxHeight = 360.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = if (state.isPlaying) "Pause" else "Play") {
                ackPlaying = !state.isPlaying
                ackSeq++
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        CoverArt(
            model = state.coverUrl ?: fallbackCoverUrl,
            contentDescription = state.title,
            title = state.title,
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
            initialsSize = 64.sp,
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = ackPlaying != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(colors.background.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (ackPlaying == true) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    // The clickable's onClickLabel already announces the
                    // action; a second description here would double-speak.
                    contentDescription = null,
                    tint = colors.onBackground,
                    modifier = Modifier.size(48.dp),
                )
            }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BarTag(text = "CHAPTER")
                Text(
                    text = formatTimestampHms(ChapterMath.elapsedSec(absSec, chapter).toLong()),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                text = ChapterMath.remainingLabel(ChapterMath.remainingSecAtSpeed(absSec, chapter, speed)),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Muted tag naming which bar is which, shown only while both bars are on
 * screen (the chapter row's presence is what makes the pairing
 * ambiguous). Sits inline with the under-bar timestamps so it costs no
 * vertical space. 11sp + medium weight instead of the original 9sp: the
 * tag was the smallest text on the screen and dropped out first at a
 * glance. Still a step under the timestamps and in the muted tone.
 */
@Composable
private fun BarTag(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        maxLines = 1,
    )
}

@Composable
private fun ScrubberRow(
    positionSec: Long,
    durationSec: Long,
    speed: Float,
    labeled: Boolean,
    onScrub: (Long) -> Unit,
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (labeled) {
                    BarTag(text = "BOOK")
                }
                Text(
                    text = formatTimestampHms(displaySec),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            // Same right-endpoint convention as the chapter bar and the TV
            // book bar: speed-aware negative countdown plus book percent.
            // A drag shows the countdown from the drag position live.
            Text(
                text = if (durationKnown) {
                    val remaining = BookProgress.remainingSecAtSpeed(displaySec, durationSec, speed)
                    "${ChapterMath.remainingLabel(remaining)} · ${BookProgress.percent(displaySec, durationSec)}%"
                } else {
                    formatTimestampHms(0)
                },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

/**
 * Transport row, magnitudes growing outward from Play like the TV row:
 * 5m, 30s, Play, 30s, 5m. Every skip control shares one pattern: glyph on
 * top, value-plus-unit tag from SkipLabels underneath ("30s", "5m"), the
 * same text pattern the TV rows use. The 30s pair wears the un-numbered
 * Replay glyph (mirrored for forward) instead of Replay30/Forward30, so
 * the only quantity on the button is the labeled one; the old in-glyph
 * "30" carried no unit, which is exactly the inconsistency this fixes.
 * 12dp spacing plus the 44dp long-skip targets keep the five-button row
 * inside the foldable cover screen's ~330dp of usable width.
 */
@Composable
private fun PrimaryControls(
    isPlaying: Boolean,
    onLongSkipBack: () -> Unit,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onLongSkipForward: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SkipButton(
            icon = Icons.Filled.FastRewind,
            mirror = false,
            label = SkipLabels.amount(SeekTargets.LONG_SKIP_SECONDS),
            contentDescription = "Skip back 5 minutes",
            buttonSize = 44.dp,
            iconSize = 20.dp,
            onClick = onLongSkipBack,
        )
        SkipButton(
            icon = Icons.Filled.Replay,
            mirror = false,
            label = SkipLabels.amount(SeekTargets.SKIP_SECONDS),
            contentDescription = "Skip back 30 seconds",
            buttonSize = 56.dp,
            iconSize = 28.dp,
            onClick = onSkipBack,
        )
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
        SkipButton(
            icon = Icons.Filled.Replay,
            mirror = true,
            label = SkipLabels.amount(SeekTargets.SKIP_SECONDS),
            contentDescription = "Skip forward 30 seconds",
            buttonSize = 56.dp,
            iconSize = 28.dp,
            onClick = onSkipForward,
        )
        SkipButton(
            icon = Icons.Filled.FastForward,
            mirror = false,
            label = SkipLabels.amount(SeekTargets.LONG_SKIP_SECONDS),
            contentDescription = "Skip forward 5 minutes",
            buttonSize = 44.dp,
            iconSize = 20.dp,
            onClick = onLongSkipForward,
        )
    }
}

/**
 * One skip control shape for every magnitude: glyph over a unit tag. The
 * Material set has no un-numbered forward-replay glyph, so the forward 30s
 * button mirrors Replay horizontally ([mirror]); the arc reads clockwise,
 * which is the forward cue. Sizes scale with magnitude (Play largest, 30s
 * next, 5m outermost and smallest) so the visual hierarchy still matches
 * the jump hierarchy.
 */
@Composable
private fun SkipButton(
    icon: ImageVector,
    mirror: Boolean,
    label: String,
    contentDescription: String,
    buttonSize: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = colors.onBackground,
                modifier = Modifier
                    .size(iconSize)
                    .scale(scaleX = if (mirror) -1f else 1f, scaleY = 1f),
            )
            Text(
                text = label,
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SecondaryChips(
    speed: Float,
    sleepLabel: String,
    showChapters: Boolean,
    chaptersLabel: String,
    undoAvailable: Boolean,
    onSpeedClick: () -> Unit,
    onSleepClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onChaptersClick: () -> Unit,
    onUndoClick: () -> Unit,
) {
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
                label = { Text(text = chaptersLabel) },
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
private const val COVER_ACK_MS = 650L
