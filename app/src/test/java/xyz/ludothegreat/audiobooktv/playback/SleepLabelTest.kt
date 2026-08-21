package xyz.ludothegreat.audiobooktv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the compact sleep-button label rule. The control row had a wrap
 * regression on the TV device once the Play button swapped to Pause -- the
 * 6th button got squeezed and the word "Sleep" word-wrapped to "Sle /
 * ep". Labels here are deliberately short so 6 buttons + Pause always
 * fit in the row.
 */
class SleepLabelTest {

    @Test
    fun `idle returns Sleep`() {
        assertEquals("Sleep", formatSleepLabel(selectedMinutes = 0, remainingSec = null))
        assertEquals("Sleep", formatSleepLabel(selectedMinutes = 0, remainingSec = 0))
    }

    @Test
    fun `armed preset shows minutes only`() {
        assertEquals("5m", formatSleepLabel(selectedMinutes = 5, remainingSec = null))
        assertEquals("30m", formatSleepLabel(selectedMinutes = 30, remainingSec = null))
        assertEquals("60m", formatSleepLabel(selectedMinutes = 60, remainingSec = null))
    }

    @Test
    fun `active countdown shows M colon SS without the Sleep prefix`() {
        assertEquals("4:59", formatSleepLabel(selectedMinutes = 5, remainingSec = 299))
        assertEquals("0:30", formatSleepLabel(selectedMinutes = 5, remainingSec = 30))
        assertEquals("0:01", formatSleepLabel(selectedMinutes = 5, remainingSec = 1))
    }

    @Test
    fun `over an hour still uses M colon SS, not H colon MM colon SS`() {
        // 60 min preset = 3600s on initial tick; rendering it as "60:00"
        // is fine -- the button text is already 4 chars wide so there's
        // room. We don't ship a >60 preset so we don't worry about that.
        assertEquals("60:00", formatSleepLabel(selectedMinutes = 60, remainingSec = 3600))
    }

    @Test
    fun `zero remaining drops back to the armed minutes label`() {
        assertEquals("5m", formatSleepLabel(selectedMinutes = 5, remainingSec = 0))
    }

    @Test
    fun `end of chapter alone shows EOC in every idle state`() {
        assertEquals("EOC", formatSleepLabel(selectedMinutes = 0, remainingSec = null, endOfChapter = true))
        assertEquals("EOC", formatSleepLabel(selectedMinutes = 0, remainingSec = 0, endOfChapter = true))
    }

    @Test
    fun `combined mode armed shows the minutes with a plus suffix`() {
        assertEquals("5m+", formatSleepLabel(selectedMinutes = 5, remainingSec = null, endOfChapter = true))
        assertEquals("30m+", formatSleepLabel(selectedMinutes = 30, remainingSec = null, endOfChapter = true))
    }

    @Test
    fun `combined mode ticking keeps the plus suffix`() {
        assertEquals("4:59+", formatSleepLabel(selectedMinutes = 5, remainingSec = 299, endOfChapter = true))
        assertEquals("0:01+", formatSleepLabel(selectedMinutes = 5, remainingSec = 1, endOfChapter = true))
    }

    @Test
    fun `combined mode after the countdown fires shows EOC while the chapter runs out`() {
        assertEquals(
            "EOC",
            formatSleepLabel(selectedMinutes = 5, remainingSec = null, endOfChapter = true, eocWaiting = true),
        )
    }

    @Test
    fun `eoc waiting outranks a stale ticking value`() {
        // Belt and braces: if state ever carries both, the chapter phase is
        // the truth the user needs.
        assertEquals(
            "EOC",
            formatSleepLabel(selectedMinutes = 5, remainingSec = 90, endOfChapter = true, eocWaiting = true),
        )
    }

    @Test
    fun `plain labels are untouched when eoc flags are off`() {
        assertEquals("4:59", formatSleepLabel(selectedMinutes = 5, remainingSec = 299))
        assertEquals("5m", formatSleepLabel(selectedMinutes = 5, remainingSec = null))
        assertEquals("Sleep", formatSleepLabel(selectedMinutes = 0, remainingSec = null))
    }
}
