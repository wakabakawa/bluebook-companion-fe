package com.achievo.bluebook.state

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.achievo.bluebook.data.*
import com.achievo.bluebook.data.remote.AttemptRepository
import com.achievo.bluebook.data.remote.CatalogRepository
import com.achievo.bluebook.data.remote.CohortRepository
import com.achievo.bluebook.data.remote.toUserMessage
import io.github.jan.supabase.auth.auth
import java.util.UUID
import kotlinx.coroutines.launch

enum class AuthPhase { IDLE, SIGNING_IN, READY, OFFLINE }

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

    // ---- auth (anonymous sign-in, gated on the privacy notice - FR04) ----
    var authPhase by mutableStateOf(AuthPhase.IDLE)
        private set
    var authError by mutableStateOf<String?>(null)
        private set

    // ---- catalogue (v_sheet_catalog / v_sheet_items, replaces SampleData.kt) ----
    var sheets by mutableStateOf<List<Sheet>>(emptyList())
        private set
    var catalogLoading by mutableStateOf(false)
        private set
    var catalogError by mutableStateOf<String?>(null)
        private set

    init {
        if (privacyAccepted) ensureSession()
    }

    private fun ensureSession() {
        if (authPhase == AuthPhase.SIGNING_IN) return
        authPhase = AuthPhase.SIGNING_IN
        viewModelScope.launch {
            AuthRepository.ensureSession(store)
                .onSuccess { authPhase = AuthPhase.READY; loadCatalog() }
                .onFailure {
                    authPhase = AuthPhase.OFFLINE
                    authError = it.toUserMessage("Couldn't sign in.")
                }
        }
    }

    fun loadCatalog() {
        if (catalogLoading) return
        catalogLoading = true
        catalogError = null
        viewModelScope.launch {
            runCatching { CatalogRepository.loadSheets() }
                .onSuccess { sheets = it }
                .onFailure { catalogError = it.toUserMessage("Couldn't load the exam set catalogue.") }
            catalogLoading = false
        }
    }

    fun dismissCatalogError() { catalogError = null }

    /** Retries anonymous sign-in after an OFFLINE failure (FR04's silent sign-in otherwise has no recovery path). */
    fun retrySignIn() = ensureSession()

    fun sheet(id: String): Sheet? = sheets.firstOrNull { it.id == id }
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
            startedAt = System.currentTimeMillis(),
            attemptId = UUID.randomUUID().toString()
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

    // ---- submit (FR09): scored server-side via submit_attempt(), the only place the key is read ----
    var submitLoading by mutableStateOf(false)
        private set
    var submitError by mutableStateOf<String?>(null)
        private set
    var submittedAttemptId by mutableStateOf<String?>(null)
        private set

    /** On failure the draft is kept intact (not cleared) so the student's answers are never lost. */
    fun submit() {
        val d = draft ?: return
        val sheet = sheet(d.sheetId) ?: return
        if (submitLoading) return
        submitLoading = true
        submitError = null
        viewModelScope.launch {
            runCatching { AttemptRepository.submit(sheet.id, d) }
                .onSuccess { a ->
                    attempts = attempts + a
                    store.saveAttempts(attempts)
                    draft = null
                    store.saveDraft(null)
                    submittedAttemptId = a.id
                }
                .onFailure { submitError = it.toUserMessage("Couldn't submit - your answers are saved. Try again.") }
            submitLoading = false
        }
    }

    fun dismissSubmitError() { submitError = null }
    fun consumeSubmittedAttempt() { submittedAttemptId = null }

    // ---- withdraw a submitted attempt: deletes it server-side (own_attempt_erase RLS) ----
    var withdrawLoading by mutableStateOf(false)
        private set
    var withdrawError by mutableStateOf<String?>(null)
        private set

    fun withdrawAttempt(attemptId: String, onDone: () -> Unit = {}) {
        if (withdrawLoading) return
        withdrawLoading = true
        withdrawError = null
        viewModelScope.launch {
            runCatching { AttemptRepository.withdraw(attemptId) }
                .onSuccess {
                    attempts = attempts.filterNot { it.id == attemptId }
                    store.saveAttempts(attempts)
                    onDone()
                }
                .onFailure { withdrawError = it.toUserMessage("Couldn't withdraw this attempt. Try again.") }
            withdrawLoading = false
        }
    }

    fun dismissWithdrawError() { withdrawError = null }

    // ---- profile, privacy, settings ----
    fun saveProfile(p: Profile) { profile = p; store.saveProfile(p) }
    fun updateExamDate(iso: String?) { examDate = iso; store.saveExamDate(iso) }
    fun acceptPrivacy() { privacyAccepted = true; store.setPrivacyAccepted(true); ensureSession() }
    fun hideReminder() { reminderHidden = true; store.setReminderHidden(true) }

    fun clearLocalData() {
        store.clearHistoryAndProfile()
        attempts = emptyList()
        draft = null
        profile = Profile()
        examDate = null
        reviewPlan = null
        reviewPlanError = null
        authPhase = AuthPhase.IDLE
        viewModelScope.launch {
            runCatching { Supa.client.auth.signOut() }
            ensureSession()
        }
    }

    // ---- review plan (FR21) ----
    var reviewBudgetMin by mutableStateOf(AppConfig.DEFAULT_BUDGET_MIN)
        private set
    var reviewPlan by mutableStateOf<ReviewPlan?>(null)
        private set
    var reviewPlanLoading by mutableStateOf(false)
        private set
    var reviewPlanError by mutableStateOf<String?>(null)
        private set

    fun setReviewBudget(minutes: Int) {
        reviewBudgetMin = minutes.coerceIn(AppConfig.MIN_BUDGET_MIN, AppConfig.MAX_BUDGET_MIN)
    }

    fun loadReviewPlan() {
        if (reviewPlanLoading) return
        reviewPlanLoading = true
        reviewPlanError = null
        viewModelScope.launch {
            ReviewPlanApi.solve(reviewBudgetMin, AppConfig.DEFAULT_MIN_STRONG)
                .onSuccess { reviewPlan = it }
                .onFailure { reviewPlanError = it.message ?: "Couldn't build a review plan." }
            reviewPlanLoading = false
        }
    }

    fun dismissReviewPlanError() { reviewPlanError = null }

    // ---- cohort comparison (FR22), topic-level only - reads v_topic_stats directly ----
    var cohortSlices by mutableStateOf<List<CohortSlice>>(emptyList())
        private set
    var cohortLoading by mutableStateOf(false)
        private set
    var cohortError by mutableStateOf<String?>(null)
        private set

    fun loadCohort() {
        if (cohortLoading) return
        cohortLoading = true
        cohortError = null
        viewModelScope.launch {
            runCatching { CohortRepository.loadTopicStats() }
                .onSuccess { cohortSlices = it }
                .onFailure { cohortError = it.toUserMessage("Couldn't load the cohort comparison.") }
            cohortLoading = false
        }
    }

    fun dismissCohortError() { cohortError = null }
}
