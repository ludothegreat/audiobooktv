package xyz.ludothegreat.audiobooktv.ui.touch.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import xyz.ludothegreat.audiobooktv.ui.library.LibraryFilter
import xyz.ludothegreat.audiobooktv.ui.library.LibraryViewModel
import xyz.ludothegreat.audiobooktv.ui.library.SeriesLabel
import xyz.ludothegreat.audiobooktv.ui.library.StatusSegment

/**
 * Touch library. Same VM, same LibrarySorter ordering, and the same
 * LibraryFilter search/segment logic as the TV grid -- the only delta is
 * the presentation: adaptive grid sized for phones/tablets, Material3
 * search field + FilterChips instead of the TV header, and pull-to-refresh
 * in place of the D-pad refresh menu item.
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
    val visible = state.visibleBooks

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
                SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)
                SegmentChipRow(segment = state.segment, onSegmentSelect = viewModel::onSegmentSelect)
                if (visible.isEmpty()) {
                    CenteredMessage(primary = LibraryFilter.emptyLine(state.query, state.segment))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(visible, key = { it.id }) { book ->
                            BookTile(book = book, onClick = { onBookSelected(book) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search title, author, series") },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.onBackground,
            unfocusedTextColor = colors.onBackground,
            cursorColor = colors.primary,
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.onSurfaceVariant,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
    )
}

@Composable
private fun SegmentChipRow(segment: StatusSegment, onSegmentSelect: (StatusSegment) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        StatusSegment.entries.forEach { candidate ->
            FilterChip(
                selected = candidate == segment,
                onClick = { onSegmentSelect(candidate) },
                label = { Text(candidate.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                ),
            )
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
            text = SeriesLabel.numberedTitle(book.title, book.series),
            color = colors.onBackground,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(top = 4.dp),
        )
        SeriesLabel.seriesLine(book.series)?.let { line ->
            Text(
                text = line,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
            )
        }
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
