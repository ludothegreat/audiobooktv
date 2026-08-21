package xyz.ludothegreat.audiobooktv.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import coil3.compose.AsyncImage

/**
 * Cover rendering shared by both surfaces (player and library, TV and
 * touch). Two deliberate choices live here so the four call sites cannot
 * drift apart:
 *
 * - [ContentScale.Fit], never Crop: covers carry text, and center-cropping
 *   amputates it. Non-matching aspect ratios letterbox against
 *   [containerColor] instead.
 * - A designed empty state: the title's initials on the container color sit
 *   under the image, so "no cover" and "still loading" read as intentional
 *   instead of a blank rectangle that looks broken.
 *
 * Deliberately theme-agnostic (BasicText, no material import): callers on
 * each surface pass their own scheme's surfaceVariant/onSurfaceVariant
 * tokens, keeping Palette.kt the single color source.
 */
@Composable
fun CoverArt(
    model: Any?,
    contentDescription: String?,
    title: String,
    containerColor: Color,
    contentColor: Color,
    initialsSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        val initials = CoverInitials.from(title)
        if (initials.isNotEmpty()) {
            BasicText(
                text = initials,
                style = TextStyle(
                    color = contentColor,
                    fontSize = initialsSize,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
