package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms

/**
 * Jump-to-chapter picker. Same control idiom as SpeedPanel and
 * SleepTimerPanel: each chapter is a focusable Surface row, the chapter the
 * player is currently inside is filled with primary (green) and every row
 * gets the secondary (orange) outline on focus. The list opens scrolled to
 * the current chapter so long books don't start 80 rows away from "here".
 */
@Composable
fun ChapterPanel(
    chapters: List<AbsChapter>,
    currentIndex: Int?,
    onSelect: (AbsChapter) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    BackHandler(enabled = true) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Chapters",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (chapters.isEmpty()) {
                    Text(
                        text = "No chapters in this book.",
                        color = colors.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                } else {
                    val initialFocus = remember { FocusRequester() }
                    val focusIndex = currentIndex ?: 0
                    // Opening scrolled to the current chapter also guarantees
                    // the row carrying the FocusRequester is composed, which
                    // requestFocus needs on a lazy list.
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = focusIndex)

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    ) {
                        itemsIndexed(items = chapters, key = { index, _ -> index }) { index, chapter ->
                            ChapterRow(
                                index = index,
                                chapter = chapter,
                                isCurrent = index == currentIndex,
                                onClick = {
                                    onSelect(chapter)
                                    onDismiss()
                                },
                                colors = colors,
                                modifier = if (index == focusIndex) {
                                    Modifier.focusRequester(initialFocus)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }

                    LaunchedEffect(Unit) { initialFocus.requestFocus() }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    index: Int,
    chapter: AbsChapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
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
        modifier = modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                fontSize = 16.sp,
                modifier = Modifier.width(40.dp),
            )
            Text(
                text = chapterRowTitle(chapter, index),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = formatTimestampHms(ChapterMath.chapterDurationSec(chapter).toLong()),
                fontSize = 14.sp,
            )
        }
    }
}

private fun chapterRowTitle(chapter: AbsChapter, index: Int): String = chapter.title?.takeIf { it.isNotBlank() } ?: "Chapter ${index + 1}"
