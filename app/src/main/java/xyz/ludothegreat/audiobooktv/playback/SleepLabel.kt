package xyz.ludothegreat.audiobooktv.playback

/**
 * Player control-row label rule for the sleep-timer button. The button
 * lives in a 6-control row alongside « 30, Play/Pause, 30 », speed, and
 * Mark — when Play swaps to Pause (5 chars) the row gets tight, so the
 * label must stay short.
 *
 *   no preset, no countdown   -> "Sleep"
 *   preset set, no countdown  -> "Nm"   (e.g. "5m", "30m")
 *   countdown active          -> "M:SS" (e.g. "4:59")
 */
fun formatSleepLabel(selectedMinutes: Int, remainingSec: Long?): String {
    if (remainingSec != null && remainingSec > 0) {
        val m = remainingSec / 60
        val s = remainingSec % 60
        return "%d:%02d".format(m, s)
    }
    if (selectedMinutes > 0) {
        return "${selectedMinutes}m"
    }
    return "Sleep"
}
