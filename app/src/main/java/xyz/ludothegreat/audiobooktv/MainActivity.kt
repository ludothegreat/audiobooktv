package xyz.ludothegreat.audiobooktv

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import xyz.ludothegreat.audiobooktv.data.settings.AppSettings
import xyz.ludothegreat.audiobooktv.ui.FormFactorRouter
import xyz.ludothegreat.audiobooktv.ui.UiSurface
import xyz.ludothegreat.audiobooktv.ui.nav.RootScaffold
import xyz.ludothegreat.audiobooktv.ui.setup.SetupScreen
import xyz.ludothegreat.audiobooktv.ui.theme.AppTheme
import xyz.ludothegreat.audiobooktv.ui.theme.AudiobooktvMaterialTheme
import xyz.ludothegreat.audiobooktv.ui.theme.AudiobooktvTheme
import xyz.ludothegreat.audiobooktv.ui.touch.TouchRootScaffold
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager

    @Inject lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Form factor is decided once at activity start. Both branches share the
        // session/credentials flow and the same data + playback layers below.
        val surface = FormFactorRouter.choose(
            hasLeanback = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK),
        )
        setContent {
            val theme by appSettings.selectedTheme.collectAsState(initial = AppTheme.Gruvbox)
            when (surface) {
                UiSurface.Tv -> AudiobooktvTheme(theme = theme) { TvContent() }
                UiSurface.Touch -> AudiobooktvMaterialTheme(theme = theme) { TouchContent() }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TvContent() {
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

    @androidx.compose.runtime.Composable
    private fun TouchContent() {
        // Setup + nav for the touch surface land in later increments. For now
        // the placeholder confirms form-factor routing is wiring up.
        TouchRootScaffold()
    }
}
