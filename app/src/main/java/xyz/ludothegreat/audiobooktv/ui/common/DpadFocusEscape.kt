package xyz.ludothegreat.audiobooktv.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Escape hatch for text fields on the TV surface. A focused Compose
 * TextField consumes every D-pad direction as an editor key, so without
 * this the remote dead-ends inside the field: proven on the TV device, where
 * Right/Down/Up from the library search field all went nowhere and the
 * segment chips were unreachable. While the on-screen keyboard is up it
 * consumes the D-pad before the app sees these events, so this handler
 * only takes effect once the IME is dismissed; at that point every
 * direction becomes a focus move. Cursor movement inside the text stays
 * available through the IME's own arrow keys.
 */
fun Modifier.dpadFocusEscape(focusManager: FocusManager): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    val direction = when (event.key) {
        Key.DirectionRight -> FocusDirection.Right
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionDown -> FocusDirection.Down
        Key.DirectionUp -> FocusDirection.Up
        else -> null
    }
    if (direction != null) {
        focusManager.moveFocus(direction)
        true
    } else {
        false
    }
}
