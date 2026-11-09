package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.ui.theme.Maroon
import kotlinx.coroutines.delay

/** FR01: Achievo logo, then the app name, then Home with no login. TODO: swap text for the logo drawables. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(1000); step = 1
        delay(1000); onDone()
    }
    Box(Modifier.fillMaxSize().background(Maroon), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().background(Color(0xFFF8F7F5)).padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
            if (step == 0) {
                Text("ACHIEVO INSTITUTE", color = Color(0xFF1B6B3A), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                Text("the MAROON BLUEBOOK", color = Maroon, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
