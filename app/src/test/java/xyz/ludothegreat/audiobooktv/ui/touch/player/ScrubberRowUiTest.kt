package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import xyz.ludothegreat.audiobooktv.ComposeUiTest

/**
 * The v1.2.0 data-loss path this locks down: while duration is still unknown
 * the Slider stayed draggable, so one stray callback during load committed a
 * seek the clamp could not distinguish from a real seek-to-start, and the
 * zero was pushed to the server and synced to every device.
 *
 * The guard is `enabled = durationSec > 0`. Nothing exercised it until now,
 * and the on-device attempt to reproduce it failed because a warm cache makes
 * duration known almost immediately. A composable test can hold that state
 * open indefinitely, which is the whole reason this rig is worth having.
 */
@Category(ComposeUiTest::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class ScrubberRowUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun scrubber(durationSec: Long) {
        compose.setContent {
            MaterialTheme {
                ScrubberRow(
                    positionSec = 0,
                    durationSec = durationSec,
                    speed = 1.0f,
                    labeled = false,
                    onScrub = {},
                )
            }
        }
    }

    // Selected by tag, not by "the first progress node": adding any other
    // progress indicator to this row would silently retarget both assertions.
    private fun slider() = compose.onNodeWithTag(SCRUBBER_TEST_TAG)

    @Test
    fun `slider is disabled while the duration is still unknown`() {
        scrubber(durationSec = 0)
        slider().assertIsNotEnabled()
    }

    @Test
    fun `slider becomes usable once a real duration arrives`() {
        scrubber(durationSec = 3600)
        slider().assertIsEnabled()
    }
}
