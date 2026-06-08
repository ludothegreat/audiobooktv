package xyz.ludothegreat.audiobooktv.ui.touch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import xyz.ludothegreat.audiobooktv.ui.nav.RootViewModel
import xyz.ludothegreat.audiobooktv.ui.touch.player.TouchPlayerScreen

/**
 * Touch presentation surface root. Increment 2: routes to the touch player.
 * Library and settings screens land in subsequent increments; until then a
 * book selected on TV (saved via RootViewModel.resolveInitialState) opens
 * here on first launch, exercising the cross-device-resume path.
 */
@Composable
fun TouchRootScaffold(viewModel: RootViewModel = hiltViewModel()) {
    val rootState by viewModel.state.collectAsState()
    var currentBookId by remember { mutableStateOf<String?>(null) }
    var currentCoverUrl by remember { mutableStateOf<String?>(null) }
    var initialApplied by remember { mutableStateOf(false) }

    LaunchedEffect(rootState.resolved, initialApplied) {
        if (rootState.resolved && !initialApplied) {
            initialApplied = true
            rootState.initial?.let { active ->
                currentBookId = active.bookId
                currentCoverUrl = active.coverUrl
            }
        }
    }

    if (currentBookId == null) {
        // Library + Settings touch screens land in subsequent increments. For
        // now a missing active book shows a friendly placeholder so the
        // routing scaffold is observable on a real device.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No active book. Touch library lands in the next increment.",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    } else {
        TouchPlayerScreen(
            itemId = currentBookId,
            coverUrl = currentCoverUrl,
        )
    }
}
