package xyz.ludothegreat.audiobooktv.data.auth

/**
 * Audiobookshelf version gate (decision #25). The app declares a minimum
 * supported ABS server version and refuses to proceed with anything older.
 *
 * Version strings come back from ABS as e.g. `v2.20.0`, `2.21.0-beta`, or
 * `2.20.0-rc1`. The parser strips a leading `v` and drops any `-suffix`
 * before comparing, so pre-releases compare as their stable base.
 */
object VersionGate {
    const val MIN_SERVER_VERSION = "2.20.0"

    fun isSupported(reported: String, minimum: String = MIN_SERVER_VERSION): Boolean {
        val v = parse(reported) ?: return false
        val m = parse(minimum) ?: return false
        return compare(v, m) >= 0
    }

    internal fun parse(text: String): IntArray? {
        val cleaned = text.trim().removePrefix("v").substringBefore('-')
        val parts = cleaned.split('.')
        if (parts.isEmpty()) return null
        return IntArray(parts.size) { parts[it].toIntOrNull() ?: return null }
    }

    internal fun compare(a: IntArray, b: IntArray): Int {
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val ai = if (i < a.size) a[i] else 0
            val bi = if (i < b.size) b[i] else 0
            if (ai != bi) return ai - bi
        }
        return 0
    }
}
