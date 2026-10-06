package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = WaterPrimaryDark,
    onPrimary = WaterOnPrimaryDark,
    primaryContainer = WaterPrimaryContainerDark,
    onPrimaryContainer = WaterOnPrimaryContainerDark,
    secondary = WaterSecondaryDark,
    onSecondary = WaterOnSecondaryDark,
    secondaryContainer = WaterSecondaryContainerDark,
    onSecondaryContainer = WaterOnSecondaryContainerDark,
    tertiary = WaterTertiaryDark,
    onTertiary = WaterOnTertiaryDark,
    tertiaryContainer = WaterTertiaryContainerDark,
    onTertiaryContainer = WaterOnTertiaryContainerDark,
    background = WaterBackgroundDark,
    onBackground = WaterOnBackgroundDark,
    surface = WaterSurfaceDark,
    onSurface = WaterOnSurfaceDark,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = WaterPrimaryLight,
    onPrimary = WaterOnPrimaryLight,
    primaryContainer = WaterPrimaryContainerLight,
    onPrimaryContainer = WaterOnPrimaryContainerLight,
    secondary = WaterSecondaryLight,
    onSecondary = WaterOnSecondaryLight,
    secondaryContainer = WaterSecondaryContainerLight,
    onSecondaryContainer = WaterOnSecondaryContainerLight,
    tertiary = WaterTertiaryLight,
    onTertiary = WaterOnTertiaryLight,
    tertiaryContainer = WaterTertiaryContainerLight,
    onTertiaryContainer = WaterOnTertiaryContainerLight,
    background = WaterBackgroundLight,
    onBackground = WaterOnBackgroundLight,
    surface = WaterSurfaceLight,
    onSurface = WaterOnSurfaceLight,
  )

@Composable
fun HydroSenseTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun HydroTankTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) = HydroSenseTheme(darkTheme, dynamicColor, content)
