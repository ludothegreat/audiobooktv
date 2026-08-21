package xyz.ludothegreat.audiobooktv.ui.player

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import xyz.ludothegreat.audiobooktv.R
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.playback.BookProgress
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
import xyz.ludothegreat.audiobooktv.playback.SeekTargets
import xyz.ludothegreat.audiobooktv.playback.formatSleepLabel
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import xyz.ludothegreat.audiobooktv.ui.common.CoverArt

@Composable
fun PlayerScreen(
    itemId: String?,
    coverUrl: String?,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val chapterIndex = ChapterMath.indexAt(state.positionSec.toDouble(), state.chapters)

    LaunchedEffect(itemId) {
        if (!itemId.isNullOrBlank()) {
            viewModel.load(itemId, coverUrl)
        }
    }

    // 40dp keeps everything comfortably inside the TV action-safe area
    // (48px at 1080p) while giving the nine-control row the width it
    // needs next to the cover.
    Box(modifier = Modifier.fillMaxSize().background(colors.background).padding(40.dp)) {
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

        // Cover width and gap are sized around the control row. The TV device
        // renders 1920x1080 at density 320 (960dp), and this screen mounts
        // INSIDE RootScaffold's 120dp nav rail, so the column beside the
        // 248dp cover and 24dp gap gets 840 - 80 - 248 - 24 = 488dp. That
        // budget fits SEVEN compact controls in the worst state (Pause +
        // 1.75x + 59:59+ + Ch 10/30 under NeonLightning's monospace is
        // ~482dp); it does NOT fit nine, which is why the 5m long-skip
        // pair lives on the jump row below with Undo. Verified on-device:
        // nine in one row ellipsized "Ch 10/30" to "Ch ..." -- the
        // ControlButton ellipsis canary remains the tripwire for drift.
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CoverArt(
                model = state.coverUrl ?: coverUrl,
                contentDescription = state.title,
                title = state.title,
                containerColor = colors.surfaceVariant,
                contentColor = colors.onSurfaceVariant,
                initialsSize = 64.sp,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(248.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
                    if (state.isReconnecting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Reconnecting...",
                            color = colors.error,
                            fontSize = 14.sp,
                        )
                    }
                }

                Column {
                    if (chapterIndex != null) {
                        // The VM keeps chapterTitle in step with playback; the
                        // numbered fallback covers ABS chapters with no title.
                        Text(
                            text = state.chapterTitle.ifBlank { "Chapter ${chapterIndex + 1}" },
                            color = colors.primary,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ChapterProgressRow(
                            positionSec = state.positionSec,
                            chapter = state.chapters[chapterIndex],
                            speed = state.speed,
                            colors = colors,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    ProgressRow(
                        positionSec = state.positionSec,
                        durationSec = state.durationSec,
                        speed = state.speed,
                        labeled = chapterIndex != null,
                        colors = colors,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    ControlRow(
                        isPlaying = state.isPlaying,
                        speed = state.speed,
                        sleepLabel = formatSleepLabel(
                            selectedMinutes = state.sleepTimerMinutes,
                            remainingSec = state.sleepTimerRemainingSec,
                            endOfChapter = state.sleepEndOfChapter && state.chapters.isNotEmpty(),
                            eocWaiting = state.sleepEocWaiting,
                        ),
                        onSkipBack = viewModel::skipBack30,
                        onPlayPause = viewModel::togglePlayPause,
                        onSkipForward = viewModel::skipForward30,
                        onCycleSpeed = viewModel::openSpeedPanel,
                        onBookmark = viewModel::openBookmarkPanel,
                        onSleepTimer = viewModel::openSleepTimerPanel,
                        chaptersControl = if (state.chapters.isNotEmpty()) {
                            ChaptersControl(
                                label = ChapterMath.counterLabel(chapterIndex, state.chapters.size),
                                onOpen = viewModel::openChapterPanel,
                            )
                        } else {
                            null
                        },
                        colors = colors,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    JumpRow(
                        onLongSkipBack = viewModel::skipBackLong,
                        onLongSkipForward = viewModel::skipForwardLong,
                        undoTargetSec = state.undoSeekTargetSec,
                        onUndo = viewModel::undoSeek,
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

        if (state.bookmarkPanelVisible) {
            BookmarkPanel(
                positionSec = state.positionSec,
                bookmarks = state.bookmarks,
                loading = state.bookmarksLoading,
                errorText = state.bookmarkNotice?.takeIf { it.isError }?.text,
                onAddHere = viewModel::addBookmarkHere,
                onJump = viewModel::jumpToBookmark,
                onRename = viewModel::renameBookmark,
                onDelete = viewModel::deleteBookmark,
                onDismiss = viewModel::closeBookmarkPanel,
            )
        }

        if (state.sleepTimerPanelVisible) {
            SleepTimerPanel(
                currentMinutes = state.sleepTimerMinutes,
                remainingSec = state.sleepTimerRemainingSec,
                endOfChapter = state.sleepEndOfChapter,
                showEndOfChapter = state.chapters.isNotEmpty(),
                onSelect = viewModel::setSleepTimerMinutes,
                onToggleEndOfChapter = viewModel::setSleepEndOfChapter,
                onDismiss = viewModel::closeSleepTimerPanel,
            )
        }

        if (state.chapterPanelVisible) {
            ChapterPanel(
                chapters = state.chapters,
                currentIndex = chapterIndex,
                onSelect = viewModel::jumpToChapter,
                onDismiss = viewModel::closeChapterPanel,
            )
        }
    }
}

/**
 * Chapter half of the dual position display: elapsed inside the chapter, a
 * bar filled with the dim primary token so it reads as the book bar's
 * quieter sibling, and the "-12:34" until-next-chapter countdown at the
 * playback rate. Rendered only while the head is inside a chapter -- a book
 * without chapter data keeps the plain single-bar layout.
 */
@Composable
private fun ChapterProgressRow(
    positionSec: Long,
    chapter: AbsChapter,
    speed: Float,
    colors: androidx.tv.material3.ColorScheme,
) {
    val absSec = positionSec.toDouble()
    val fraction = ChapterMath.progressFraction(absSec, chapter)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BarTag(text = "CHAPTER", colors = colors)
        Text(
            text = formatTimestampHms(ChapterMath.elapsedSec(absSec, chapter).toLong()),
            color = colors.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        // weight, not fillMaxWidth(0.7f): the endpoint text measures first
        // at its intrinsic width and the bar takes exactly what is left, so
        // adding the tag column cannot squeeze "-10:04:03 · 25%" into a
        // two-line wrap (which the old fixed fraction did on the TV device).
        Box(
            modifier = Modifier
                .height(6.dp)
                .weight(1f)
                .background(colors.surface, RoundedCornerShape(3.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(colors.primaryContainer, RoundedCornerShape(3.dp)),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = ChapterMath.remainingLabel(ChapterMath.remainingSecAtSpeed(absSec, chapter, speed)),
            color = colors.onSurfaceVariant,
            fontSize = 14.sp,
        )
    }
}

/**
 * Book half of the dual position display. The right endpoint follows the
 * chapter bar's convention: a speed-aware negative countdown plus the
 * whole-book percent, instead of the static total duration. While duration
 * is unknown the endpoint falls back to 0:00 rather than a fake countdown.
 * [labeled] is true only when the chapter bar is showing above: with two
 * bars the BOOK tag makes the pairing instant at 10 feet, with one bar
 * there is nothing to disambiguate and the tag would be noise.
 */
@Composable
private fun ProgressRow(
    positionSec: Long,
    durationSec: Long,
    speed: Float,
    labeled: Boolean,
    colors: androidx.tv.material3.ColorScheme,
) {
    val fraction = if (durationSec > 0) positionSec.toFloat() / durationSec.toFloat() else 0f
    val endLabel = if (durationSec > 0) {
        val remaining = BookProgress.remainingSecAtSpeed(positionSec, durationSec, speed)
        "${ChapterMath.remainingLabel(remaining)} · ${BookProgress.percent(positionSec, durationSec)}%"
    } else {
        formatTimestampHms(0)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (labeled) {
            BarTag(text = "BOOK", colors = colors)
        }
        Text(text = formatTimestampHms(positionSec), color = colors.onSurfaceVariant, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(12.dp))
        // Same weight-not-fraction rule as the chapter bar above.
        Box(
            modifier = Modifier
                .height(6.dp)
                .weight(1f)
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
        Text(text = endLabel, color = colors.onSurfaceVariant, fontSize = 14.sp)
    }
}

/**
 * Muted tag naming which bar is which. Fixed width so the CHAPTER and
 * BOOK rows keep their timestamps and bars column-aligned. 12sp with a
 * medium weight: the original 10sp was the smallest text on the screen
 * and the first thing to drop out at 3 metres. Still subordinate to the
 * 14sp timestamps by staying a size down and in the muted tone; weight
 * and size carry the contrast lift, not a brighter color. The ellipsis
 * is the canary if a future tag outgrows the column.
 */
@Composable
private fun BarTag(text: String, colors: androidx.tv.material3.ColorScheme) {
    Text(
        text = text,
        color = colors.onSurfaceVariant,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.5.sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.width(78.dp),
    )
}

/** The chapter affordance of the control row: its live counter label plus the picker opener. */
private data class ChaptersControl(val label: String, val onOpen: () -> Unit)

/**
 * Main control row:
 *
 *   «30s | Play | 30s» | 1x | Sleep | Mark | Ch n/N
 *
 * Skip labels come from SkipLabels over the SeekTargets constants, so the
 * text carries the same value-plus-unit pattern as the 5m jump row and can
 * never drift from the seek performed.
 * The 30s pair hugs Play (locked primary increment); the utility cluster
 * is ordered by frequency, Sleep ahead of Mark (critic: Mark is the rarer
 * action). Ch n/N stays last: it is the widest and most width-variable
 * label, so at the row's end its growth ("Ch 9/30" to "Ch 10/30") never
 * shifts a neighbour the user is about to press. The 5m long-skip pair
 * lives on [JumpRow] directly below -- nine controls were tried in this
 * row and the Ch counter ellipsized on the TV device (488dp column, see the
 * cover comment), which is why the long-skip pair lives on its own row.
 */
@Composable
private fun ControlRow(
    isPlaying: Boolean,
    speed: Float,
    sleepLabel: String,
    onSkipBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipForward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onBookmark: () -> Unit,
    onSleepTimer: () -> Unit,
    chaptersControl: ChaptersControl?,
    colors: androidx.tv.material3.ColorScheme,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(label = SkipLabels.back(SeekTargets.SKIP_SECONDS), onClick = onSkipBack, colors = colors)
        ControlButton(label = if (isPlaying) "Pause" else "Play", onClick = onPlayPause, emphasised = true, colors = colors)
        ControlButton(label = SkipLabels.forward(SeekTargets.SKIP_SECONDS), onClick = onSkipForward, colors = colors)
        ControlButton(label = formatSpeed(speed), onClick = onCycleSpeed, colors = colors)
        ControlButton(label = sleepLabel, onClick = onSleepTimer, colors = colors)
        ControlButton(label = "Mark", onClick = onBookmark, colors = colors)
        // Null when the book has no chapter data: chapterless books keep the
        // 6-button row. The label is the live "Ch n/N" counter, which both
        // answers "where am I" without opening the picker and is narrower
        // than the word "Chapters" that used to clip at the screen edge.
        // The ControlButton ellipsis canary still guards label drift.
        if (chaptersControl != null) {
            ControlButton(label = chaptersControl.label, onClick = chaptersControl.onOpen, colors = colors)
        }
    }
}

/**
 * The coarse-jump row under the transport: the labeled 5m long-skip pair
 * (the dual-granularity add) plus Undo when there is a seek to undo.
 * « 5m sits directly under « 30 so the two granularities column-align by
 * direction -- back-jumps stacked left, forward-jumps stacked right. Undo
 * comes last because it appears and disappears; at the row's end it never
 * shifts the long-skip targets the user is aiming at.
 */
@Composable
private fun JumpRow(
    onLongSkipBack: () -> Unit,
    onLongSkipForward: () -> Unit,
    undoTargetSec: Long?,
    onUndo: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        ControlButton(label = SkipLabels.back(SeekTargets.LONG_SKIP_SECONDS), onClick = onLongSkipBack, colors = colors)
        ControlButton(label = SkipLabels.forward(SeekTargets.LONG_SKIP_SECONDS), onClick = onLongSkipForward, colors = colors)
        if (undoTargetSec != null) {
            ControlButton(
                label = "Undo seek to ${formatTimestampHms(undoTargetSec)}",
                onClick = onUndo,
                colors = colors,
            )
        }
    }
}

/**
 * Compact control button. tv Surface, not tv Button: Button enforces a
 * ~58dp minimum width, and seven of those plus spacing (~430dp) leave no
 * headroom in the 488dp column for the worst-state labels (Pause, 1.75x,
 * 59:59+, Ch 10/30). Surface sizes to its label, which is what keeps the
 * seven-control main row plus the jump row inside the budget.
 */
@Composable
private fun ControlButton(
    label: String,
    onClick: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
    emphasised: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        // Resting-primary vs focused: an emphasised control at rest wears the
        // dim primaryContainer tone; the full primary fill lights up only
        // together with the orange focus ring. Without the split, Play and
        // the selected nav pill were both bright accent pills whenever focus
        // sat elsewhere, and a viewer at 10 feet saw two "active" controls.
        // The ring stays the focus indicator; tone is the disambiguator.
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (emphasised) colors.primaryContainer else colors.surface,
            contentColor = if (emphasised) colors.onPrimaryContainer else colors.onSurface,
            focusedContainerColor = if (emphasised) colors.primary else colors.surface,
            focusedContentColor = if (emphasised) colors.onPrimary else colors.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.secondary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        modifier = Modifier.height(40.dp),
    ) {
        // fillMaxHeight only, never fillMaxSize: this width-less tv Surface
        // sits in a bounded Row, where a fillMaxSize content Box makes the
        // first button claim the row's whole remaining width and render its
        // siblings at zero width (the wave-0 "Rename slabs" trap, see
        // BookmarkPanel.RowActionButton and LibraryScreen.SegmentChip).
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            // softWrap=false + maxLines=1 forbid the half-word wrap we saw on
            // the TV device once Play swapped to Pause, and overflow=Ellipsis is
            // the canary: without it the default is Clip, which cut "Ch 10/30"
            // mid-glyph at the screen edge instead of flagging the layout
            // problem with a visible "...".
            Text(
                text = label,
                fontSize = 16.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatSpeed(speed: Float): String {
    val s = if (speed % 1f == 0f) "%.0f".format(speed) else "%.2f".format(speed).trimEnd('0').trimEnd('.')
    return "${s}x"
}
