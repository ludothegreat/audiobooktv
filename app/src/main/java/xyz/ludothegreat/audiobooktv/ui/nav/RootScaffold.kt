package xyz.ludothegreat.audiobooktv.ui.nav

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import xyz.ludothegreat.audiobooktv.R
import xyz.ludothegreat.audiobooktv.ui.library.LibraryScreen
import xyz.ludothegreat.audiobooktv.ui.player.PlayerScreen
import xyz.ludothegreat.audiobooktv.ui.settings.SettingsScreen

enum class NavDestination(val labelRes: Int) {
    NowPlaying(R.string.nav_now_playing),
    Library(R.string.nav_library),
    Settings(R.string.nav_settings),
}

@Composable
fun RootScaffold(viewModel: RootViewModel = hiltViewModel()) {
    var selected by remember { mutableStateOf(NavDestination.Library) }
    var currentBookId by remember { mutableStateOf<String?>(null) }
    var currentCoverUrl by remember { mutableStateOf<String?>(null) }
    var initialApplied by remember { mutableStateOf(false) }

    val rootState by viewModel.state.collectAsState()
    LaunchedEffect(rootState.resolved, initialApplied) {
        if (rootState.resolved && !initialApplied) {
            initialApplied = true
            rootState.initial?.let { active ->
                currentBookId = active.bookId
                currentCoverUrl = active.coverUrl
                selected = NavDestination.NowPlaying
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavRail(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxHeight()
                .width(160.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        )
        Box(modifier = Modifier.fillMaxSize()) {
            when (selected) {
                NavDestination.NowPlaying -> PlayerScreen(
                    itemId = currentBookId,
                    coverUrl = currentCoverUrl,
                )
                NavDestination.Library -> LibraryScreen(
                    onBookSelected = { book ->
                        currentBookId = book.id
                        currentCoverUrl = book.coverUrl
                        selected = NavDestination.NowPlaying
                    },
                )
                NavDestination.Settings -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun NavRail(
    selected: NavDestination,
    onSelect: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NavDestination.entries.forEach { dest ->
            val isSelected = dest == selected
            Button(
                onClick = { onSelect(dest) },
                shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
                colors = if (isSelected) {
                    ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(text = stringResource(dest.labelRes))
            }
            Spacer(modifier = Modifier.height(0.dp))
        }
    }
}
