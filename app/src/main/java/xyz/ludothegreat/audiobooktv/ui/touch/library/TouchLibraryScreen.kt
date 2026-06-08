package xyz.ludothegreat.audiobooktv.ui.touch.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.ui.library.LibraryViewModel

/**
 * Touch library. Same VM and same LibrarySorter (in-progress first, then
 * never-played alphabetical by author) as the TV grid -- the only delta is
 * the presentation: adaptive grid sized for phones/tablets instead of a
 * fixed-5-wide TV layout, with pull-to-refresh in place of the D-pad
 * refresh menu item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchLibraryScreen(
    onBookSelected: (Book) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val pullState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = viewModel::refresh,
        state = pullState,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        when {
            state.books.isEmpty() && state.error != null -> CenteredMessage(
                primary = state.error ?: "Couldn't load library.",
                secondary = "Pull down to retry.",
            )
            state.books.isEmpty() && !state.loading -> CenteredMessage(
                primary = "Load books into your Audiobookshelf library.",
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (state.offline) {
                    OfflineBadge()
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 140.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.books, key = { it.id }) { book ->
                        BookTile(book = book, onClick = { onBookSelected(book) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookTile(book: Book, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tileAlpha = if (book.isFinished) 0.5f else 1f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(tileAlpha)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface),
            )
            if (book.progressFraction in 0.001..0.999) {
                LinearProgressIndicator(
                    progress = { book.progressFraction.toFloat() },
                    color = colors.primary,
                    trackColor = colors.surfaceVariant,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(),
                )
            }
        }
        Text(
            text = book.title,
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun OfflineBadge() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.errorContainer)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Offline -- showing cached library",
            color = colors.onErrorContainer,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun CenteredMessage(primary: String, secondary: String? = null) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = primary,
                color = colors.onBackground,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            secondary?.let {
                Text(
                    text = it,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
