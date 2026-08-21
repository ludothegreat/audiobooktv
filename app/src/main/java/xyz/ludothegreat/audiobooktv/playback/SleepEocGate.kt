package xyz.ludothegreat.audiobooktv.playback

/**
 * End-of-chapter sleep gate: armed, it watches the stream of chapter
 * indices from the player ticker and says "pause now" exactly once, at
 * the first chapter boundary playback crosses on its own.
 *
 * The rules that make it safe to leave running against live playback:
 *
 *  - It only ever compares indices; it never touches the player. The
 *    ViewModel owns the pause, so this stays pure and JVM-testable.
 *  - A user seek is not a boundary. The ViewModel calls [reseed] from its
 *    seek path, so jumping into another chapter re-targets the stop to
 *    that chapter's end instead of firing mid-listen. The semantic is
 *    always "stop when the chapter I am hearing runs out".
 *  - Entering a chapter from null (armed before chapters were known, or
 *    playback sitting in a between-chapters gap) starts tracking without
 *    firing: nothing the user was hearing has ended yet. Leaving a
 *    chapter -- to the next one or into a gap -- is the boundary.
 *  - Firing consumes the arm. Whether the next play re-arms is the
 *    ViewModel's policy (it mirrors the countdown preset rules), not
 *    this class's.
 *
 * Not thread-safe by design: every caller is a ViewModel entry point on
 * the main thread, same contract as [SeekHistory].
 */
class SleepEocGate {

    var armed: Boolean = false
        private set

    private var trackedIndex: Int? = null

    /** Arms the stop, tracking from the chapter currently playing (or null). */
    fun arm(currentChapterIndex: Int?) {
        armed = true
        trackedIndex = currentChapterIndex
    }

    /** The off switch: nothing fires until [arm] is called again. */
    fun disarm() {
        armed = false
        trackedIndex = null
    }

    /**
     * Re-targets the gate after a user-initiated seek (or a playback resume
     * whose position may have moved externally) without treating the move
     * as a boundary. No-op while disarmed.
     */
    fun reseed(currentChapterIndex: Int?) {
        if (!armed) return
        trackedIndex = currentChapterIndex
    }

    /**
     * Feed one ticker observation. Returns true exactly when playback just
     * crossed out of a tracked chapter -- the caller pauses. The arm is
     * consumed on fire.
     */
    fun onChapterTick(chapterIndex: Int?): Boolean {
        if (!armed) return false
        val previous = trackedIndex
        if (chapterIndex == previous) return false
        trackedIndex = chapterIndex
        if (previous == null) return false
        disarm()
        return true
    }
}
