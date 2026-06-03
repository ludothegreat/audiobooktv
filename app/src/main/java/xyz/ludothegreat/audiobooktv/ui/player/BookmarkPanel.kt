package xyz.ludothegreat.audiobooktv.ui.player

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import xyz.ludothegreat.audiobooktv.domain.Bookmark

@Composable
fun BookmarkPanel(
    positionSec: Long,
    bookmarks: List<Bookmark>,
    loading: Boolean,
    onAddHere: () -> Unit,
    onJump: (Bookmark) -> Unit,
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
                    text = "Bookmarks",
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(8.dp))

                val addFocus = remember { FocusRequester() }

                Surface(
                    onClick = {
                        onAddHere()
                        onDismiss()
                    },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer,
                        focusedContainerColor = colors.primary,
                        focusedContentColor = colors.onPrimary,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .focusRequester(addFocus),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            text = "+ Bookmark at ${formatTimestamp(positionSec)}",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(start = 20.dp),
                        )
                    }
                }

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
                                onClick = {
                                    onJump(bookmark)
                                    onDismiss()
                                },
                                colors = colors,
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) { addFocus.requestFocus() }
            }
        }
    }
}

@Composable
private fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    colors: androidx.tv.material3.ColorScheme,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.background,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.primary,
            focusedContentColor = colors.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatTimestamp(bookmark.timeSec),
                fontSize = 16.sp,
                modifier = Modifier.width(80.dp),
            )
            Text(
                text = bookmark.title.ifBlank { "—" },
                fontSize = 16.sp,
                maxLines = 1,
            )
        }
    }
}

private fun formatTimestamp(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
