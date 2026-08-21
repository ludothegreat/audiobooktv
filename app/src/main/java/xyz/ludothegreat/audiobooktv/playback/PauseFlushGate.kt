package xyz.ludothegreat.audiobooktv.playback

/**
 * Decides whether the isPlaying=false player callback may flush a progress
 * sync (the pause flush that makes "sync happens AT pause" true).
 *
 * The one case where it must not: a book switch while the old book is
 * playing. load() installs the NEW session id and the NEW track table
 * before setMediaItems stops the old playback, and Media3 delivers the
 * resulting isPlaying=false only afterwards. A flush at that moment reads a
 * mid-transition playhead against the new session: the old book's absolute
 * position lands in the NEW book's progress. That is cross-book bleed
 * (resume-integrity case C1), observed live: opening a 3% book while a 25%
 * book was playing moved the fresh book's server position to 12398s.
 *
 * load() flushes the old session itself (with the old track table, at the
 * old position) and then arms this gate exactly when it knows the stop
 * callback is coming (old playback was live). The callback consumes the
 * armed state once; every later pause flushes normally. Pure logic so the
 * arm-consume contract is provable on the JVM.
 */
class PauseFlushGate {
    var armed = false
        private set

    /** The imminent isPlaying=false belongs to a book switch, not the user. */
    fun armForTransition() {
        armed = true
    }

    /**
     * True when the pause callback should flush progress. Consumes one armed
     * transition, so suppression can never outlive the switch that armed it.
     */
    fun shouldFlushOnPause(): Boolean {
        if (armed) {
            armed = false
            return false
        }
        return true
    }
}
