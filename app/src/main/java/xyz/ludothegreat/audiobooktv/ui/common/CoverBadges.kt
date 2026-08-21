package xyz.ludothegreat.audiobooktv.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cover-status overlays shared by the TV and touch library cards. Like
 * CoverArt they are deliberately toolkit-agnostic (foundation only, colors
 * passed by the caller) so one implementation serves both surfaces and
 * Palette.kt stays the single color source.
 */

/**
 * Thin progress strip along a cover's bottom edge for STARTED books.
 * Callers derive [fraction] via CardMeta.barFraction; the coerce here is
 * only defence for future call sites, not a place to hide bad math.
 */
@Composable
fun CoverProgressBar(
    fraction: Float,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 4.dp,
) {
    Box(modifier = modifier.fillMaxWidth().height(height).background(trackColor)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(fillColor),
        )
    }
}

/** Filled-circle check for FINISHED covers, kept at full strength on top of the dimmed cover. */
@Composable
fun FinishedCheckBadge(
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Box(
        modifier = modifier.size(size).background(containerColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            imageVector = Icons.Filled.Check,
            contentDescription = "Finished",
            colorFilter = ColorFilter.tint(contentColor),
            modifier = Modifier.size(size * 0.68f),
        )
    }
}
