package xyz.ludothegreat.audiobooktv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val GruvboxBg = Color(0xFF1D2021)
private val GruvboxBgAlt = Color(0xFF282828)
private val GruvboxFg = Color(0xFFEBDBB2)
private val GruvboxFgDim = Color(0xFFA89984)
private val GruvboxGreen = Color(0xFFB8BB26)
private val GruvboxGreenDim = Color(0xFF98971A)
private val GruvboxRed = Color(0xFFCC241D)
private val GruvboxOrange = Color(0xFFD65D0E)

private val AudiobooktvColors = darkColorScheme(
    primary = GruvboxGreen,
    onPrimary = GruvboxBg,
    primaryContainer = GruvboxGreenDim,
    onPrimaryContainer = GruvboxBg,
    secondary = GruvboxOrange,
    onSecondary = GruvboxBg,
    background = GruvboxBg,
    onBackground = GruvboxFg,
    surface = GruvboxBgAlt,
    onSurface = GruvboxFg,
    surfaceVariant = GruvboxBgAlt,
    onSurfaceVariant = GruvboxFgDim,
    error = GruvboxRed,
    onError = GruvboxFg,
    border = GruvboxFgDim,
)

@Composable
fun AudiobooktvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AudiobooktvColors,
        content = content,
    )
}
