package xyz.ludothegreat.audiobooktv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.tv.material3.ColorScheme
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

enum class AppTheme(val displayName: String) {
    Gruvbox("Gruvbox"),
    NeonLightning("NeonLightning"),
}

private val GruvboxBg = Color(0xFF1D2021)
private val GruvboxBgAlt = Color(0xFF282828)
private val GruvboxFg = Color(0xFFEBDBB2)
private val GruvboxFgDim = Color(0xFFA89984)
private val GruvboxGreen = Color(0xFFB8BB26)
private val GruvboxGreenDim = Color(0xFF98971A)
private val GruvboxRed = Color(0xFFCC241D)
private val GruvboxOrange = Color(0xFFD65D0E)

private val GruvboxScheme = darkColorScheme(
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

private val NeonBg = Color(0xFF000000)
private val NeonSurface = Color(0xFF3A3A3A)
private val NeonRail = Color(0xFF0E0E0E)
private val NeonFg = Color(0xFFE6E6E6)
private val NeonFgDim = Color(0xFF9A9A9A)
private val NeonMagentaColor = Color(0xFF91007D)
private val NeonMagentaDeep = Color(0xFF61094E)
private val NeonOrange = Color(0xFFFAA61A)
private val NeonRed = Color(0xFFF04747)

private val NeonLightningScheme = darkColorScheme(
    primary = NeonMagentaColor,
    onPrimary = Color.White,
    primaryContainer = NeonMagentaDeep,
    onPrimaryContainer = Color.White,
    secondary = NeonOrange,
    onSecondary = NeonBg,
    background = NeonBg,
    onBackground = NeonFg,
    surface = NeonSurface,
    onSurface = NeonFg,
    surfaceVariant = NeonRail,
    onSurfaceVariant = NeonFgDim,
    error = NeonRed,
    onError = Color.White,
    border = NeonFgDim,
)

private fun schemeFor(theme: AppTheme): ColorScheme = when (theme) {
    AppTheme.Gruvbox -> GruvboxScheme
    AppTheme.NeonLightning -> NeonLightningScheme
}

private fun typographyFor(theme: AppTheme): Typography {
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
fun AudiobooktvTheme(theme: AppTheme = AppTheme.Gruvbox, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = schemeFor(theme),
        typography = typographyFor(theme),
        content = content,
    )
}
