package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchBookmarkSheet(
    bookmarks: List<Bookmark>,
    loading: Boolean,
    currentPositionSec: Long,
    onAddHere: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
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
                text = "Bookmarks",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.size(8.dp))
            AddHereRow(currentPositionSec = currentPositionSec, onClick = onAddHere)
            Spacer(modifier = Modifier.size(12.dp))
            when {
                loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                bookmarks.isEmpty() -> Text(
                    text = "No bookmarks for this book yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(bookmarks, key = { it.timeSec }) { bookmark ->
                        BookmarkRow(bookmark = bookmark, onClick = { onJump(bookmark) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHereRow(currentPositionSec: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = colors.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "Add bookmark at ${formatTimestampHms(currentPositionSec)}",
            color = colors.onPrimaryContainer,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun BookmarkRow(bookmark: Bookmark, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = formatTimestampHms(bookmark.timeSec),
            color = colors.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = bookmark.title.ifBlank { "(no label)" },
            color = colors.onSurface,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
