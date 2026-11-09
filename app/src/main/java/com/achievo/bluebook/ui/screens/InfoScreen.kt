package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.ui.components.AppCard
import com.achievo.bluebook.ui.components.BluebookTopBar
import com.achievo.bluebook.ui.theme.Mono
import com.achievo.bluebook.ui.theme.Maroon

private val guide = listOf(
    "Exam Date" to "1. Tap the Exam Date card on Home.\n2. Choose a date.\nThe card updates to show your target exam date.",
    "Answer Sheets" to "1. Tap \"Start New Sheet?\" or the Answer Sheet icon, then pick your set and subject.\n" +
        "2. Read the question on your book and tap A, B, C, or D here.\n" +
        "3. Tap the flag if you're not sure, so you can review it later.\n" +
        "4. Use \"Save and Exit\" to pause. Your sheet is kept on this device.\n" +
        "5. Tap Submit when you're done. Submitted sheets can't be edited.",
    "Results" to "1. Open Results from the navigation bar or the score card on Home.\n" +
        "2. Check your score, your performance per difficulty, your mastery per branch, and your answering speed.\n" +
        "3. Open the Answer Key to see which items to go back to, and which page of your book has the solution."
)

/** FR03: orientation guide, reachable from the navigation bar at any time. */
@Composable
fun InfoScreen() {
    var open by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar()
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("GUIDE", Modifier.fillMaxWidth(), fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
            item {
                AppCard {
                    Text(
                        "This app is a companion to your printed Maroon Bluebook. It does not contain the questions. " +
                            "Open your book to the exam set, then encode your answers on the digital answer sheet.",
                        fontSize = 14.sp
                    )
                }
            }
            items(guide) { (title, body) ->
                val shape = RoundedCornerShape(14.dp)
                Column {
                    Box(
                        Modifier.fillMaxWidth().clip(shape).background(Maroon).clickable { open = if (open == title) null else title }.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium) }
                    if (open == title) Text(body, Modifier.padding(16.dp), fontFamily = Mono, fontSize = 14.sp)
                }
            }
        }
    }
}
