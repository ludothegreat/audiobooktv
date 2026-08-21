package xyz.ludothegreat.audiobooktv.ui.player

/**
 * One formatter for every skip-control label on both surfaces, fed by the
 * SeekTargets increment constants so the text can never drift from the
 * seek the button performs. The pattern is uniform across magnitudes:
 * value plus unit ("30s", "5m"), guillemet on the outside for direction.
 * No space inside the label: the TV player column is 488dp and the
 * seven-control row spends the character the space would cost (the unit
 * suffix itself is what fits in its place).
 */
internal object SkipLabels {

    /**
     * "30s" / "5m". Whole minutes read in minutes; anything else stays in
     * seconds ("90s", never a rounded "2m" lie).
     */
    fun amount(seconds: Long): String = if (seconds >= 60 && seconds % 60 == 0L) {
        "${seconds / 60}m"
    } else {
        "${seconds}s"
    }

    fun back(seconds: Long): String = "«${amount(seconds)}"

    fun forward(seconds: Long): String = "${amount(seconds)}»"
}
