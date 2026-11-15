package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = DarkOnPrimary,
  primaryContainer = DarkPrimaryContainer,
  onPrimaryContainer = IslamicGreenContainer,
  secondary = IslamicGoldAccent,
  onSecondary = Color.Black,
  secondaryContainer = DarkSurfaceCard,
  onSecondaryContainer = IslamicGoldLight,
  background = DarkBackground,
  surface = DarkSurface,
  surfaceVariant = DarkSurfaceCard,
  onBackground = DarkTextPrimary,
  onSurface = DarkTextPrimary,
  onSurfaceVariant = DarkTextSecondary,
  outline = DarkSurfaceBorder
)

private val LightColorScheme = lightColorScheme(
  primary = IslamicGreenPrimary,
  onPrimary = Color.White,
  primaryContainer = IslamicGreenContainer,
  onPrimaryContainer = OnIslamicGreenContainer,
  secondary = IslamicGoldAccent,
  onSecondary = Color.White,
  secondaryContainer = IslamicGoldContainer,
  onSecondaryContainer = OnIslamicGoldContainer,
  tertiary = IslamicGreenLight,
  background = OffWhiteBackground,
  surface = SurfacePureWhite,
  surfaceVariant = Color(0xFFF0F5F2),
  onBackground = TextPrimaryDark,
  onSurface = TextPrimaryDark,
  onSurfaceVariant = TextSecondaryMuted,
  outline = SurfaceBorder
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep school brand green and gold identity consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
