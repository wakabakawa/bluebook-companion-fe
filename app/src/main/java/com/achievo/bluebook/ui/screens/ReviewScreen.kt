package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

private enum class Filter(val label: String, val color: Color) {
    RIGHT("Right", CorrectGreen), WRONG("Wrong", WrongRed), BLANK("Blank", BlankBrown), FLAGGED("Flagged", FlagAmber)
}

/** FR11: item-by-item review with filters, tags and the page of the printed book. Never shows question text. */
@Composable
fun ReviewScreen(vm: AppViewModel, attemptId: String, onBack: () -> Unit) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    val key = s?.key
    val tags = s?.tags
    if (a == null || s == null || key == null || tags == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var filter by remember { mutableStateOf<Filter?>(null) }
    val indices = a.answers.indices.filter { i ->
        when (filter) {
            null -> true
            Filter.RIGHT -> a.answers[i] != null && a.answers[i] == key[i]
            Filter.WRONG -> a.answers[i] != null && a.answers[i] != key[i]
            Filter.BLANK -> a.answers[i] == null
            Filter.FLAGGED -> i in a.flagged
        }
    }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(actionLabel = "RESET", onAction = { filter = null })
        Text(
            "${s.branch}: ${s.setName.uppercase()}  •  ${indices.size} of ${a.answers.size} items",
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (indices.isEmpty()) item { Text("No items in this filter.", color = Muted) }
            items(indices) { i ->
                val t = tags[i]
                val line = listOfNotNull(t.branch, t.topic, t.keyConcept, t.cognitiveSkill, t.difficulty.label, t.bookPage?.let { "Book pg $it" })
                    .joinToString(" · ")
                AnswerReviewRow(i + 1, a.answers[i], key[i], i in a.flagged, line)
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Filter.values().forEach { f ->
                    Button(
                        onClick = { filter = if (filter == f) null else f },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = f.color.copy(alpha = if (filter == null || filter == f) 1f else 0.45f))
                    ) { Text(f.label, fontSize = 13.sp) }
                }
            }
            OutlineButton("Back", onBack, Modifier.fillMaxWidth())
        }
    }
}
