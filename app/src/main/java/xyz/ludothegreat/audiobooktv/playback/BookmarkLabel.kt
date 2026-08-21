package xyz.ludothegreat.audiobooktv.playback

/**
 * Row-text rule for the bookmark lists on both surfaces. addBookmarkHere
 * titles a new bookmark with the formatted timestamp itself, so an
 * unrenamed bookmark used to render as "1:23:45  1:23:45". Display only:
 * BookmarkList and the domain model are untouched, so renames and server
 * payloads still carry the literal title.
 */
object BookmarkLabel {

    /**
     * The secondary label for a row that always leads with
     * formatTimestampHms(timeSec): null when the title is just that same
     * timestamp (print it once), [blankFallback] when the title is blank,
     * and the title itself otherwise. The comparison trims so a padded
     * default still collapses, but a kept custom title is returned
     * untouched.
     */
    fun secondary(title: String, timeSec: Long, blankFallback: String): String? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return blankFallback
        return if (trimmed == formatTimestampHms(timeSec)) null else title
    }
}
