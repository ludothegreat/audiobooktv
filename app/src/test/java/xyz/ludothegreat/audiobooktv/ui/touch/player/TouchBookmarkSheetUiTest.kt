package xyz.ludothegreat.audiobooktv.ui.touch.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import xyz.ludothegreat.audiobooktv.ComposeUiTest
import xyz.ludothegreat.audiobooktv.domain.Bookmark
import xyz.ludothegreat.audiobooktv.playback.BookmarkList

/**
 * The v1.2.0 crash this locks down: the sheet keys its LazyColumn on
 * Bookmark.timeSec, so two bookmarks sharing a second threw
 * "Key ... was already used" and took the sheet down. BookmarkList.normalize
 * is the guard, and nothing exercised the two together until now.
 *
 * These run on the JVM under Robolectric, so they gate a push the same way
 * the pure-logic suites do rather than waiting for someone to hold a device.
 */
@Category(ComposeUiTest::class)
@RunWith(RobolectricTestRunner::class)
// Plain Application, not the Hilt one: booting the real graph constructs
// EncryptedSharedPreferences against AndroidKeyStore, which does not exist on
// the JVM. These composables take their data as parameters, so the DI graph is
// not needed to exercise them.
@Config(sdk = [35], application = android.app.Application::class)
class TouchBookmarkSheetUiTest {

    @get:Rule
    val compose = createComposeRule()

    private fun sheet(bookmarks: List<Bookmark>) {
        compose.setContent {
            MaterialTheme {
                BookmarkRows(
                    bookmarks = bookmarks,
                    onJump = {},
                    onRenameRequest = {},
                    onDelete = {},
                )
            }
        }
    }

    @Test
    fun `two bookmarks at the same second render without a duplicate key crash`() {
        // Raw, un-normalized input is exactly what reached the sheet in v1.2.0.
        val collided = listOf(
            Bookmark(timeSec = 90, title = "first", id = "1"),
            Bookmark(timeSec = 90, title = "second", id = "2"),
        )
        sheet(BookmarkList.normalize(collided))
        // Surviving composition at all is the assertion; normalize collapses
        // the pair so exactly one row is keyed on that second.
        compose.onAllNodesWithText("1:30", substring = true)
            .fetchSemanticsNodes()
            .let { check(it.size == 1) { "expected one row for the collided second, got ${it.size}" } }
    }

    @Test
    fun `distinct bookmarks each get their own row`() {
        sheet(
            BookmarkList.normalize(
                listOf(
                    Bookmark(timeSec = 90, title = "one", id = "1"),
                    Bookmark(timeSec = 3661, title = "two", id = "2"),
                ),
            ),
        )
        compose.onNodeWithText("1:30", substring = true).assertIsDisplayed()
        compose.onNodeWithText("1:01:01", substring = true).assertIsDisplayed()
    }
}
