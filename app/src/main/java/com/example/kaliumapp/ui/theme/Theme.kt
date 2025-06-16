package com.example.kaliumapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun KaliumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val textColor = if (darkTheme) TextColorDark else TextColorLight

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = PrimaryColor,
            secondary = SecondaryColor,
            background = Color(0xFF121212),
            onBackground = textColor,
            onSurface = textColor,
            onPrimary = Color.Black,
            onSecondary = Color.White
        )
    } else {
        lightColorScheme(
            primary = PrimaryColor,
            secondary = SecondaryColor,
            background = BackgroundColor,
            onBackground = textColor,
            onSurface = textColor,
            onPrimary = Color.Black,
            onSecondary = Color.White
        )
    }

    val appTypography = Typography(
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = textColor
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = textColor
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = textColor
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography,
        content = content
    )
}