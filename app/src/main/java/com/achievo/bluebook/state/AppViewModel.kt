package com.achievo.bluebook.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.achievo.bluebook.data.*
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)

    var attempts by mutableStateOf(store.loadAttempts())
        private set
    var draft by mutableStateOf(store.loadDraft())
        private set
    var profile by mutableStateOf(store.loadProfile())
        private set
    var examDate by mutableStateOf(store.loadExamDate())
        private set
    var privacyAccepted by mutableStateOf(store.privacyAccepted())
        private set
    var reminderHidden by mutableStateOf(store.reminderHidden())
        private set

    fun sheet(id: String): Sheet? = SampleData.sheets.firstOrNull { it.id == id }
    fun attempt(id: String): Attempt? = attempts.firstOrNull { it.id == id }

    // ---- answer sheet (FR06 to FR09) ----
    fun startSheet(s: Sheet) {
        val d = Draft(
            sheetId = s.id,
            answers = List(s.itemCount) { null },
            flagged = emptySet(),
            itemSeconds = List(s.itemCount) { 0 },
            elapsedSeconds = 0,
            lastMarkSeconds = 0,
            startedAt = System.currentTimeMillis()
        )
        draft = d
        store.saveDraft(d)
    }

    /** Tapping the chosen letter again clears it, so an item can go back to blank. */
    fun setAnswer(index: Int, letter: Char) {
        val d = draft ?: return
        val delta = (d.elapsedSeconds - d.lastMarkSeconds).coerceAtLeast(0)
        val answers = d.answers.toMutableList()
        answers[index] = if (answers[index] == letter) null else letter
        val secs = d.itemSeconds.toMutableList()
        secs[index] = secs[index] + delta   // time since the previous tap is credited to this item
        val updated = d.copy(answers = answers, itemSeconds = secs, lastMarkSeconds = d.elapsedSeconds)
        draft = updated
        store.saveDraft(updated)
    }

    fun toggleFlag(index: Int) {
        val d = draft ?: return
        val f = if (index in d.flagged) d.flagged - index else d.flagged + index
        val updated = d.copy(flagged = f)
        draft = updated
        store.saveDraft(updated)
    }

    /** Called once per second by the sheet screen while it is visible (FR08 pauses in background). */
    fun tick() {
        val d = draft ?: return
        val updated = d.copy(elapsedSeconds = d.elapsedSeconds + 1)
        draft = updated
        if (updated.elapsedSeconds % 5 == 0) store.saveDraft(updated)
    }

    fun persistDraft() { store.saveDraft(draft) }

    fun discardDraft() {
        draft = null
        store.saveDraft(null)
    }

    /** Submitted attempts are final (FR09). TODO: record answer key version (BR06) once keys are versioned. */
    fun submit(): String? {
        val d = draft ?: return null
        val a = Attempt(
            id = UUID.randomUUID().toString(),
            sheetId = d.sheetId,
            answers = d.answers,
            flagged = d.flagged,
            itemSeconds = d.itemSeconds,
            totalSeconds = d.elapsedSeconds,
            submittedAt = System.currentTimeMillis()
        )
        attempts = attempts + a
        store.saveAttempts(attempts)
        draft = null
        store.saveDraft(null)
        return a.id
    }

    // ---- profile, privacy, settings ----
    fun saveProfile(p: Profile) { profile = p; store.saveProfile(p) }
    fun updateExamDate(iso: String?) { examDate = iso; store.saveExamDate(iso) }
    fun acceptPrivacy() { privacyAccepted = true; store.setPrivacyAccepted(true) }
    fun hideReminder() { reminderHidden = true; store.setReminderHidden(true) }

    fun clearLocalData() {
        store.clearHistoryAndProfile()
        attempts = emptyList()
        draft = null
        profile = Profile()
        examDate = null
    }
}
