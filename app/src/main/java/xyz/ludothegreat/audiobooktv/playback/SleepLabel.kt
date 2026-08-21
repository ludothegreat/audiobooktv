package xyz.ludothegreat.audiobooktv.playback

/**
 * Player control-row label rule for the sleep-timer button. The button
 * lives in the nine-control row alongside the two skip pairs, Play/Pause,
 * speed, Mark, and Ch n/N -- the row is width-budgeted to the pixel, so
 * every state of this label stays deliberately short.
 *
 *   no preset, no countdown        -> "Sleep"
 *   preset set, no countdown       -> "Nm"    (e.g. "5m", "30m")
 *   countdown active               -> "M:SS"  (e.g. "4:59")
 *   end-of-chapter only            -> "EOC"
 *   EOC after Nm, not yet ticking  -> "Nm+"   (the + means EOC follows)
 *   EOC after Nm, ticking          -> "M:SS+"
 *   countdown done, EOC pending    -> "EOC"
 *
 * [endOfChapter] is the EFFECTIVE mode for the loaded book: callers pass
 * false when the book has no chapters, because an end-of-chapter stop can
 * never fire there and the label must not promise one. [eocWaiting] means
 * the countdown part of a combined timer has already fired and playback
 * is now running out the chapter.
 */
fun formatSleepLabel(
    selectedMinutes: Int,
    remainingSec: Long?,
    endOfChapter: Boolean = false,
    eocWaiting: Boolean = false,
): String {
    if (eocWaiting) return "EOC"
    if (remainingSec != null && remainingSec > 0) {
        val m = remainingSec / 60
        val s = remainingSec % 60
        val ticking = "%d:%02d".format(m, s)
        return if (endOfChapter) "$ticking+" else ticking
    }
    if (endOfChapter) {
        return if (selectedMinutes > 0) "${selectedMinutes}m+" else "EOC"
    }
    if (selectedMinutes > 0) {
        return "${selectedMinutes}m"
    }
    return "Sleep"
}
