package xyz.ludothegreat.audiobooktv.playback

import xyz.ludothegreat.audiobooktv.domain.Bookmark

/**
 * Single normalizer for every list of bookmarks that reaches player state.
 *
 * Identity is the server id when both sides carry one, with timeSec as the
 * fallback, and same-timeSec entries always collapse regardless of id:
 * Audiobookshelf upserts bookmarks by time (verified against 2.35.1, where a
 * second POST at the same time updates the existing bookmark in place), so two
 * entries at one timestamp can only ever be two views of the same server
 * bookmark. That collapse is also what keeps the touch sheet's LazyColumn key
 * on timeSec legal; a duplicate key throws and takes the sheet down.
 *
 * Later entries win a collision: every merge appends the fresher server
 * response after the cached list, so keeping the later entry keeps server
 * truth (a rename or an upsert carries the newer title).
 *
 * Scope note: identity-by-time only holds within one book. A future global
 * bookmarks view must not feed a cross-book list through this normalizer
 * without adding libraryItemId to the identity.
 */
object BookmarkList {

    fun normalize(bookmarks: List<Bookmark>): List<Bookmark> {
        val out = ArrayList<Bookmark>(bookmarks.size)
        for (candidate in bookmarks) {
            out.removeAll { existing -> sameBookmark(existing, candidate) }
            out.add(candidate)
        }
        return out.sortedBy { it.timeSec }
    }

    /**
     * Shared identity rule so ViewModel replace/remove operations agree with
     * normalize about which entries are the same server bookmark.
     */
    fun sameBookmark(a: Bookmark, b: Bookmark): Boolean = (a.id != null && a.id == b.id) || a.timeSec == b.timeSec
}
