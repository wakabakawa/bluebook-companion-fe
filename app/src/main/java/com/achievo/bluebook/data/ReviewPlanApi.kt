package com.achievo.bluebook.data

import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Thrown for anything the user needs told about. [httpStatus] is null for transport failures. */
class ReviewPlanException(val httpStatus: Int?, message: String) : Exception(message)

/** FR21: the review-plan optimizer endpoint. The one network call in the app. */
object ReviewPlanApi {
    private val jsonMediaType = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    suspend fun solve(
        profileId: String,
        topics: List<TopicStat>,
        budgetMin: Int = AppConfig.DEFAULT_BUDGET_MIN,
        minStrong: Int = AppConfig.DEFAULT_MIN_STRONG
    ): Result<ReviewPlan> = withContext(Dispatchers.IO) {
        runCatching {
            val clampedBudget = budgetMin.coerceIn(1, 600)
            val clampedMinStrong = minStrong.coerceIn(0, 5)
            val body = requestBody(profileId, topics, clampedBudget, clampedMinStrong)

            val request = Request.Builder()
                .url(AppConfig.API_BASE_URL + AppConfig.REVIEW_PLAN_PATH)
                .post(body.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val payload = response.body?.string().orEmpty()
                when (response.code) {
                    200 -> runCatching { planFrom(JSONObject(payload)) }.getOrElse {
                        throw ReviewPlanException(200, "The server sent a plan this app couldn't read.")
                    }
                    400, 422 -> throw ReviewPlanException(response.code, problemDetail(payload))
                    504 -> throw ReviewPlanException(504, "The review planner took too long. Try again.")
                    else -> throw ReviewPlanException(response.code, "The review planner is unavailable (error ${response.code}).")
                }
            }
        }.recoverCatching { e ->
            if (e is ReviewPlanException) throw e
            throw ReviewPlanException(null, "Couldn't reach the review planner. Make sure the server is running.")
        }
    }

    private fun problemDetail(payload: String): String = runCatching {
        val o = JSONObject(payload)
        o.optString("detail").ifBlank { o.optString("title") }.ifBlank { "The review planner rejected this request." }
    }.getOrDefault("The review planner rejected this request.")

    private fun requestBody(profileId: String, topics: List<TopicStat>, budgetMin: Int, minStrong: Int): JSONObject {
        val params = JSONObject()
            .put("budget_min", budgetMin)
            .put("min_strong", minStrong)

        val wireTopics = JSONArray()
        topics.forEach { wireTopics.put(it.toWireJson()) }

        return JSONObject()
            .put("request_id", UUID.randomUUID().toString())
            .put("profile_id", profileId)
            .put("edition_id", AppConfig.BOOK_EDITION_ID)
            .put("params", params)
            .put("topics", wireTopics)
    }
}

private fun TopicStat.toWireJson(): JSONObject {
    val o = JSONObject()
        .put("topic_id", topicId)
        .put("items_in_book", itemsInBook)
        .put("answered", answered)
        .put("correct", correct)
        .put("ref_time_sec", refTimeSec)
    // org.json's put(key, null) removes the key — use JSONObject.NULL to keep it present
    o.put("days_since_practice", daysSincePractice ?: JSONObject.NULL)
    o.put("cohort_mastery", JSONObject.NULL)
    o.put("book_page_ref", bookPageRef ?: JSONObject.NULL)
    return o
}

private fun planFrom(o: JSONObject): ReviewPlan {
    val cert = o.getJSONObject("certificate")
    val engine = o.optJSONObject("engine")
    val entries = cert.optJSONArray("plan") ?: JSONArray()
    val excluded = cert.optJSONArray("excluded_topic_ids") ?: JSONArray()
    val warnings = o.optJSONArray("warnings") ?: JSONArray()

    return ReviewPlan(
        requestId = o.getString("request_id"),
        status = runCatching { PlanStatus.valueOf(o.getString("status")) }.getOrDefault(PlanStatus.UNKNOWN),
        exact = cert.optBoolean("exact", true),
        objective = cert.getDouble("objective"),
        budgetMin = cert.getInt("budget_min"),
        totalCostMin = cert.getInt("total_cost_min"),
        qRequested = cert.getInt("q_requested"),
        qUsed = cert.getInt("q_used"),
        strongSelected = cert.optInt("strong_selected", 0),
        entries = List(entries.length()) { entryFrom(entries.getJSONObject(it)) },
        excludedTopicIds = List(excluded.length()) { excluded.getString(it) },
        warnings = List(warnings.length()) { warnings.getString(it) },
        engineLabel = engine?.let { "${it.optString("name")} ${it.optString("version")}".trim() }
    )
}

private fun entryFrom(o: JSONObject): ReviewPlanEntry = ReviewPlanEntry(
    rank = o.getInt("rank"),
    topicId = o.getString("topic_id"),
    costMin = o.getInt("cost_min"),
    value = o.getDouble("value"),
    mastery = o.getDouble("mastery"),
    weakness = o.getDouble("weakness"),
    staleness = o.getDouble("staleness"),
    strong = o.optBoolean("strong", false),
    reliable = o.optBoolean("reliable", false),
    bookPageRef = if (o.isNull("book_page_ref")) null else o.getInt("book_page_ref")
)
