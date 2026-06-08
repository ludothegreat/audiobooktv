package xyz.ludothegreat.audiobooktv.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw color tokens shared by both the TV theme (androidx.tv.material3) and the
 * touch theme (androidx.compose.material3). Keeping them in one file is the
 * only enforcement we have against the two surfaces drifting apart visually.
 *
 * Anyone introducing a new palette must add tokens here and consume them from
 * both `AudiobooktvTheme` (TV) and `AudiobooktvMaterialTheme` (touch).
 */
internal object Palette {
    // Gruvbox
    val GruvboxBg = Color(0xFF1D2021)
    val GruvboxBgAlt = Color(0xFF282828)
    val GruvboxFg = Color(0xFFEBDBB2)
    val GruvboxFgDim = Color(0xFFA89984)
    val GruvboxGreen = Color(0xFFB8BB26)
    val GruvboxGreenDim = Color(0xFF98971A)
    val GruvboxRed = Color(0xFFCC241D)
    val GruvboxOrange = Color(0xFFD65D0E)

    // NeonLightning
    val NeonBg = Color(0xFF000000)
    val NeonSurface = Color(0xFF3A3A3A)
    val NeonRail = Color(0xFF0E0E0E)
    val NeonFg = Color(0xFFE6E6E6)
    val NeonFgDim = Color(0xFF9A9A9A)
    val NeonMagenta = Color(0xFF91007D)
    val NeonMagentaDeep = Color(0xFF61094E)
    val NeonOrange = Color(0xFFFAA61A)
    val NeonRed = Color(0xFFF04747)
}
