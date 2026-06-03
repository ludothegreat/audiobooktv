package xyz.ludothegreat.audiobooktv.domain

data class Book(
    val id: String,
    val title: String,
    val author: String?,
    val series: String?,
    val coverUrl: String?,
    val durationSec: Long,
    val numChapters: Int,
    val progressFraction: Double, // 0.0 .. 1.0
    val isFinished: Boolean,
    val lastUpdate: Long, // epoch ms, 0 if never played
)
