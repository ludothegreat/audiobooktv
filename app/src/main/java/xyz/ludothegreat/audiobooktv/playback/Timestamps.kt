package xyz.ludothegreat.audiobooktv.playback

/**
 * Player-screen timestamp formatting: H:MM:SS for books with hours, MM:SS
 * otherwise. Bookmark labels and the position indicator share the same rule
 * so the on-screen value and the label written into ABS match exactly.
 */
fun formatTimestampHms(seconds: Long): String {
    val secs = seconds.coerceAtLeast(0)
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
