package xyz.ludothegreat.audiobooktv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import xyz.ludothegreat.audiobooktv.ui.nav.RootScaffold
import xyz.ludothegreat.audiobooktv.ui.theme.AudiobooktvTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudiobooktvTheme {
                RootScaffold()
            }
        }
    }
}
