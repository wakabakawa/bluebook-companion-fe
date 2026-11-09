package com.achievo.bluebook.data

import android.content.Context
import java.util.UUID
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
     * Anonymous per-install id, sent to the backend as profile_id (NFR01, BR01):
     * random, not derived from the device or the person. Generated on first use.
     */
    fun profileId(): String =
        sp.getString("profileId", null) ?: UUID.randomUUID().toString().also {
            sp.edit().putString("profileId", it).apply()
        }

    /** FR24: clears history, unfinished sheet and profile. The privacy acknowledgement is kept. */
    fun clearHistoryAndProfile() {
        sp.edit().remove("attempts").remove("draft").remove("nick").remove("school")
            .remove("course").remove("examDate").remove("profileId").apply()
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

private fun attemptFrom(o: JSONObject) = Attempt(
    id = o.getString("id"),
    sheetId = o.getString("sheetId"),
    answers = o.getJSONArray("answers").chars(),
    flagged = o.getJSONArray("flagged").ints().toSet(),
    itemSeconds = o.getJSONArray("itemSeconds").ints(),
    totalSeconds = o.getInt("totalSeconds"),
    submittedAt = o.getLong("submittedAt")
)

private fun Draft.toJson() = JSONObject()
    .put("sheetId", sheetId)
    .put("answers", answers.toJsonArr())
    .put("flagged", JSONArray(flagged.toList()))
    .put("itemSeconds", JSONArray(itemSeconds))
    .put("elapsed", elapsedSeconds).put("lastMark", lastMarkSeconds).put("startedAt", startedAt)

private fun draftFrom(o: JSONObject) = Draft(
    sheetId = o.getString("sheetId"),
    answers = o.getJSONArray("answers").chars(),
    flagged = o.getJSONArray("flagged").ints().toSet(),
    itemSeconds = o.getJSONArray("itemSeconds").ints(),
    elapsedSeconds = o.getInt("elapsed"),
    lastMarkSeconds = o.getInt("lastMark"),
    startedAt = o.getLong("startedAt")
)
