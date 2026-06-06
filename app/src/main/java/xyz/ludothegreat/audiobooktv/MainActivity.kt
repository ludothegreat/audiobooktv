package xyz.ludothegreat.audiobooktv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import xyz.ludothegreat.audiobooktv.data.settings.AppSettings
import xyz.ludothegreat.audiobooktv.ui.nav.RootScaffold
import xyz.ludothegreat.audiobooktv.ui.setup.SetupScreen
import xyz.ludothegreat.audiobooktv.ui.theme.AppTheme
import xyz.ludothegreat.audiobooktv.ui.theme.AudiobooktvTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by appSettings.selectedTheme.collectAsState(initial = AppTheme.Gruvbox)
            AudiobooktvTheme(theme = theme) {
                val credentials by sessionManager.state.collectAsState()
                if (credentials == null) {
                    SetupScreen(
                        onConnected = { /* state flow flips, recomposes into RootScaffold */ },
                        onExit = { finish() },
                    )
                } else {
                    RootScaffold()
                }
            }
        }
    }
}
