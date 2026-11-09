package com.achievo.bluebook.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily

// TODO: add Inter (headings/body) under res/font and swap in here. Monospace stands in for the number font.
val Mono = FontFamily.Monospace

@Composable
fun BluebookTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.statusBarColor = Maroon.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Maroon,
            onPrimary = Color.White,
            secondary = SelectBlue,
            background = AppBg,
            surface = Color.White,
            onSurface = Color(0xFF1B1B1F),
            error = WrongRed
        ),
        content = content
    )
}
