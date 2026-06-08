package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Frosted Glass Dark Color Palette based on design system
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Indigo 400
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFFF472B6), // Pink 400
    onSecondary = Color(0xFF0F172A),
    tertiary = Color(0xFF38BDF8), // Sky 400
    background = Color(0xFF0F172A), // Slate 900 base
    onBackground = Color(0xFFF8FAFC), // Slate 50
    surface = Color(0x661E293B), // Translucent Slate 800 (40%)
    onSurface = Color(0xFFF1F5F9), // Slate 100
    surfaceVariant = Color(0x44334155), // Translucent Slate 700 (26%)
    onSurfaceVariant = Color(0xFFCBD5E1), // Slate 300
    outline = Color(0x33F1F5F9), // White outline alpha for glass borders
    outlineVariant = Color(0x1AF1F5F9) // Very soft white outline alpha
)

// Frosted Glass Light Color Palette based on design system
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5), // Indigo 600
    onPrimary = Color.White,
    secondary = Color(0xFFEC4899), // Pink 500
    onSecondary = Color.White,
    tertiary = Color(0xFF0EA5E9), // Sky 500
    background = Color(0xFFE0E7FF), // Lavender / Light Indigo base
    onBackground = Color(0xFF0F172A), // Slate 900
    surface = Color(0x99FFFFFF), // Translucent White (60% opacity)
    onSurface = Color(0xFF0F172A), // Slate 900
    surfaceVariant = Color(0x55FFFFFF), // Even more translucent White (33% opacity)
    onSurfaceVariant = Color(0xFF334155), // Slate 700
    outline = Color(0x80FFFFFF), // Transparent white border for glassmorphism
    outlineVariant = Color(0x33000000) // Transparent dark content shadow outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We disable dynamicColor by default to enforce our gorgeous Frosted Glass aesthetic!
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

