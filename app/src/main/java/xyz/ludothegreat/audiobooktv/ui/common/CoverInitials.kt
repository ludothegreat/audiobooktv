package xyz.ludothegreat.audiobooktv.ui.common

/**
 * Initials for the deliberate missing-cover state. A blank dark rectangle
 * reads as a broken image; one or two letters from the title read as "this
 * book has no cover", which is the truth. Pure so the tokenizing rules are
 * locked by CoverInitialsTest.
 */
object CoverInitials {

    private const val MAX_INITIALS = 2

    /**
     * First character of the first two words that start with a letter or
     * digit, uppercased. Punctuation-only tokens ("...", "&") are skipped
     * so "The ... Wish" gives "TW", not "T.". A title with nothing usable
     * gives "", which callers render as the plain placeholder panel.
     */
    fun from(title: String): String = title
        .split(Regex("\\s+"))
        .asSequence()
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
        .take(MAX_INITIALS)
        .joinToString(separator = "") { it.uppercase() }
}
