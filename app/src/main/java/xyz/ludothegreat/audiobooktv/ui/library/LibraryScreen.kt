package xyz.ludothegreat.audiobooktv.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import xyz.ludothegreat.audiobooktv.domain.Book

@Composable
fun LibraryScreen(
    onBookSelected: (Book) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val colors = MaterialTheme.colorScheme
    val visible = state.visibleBooks

    // The grid, not the search field, is the default focus target: without
    // an explicit request the first focusable in layout order (the field)
    // would grab focus on open. Requested exactly once, so chip and search
    // interaction afterwards never has focus yanked back to the grid.
    val firstTileFocus = remember { FocusRequester() }
    var initialFocusDone by remember { mutableStateOf(false) }
    val gridShowing = state.books.isNotEmpty() && visible.isNotEmpty()
    LaunchedEffect(gridShowing) {
        if (gridShowing && !initialFocusDone) {
            initialFocusDone = true
            firstTileFocus.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        when {
            state.books.isEmpty() && state.loading -> StatusMessage("Loading...", colors)
            state.books.isEmpty() && state.error != null ->
                StatusMessage(state.error ?: "Unknown error.", colors, isError = true)
            state.books.isEmpty() ->
                StatusMessage("Load books into your Audiobookshelf library.", colors)
            else -> Column(modifier = Modifier.fillMaxSize()) {
                FilterHeader(
                    query = state.query,
                    onQueryChange = viewModel::onQueryChange,
                    segment = state.segment,
                    onSegmentSelect = viewModel::onSegmentSelect,
                )
                if (visible.isEmpty()) {
                    StatusMessage(LibraryFilter.emptyLine(state.query, state.segment), colors)
                } else {
                    BookGrid(visible, onBookSelected, firstTileFocus)
                }
            }
        }
        if (state.offline) {
            Text(
                text = "Offline",
                color = colors.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun FilterHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    segment: StatusSegment,
    onSegmentSelect: (StatusSegment) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, top = 20.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
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
            modifier = Modifier.width(280.dp),
        )
        StatusSegment.entries.forEach { candidate ->
            SegmentChip(
                label = candidate.label,
                selected = candidate == segment,
                onClick = { onSegmentSelect(candidate) },
            )
        }
    }
}

/**
 * Same focus/selected idiom as the rest of the TV surface: green fill for
 * the persistent selection, 2dp orange (colors.secondary) outline for
 * D-pad focus. TV Button mangles height/padding, hence Surface.
 */
@Composable
private fun SegmentChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(18.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.primary else colors.surface,
            contentColor = if (selected) colors.onPrimary else colors.onSurface,
            focusedContainerColor = if (selected) colors.primary else colors.surface,
            focusedContentColor = if (selected) colors.onPrimary else colors.onSurface,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, colors.secondary),
                shape = RoundedCornerShape(18.dp),
            ),
        ),
        modifier = Modifier.height(36.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

@Composable
private fun StatusMessage(text: String, colors: androidx.tv.material3.ColorScheme, isError: Boolean = false) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = if (isError) colors.error else colors.onBackground,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BookGrid(
    books: List<Book>,
    onBookSelected: (Book) -> Unit,
    firstTileFocus: FocusRequester,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        itemsIndexed(items = books, key = { _, book -> book.id }) { index, book ->
            BookTile(
                book = book,
                onClick = { onBookSelected(book) },
                modifier = if (index == 0) Modifier.focusRequester(firstTileFocus) else Modifier,
            )
        }
    }
}

@Composable
private fun BookTile(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val tileAlpha = if (book.isFinished) 0.45f else 1.0f
    val seriesLine = SeriesLabel.seriesLine(book.series)

    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.surface,
            focusedContentColor = colors.onSurface,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(width = 3.dp, color = colors.secondary),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
        modifier = modifier.fillMaxWidth().alpha(tileAlpha),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(colors.background),
            )
            Text(
                text = SeriesLabel.numberedTitle(book.title, book.series),
                color = colors.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = if (seriesLine == null) 8.dp else 2.dp,
                ),
            )
            if (seriesLine != null) {
                Text(
                    text = seriesLine,
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                )
            }
        }
    }
}
