package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Shapes

private val OledMonochromeColorScheme = ColorScheme(
  primary = PureWhite,
  primaryDim = SoftWhite,
  primaryContainer = PureWhite,
  onPrimary = OledBlack,
  onPrimaryContainer = OledBlack,
  secondary = SoftWhite,
  secondaryDim = SilverMuted,
  secondaryContainer = GraphiteElevated,
  onSecondary = OledBlack,
  onSecondaryContainer = PureWhite,
  tertiary = PureWhite,
  tertiaryDim = SilverMuted,
  tertiaryContainer = GraphiteSurface,
  onTertiary = OledBlack,
  onTertiaryContainer = PureWhite,
  surfaceContainerLow = GraphiteSurface,
  surfaceContainer = GraphiteElevated,
  surfaceContainerHigh = GraphiteOutline,
  onSurface = PureWhite,
  onSurfaceVariant = SilverMuted,
  outline = GraphiteOutline,
  outlineVariant = TrackDim,
  background = OledBlack,
  onBackground = PureWhite,
)

@Composable
fun WristChronoWatchTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = OledMonochromeColorScheme,
    typography = WatchTypography,
    shapes = Shapes(),
    content = content,
  )
}
