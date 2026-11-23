package com.achievo.bluebook.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.achievo.bluebook.state.AppViewModel
import com.achievo.bluebook.ui.components.*
import com.achievo.bluebook.ui.theme.*
import kotlinx.coroutines.delay

/** FR06 to FR09. Timer pauses while the app is in the background (FR08). */
@Composable
fun AnswerSheetScreen(
    vm: AppViewModel,
    sheetId: String,
    onExit: () -> Unit,
    onSubmitted: (String) -> Unit
) {
    val sheet = vm.sheet(sheetId)
    val draft = vm.draft
    var submitted by remember { mutableStateOf(false) }

    // Must run even once submit() succeeds and clears the draft below - otherwise the early
    // return never lets this screen navigate away, and it's left showing nothing (blank screen).
    LaunchedEffect(vm.submittedAttemptId) {
        vm.submittedAttemptId?.let { onSubmitted(it); vm.consumeSubmittedAttempt() }
    }

    if (sheet == null || draft == null || draft.sheetId != sheetId) {
        // After submitting, the draft is cleared on purpose, so don't navigate back in that case.
        LaunchedEffect(submitted) { if (!submitted) onExit() }
        return
    }

    vm.submitError?.let { msg ->
        NoticeDialog(
            title = "COULDN'T SUBMIT",
            body = msg,
            confirm = "Retry", onConfirm = { vm.dismissSubmitError(); vm.submit() },
            dismiss = "Close", onDismiss = { vm.dismissSubmitError() }
        )
    }

    var showReminder by remember { mutableStateOf(!vm.reminderHidden) }
    var dontAsk by remember { mutableStateOf(false) }
    var showSubmit by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    val blanks = draft.answers.count { it == null }
    val answered = draft.answers.size - blanks
    val remaining = (sheet.allotmentSeconds - draft.elapsedSeconds).coerceAtLeast(0)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(1000)
                if (!showReminder && !showSubmit && !showExitConfirm) vm.tick()
            }
        }
    }

    fun saveAndExit() {
        vm.persistDraft()
        onExit()
    }
    BackHandler { showExitConfirm = true }

    if (showExitConfirm) {
        NoticeDialog(
            title = "SAVE AND EXIT?",
            body = "Your progress is saved - you can continue later from Home.",
            confirm = "Exit", onConfirm = { showExitConfirm = false; saveAndExit() },
            dismiss = "Cancel", onDismiss = { showExitConfirm = false }
        )
    }

    if (showReminder) {
        NoticeDialog(
            title = "REMINDER!",
            body = "A self-study guide - cheating only tricks yourself. Good luck!",
            confirm = "Agree",
            onConfirm = {
                if (dontAsk) vm.hideReminder()
                showReminder = false
            },
            extra = {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                    Text("1. Read the question on your book and tap A, B, C, or D here.", fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("2. Tap the flag if you're not sure, so you can review it later.", fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("3. \"Save and Exit\" pauses and returns you to Home - your sheet stays saved on this device.", fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("4. Tap Submit when you're done. Submitted sheets can't be edited.", fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dontAsk, onCheckedChange = { dontAsk = it })
                    Text("Don't ask again", fontSize = 13.sp)
                }
            }
        )
    }

    if (showSubmit) {
        val hasBlanks = blanks > 0
        NoticeDialog(
            title = if (hasBlanks) "BLANK ITEMS" else "CHECK CAREFULLY",
            body = if (hasBlanks) {
                "$blanks item(s) left blank - scored as incorrect.\n\nSubmit anyway?"
            } else {
                "You can't edit answers after submitting.\n\nSubmit now?"
            },
            confirm = "Submit",
            onConfirm = {
                showSubmit = false
                submitted = true
                vm.submit()
            },
            dismiss = "Back",
            onDismiss = { showSubmit = false }
        )
    }

    Column(Modifier.fillMaxSize()) {
        BluebookTopBar()
        AppCard(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${sheet.branch}: ${sheet.setName.uppercase()}", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Outlined.Timer, contentDescription = "Time left", modifier = Modifier.size(22.dp))
                Text(" ${mmss(remaining)} mins", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { answered / draft.answers.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SelectBlue, trackColor = TrackGray
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("$answered Answered", color = SelectBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("$blanks Blank", color = FlagAmber, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("${draft.flagged.size} Flagged", color = FlagAmber, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }
        // TODO FR06: "jump to item number" is required but not in the hi-fi. Waiting on the designer.
        LazyColumn(
            Modifier.weight(1f), state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(draft.answers.size) { i ->
                AnswerInputRow(
                    number = i + 1,
                    selected = draft.answers[i],
                    flagged = i in draft.flagged,
                    onPick = { vm.setAnswer(i, it) },
                    onFlag = { vm.toggleFlag(i) }
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton("Save and Exit", { showExitConfirm = true }, Modifier.weight(1f), enabled = !vm.submitLoading)
            PrimaryButton(
                if (vm.submitLoading) "Submitting…" else "Submit",
                { showSubmit = true }, Modifier.weight(1f), enabled = !vm.submitLoading
            )
        }
    }
}
