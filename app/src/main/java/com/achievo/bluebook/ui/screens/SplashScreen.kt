package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.achievo.bluebook.R
import com.achievo.bluebook.ui.theme.Maroon
import kotlinx.coroutines.delay

/** FR01: Achievo logo, then the app name, then Home with no login. */
@Composable
fun SplashScreen(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(1000); step = 1
        delay(1000); onDone()
    }
    Box(Modifier.fillMaxSize().background(Maroon), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().background(Color(0xFFF8F7F5)).padding(horizontal = 32.dp, vertical = 56.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(if (step == 0) R.drawable.logo_achievo else R.drawable.logo_maroon_bluebook),
                contentDescription = if (step == 0) "Achievo Institute" else "The Maroon Bluebook",
                modifier = Modifier.height(140.dp)
            )
        }
    }
}
