package xyz.ludothegreat.audiobooktv.ui.library

import xyz.ludothegreat.audiobooktv.domain.metadataField

/**
 * Pure text/geometry logic for the library cards on both surfaces. All of
 * it runs over the cached list the grid already holds: no API calls, no
 * Android types, so CardMetaTest can lock it down on the JVM.
 */
internal object CardMeta {

    /**
     * The caption meta row split into its two slots. The author ellipsizes
     * inside a flexible slot; the duration renders at intrinsic width in a
     * rigid slot, so a long author name can never truncate "16h 27m" down
     * to "16h 2...". Joining both into one string is what made truncation
     * possible; keep them apart.
     */
    data class MetaParts(val author: String?, val duration: String?)

    /**
     * Total length as "11h 51m" / "42m". Rounded to the nearest minute with
     * carry (10h 59m 40s reads "11h 0m", never "10h 60m"), and any positive
     * duration shows at least "1m" so a short file never claims "0m". Null
     * when the duration is unknown (<= 0) so the card omits the value
     * instead of showing a made-up zero.
     */
    fun durationLabel(durationSec: Long): String? {
        if (durationSec <= 0) return null
        val totalMinutes = ((durationSec + 30) / 60).coerceAtLeast(1)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /**
     * The card's muted metadata row: author and total duration from the
     * item metadata fields. Never parsed out of a joined display-name
     * string; a missing field is simply omitted (show what exists, no
     * guessing), and null drops the whole row.
     */
    fun metaParts(author: String?, durationSec: Long): MetaParts? {
        val authorPart = metadataField(author)
        val durationPart = durationLabel(durationSec)
        if (authorPart == null && durationPart == null) return null
        return MetaParts(authorPart, durationPart)
    }

    /** Cover-bar fill fraction; server fractions outside 0..1 are clamped. */
    fun barFraction(progressFraction: Double): Float = progressFraction.toFloat().coerceIn(0f, 1f)
}
