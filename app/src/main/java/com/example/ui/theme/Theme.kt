package com.example.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

/**
 * Local mobile-UI accent override applied exclusively when a Style Studio pack
 * with target = "mobile-ui" is activated. Never misrepresents TV launcher changes.
 */
@Immutable
data class MobileUiAccentOverride(
    val packId: String,
    val packName: String,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondaryDark: Color,
    val secondaryLight: Color
)

@Immutable
data class TVeoExtendedColors(
    val warning: Color,
    val warningContainer: Color,
    val reduceMotion: Boolean
)

val LocalTVeoExtendedColors = staticCompositionLocalOf {
    TVeoExtendedColors(
        warning = AmberWarningDark,
        warningContainer = AmberWarningContainerDark,
        reduceMotion = false
    )
}

val TVeoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private fun buildDarkScheme(override: MobileUiAccentOverride?): ColorScheme = darkColorScheme(
    primary = override?.primaryDark ?: MintTurquoiseDark,
    onPrimary = OnMintTurquoiseDark,
    primaryContainer = MintTurquoiseContainerDark,
    onPrimaryContainer = OnMintTurquoiseContainerDark,
    secondary = override?.secondaryDark ?: SoftIndigoDark,
    onSecondary = OnSoftIndigoDark,
    secondaryContainer = SoftIndigoContainerDark,
    onSecondaryContainer = OnSoftIndigoContainerDark,
    tertiary = SoftIndigoDark,
    background = InkDarkBackground,
    onBackground = InkDarkTextPrimary,
    surface = InkDarkSurface,
    onSurface = InkDarkTextPrimary,
    surfaceVariant = InkDarkSurfaceElevated,
    onSurfaceVariant = InkDarkTextSecondary,
    surfaceContainerLow = InkDarkSurface,
    surfaceContainer = InkDarkSurfaceElevated,
    surfaceContainerHigh = InkDarkSurfaceHighest,
    outline = InkDarkOutline,
    error = CoralErrorDark,
    onError = OnCoralErrorDark,
    errorContainer = CoralErrorContainerDark,
    onErrorContainer = InkDarkTextPrimary
)

private fun buildLightScheme(override: MobileUiAccentOverride?): ColorScheme = lightColorScheme(
    primary = override?.primaryLight ?: MintTurquoiseLight,
    onPrimary = OnMintTurquoiseLight,
    primaryContainer = MintTurquoiseContainerLight,
    onPrimaryContainer = OnMintTurquoiseContainerLight,
    secondary = override?.secondaryLight ?: SoftIndigoLight,
    onSecondary = OnSoftIndigoLight,
    secondaryContainer = SoftIndigoContainerLight,
    onSecondaryContainer = OnSoftIndigoContainerLight,
    tertiary = SoftIndigoLight,
    background = WarmLightBackground,
    onBackground = WarmLightTextPrimary,
    surface = WarmLightSurface,
    onSurface = WarmLightTextPrimary,
    surfaceVariant = WarmLightSurfaceElevated,
    onSurfaceVariant = WarmLightTextSecondary,
    surfaceContainerLow = WarmLightSurface,
    surfaceContainer = WarmLightSurfaceElevated,
    surfaceContainerHigh = WarmLightSurfaceHighest,
    outline = WarmLightOutline,
    error = CoralErrorLight,
    onError = OnCoralErrorLight,
    errorContainer = CoralErrorContainerLight,
    onErrorContainer = WarmLightTextPrimary
)

@Composable
fun TVeoCleanTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    userReduceMotion: Boolean = false,
    mobileAccentOverride: MobileUiAccentOverride? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> systemDark
    }

    val systemAnimDisabled = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    val effectiveReduceMotion = userReduceMotion || systemAnimDisabled

    val colorScheme = remember(useDark, mobileAccentOverride) {
        if (useDark) buildDarkScheme(mobileAccentOverride) else buildLightScheme(mobileAccentOverride)
    }

    val extendedColors = remember(useDark, effectiveReduceMotion) {
        TVeoExtendedColors(
            warning = if (useDark) AmberWarningDark else AmberWarningLight,
            warningContainer = if (useDark) AmberWarningContainerDark else AmberWarningContainerLight,
            reduceMotion = effectiveReduceMotion
        )
    }

    CompositionLocalProvider(LocalTVeoExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TVeoTypography,
            shapes = TVeoShapes,
            content = content
        )
    }
}
