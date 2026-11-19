package com.achievo.bluebook.data.remote

import com.achievo.bluebook.data.Attempt
import com.achievo.bluebook.data.Draft
import com.achievo.bluebook.data.ItemOutcome
import com.achievo.bluebook.data.Supa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.time.Instant

@Serializable
private data class ResponseIn(val position: Int, val choice: String?, val flagged: Boolean, val seconds: Int)

@Serializable
private data class SubmitParams(
    val p_attempt_id: String,
    val p_exam_set_id: String,
    val p_started_at: String,
    val p_total_seconds: Int,
    val p_responses: List<ResponseIn>
)

@Serializable
private data class ResultRow(
    val position: Int,
    val selected_choice: String?,
    val is_correct: Boolean,
    val key_version_id: String?
)

/** The only path that scores an attempt — the answer key never reaches this client (SEC-08/09/11). */
object AttemptRepository {
    suspend fun submit(sheetId: String, draft: Draft): Attempt {
        val params = SubmitParams(
            p_attempt_id = draft.attemptId,
            p_exam_set_id = sheetId,
            p_started_at = Instant.ofEpochMilli(draft.startedAt).toString(),
            p_total_seconds = draft.elapsedSeconds,
            p_responses = draft.answers.mapIndexed { i, ans ->
                ResponseIn(position = i + 1, choice = ans?.toString(), flagged = i in draft.flagged, seconds = draft.itemSeconds[i])
            }
        )
        val paramsJson = Json.encodeToJsonElement(params).jsonObject
        val rows = Supa.client.postgrest.rpc("submit_attempt", paramsJson).decodeList<ResultRow>()

        val outcomes = rows.map { if (it.selected_choice == null) ItemOutcome.BLANK else if (it.is_correct) ItemOutcome.CORRECT else ItemOutcome.WRONG }

        return Attempt(
            id = draft.attemptId,
            sheetId = sheetId,
            answers = draft.answers,
            flagged = draft.flagged,
            itemSeconds = draft.itemSeconds,
            totalSeconds = draft.elapsedSeconds,
            submittedAt = System.currentTimeMillis(),
            outcomes = outcomes,
            keyVersionId = rows.firstOrNull()?.key_version_id
        )
    }
}
