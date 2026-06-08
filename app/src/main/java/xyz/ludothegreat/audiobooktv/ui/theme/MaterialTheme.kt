package xyz.ludothegreat.audiobooktv.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// Material3 (touch) equivalents of the TV ColorSchemes in Theme.kt. Slot mapping
// matches the TV theme as closely as Material3 allows -- "border" is TV-only and
// has no Material3 slot, so it's dropped here. Tokens come from Palette so both
// surfaces stay in lockstep.

private val GruvboxM3 = darkColorScheme(
    primary = Palette.GruvboxGreen,
    onPrimary = Palette.GruvboxBg,
    primaryContainer = Palette.GruvboxGreenDim,
    onPrimaryContainer = Palette.GruvboxBg,
    secondary = Palette.GruvboxOrange,
    onSecondary = Palette.GruvboxBg,
    background = Palette.GruvboxBg,
    onBackground = Palette.GruvboxFg,
    surface = Palette.GruvboxBgAlt,
    onSurface = Palette.GruvboxFg,
    surfaceVariant = Palette.GruvboxBgAlt,
    onSurfaceVariant = Palette.GruvboxFgDim,
    error = Palette.GruvboxRed,
    onError = Palette.GruvboxFg,
    outline = Palette.GruvboxFgDim,
)

private val NeonLightningM3 = darkColorScheme(
    primary = Palette.NeonMagenta,
    onPrimary = Color.White,
    primaryContainer = Palette.NeonMagentaDeep,
    onPrimaryContainer = Color.White,
    secondary = Palette.NeonOrange,
    onSecondary = Palette.NeonBg,
    background = Palette.NeonBg,
    onBackground = Palette.NeonFg,
    surface = Palette.NeonSurface,
    onSurface = Palette.NeonFg,
    surfaceVariant = Palette.NeonRail,
    onSurfaceVariant = Palette.NeonFgDim,
    error = Palette.NeonRed,
    onError = Color.White,
    outline = Palette.NeonFgDim,
)

private fun materialSchemeFor(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.Gruvbox -> GruvboxM3
    AppTheme.NeonLightning -> NeonLightningM3
}

private fun materialTypographyFor(theme: AppTheme): Typography {
    val family = when (theme) {
        AppTheme.Gruvbox -> FontFamily.Default
        AppTheme.NeonLightning -> FontFamily.Monospace
    }
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.f(),
        displayMedium = base.displayMedium.f(),
        displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(),
        headlineMedium = base.headlineMedium.f(),
        headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f(),
        titleMedium = base.titleMedium.f(),
        titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(),
        bodyMedium = base.bodyMedium.f(),
        bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(),
        labelMedium = base.labelMedium.f(),
        labelSmall = base.labelSmall.f(),
    )
}

@Composable
fun AudiobooktvMaterialTheme(
    theme: AppTheme = AppTheme.Gruvbox,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = materialSchemeFor(theme),
        typography = materialTypographyFor(theme),
        content = content,
    )
}
