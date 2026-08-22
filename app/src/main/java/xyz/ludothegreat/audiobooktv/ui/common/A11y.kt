package xyz.ludothegreat.audiobooktv.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

/**
 * Controls on this app draw compact visual labels that do not survive being
 * read aloud: "<<30s", "1x", "Ch 11/30", or a cover tile whose meaning is
 * split across a badge, a title line and a caption. A screen reader needs one
 * sentence per control instead.
 *
 * semantics, NOT clearAndSetSemantics: clearing the subtree also strips the
 * click action these controls declare, leaving a reader able to read them and
 * unable to press them. Measured on device: clearAndSetSemantics dropped every
 * control out of the actionable tree entirely. The cost of merging instead is
 * that a reader may hear the spoken label followed by the visual abbreviation;
 * redundant, but usable, which the alternative was not.
 */
fun Modifier.describedAction(label: String, role: Role = Role.Button): Modifier = this.semantics(mergeDescendants = true) {
    contentDescription = label
    this.role = role
}

/**
 * Selectable controls also carry state, so the reader can say "selected"
 * rather than leaving a green fill as the only cue.
 */
fun Modifier.describedSelectable(label: String, isSelected: Boolean): Modifier = this.semantics(mergeDescendants = true) {
    contentDescription = label
    role = Role.Tab
    selected = isSelected
}
