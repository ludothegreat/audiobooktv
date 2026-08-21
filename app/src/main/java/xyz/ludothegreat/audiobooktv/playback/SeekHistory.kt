package xyz.ludothegreat.audiobooktv.playback

/**
 * Why a seek needs a cause: the undo affordance and diagnostics both want to
 * know what kind of jump produced a position, and the smoothing merge with the
 * chapter work needs a slot for its jumps without touching this file again.
 */
enum class SeekCause {
    Skip,
    Scrub,
    BookmarkJump,
    ChapterJump,
    Undo,
}

data class SeekRecord(
    val fromSec: Long,
    val toSec: Long,
    val cause: SeekCause,
    val atMs: Long,
)

/**
 * Ring buffer of the last user-initiated seeks, powering "undo seek".
 *
 * Undo is itself a normal seek through the clamp-seek-push path, and it is
 * recorded here like any other (cause = [SeekCause.Undo]). That is what makes
 * undo-of-undo work as redo with no second code path: after undoing A -> B the
 * newest record is B -> A, so the next undo offers A again.
 *
 * Not thread-safe by design: every caller is a ViewModel entry point on the
 * main thread.
 */
class SeekHistory(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "SeekHistory capacity must be positive, got $capacity" }
    }

    private val records = ArrayDeque<SeekRecord>()

    val size: Int get() = records.size

    /**
     * Records one user-initiated seek. Returns false when the seek carries
     * nothing to undo: a no-move seek (from == to), or an exact repeat of the
     * newest record, which is what a double-fired UI control produces
     * (Compose can deliver onValueChangeFinished twice for one drag).
     */
    fun record(fromSec: Long, toSec: Long, cause: SeekCause, atMs: Long): Boolean {
        if (fromSec == toSec) return false
        val newest = records.lastOrNull()
        val doubleFired = newest != null &&
            newest.fromSec == fromSec &&
            newest.toSec == toSec &&
            newest.cause == cause
        if (doubleFired) return false
        records.addLast(SeekRecord(fromSec = fromSec, toSec = toSec, cause = cause, atMs = atMs))
        while (records.size > capacity) {
            records.removeFirst()
        }
        return true
    }

    /** The newest record; undoing means seeking back to its fromSec. */
    fun peekUndo(): SeekRecord? = records.lastOrNull()

    /**
     * Drops all history. Must be called when the loaded book changes:
     * a fromSec from another book is a valid-looking position that would
     * push a bogus seek to the server, and every seek push is data loss
     * territory, not a glitch.
     */
    fun clear() {
        records.clear()
    }

    /** Oldest-to-newest view for tests and diagnostics. */
    fun snapshot(): List<SeekRecord> = records.toList()

    companion object {
        const val DEFAULT_CAPACITY: Int = 10
    }
}
