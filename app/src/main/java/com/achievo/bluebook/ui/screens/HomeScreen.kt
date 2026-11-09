package com.achievo.bluebook.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achievo.bluebook.data.scoreOf
import com.achievo.bluebook.data.topicSlices
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onStartNew: () -> Unit,
    onContinue: (String) -> Unit,
    onOpenResult: (String) -> Unit,
    onReviewPlan: () -> Unit
) {
    val activity = LocalContext.current as? Activity
    var showPicker by remember { mutableStateOf(false) }

    // FR04: privacy notice must be acknowledged before the first submission. Shown on first launch.
    if (!vm.privacyAccepted) {
        NoticeDialog(
            title = "PRIVACY NOTICE",
            body = "ALL submissions are anonymous, and data will be handled in compliance with the Data Privacy Act of 2012!",
            confirm = "Agree", onConfirm = { vm.acceptPrivacy() },
            dismiss = "Exit", onDismiss = { activity?.finish() }
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.updateExamDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }

    val draft = vm.draft
    val draftSheet = draft?.let { vm.sheet(it.sheetId) }
    val latest = vm.attempts.lastOrNull()
    val latestSheet = latest?.let { vm.sheet(it.sheetId) }
    val weak = if (latest != null && latestSheet != null) {
        topicSlices(latestSheet, latest).filter { it.answered > 0 }.sortedBy { it.percent }.take(3)
    } else emptyList()

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar()
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Exam date card (in the hi-fi, but not yet in any requirement or user story)
            item {
                AppCard(onClick = { showPicker = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            val iso = vm.examDate
                            if (iso == null) {
                                Text("No Exam Date Set", color = FlagAmber, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("You haven't set your target exam date yet.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                            } else {
                                Text("Exam Date:", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("MM/dd/yyyy")), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Maroon)
                    }
                }
            }
            item {
                AppCard(onClick = onStartNew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ListAlt, contentDescription = null, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Start New Sheet?", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Maroon)
                    }
                }
            }
            // FR07: unfinished sheet with set name
            if (draft != null && draftSheet != null) {
                item {
                    AppCard(onClick = { onContinue(draftSheet.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ListAlt, contentDescription = null, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Continue?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${draftSheet.subject.take(4).uppercase()} - ${draftSheet.setName.uppercase()}", fontSize = 12.sp, color = Muted)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text(" ${mmss(draftSheet.allotmentSeconds - draft.elapsedSeconds)} mins", fontSize = 11.sp, color = Muted)
                                }
                            }
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = Maroon)
                        }
                    }
                }
            }
            if (latest != null && latestSheet != null) {
                item {
                    ResultSummaryCard(
                        latestSheet, scoreOf(latestSheet, latest),
                        dateText(latest.submittedAt), "${mmss(latest.totalSeconds)} mins"
                    ) { onOpenResult(latest.id) }
                }
            }
            if (weak.isNotEmpty() && latestSheet != null) {
                item {
                    AppCard(color = CardPink) {
                        Text("Areas to improve:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        HorizontalDivider(color = Maroon, thickness = 3.dp, modifier = Modifier.padding(vertical = 6.dp))
                        weak.forEachIndexed { i, s ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}.", color = Maroon, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("${latestSheet.branch}: ${s.label}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Text(listOfNotNull(latestSheet.setName, s.note).joinToString(" | "), fontSize = 11.sp, color = Muted)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${s.percent}%", color = Maroon, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text("${s.correct}/${s.answered} items", fontSize = 11.sp, color = Muted)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlineButton("View Review Plan", onReviewPlan, Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
