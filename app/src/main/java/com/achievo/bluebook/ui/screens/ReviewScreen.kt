package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.ItemOutcome
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

/** FR11, scoped to SEC-11: lists only the items the student got wrong, each tagged with its
 *  topic/subtopic/difficulty/book page so they know where to go study - never the correct
 *  letter, never a right-vs-wrong comparison. The backend itself no longer even returns
 *  enough to reconstruct one (submit_attempt() only ever returns is_correct). */
@Composable
fun ReviewScreen(vm: AppViewModel, attemptId: String, onBack: () -> Unit) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    val outcomes = a?.outcomes
    val tags = s?.tags
    if (a == null || s == null || outcomes == null || tags == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val wrongIndices = a.answers.indices.filter { outcomes[it] == ItemOutcome.WRONG }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        Text(
            "${s.branch}: ${s.setName.uppercase()}  •  ${wrongIndices.size} item(s) to review",
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold
        )
        LazyColumn(
            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (wrongIndices.isEmpty()) {
                item { Text("No wrong answers on this attempt - nice work!", color = Muted) }
            }
            items(wrongIndices) { i ->
                val t = tags[i]
                val line = listOfNotNull(t.topic, t.keyConcept, t.difficulty.label, t.bookPage?.let { "Book pg $it" })
                    .joinToString(" · ")
                MistakeRow(number = i + 1, chosen = a.answers[i], tagLine = line)
            }
        }
        OutlineButton("Back", onBack, Modifier.fillMaxWidth().padding(16.dp))
    }
}

@Composable
private fun MistakeRow(number: Int, chosen: Char?, tagLine: String) {
    AppCard(color = WrongBg) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$number.", fontFamily = Mono, fontWeight = FontWeight.Bold, color = WrongRed, modifier = Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text(if (chosen == null) "Left blank" else "You answered $chosen", fontWeight = FontWeight.SemiBold, color = WrongRed)
                Text(tagLine, fontSize = 12.sp, color = Muted)
            }
        }
    }
}
