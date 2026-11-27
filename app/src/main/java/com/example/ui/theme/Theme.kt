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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ForgeOrange,
    onPrimary = Color(0xFF1E1100),
    primaryContainer = Color(0xFF3E2200),
    onPrimaryContainer = ForgeOrangeBright,
    secondary = CyberCyan,
    onSecondary = Color(0xFF002029),
    secondaryContainer = Color(0xFF003B4A),
    onSecondaryContainer = Color(0xFFA7F0FF),
    tertiary = BoltViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2B1C5A),
    onTertiaryContainer = Color(0xFFDDD4FF),
    background = StudioNavyDark,
    onBackground = StudioTextPrimaryDark,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimaryDark,
    surfaceVariant = StudioCardDark,
    onSurfaceVariant = StudioTextSecondaryDark,
    surfaceContainerHigh = StudioCardAltDark,
    outline = StudioOutlineDark,
    error = CrimsonError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ForgeOrangeDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF3E1C00),
    secondary = CyberCyanDim,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF4FC),
    onSecondaryContainer = Color(0xFF002D38),
    tertiary = BoltViolet,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE7F6),
    onTertiaryContainer = Color(0xFF22124D),
    background = StudioBgLight,
    onBackground = StudioTextPrimaryLight,
    surface = StudioSurfaceLight,
    onSurface = StudioTextPrimaryLight,
    surfaceVariant = StudioCardLight,
    onSurfaceVariant = StudioTextSecondaryLight,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = StudioOutlineLight,
    error = CrimsonError,
    onError = Color.White
)

val BlockForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun BlockForgeTheme(
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
        typography = BlockForgeTypography,
        shapes = BlockForgeShapes,
        content = content
    )
}
