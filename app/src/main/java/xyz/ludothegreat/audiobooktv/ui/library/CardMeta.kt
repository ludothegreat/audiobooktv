package xyz.ludothegreat.audiobooktv.ui.library

import xyz.ludothegreat.audiobooktv.domain.metadataField

/**
 * Pure text/geometry logic for the library cards on both surfaces. All of
 * it runs over the cached list the grid already holds: no API calls, no
 * Android types, so CardMetaTest can lock it down on the JVM.
 */
internal object CardMeta {

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
     * The card's muted metadata line: author and total duration from the
     * item metadata fields, joined with a middle dot. Never parsed out of a
     * joined display-name string; a missing field is simply omitted (show
     * what exists, no guessing), and null drops the whole line.
     */
    fun metaLine(author: String?, durationSec: Long): String? {
        val parts = listOfNotNull(metadataField(author), durationLabel(durationSec))
        return if (parts.isEmpty()) null else parts.joinToString(" · ")
    }

    /** Cover-bar fill fraction; server fractions outside 0..1 are clamped. */
    fun barFraction(progressFraction: Double): Float = progressFraction.toFloat().coerceIn(0f, 1f)
}
