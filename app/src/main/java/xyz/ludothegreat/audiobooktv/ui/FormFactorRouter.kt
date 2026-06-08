package xyz.ludothegreat.audiobooktv.ui

enum class UiSurface { Tv, Touch }

/**
 * Pure form-factor decision. The Activity passes
 * `packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)` and gets
 * back the presentation surface to mount. Keeping this as a Boolean-in / enum-out
 * helper means the routing rule is unit-testable without faking PackageManager.
 *
 * FEATURE_LEANBACK is the authoritative "Android TV launcher is present" signal --
 * a phone or tablet will not advertise it even when the APK declares the leanback
 * uses-feature. UiModeManager / Configuration.screenLayout are weaker signals
 * that change with foldables and DeX-style modes; Leanback is stable.
 */
object FormFactorRouter {
    fun choose(hasLeanback: Boolean): UiSurface = if (hasLeanback) UiSurface.Tv else UiSurface.Touch
}
