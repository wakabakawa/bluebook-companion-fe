package com.achievo.bluebook.data

enum class Difficulty(val label: String) { EASY("Easy"), AVERAGE("Average"), HARD("Hard") }

/** Classification tags per item (RE: FR11, FR28). No question text is ever stored (BR01). */
data class ItemTag(
    val subject: String,
    val branch: String,
    val topic: String,
    val keyConcept: String,
    val cognitiveSkill: String,
    val difficulty: Difficulty,
    val bookPage: Int?,
    /** Per-item reference time in seconds (item override, else its difficulty's default — CC03). */
    val refSeconds: Int
)

data class Sheet(
    val id: String,
    val setName: String,
    val subject: String,
    val branch: String,
    val itemCount: Int,
    val allotmentSeconds: Int,
    val tags: List<ItemTag>?,
    val published: Boolean
) {
    val title: String get() = "$branch: $setName"
}

data class Draft(
    val sheetId: String,
    val answers: List<Char?>,
    val flagged: Set<Int>,
    val itemSeconds: List<Int>,
    val elapsedSeconds: Int,
    val lastMarkSeconds: Int,
    val startedAt: Long,
    /** Client-generated, stable for the life of this draft so a failed-then-retried submit_attempt() call is idempotent. */
    val attemptId: String
)

enum class ItemOutcome { CORRECT, WRONG, BLANK }

data class Attempt(
    val id: String,
    val sheetId: String,
    val answers: List<Char?>,
    val flagged: Set<Int>,
    val itemSeconds: List<Int>,
    val totalSeconds: Int,
    val submittedAt: Long,
    /** null = not yet scored. Scored locally (bridge) until server-side scoring (submit_attempt RPC) lands. */
    val outcomes: List<ItemOutcome>? = null,
    val keyVersionId: String? = null
)

data class Profile(val nickname: String = "", val school: String = "", val course: String = "")

/** FR21 optimizer outcome. UNKNOWN keeps a newer server from crashing an older build. */
enum class PlanStatus { OPTIMAL, OPTIMAL_RELAXED_Q, BUDGET_TOO_SMALL, EMPTY_INPUT, UNKNOWN }

/** Label/page/staleness are echoed by plan-gateway itself, so the client never needs its own topic lookup. */
data class ReviewPlanEntry(
    val rank: Int,
    val topicId: String,
    val topicLabel: String,
    val subjectLabel: String,
    val branchLabel: String,
    val costMin: Int,
    val value: Double,
    val mastery: Double,
    val weakness: Double,
    val staleness: Double,
    val strong: Boolean,
    val reliable: Boolean,
    val bookPageRef: Int?
)

/** One review plan from the plan-gateway Supabase Edge Function (FR21). */
data class ReviewPlan(
    val requestId: String,
    val status: PlanStatus,
    val exact: Boolean,
    val objective: Double,
    val budgetMin: Int,
    val totalCostMin: Int,
    val qRequested: Int,
    val qUsed: Int,
    val strongSelected: Int,
    val entries: List<ReviewPlanEntry>,
    val excludedTopicIds: List<String>,
    val warnings: List<String>,
    val engineLabel: String?
)
