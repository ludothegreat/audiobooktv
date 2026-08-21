package xyz.ludothegreat.audiobooktv.ui.common

/**
 * Window in which the second press of a two-press bookmark delete must land
 * before the armed state reverts on its own. One constant shared by the TV
 * bookmark panel and the touch bookmark sheet so the two surfaces cannot
 * drift to different confirm behaviours (decision #28: surfaces diverge only
 * at the mount point, not in policy).
 */
const val DELETE_CONFIRM_WINDOW_MS = 3_000L
