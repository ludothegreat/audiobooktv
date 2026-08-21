package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.ludothegreat.audiobooktv.data.abs.dto.AbsChapter
import xyz.ludothegreat.audiobooktv.playback.ChapterMath
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchChapterSheet(
    chapters: List<AbsChapter>,
    currentIndex: Int?,
    onPick: (AbsChapter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    // Open scrolled to the current chapter; an 80-chapter book should not
    // make the user swipe from the top to find "here".
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex ?: 0)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .heightIn(max = 480.dp),
        ) {
            Text(
                text = "Chapters",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(8.dp))
            if (chapters.isEmpty()) {
                Text(
                    text = "No chapters in this book.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(chapters, key = { index, _ -> index }) { index, chapter ->
                        ChapterRow(
                            index = index,
                            chapter = chapter,
                            isCurrent = index == currentIndex,
                            onClick = { onPick(chapter) },
                        )
                    }
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
) {
    val colors = MaterialTheme.colorScheme
    val rowBackground = if (isCurrent) colors.primaryContainer else colors.surface
    val contentColor = if (isCurrent) colors.onPrimaryContainer else colors.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "${index + 1}",
            color = if (isCurrent) contentColor else colors.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = chapter.title?.takeIf { it.isNotBlank() } ?: "Chapter ${index + 1}",
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatTimestampHms(ChapterMath.chapterDurationSec(chapter).toLong()),
            color = if (isCurrent) contentColor else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
