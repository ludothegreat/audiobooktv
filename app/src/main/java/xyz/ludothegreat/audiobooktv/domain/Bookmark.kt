package xyz.ludothegreat.audiobooktv.domain

/**
 * ABS bookmarks carry no server UUID (verified against Audiobookshelf 2.35.1:
 * the payload is libraryItemId + time + title + createdAt, and the server
 * addresses a bookmark by its time). [id] is the createdAt epoch-millis as a
 * string: unique per bookmark, preserved across renames, and the closest thing
 * to a server identity the API exposes. Null only for values built before the
 * server has answered; every server response populates it.
 */
data class Bookmark(
    val id: String? = null,
    val timeSec: Long,
    val title: String,
)
