package com.achievo.bluebook.data

import com.achievo.bluebook.data.remote.toUserMessage
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
import kotlinx.serialization.Serializable
import java.util.UUID

/** Thrown for anything the user needs told about. [httpStatus] is null for transport failures. */
class ReviewPlanException(val httpStatus: Int?, message: String) : Exception(message)

@Serializable
private data class WireParams(val budget_min: Int, val min_strong: Int)

@Serializable
private data class SolveRequest(val request_id: String, val edition_id: Int, val params: WireParams)

@Serializable
private data class WirePlanEntry(
    val rank: Int,
    val topic_id: String,
    val topic_label: String,
    val subject_label: String,
    val branch_label: String,
    val cost_min: Int,
    val value: Double,
    val mastery: Double,
    val weakness: Double,
    val staleness: Double,
    val strong: Boolean,
    val reliable: Boolean,
    val book_page_ref: Int?
)

@Serializable
private data class WireCertificate(
    val exact: Boolean,
    val objective: Double,
    val budget_min: Int,
    val total_cost_min: Int,
    val q_requested: Int,
    val q_used: Int,
    val strong_selected: Int,
    val plan: List<WirePlanEntry>,
    val excluded_topic_ids: List<String>
)

@Serializable
private data class WireEngineMeta(val name: String, val version: String, val algorithm: String)

@Serializable
private data class WireMetrics(val n_topics: Int, val dp_cells: Int, val runtime_ms: Double)

@Serializable
private data class SolveResponse(
    val request_id: String,
    val status: String,
    val engine: WireEngineMeta,
    val certificate: WireCertificate,
    val metrics: WireMetrics,
    val warnings: List<String>
)

/** FR21: the review-plan optimizer, served by the plan-gateway Supabase Edge Function. Builds its
 *  own topics[] from the caller's authenticated DB state server-side - the request carries only
 *  the budget/strong-topic tuning, never the student's history. */
object ReviewPlanApi {
    private const val FUNCTION_NAME = "plan-gateway"
    private val jsonHeaders = Headers.build { append(HttpHeaders.ContentType, "application/json") }

    suspend fun solve(
        budgetMin: Int = AppConfig.DEFAULT_BUDGET_MIN,
        minStrong: Int = AppConfig.DEFAULT_MIN_STRONG
    ): Result<ReviewPlan> = runCatching {
        val body = SolveRequest(
            request_id = UUID.randomUUID().toString(),
            edition_id = AppConfig.BOOK_EDITION_ID,
            params = WireParams(budgetMin.coerceIn(1, 600), minStrong.coerceIn(0, 5))
        )
        val response = Supa.client.functions.invoke(FUNCTION_NAME, body, headers = jsonHeaders)
        planFrom(response.body())
    }.recoverCatching { e ->
        if (e is RestException) throw ReviewPlanException(e.statusCode, e.toUserMessage("The review planner rejected this request."))
        throw ReviewPlanException(null, "Couldn't reach the review planner. Check your connection.")
    }
}

private fun planFrom(r: SolveResponse): ReviewPlan {
    val cert = r.certificate
    return ReviewPlan(
        requestId = r.request_id,
        status = runCatching { PlanStatus.valueOf(r.status) }.getOrDefault(PlanStatus.UNKNOWN),
        exact = cert.exact,
        objective = cert.objective,
        budgetMin = cert.budget_min,
        totalCostMin = cert.total_cost_min,
        qRequested = cert.q_requested,
        qUsed = cert.q_used,
        strongSelected = cert.strong_selected,
        entries = cert.plan.map { entryFrom(it) },
        excludedTopicIds = cert.excluded_topic_ids,
        warnings = r.warnings,
        engineLabel = "${r.engine.name} ${r.engine.version}".trim()
    )
}

private fun entryFrom(w: WirePlanEntry) = ReviewPlanEntry(
    rank = w.rank,
    topicId = w.topic_id,
    topicLabel = w.topic_label,
    subjectLabel = w.subject_label,
    branchLabel = w.branch_label,
    costMin = w.cost_min,
    value = w.value,
    mastery = w.mastery,
    weakness = w.weakness,
    staleness = w.staleness,
    strong = w.strong,
    reliable = w.reliable,
    bookPageRef = w.book_page_ref
)
