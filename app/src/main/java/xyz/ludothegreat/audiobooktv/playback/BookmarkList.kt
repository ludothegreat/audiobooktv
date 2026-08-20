package xyz.ludothegreat.audiobooktv.playback

import xyz.ludothegreat.audiobooktv.domain.Bookmark

/**
 * Single normalizer for every list of bookmarks that reaches player state.
 *
 * [Bookmark] carries no id, so timeSec is the only identity available and the
 * touch bookmark sheet keys its LazyColumn on it. Two bookmarks at the same
 * second -- trivial to make by tapping "add bookmark" twice, and also possible
 * from a second client writing to the same Audiobookshelf account -- would
 * throw "Key was already used" and take the sheet down. Enforcing uniqueness
 * here is what makes that key legal.
 *
 * Earliest-inserted wins on a tie; the title is derived from the timestamp, so
 * duplicates carry no distinct information to preserve.
 */
object BookmarkList {
    fun normalize(bookmarks: List<Bookmark>): List<Bookmark> = bookmarks.distinctBy { it.timeSec }.sortedBy { it.timeSec }
}
