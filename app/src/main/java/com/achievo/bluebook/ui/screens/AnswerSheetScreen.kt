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

    if (sheet == null || draft == null || draft.sheetId != sheetId) {
        // After submitting, the draft is cleared on purpose, so don't navigate back in that case.
        LaunchedEffect(submitted) { if (!submitted) onExit() }
        return
    }

    var showReminder by remember { mutableStateOf(!vm.reminderHidden) }
    var dontAsk by remember { mutableStateOf(false) }
    var showSubmit by remember { mutableStateOf(false) }

    val blanks = draft.answers.count { it == null }
    val answered = draft.answers.size - blanks
    val remaining = (sheet.allotmentSeconds - draft.elapsedSeconds).coerceAtLeast(0)

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(1000)
                if (!showReminder && !showSubmit) vm.tick()
            }
        }
    }

    fun saveAndExit() {
        vm.persistDraft()
        onExit()
    }
    BackHandler { saveAndExit() }

    if (showReminder) {
        NoticeDialog(
            title = "REMINDER!",
            body = "This is a self-study guide, cheating only leads to tricking yourself. Answer truthfully and good luck!",
            confirm = "Agree",
            onConfirm = {
                if (dontAsk) vm.hideReminder()
                showReminder = false
            },
            extra = {
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
                "You have $blanks blank item(s) remaining on your answer sheet. Blank items will be scored as incorrect.\n\nAre you sure you want to submit your attempt?"
            } else {
                "Once submitted, you won't be able to edit your answers again! If you're not sure, go back and check your answers.\n\nAre you sure you want to submit your attempt?"
            },
            confirm = "Submit",
            onConfirm = {
                showSubmit = false
                submitted = true
                vm.submit()?.let(onSubmitted)
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
            PrimaryButton("Save and Exit", { saveAndExit() }, Modifier.weight(1f))
            PrimaryButton("Submit", { showSubmit = true }, Modifier.weight(1f))
        }
    }
}
