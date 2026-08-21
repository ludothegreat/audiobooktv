package xyz.ludothegreat.audiobooktv.domain

/**
 * ABS minified metadata reports a missing text field as "" rather than null
 * (measured against Audiobookshelf 2.35.1: an untagged book has
 * authorName = "" while its title carries the raw folder string). Normalize
 * at the edge so every consumer works with "null means missing" instead of
 * each one re-checking for blank, and so a stale cache written before this
 * rule still renders correctly through the same helper.
 */
fun metadataField(raw: String?): String? {
    val trimmed = raw?.trim()
    return if (trimmed.isNullOrEmpty()) null else trimmed
}
