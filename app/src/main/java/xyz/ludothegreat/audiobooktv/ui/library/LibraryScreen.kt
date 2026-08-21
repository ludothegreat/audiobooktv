package xyz.ludothegreat.audiobooktv.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.platform.LocalFocusManager
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
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.ui.common.CoverArt
import xyz.ludothegreat.audiobooktv.ui.common.CoverPercentBadge
import xyz.ludothegreat.audiobooktv.ui.common.CoverProgressBar
import xyz.ludothegreat.audiobooktv.ui.common.FinishedCheckBadge
import xyz.ludothegreat.audiobooktv.ui.common.dpadFocusEscape

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
    val focusManager = LocalFocusManager.current
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
            // Without the escape, the focused field consumed every D-pad
            // direction and the chips and grid were unreachable by remote
            // (proven on the TV device). See dpadFocusEscape for the mechanism.
            modifier = Modifier
                .width(280.dp)
                .dpadFocusEscape(focusManager),
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
        // Selected chip rests in the dim primaryContainer tone; the full
        // primary fill is reserved for focus (plus the orange ring). While
        // the user browses the grid the selected segment stays readable as
        // "the active filter" without competing with the focused card --
        // same resting-vs-focused idiom as the nav pill and Play button.
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) colors.primaryContainer else colors.surface,
            contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
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
        // fillMaxHeight only, never fillMaxSize: this width-less tv Surface
        // sits in a bounded Row, where a fillMaxSize content Box makes the
        // chip claim the row's whole remaining width. Weightless Row children
        // measure in order, so the first chip (All) swallowed the row and
        // New/Started/Finished rendered at zero width. Same trap as
        // BookmarkPanel.RowActionButton (the wave-0 "Rename slabs" defect).
        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
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
    // One derivation for card treatment AND the segment chips: a book the
    // Finished chip claims always carries the check, a book the Started
    // chip claims always carries the bar. Do not re-derive from raw
    // progressFraction here or the two views drift apart.
    val status = LibraryFilter.statusOf(book)
    // Finished tiles dim cover and text but NOT the overlay badge or the
    // focus border: the dim used to sit on the whole Surface, which faded
    // the orange focus ring to 45% as well.
    val contentAlpha = if (status == StatusSegment.FINISHED) 0.45f else 1.0f
    val seriesLine = SeriesLabel.seriesLine(book.series)
    val metaParts = CardMeta.metaParts(book.author, book.durationSec)

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
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
            ) {
                CoverArt(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    title = book.title,
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.onSurfaceVariant,
                    initialsSize = 32.sp,
                    modifier = Modifier.fillMaxSize().alpha(contentAlpha),
                )
                when (status) {
                    StatusSegment.STARTED -> {
                        CoverProgressBar(
                            fraction = CardMeta.barFraction(book.progressFraction),
                            trackColor = colors.background,
                            fillColor = colors.primary,
                            height = 6.dp,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                        // The bar sliver is invisible at couch distance;
                        // the percent is the value that reads at 3 metres.
                        // Same cached fraction, no extra fetch.
                        CardMeta.percentLabel(book.progressFraction)?.let { percent ->
                            CoverPercentBadge(
                                text = percent,
                                containerColor = colors.background.copy(alpha = 0.72f),
                                contentColor = colors.onBackground,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 5.dp, bottom = 11.dp),
                            )
                        }
                    }
                    StatusSegment.FINISHED -> FinishedCheckBadge(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    )
                    StatusSegment.NEW, StatusSegment.ALL -> Unit
                }
            }
            TileCaption(
                title = SeriesLabel.numberedTitle(book.title, book.series),
                seriesLine = seriesLine,
                meta = metaParts,
                contentAlpha = contentAlpha,
                colors = colors,
            )
        }
    }
}

@Composable
private fun TileCaption(
    title: String,
    seriesLine: String?,
    meta: CardMeta.MetaParts?,
    contentAlpha: Float,
    colors: androidx.tv.material3.ColorScheme,
) {
    Text(
        text = title,
        color = colors.onSurface,
        fontSize = 13.sp,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
            .padding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = if (seriesLine == null && meta == null) 8.dp else 2.dp,
            ),
    )
    if (seriesLine != null) {
        Text(
            text = seriesLine,
            color = colors.onSurfaceVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(start = 8.dp, end = 8.dp, bottom = if (meta == null) 8.dp else 2.dp),
        )
    }
    if (meta != null) {
        // Two rigid-vs-flexible slots, never one joined string: the
        // unweighted duration Text measures first at intrinsic width, the
        // weighted author slot takes only what is left and ellipsizes
        // there. A long author can no longer eat the tail of "16h 27m",
        // focused or not. When the author is missing the empty weighted
        // slot keeps the duration end-aligned, so durations form a clean
        // scannable column down the grid.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(contentAlpha)
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Text(
                text = meta.author ?: "",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (meta.duration != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = meta.duration,
                    color = colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
