package com.achievo.bluebook.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.*
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*

/** Results tab: attempt history, kept on the device (FR23). */
@Composable
fun ResultsListScreen(vm: AppViewModel, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = vm.attempts.reversed().filter { a ->
        val s = vm.sheet(a.sheetId)
        query.isBlank() || (s != null && (s.branch.contains(query, true) || s.subject.contains(query, true) || s.setName.contains(query, true)))
    }
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar()
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true,
            placeholder = { Text("Search") },
            trailingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(24.dp)
        )
        if (list.isEmpty()) {
            Text(
                if (vm.attempts.isEmpty()) "No results yet. Finish an answer sheet to see your analysis here." else "No results match your search.",
                Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = Muted
            )
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { a ->
                val s = vm.sheet(a.sheetId)
                if (s != null) ResultSummaryCard(s, scoreOf(s, a), dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins") { onOpen(a.id) }
            }
        }
    }
}

/** Score card plus performance per difficulty (FR10, FR12, FR16). */
@Composable
fun ResultScreen(
    vm: AppViewModel,
    attemptId: String,
    onBack: () -> Unit,
    onAnalysis: () -> Unit,
    onKey: () -> Unit,
    onRetake: (Sheet) -> Unit
) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    if (a == null || s == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val score = scoreOf(s, a)
    val slices = difficultySlices(s, a)
    val colors = listOf(CorrectGreen, FlagAmber, WrongRed)
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ResultSummaryCard(s, score, dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins") }
            item { Spacer(Modifier.height(4.dp)) }
            itemsIndexed(slices) { i, sl ->
                LabeledBar(sl.label, if (sl.answered == 0) null else sl.percent, "Based on ${sl.answered} items..", colors[i])
            }
            item {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton("Analysis", onAnalysis, Modifier.weight(1f))
                    PrimaryButton("Answer Key", onKey, Modifier.weight(1f))
                }
            }
            item {
                // FR09: a retake is a NEW attempt; the submitted one is never edited.
                OutlineButton("Retake this set", { onRetake(s) }, Modifier.fillMaxWidth())
            }
        }
    }
}

/** Mastery per branch (all attempts), speed split, and weak topics with book pages (FR13, FR17, FR18, FR21). */
@Composable
fun MasteryScreen(vm: AppViewModel, attemptId: String, onBack: () -> Unit) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    if (a == null || s == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val score = scoreOf(s, a)
    val branches = branchMastery(vm.attempts, s.subject)
    val speed = speedOf(s, a)
    val weak = topicSlices(s, a).filter { it.answered > 0 }.sortedBy { it.percent }.take(3)

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ResultSummaryCard(s, score, dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins") }
            item { Text("${s.subject} mastery", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            items(branches) { b ->
                if (b.reliable) {
                    LabeledBar(b.label, b.percent, "Based on ${b.answered} items..", if (b.percent >= 70) CorrectGreen else if (b.percent >= 40) FlagAmber else WrongRed)
                } else {
                    // BR08: below the minimum, say so instead of showing a number
                    LabeledBar(b.label, null, if (b.answered == 0) "Too little data!" else "Too little data! (${b.answered} answered)", Muted)
                }
            }
            item { Text("Answering speed", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item {
                if (speed.answered == 0) {
                    Text("No answered items to analyse.", color = Muted)
                } else {
                    val rows = speed.buckets.chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { b ->
                                    val good = b.label.endsWith("Correct")
                                    val fg = if (good) CorrectGreen else WrongRed
                                    val pct = b.count * 100 / speed.answered
                                    Column(
                                        Modifier.weight(1f).background(if (good) CorrectBg else WrongBg, RoundedCornerShape(12.dp))
                                            .border(1.5.dp, fg, RoundedCornerShape(12.dp)).padding(10.dp)
                                    ) {
                                        Text("$pct%", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        Text(b.label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${b.count} item(s) in ${b.avgSeconds}s", color = Muted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        // FR17: state that a default reference time was applied (CC03 pending)
                        Text(
                            "Average ${speed.avgSeconds}s per item. Fast means ${AppConfig.REFERENCE_SECONDS}s or less (default reference time).",
                            fontSize = 11.sp, color = Muted
                        )
                    }
                }
            }
            if (weak.isNotEmpty()) {
                item {
                    AppCard(color = CardPink, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Areas to improve:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Maroon, thickness = 3.dp, modifier = Modifier.padding(vertical = 6.dp))
                        weak.forEachIndexed { i, t ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}.", color = Maroon, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.label, fontWeight = FontWeight.SemiBold)
                                    Text(listOfNotNull(s.setName, t.note).joinToString(" | "), fontSize = 11.sp, color = Muted)
                                }
                                Text("${t.percent}%  (${t.correct}/${t.answered})", color = Maroon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
