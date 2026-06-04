package xyz.ludothegreat.audiobooktv.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        when {
            state.books.isEmpty() && state.loading -> StatusMessage("Loading...", colors)
            state.books.isEmpty() && state.error != null ->
                StatusMessage(state.error ?: "Unknown error.", colors, isError = true)
            state.books.isEmpty() ->
                StatusMessage("Load books into your Audiobookshelf library.", colors)
            else -> BookGrid(state.books, onBookSelected)
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
private fun BookGrid(books: List<Book>, onBookSelected: (Book) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items = books, key = { it.id }) { book ->
            BookTile(book = book, onClick = { onBookSelected(book) })
        }
    }
}

@Composable
private fun BookTile(book: Book, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tileAlpha = if (book.isFinished) 0.45f else 1.0f

    Surface(
        onClick = onClick,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(6.dp)),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = colors.surface,
            contentColor = colors.onSurface,
            focusedContainerColor = colors.surface,
            focusedContentColor = colors.onSurface,
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(width = 3.dp, color = colors.secondary),
                shape = RoundedCornerShape(6.dp),
            ),
        ),
        modifier = Modifier.fillMaxWidth().alpha(tileAlpha),
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
                text = book.title,
                color = colors.onSurface,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}
