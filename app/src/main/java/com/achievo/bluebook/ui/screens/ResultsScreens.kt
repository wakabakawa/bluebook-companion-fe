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
                if (s != null) ResultSummaryCard(s, scoreOf(a), dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins", onClick = { onOpen(a.id) })
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
    onRetake: (Sheet) -> Unit
) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    if (a == null || s == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var confirmWithdraw by remember { mutableStateOf(false) }
    if (confirmWithdraw) {
        NoticeDialog(
            title = "WITHDRAW THIS ATTEMPT?",
            body = "This permanently deletes this attempt and its answers. This cannot be undone.",
            confirm = "Withdraw", onConfirm = { confirmWithdraw = false; vm.withdrawAttempt(attemptId) { onBack() } },
            dismiss = "Cancel", onDismiss = { confirmWithdraw = false }
        )
    }
    vm.withdrawError?.let { err ->
        NoticeDialog(title = "COULDN'T WITHDRAW", body = err, confirm = "OK", onConfirm = { vm.dismissWithdrawError() })
    }
    val score = scoreOf(a)
    val diffSlices = difficultySlices(s, a)
    val skillSlices = cognitiveSkillSlices(s, a)
    val diffColors = listOf(CorrectGreen, FlagAmber, WrongRed)
    // Cyclic, not positional-semantic like diffColors — cognitive skill has no fixed cardinality
    // or easy/hard ordering, just distinct identity.
    val skillColors = listOf(SelectBlue, CorrectGreen, FlagAmber, WrongRed, Muted)
    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ResultSummaryCard(s, score, dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins") }
            item { Text("By difficulty", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
            itemsIndexed(diffSlices) { i, sl ->
                LabeledBar(sl.label, if (sl.answered == 0) null else sl.percent, "Based on ${sl.answered} items..", diffColors[i])
            }
            if (skillSlices.isNotEmpty()) {
                item { Text("By cognitive skill", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                itemsIndexed(skillSlices) { i, sl ->
                    LabeledBar(sl.label, if (sl.answered == 0) null else sl.percent, "Based on ${sl.answered} items..", skillColors[i % skillColors.size])
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                PrimaryButton("Analysis", onAnalysis, Modifier.fillMaxWidth())
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // FR09: a retake is a NEW attempt; the submitted one is never edited.
                    OutlineButton("Retake this set", { onRetake(s) }, Modifier.weight(1f))
                    OutlineButton("Withdraw this attempt", { confirmWithdraw = true }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** A mastery bar that falls back to "Too little data!" below BR08's minimum, used for every
 *  granularity (subject/branch/key concept) that shares this exact reliability gating. */
@Composable
private fun MasteryBar(slice: Slice) {
    if (slice.reliable) {
        LabeledBar(
            slice.label, slice.percent, "Based on ${slice.answered} items..",
            if (slice.percent >= 70) CorrectGreen else if (slice.percent >= 40) FlagAmber else WrongRed
        )
    } else {
        // BR08: below the minimum, say so instead of showing a number
        LabeledBar(slice.label, null, if (slice.answered == 0) "Too little data!" else "Too little data! (${slice.answered} answered)", Muted)
    }
}

/** Mastery per subject/branch/key-concept (all attempts), speed split, progress over time, and
 *  weak/strong topics with book pages (FR13, FR15, FR17, FR18, FR19, FR21). */
@Composable
fun MasteryScreen(
    vm: AppViewModel,
    attemptId: String,
    onReviewPlan: () -> Unit,
    onCohortComparison: (String) -> Unit,
    onBack: () -> Unit
) {
    val a = vm.attempt(attemptId)
    val s = a?.let { vm.sheet(it.sheetId) }
    if (a == null || s == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val score = scoreOf(a)
    val subjectSlice = subjectMastery(vm.sheets, vm.attempts, s.subject)
    val branches = branchMastery(vm.sheets, vm.attempts, s.subject)
    val keyConcepts = keyConceptMastery(vm.sheets, vm.attempts, s.subject)
    val speed = speedOf(a, s.tags)
    val weak = topicSlices(s, a).filter { it.answered > 0 }.sortedBy { it.percent }.take(3)
    val strong = topicSlices(s, a).filter { it.answered > 0 }.sortedByDescending { it.percent }.take(3)
    val topicsOnSheet = (s.tags ?: emptyList()).map { it.topic }.distinct()
    val trends = topicsOnSheet
        .map { topic -> topic to topicProgress(vm.sheets, vm.attempts, topic) }
        .filter { (_, pts) -> pts.size >= 2 }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar(onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ResultSummaryCard(s, score, dateText(a.submittedAt), "${mmss(a.totalSeconds)} mins") }
            item { Text("${s.subject} mastery", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item { MasteryBar(subjectSlice) }
            items(branches) { b -> MasteryBar(b) }
            if (keyConcepts.isNotEmpty()) {
                item { Text("Key concepts", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                items(keyConcepts) { kc -> MasteryBar(kc) }
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
                        // FR17: per-item reference time (CC03), falling back to the flat default only if untagged
                        Text(
                            "Average ${speed.avgSeconds}s per item. Fast means at or under each item's own " +
                                "reference time (by difficulty), falling back to ${AppConfig.REFERENCE_SECONDS}s only if untagged.",
                            fontSize = 11.sp, color = Muted
                        )
                    }
                }
            }
            if (trends.isNotEmpty()) {
                item { Text("Progress Over Time", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        trends.forEach { (topic, pts) -> ProgressTrendRow(topic, pts) }
                    }
                }
            }
            if (weak.isNotEmpty()) {
                item {
                    TopicRankCard("Areas to improve:", weak, null, s.setName, CardPink, Maroon) {
                        OutlineButton("View Review Plan", onReviewPlan, Modifier.fillMaxWidth())
                    }
                }
            }
            if (strong.isNotEmpty()) {
                item { TopicRankCard("Your strengths:", strong, null, s.setName, CorrectBg, CorrectGreen) }
            }
            item {
                OutlineButton(
                    "Compare with other readers", { onCohortComparison(s.subject) },
                    Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }
    }
}
