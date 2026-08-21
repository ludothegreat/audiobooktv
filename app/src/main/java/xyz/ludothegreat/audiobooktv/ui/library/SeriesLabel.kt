package xyz.ludothegreat.audiobooktv.ui.library

/**
 * Turns the ABS `seriesName` string ("Dungeon Crawler Carl #4", or
 * "First Series #1, Second Series #3" for multi-series books) into card
 * text. String parsing only: the minified library item carries no
 * structured series data, and fetching full metadata per book is out of
 * scope (brief: no metadata fetch beyond what the item already has).
 */
internal object SeriesLabel {

    data class Parsed(val name: String, val sequence: String?)

    /**
     * Picks the first ", "-separated entry that carries a "#sequence"
     * marker, falling back to the whole string as a plain series name.
     * The sequence must start with a digit ("4", "1.5"); anything else
     * ("#TBD") is kept as part of the name rather than shown as a number.
     * Known limit: a single series whose own name contains ", " cannot be
     * told apart from a multi-series join, so its name gets truncated at
     * the comma when a later entry has a "#".
     */
    fun parse(seriesName: String?): Parsed? {
        val raw = seriesName?.trim().orEmpty()
        if (raw.isEmpty()) return null

        val entries = raw.split(", ")
        val withSequence = entries.firstNotNullOfOrNull { entry ->
            val hash = entry.lastIndexOf('#')
            if (hash <= 0) return@firstNotNullOfOrNull null
            val name = entry.substring(0, hash).trim()
            val sequence = entry.substring(hash + 1).trim()
            if (name.isEmpty() || sequence.isEmpty() || !sequence.first().isDigit()) {
                null
            } else {
                Parsed(name, sequence)
            }
        }
        return withSequence ?: Parsed(entries.first().trim(), sequence = null)
    }

    /** "4. Title" when the sequence is known, the plain title otherwise. */
    fun numberedTitle(title: String, seriesName: String?): String {
        val sequence = parse(seriesName)?.sequence ?: return title
        return "$sequence. $title"
    }

    /** The series name for the card's second line, or null to omit it. */
    fun seriesLine(seriesName: String?): String? = parse(seriesName)?.name
}
