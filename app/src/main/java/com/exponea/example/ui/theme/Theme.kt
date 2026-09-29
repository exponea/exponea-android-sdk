package com.exponea.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrYellow,
    onPrimary = BrBlack,
    secondary = BrBlack,
    onSecondary = BrWhite,
    background = BrGrey100,
    onBackground = BrBlack,
    surface = BrWhite,
    onSurface = BrBlack,
    error = BrRed
)

@Composable
fun BloomreachTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = BloomreachTypography,
        content = content
    )
}
