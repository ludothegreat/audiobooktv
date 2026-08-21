package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.BookmarkLabel
import xyz.ludothegreat.audiobooktv.playback.formatTimestampHms
import xyz.ludothegreat.audiobooktv.ui.common.DELETE_CONFIRM_WINDOW_MS
import xyz.ludothegreat.audiobooktv.ui.common.dpadFocusEscape

@Composable
fun BookmarkPanel(
    positionSec: Long,
    bookmarks: List<Bookmark>,
    loading: Boolean,
    errorText: String?,
    onAddHere: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onRename: (Bookmark, String) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var renaming by remember { mutableStateOf<Bookmark?>(null) }

    // Back cancels the rename first so a stray press doesn't lose the panel.
    BackHandler(enabled = true) {
        if (renaming != null) renaming = null else onDismiss()
    }

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
            val target = renaming
            if (target != null) {
                RenameEditor(
                    bookmark = target,
                    onSave = { newTitle ->
                        onRename(target, newTitle)
                        renaming = null
                    },
                    onCancel = { renaming = null },
                    colors = colors,
                )
            } else {
                BookmarkListContent(
                    positionSec = positionSec,
                    bookmarks = bookmarks,
                    loading = loading,
                    errorText = errorText,
                    onAddHere = onAddHere,
                    onJump = onJump,
                    onRenameRequest = { renaming = it },
                    onDelete = onDelete,
                    onDismiss = onDismiss,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun BookmarkListContent(
    positionSec: Long,
    bookmarks: List<Bookmark>,
    loading: Boolean,
    errorText: String?,
    onAddHere: () -> Unit,
    onJump: (Bookmark) -> Unit,
    onRenameRequest: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Bookmarks",
            color = colors.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        errorText?.let { msg ->
            Text(
                text = msg,
                color = colors.error,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        val addFocus = remember { FocusRequester() }

        AddHereButton(
            positionSec = positionSec,
            onClick = {
                onAddHere()
                onDismiss()
            },
            focusRequester = addFocus,
            colors = colors,
        )

        Spacer(modifier = Modifier.height(4.dp))

        when {
            loading -> Text(
                text = "Loading...",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
            bookmarks.isEmpty() -> Text(
                text = "No bookmarks yet.",
                color = colors.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
            ) {
                items(items = bookmarks, key = { it.timeSec }) { bookmark ->
                    BookmarkRow(
                        bookmark = bookmark,
                        onJump = {
                            onJump(bookmark)
                            onDismiss()
                        },
                        onRenameRequest = { onRenameRequest(bookmark) },
                        onDelete = { onDelete(bookmark) },
                        colors = colors,
                    )
                }
            }
        }

        LaunchedEffect(Unit) { addFocus.requestFocus() }
    }
}

@Composable
private fun AddHereButton(
    positionSec: Long,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    colors: androidx.tv.material3.ColorScheme,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            focusedContainerColor = colors.primary,
            focusedContentColor = colors.onPrimary,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.secondary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .focusRequester(focusRequester),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = "+ Bookmark at ${formatTimestampHms(positionSec)}",
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 20.dp),
            )
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onJump: () -> Unit,
    onRenameRequest: () -> Unit,
    onDelete: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    // Two-press delete: DPAD selects are easy to fire by accident and a
    // deleted bookmark's custom title is unrecoverable. The armed state
    // reverts on its own so a wandering focus doesn't leave a live trigger.
    var confirmingDelete by remember(bookmark.timeSec) { mutableStateOf(false) }
    LaunchedEffect(confirmingDelete) {
        if (confirmingDelete) {
            delay(DELETE_CONFIRM_WINDOW_MS)
            confirmingDelete = false
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            onClick = onJump,
            shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = colors.background,
                contentColor = colors.onSurface,
                focusedContainerColor = colors.background,
                focusedContentColor = colors.onSurface,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(2.dp, colors.secondary),
                    shape = RoundedCornerShape(8.dp),
                ),
            ),
            modifier = Modifier.weight(1f).height(48.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatTimestampHms(bookmark.timeSec),
                    fontSize = 16.sp,
                    modifier = Modifier.width(80.dp),
                )
                // Null for an unrenamed bookmark, whose title IS the
                // timestamp: print the time once instead of twice.
                val secondary = BookmarkLabel.secondary(bookmark.title, bookmark.timeSec, "-")
                if (secondary != null) {
                    Text(
                        text = secondary,
                        fontSize = 16.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        RowActionButton(
            label = "Rename",
            onClick = onRenameRequest,
            focusBorderColor = colors.secondary,
            colors = colors,
        )
        RowActionButton(
            label = if (confirmingDelete) "Sure?" else "Delete",
            onClick = {
                if (confirmingDelete) onDelete() else confirmingDelete = true
            },
            // Destructive control: red focus outline per the focus-indicator
            // pattern, so Delete never looks like a safe action.
            focusBorderColor = colors.error,
            colors = colors,
        )
    }
}

@Composable
private fun RowActionButton(
    label: String,
    onClick: () -> Unit,
    focusBorderColor: Color,
    colors: androidx.tv.material3.ColorScheme,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.background,
            contentColor = colors.onSurfaceVariant,
            focusedContainerColor = colors.background,
            focusedContentColor = colors.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, focusBorderColor),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        modifier = Modifier.height(48.dp),
    ) {
        // fillMaxHeight only: a fillMaxSize content Box makes this width-less
        // tv Surface expand to the Row's full remaining width. Weightless
        // children measure first in a Row, so that one greedy button swallowed
        // the whole row and the jump surface and Delete rendered at zero width
        // (the wave-0 "bare Rename slabs" defect).
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, fontSize = 14.sp, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun RenameEditor(
    bookmark: Bookmark,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    var title by remember { mutableStateOf(bookmark.title) }
    val fieldFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Rename bookmark at ${formatTimestampHms(bookmark.timeSec)}",
            color = colors.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                cursorColor = colors.primary,
                focusedBorderColor = colors.secondary,
                unfocusedBorderColor = colors.onSurfaceVariant,
                focusedLabelColor = colors.secondary,
                unfocusedLabelColor = colors.onSurfaceVariant,
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
            ),
            // Same D-pad trap as the library search field: without the
            // escape, a focused TextField consumes Down after the IME is
            // dismissed and Save/Cancel are unreachable by remote.
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(fieldFocus)
                .dpadFocusEscape(focusManager),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorButton(
                label = "Save",
                onClick = { onSave(title) },
                enabled = title.isNotBlank(),
                emphasised = true,
                colors = colors,
            )
            EditorButton(
                label = "Cancel",
                onClick = onCancel,
                enabled = true,
                emphasised = false,
                colors = colors,
            )
        }
        LaunchedEffect(Unit) { fieldFocus.requestFocus() }
    }
}

@Composable
private fun EditorButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasised: Boolean,
    colors: androidx.tv.material3.ColorScheme,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (emphasised) colors.primary else colors.background,
            contentColor = if (emphasised) colors.onPrimary else colors.onSurface,
            focusedContainerColor = if (emphasised) colors.primary else colors.background,
            focusedContentColor = if (emphasised) colors.onPrimary else colors.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.secondary),
                shape = RoundedCornerShape(8.dp),
            ),
        ),
        modifier = Modifier.height(44.dp),
    ) {
        // Same trap as RowActionButton: fillMaxSize here would let Save
        // swallow the editor row and push Cancel out of existence.
        Box(
            modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, fontSize = 16.sp, maxLines = 1, softWrap = false)
        }
    }
}
