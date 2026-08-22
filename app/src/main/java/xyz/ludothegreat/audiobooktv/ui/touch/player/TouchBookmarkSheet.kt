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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.BookmarkLabel
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import xyz.ludothegreat.audiobooktv.ui.common.DELETE_CONFIRM_WINDOW_MS
import xyz.ludothegreat.audiobooktv.ui.player.BookmarkNotice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchBookmarkSheet(
    bookmarks: List<Bookmark>,
    loading: Boolean,
    currentPositionSec: Long,
    notice: BookmarkNotice?,
    onAddHere: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onRename: (Bookmark, String) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var renaming by remember { mutableStateOf<Bookmark?>(null) }

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
            // The sheet's window covers the player's snackbar host, so
            // rename/delete outcomes are echoed here where the user is
            // actually looking.
            notice?.let { n ->
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = n.text,
                    color = if (n.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
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
                else -> BookmarkRows(
                    bookmarks = bookmarks,
                    onJump = onJump,
                    onRenameRequest = { renaming = it },
                    onDelete = onDelete,
                )
            }
        }
    }

    renaming?.let { target ->
        RenameBookmarkDialog(
            bookmark = target,
            onSave = { newTitle ->
                onRename(target, newTitle)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
}

/**
 * The keyed list, lifted out of the sheet so it can be composed under test
 * without a ModalBottomSheet. The key is Bookmark.timeSec, which is only
 * unique because BookmarkList.normalize deduplicates by it first; a raw list
 * with two bookmarks in the same second crashes composition outright. That
 * pairing is what TouchBookmarkSheetUiTest holds down.
 */
@Composable
internal fun BookmarkRows(
    bookmarks: List<Bookmark>,
    onJump: (Bookmark) -> Unit,
    onRenameRequest: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(bookmarks, key = { it.timeSec }) { bookmark ->
            BookmarkRow(
                bookmark = bookmark,
                onClick = { onJump(bookmark) },
                onRenameRequest = { onRenameRequest(bookmark) },
                onDelete = { onDelete(bookmark) },
            )
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
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onRenameRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // Two-step delete, mirroring the TV panel's two-press confirm: the trash
    // can sits one finger-width from the jump target, a deleted bookmark's
    // custom title is unrecoverable, and the server API cannot undo it. The
    // first tap arms the row ("Sure?" in the error colour), the second tap
    // inside the shared window deletes, and the armed state disarms itself so
    // a stale row cannot fire minutes later. Keyed on timeSec so a list
    // change (jump, add, another delete) resets the armed state.
    var confirmingDelete by remember(bookmark.timeSec) { mutableStateOf(false) }
    LaunchedEffect(confirmingDelete) {
        if (confirmingDelete) {
            delay(DELETE_CONFIRM_WINDOW_MS)
            confirmingDelete = false
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
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
            // Null for an unrenamed bookmark, whose title IS the timestamp:
            // print the time once instead of twice.
            val secondary = BookmarkLabel.secondary(bookmark.title, bookmark.timeSec, "(no label)")
            if (secondary != null) {
                Text(
                    text = secondary,
                    color = colors.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                )
            }
        }
        IconButton(onClick = onRenameRequest) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Rename bookmark",
                tint = colors.onSurfaceVariant,
            )
        }
        if (confirmingDelete) {
            TextButton(onClick = onDelete) {
                Text(
                    text = "Sure?",
                    color = colors.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            IconButton(onClick = { confirmingDelete = true }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete bookmark",
                    tint = colors.error,
                )
            }
        }
    }
}

@Composable
private fun RenameBookmarkDialog(
    bookmark: Bookmark,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf(bookmark.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Rename bookmark at ${formatTimestampHms(bookmark.timeSec)}") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title) },
                enabled = title.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
