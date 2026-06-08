package xyz.ludothegreat.audiobooktv.ui.touch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import xyz.ludothegreat.audiobooktv.domain.Book
import xyz.ludothegreat.audiobooktv.ui.nav.RootViewModel
import xyz.ludothegreat.audiobooktv.ui.touch.library.TouchLibraryScreen
import xyz.ludothegreat.audiobooktv.ui.touch.player.TouchPlayerScreen
import xyz.ludothegreat.audiobooktv.ui.touch.settings.TouchSettingsScreen
import xyz.ludothegreat.audiobooktv.ui.touch.setup.TouchSetupScreen

private const val TABLET_MIN_WIDTH_DP = 600

/**
 * Touch presentation root. Bottom NavigationBar on phones, NavigationRail on
 * tablets (sw >= 600dp). Setup gating is delegated to SessionManager just
 * like the TV path. Active-book hydration uses the shared RootViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouchRootScaffold(
    sessionManager: SessionManager,
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val credentials by sessionManager.state.collectAsState()
    if (credentials == null) {
        TouchSetupScreen(onConnected = { /* state flow flips */ })
        return
    }

    val rootState by rootViewModel.state.collectAsState()
    var selected by remember { mutableStateOf(TouchDestination.Library) }
    var currentBookId by remember { mutableStateOf<String?>(null) }
    var currentCoverUrl by remember { mutableStateOf<String?>(null) }
    var initialApplied by remember { mutableStateOf(false) }

    LaunchedEffect(rootState.resolved, initialApplied) {
        if (rootState.resolved && !initialApplied) {
            initialApplied = true
            rootState.initial?.let { active ->
                currentBookId = active.bookId
                currentCoverUrl = active.coverUrl
                selected = TouchDestination.NowPlaying
            }
        }
    }

    val widthDp = LocalConfiguration.current.smallestScreenWidthDp
    val isTablet = widthDp >= TABLET_MIN_WIDTH_DP

    val onBookSelected: (Book) -> Unit = { book ->
        currentBookId = book.id
        currentCoverUrl = book.coverUrl
        selected = TouchDestination.NowPlaying
    }
    val openLibrary = { selected = TouchDestination.Library }

    if (isTablet) {
        TabletShell(selected = selected, onSelect = { selected = it }) {
            TouchContent(
                selected = selected,
                currentBookId = currentBookId,
                currentCoverUrl = currentCoverUrl,
                onOpenLibrary = openLibrary,
                onBookSelected = onBookSelected,
            )
        }
    } else {
        PhoneShell(selected = selected, onSelect = { selected = it }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                TouchContent(
                    selected = selected,
                    currentBookId = currentBookId,
                    currentCoverUrl = currentCoverUrl,
                    onOpenLibrary = openLibrary,
                    onBookSelected = onBookSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell(
    selected: TouchDestination,
    onSelect: (TouchDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                TouchDestination.entries.forEach { dest ->
                    NavigationBarItem(
                        selected = dest == selected,
                        onClick = { onSelect(dest) },
                        icon = { Icon(imageVector = iconFor(dest), contentDescription = null) },
                        label = { Text(text = stringResource(dest.labelRes)) },
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun TabletShell(
    selected: TouchDestination,
    onSelect: (TouchDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        NavigationRail {
            TouchDestination.entries.forEach { dest ->
                NavigationRailItem(
                    selected = dest == selected,
                    onClick = { onSelect(dest) },
                    icon = { Icon(imageVector = iconFor(dest), contentDescription = null) },
                    label = { Text(text = stringResource(dest.labelRes)) },
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
private fun TouchContent(
    selected: TouchDestination,
    currentBookId: String?,
    currentCoverUrl: String?,
    onOpenLibrary: () -> Unit,
    onBookSelected: (Book) -> Unit,
) {
    when (selected) {
        TouchDestination.NowPlaying -> TouchPlayerScreen(
            itemId = currentBookId,
            coverUrl = currentCoverUrl,
            onOpenLibrary = onOpenLibrary,
        )
        TouchDestination.Library -> TouchLibraryScreen(onBookSelected = onBookSelected)
        TouchDestination.Settings -> TouchSettingsScreen()
    }
}

private fun iconFor(dest: TouchDestination) = when (dest) {
    TouchDestination.NowPlaying -> Icons.Filled.PlayCircle
    TouchDestination.Library -> Icons.Filled.LibraryBooks
    TouchDestination.Settings -> Icons.Filled.Settings
}
