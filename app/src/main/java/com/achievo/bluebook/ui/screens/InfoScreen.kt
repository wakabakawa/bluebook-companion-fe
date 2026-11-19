package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.ui.components.AppCard
import com.achievo.bluebook.ui.components.BluebookTopBar
import com.achievo.bluebook.ui.theme.Maroon
import com.achievo.bluebook.ui.theme.Muted

private data class GuideTopic(val title: String, val icon: ImageVector, val body: String)

private val guide = listOf(
    GuideTopic(
        "Exam Date", Icons.Outlined.CalendarMonth,
        "1. Tap the Exam Date card on Home.\n2. Choose a date.\nThe card updates to show your target exam date."
    ),
    GuideTopic(
        "Answer Sheets", Icons.Outlined.ListAlt,
        "1. Tap \"Start New Sheet?\" or the Answer Sheet tab, then pick your set and subject.\n" +
            "2. Read the question on your book and tap A, B, C, or D here.\n" +
            "3. Tap the flag if you're not sure, so you can review it later.\n" +
            "4. \"Save and Exit\" pauses and returns you to Home - your sheet stays saved on this device.\n" +
            "5. Tap Submit when you're done. Submitted sheets can't be edited."
    ),
    GuideTopic(
        "Results & Analysis", Icons.Outlined.Analytics,
        "1. Open Results from the navigation bar, or tap your latest score card on Home.\n" +
            "2. Your score breaks down into Correct, Wrong, Blank, and Flagged.\n" +
            "3. Tap \"Analysis\" to see your mastery by subject, branch, and key concept, your performance by " +
            "difficulty and cognitive skill, your answering speed, and your progress over time."
    ),
    GuideTopic(
        "Review Plan", Icons.AutoMirrored.Outlined.MenuBook,
        "1. Under \"Areas to improve\" (on Home or Analysis), tap \"View Review Plan\".\n" +
            "2. Set your study time budget, then tap \"Build plan\".\n" +
            "3. Your plan ranks topics to review first, with the book page for each - \"LOW DATA\" means you've " +
            "answered too few items of that topic to be sure yet."
    ),
    GuideTopic(
        "Cohort Comparison", Icons.Outlined.Groups,
        "On Analysis, tap \"Compare with other readers\" to see how your mastery per topic compares to other " +
            "anonymous readers of this book. Never another person's identity - just an average, and only once " +
            "enough readers have attempted that topic."
    ),
    GuideTopic(
        "Estimated Score", Icons.Outlined.Insights,
        "On Home, tap \"Estimated Entrance Test Score\" for a rough score range based on your overall mastery. " +
            "This is a placeholder estimate, not a real prediction - Achievo hasn't supplied real passing-score " +
            "cutoffs yet."
    ),
    GuideTopic(
        "Deleting a Sheet or Result", Icons.Outlined.DeleteOutline,
        "• An unfinished sheet: on Home, press and hold the \"Continue?\" card to discard it.\n" +
            "• A submitted result: open it, then tap \"Withdraw Set\" next to \"Retake Set\".\n" +
            "Both are permanent - your answers can't be recovered after."
    )
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
                Text(
                    "Tap a topic below for step-by-step help.", Modifier.fillMaxWidth(),
                    fontSize = 13.sp, color = Muted, textAlign = TextAlign.Center
                )
            }
            item {
                AppCard {
                    Text(
                        "This app is a companion to your printed Maroon Bluebook. It does not contain the questions. " +
                            "Open your book to the exam set, then encode your answers on the digital answer sheet.",
                        fontSize = 14.sp
                    )
                }
            }
            items(guide) { topic ->
                val isOpen = open == topic.title
                AppCard(onClick = { open = if (isOpen) null else topic.title }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(topic.icon, contentDescription = null, tint = Maroon, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(topic.title, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Icon(if (isOpen) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = Muted)
                    }
                    if (isOpen) {
                        HorizontalDivider(color = Maroon, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                        Text(topic.body, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
