package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = OnAmberPrimary,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    secondary = CoralSecondary,
    onSecondary = OnCoralSecondary,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = OnCoralContainer,
    tertiary = TealTertiary,
    onTertiary = OnTealTertiary,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = ObsidianBg,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SlateText,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    surfaceContainerHighest = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineVariant,
    error = ErrorCoral,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = AmberLightPrimary,
    onPrimary = OnAmberLightPrimary,
    primaryContainer = AmberLightContainer,
    onPrimaryContainer = OnAmberLightContainer,
    secondary = CoralLightSecondary,
    onSecondary = OnCoralLightSecondary,
    secondaryContainer = CoralLightContainer,
    onSecondaryContainer = OnCoralLightContainer,
    tertiary = TealLightTertiary,
    onTertiary = OnTealLightTertiary,
    tertiaryContainer = TealLightContainer,
    onTertiaryContainer = OnTealLightContainer,
    background = ParchmentBg,
    onBackground = CharcoalText,
    surface = ParchmentSurface,
    onSurface = CharcoalText,
    surfaceVariant = ParchmentSurfaceHigh,
    onSurfaceVariant = CharcoalMutedText,
    surfaceContainerLowest = ParchmentSurface,
    surfaceContainerLow = ParchmentBg,
    surfaceContainer = ParchmentSurfaceHigh,
    surfaceContainerHigh = ParchmentSurfaceHighest,
    surfaceContainerHighest = ParchmentSurfaceHighest,
    outline = ParchmentOutline,
    outlineVariant = ParchmentOutlineVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

val LuminaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun LuminaStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LuminaTypography,
        shapes = LuminaShapes,
        content = content
    )
}
