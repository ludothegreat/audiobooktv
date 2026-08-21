package xyz.ludothegreat.audiobooktv.ui.library

import xyz.ludothegreat.audiobooktv.domain.Book
import java.text.Normalizer

/**
 * The four library segments. ALL is the default and shows the whole grid;
 * the other three partition it by listening status derived from the
 * mediaProgress fields the library list already carries.
 */
enum class StatusSegment(val label: String) {
    ALL("All"),
    NEW("New"),
    STARTED("Started"),
    FINISHED("Finished"),
}

/**
 * Client-side search + status segmentation over the cached library list.
 * Pure so it can be locked down by JVM tests: no API calls, no Android
 * types. Filtering must preserve the incoming order because the list
 * arrives already sorted by LibrarySorter (decision #14) and each segment
 * keeps that ordering.
 */
internal object LibraryFilter {

    /**
     * Status from progress alone, not from lastUpdate: a mediaProgress row
     * with progress == 0 (opened but never listened) still reads as NEW.
     * FINISHED honours the explicit flag first because ABS can mark a book
     * finished at any fraction, and also catches progress >= 1 for servers
     * that never set the flag.
     */
    fun statusOf(book: Book): StatusSegment = when {
        book.isFinished || book.progressFraction >= 1.0 -> StatusSegment.FINISHED
        book.progressFraction > 0.0 -> StatusSegment.STARTED
        else -> StatusSegment.NEW
    }

    /** True when [book] matches [query] on title, author, or series. */
    fun matches(book: Book, query: String): Boolean = matchesFolded(book, fold(query.trim()))

    /**
     * The one entry point the ViewModel uses: segment first, then search,
     * with the query folded exactly once instead of per book. A plain
     * filter keeps relative order, which is what preserves LibrarySorter's
     * ordering inside every segment. Do not re-sort here.
     */
    fun apply(books: List<Book>, query: String, segment: StatusSegment): List<Book> {
        val folded = fold(query.trim())
        return books.filter { segmentAccepts(segment, it) && matchesFolded(it, folded) }
    }

    /**
     * Copy for the "segment/search came back empty" line. Shared by both
     * surfaces so TV and touch cannot drift apart. Only meaningful when the
     * unfiltered library is non-empty; the truly-empty-library states keep
     * their own messages.
     */
    fun emptyLine(query: String, segment: StatusSegment): String {
        val trimmed = query.trim()
        if (trimmed.isNotEmpty()) {
            val scope = if (segment == StatusSegment.ALL) "" else " in ${segment.label}"
            return "No matches for \"$trimmed\"$scope."
        }
        return when (segment) {
            StatusSegment.ALL -> "No books."
            StatusSegment.NEW -> "No new books. Everything here has been started."
            StatusSegment.STARTED -> "Nothing in progress. Pick a book to start listening."
            StatusSegment.FINISHED -> "No finished books yet."
        }
    }

    /**
     * Case-insensitive, diacritic-insensitive fold to ASCII-ish text.
     * NFD splits accented letters into base + combining marks and the marks
     * are dropped; the handful of letters NFD cannot decompose (folded
     * eszett, o-slash, ...) get explicit replacements. Both the query and
     * the book fields go through this, so the match works in either
     * direction ("Ámelie" finds "Amelie" and vice versa).
     */
    fun fold(raw: String): String {
        val decomposed = Normalizer.normalize(raw.lowercase(), Normalizer.Form.NFD)
        val out = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            if (Character.getType(ch) == Character.NON_SPACING_MARK.toInt()) continue
            when (ch) {
                'ß' -> out.append("ss")
                'æ' -> out.append("ae")
                'œ' -> out.append("oe")
                'ø' -> out.append('o')
                'đ', 'ð' -> out.append('d')
                'ł' -> out.append('l')
                'þ' -> out.append("th")
                'ı' -> out.append('i')
                else -> out.append(ch)
            }
        }
        return out.toString()
    }

    private fun segmentAccepts(segment: StatusSegment, book: Book): Boolean = segment == StatusSegment.ALL || statusOf(book) == segment

    private fun matchesFolded(book: Book, foldedQuery: String): Boolean {
        if (foldedQuery.isEmpty()) return true
        return fold(book.title).contains(foldedQuery) ||
            book.author?.let { fold(it).contains(foldedQuery) } == true ||
            book.series?.let { fold(it).contains(foldedQuery) } == true
    }
}
