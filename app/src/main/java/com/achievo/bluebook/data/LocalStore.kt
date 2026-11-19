package com.achievo.bluebook.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device storage (FR07, FR23, NFR06): unfinished sheet, attempt history, profile.
 * Nothing here identifies a person (NFR01). TODO: move to Room if the team prefers.
 */
class LocalStore(context: Context) {
    private val sp = context.getSharedPreferences("bluebook_local", Context.MODE_PRIVATE)

    fun loadAttempts(): List<Attempt> = runCatching {
        val ja = JSONArray(sp.getString("attempts", "[]"))
        List(ja.length()) { attemptFrom(ja.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun saveAttempts(list: List<Attempt>) {
        sp.edit().putString("attempts", JSONArray(list.map { it.toJson() }).toString()).apply()
    }

    fun loadDraft(): Draft? = runCatching {
        sp.getString("draft", null)?.let { draftFrom(JSONObject(it)) }
    }.getOrNull()

    fun saveDraft(d: Draft?) {
        val e = sp.edit()
        if (d == null) e.remove("draft") else e.putString("draft", d.toJson().toString())
        e.apply()
    }

    fun loadProfile() = Profile(
        sp.getString("nick", "") ?: "",
        sp.getString("school", "") ?: "",
        sp.getString("course", "") ?: ""
    )

    fun saveProfile(p: Profile) {
        sp.edit().putString("nick", p.nickname).putString("school", p.school).putString("course", p.course).apply()
    }

    fun loadExamDate(): String? = sp.getString("examDate", null)
    fun saveExamDate(iso: String?) {
        val e = sp.edit()
        if (iso == null) e.remove("examDate") else e.putString("examDate", iso)
        e.apply()
    }

    fun privacyAccepted() = sp.getBoolean("privacy", false)
    fun setPrivacyAccepted(v: Boolean) { sp.edit().putBoolean("privacy", v).apply() }

    fun reminderHidden() = sp.getBoolean("reminderHidden", false)
    fun setReminderHidden(v: Boolean) { sp.edit().putBoolean("reminderHidden", v).apply() }

    /**
     * Cache of the Supabase-issued anonymous user id (AuthRepository.ensureSession() writes
     * this) — a cache for display/offline rendering, never the source of truth. Supabase's own
     * Auth session storage is what's authoritative.
     */
    fun cachedUserId(): String? = sp.getString("cachedUserId", null)
    fun saveCachedUserId(id: String?) {
        val e = sp.edit()
        if (id == null) e.remove("cachedUserId") else e.putString("cachedUserId", id)
        e.apply()
    }

    /** FR24: clears history, unfinished sheet and profile. The privacy acknowledgement is kept. */
    fun clearHistoryAndProfile() {
        sp.edit().remove("attempts").remove("draft").remove("nick").remove("school")
            .remove("course").remove("examDate").remove("cachedUserId").apply()
    }
}

private fun List<Char?>.toJsonArr() = JSONArray(map { it?.toString() ?: "" })
private fun JSONArray.chars(): List<Char?> = List(length()) { getString(it).firstOrNull() }
private fun JSONArray.ints(): List<Int> = List(length()) { getInt(it) }

private fun Attempt.toJson() = JSONObject()
    .put("id", id).put("sheetId", sheetId)
    .put("answers", answers.toJsonArr())
    .put("flagged", JSONArray(flagged.toList()))
    .put("itemSeconds", JSONArray(itemSeconds))
    .put("totalSeconds", totalSeconds).put("submittedAt", submittedAt)
    .put("outcomes", outcomes?.let { JSONArray(it.map { o -> o.name }) } ?: JSONObject.NULL)
    .put("keyVersionId", keyVersionId ?: JSONObject.NULL)

private fun attemptFrom(o: JSONObject) = Attempt(
    id = o.getString("id"),
    sheetId = o.getString("sheetId"),
    answers = o.getJSONArray("answers").chars(),
    flagged = o.getJSONArray("flagged").ints().toSet(),
    itemSeconds = o.getJSONArray("itemSeconds").ints(),
    totalSeconds = o.getInt("totalSeconds"),
    submittedAt = o.getLong("submittedAt"),
    outcomes = o.optJSONArray("outcomes")?.let { arr -> List(arr.length()) { ItemOutcome.valueOf(arr.getString(it)) } },
    keyVersionId = if (o.isNull("keyVersionId")) null else o.optString("keyVersionId", null)
)

private fun Draft.toJson() = JSONObject()
    .put("sheetId", sheetId)
    .put("answers", answers.toJsonArr())
    .put("flagged", JSONArray(flagged.toList()))
    .put("itemSeconds", JSONArray(itemSeconds))
    .put("elapsed", elapsedSeconds).put("lastMark", lastMarkSeconds).put("startedAt", startedAt)
    .put("attemptId", attemptId)

private fun draftFrom(o: JSONObject) = Draft(
    sheetId = o.getString("sheetId"),
    answers = o.getJSONArray("answers").chars(),
    flagged = o.getJSONArray("flagged").ints().toSet(),
    itemSeconds = o.getJSONArray("itemSeconds").ints(),
    elapsedSeconds = o.getInt("elapsed"),
    lastMarkSeconds = o.getInt("lastMark"),
    startedAt = o.getLong("startedAt"),
    attemptId = o.getString("attemptId")
)
