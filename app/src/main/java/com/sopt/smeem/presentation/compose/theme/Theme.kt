package com.sopt.smeem.presentation.compose.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = point,
    secondary = pointInactive,
    onPrimary = white,
    background = white,
    onBackground = black,
    surface = gray100,
    surfaceTint = white,
    onSurface = black
)

private val LightColorScheme = lightColorScheme(
    primary = point,
    secondary = pointInactive,
    onPrimary = white,
    background = white,
    onBackground = black,
    surface = gray100,
    surfaceTint = white,
    onSurface = black,
)

@Composable
fun SmeemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}