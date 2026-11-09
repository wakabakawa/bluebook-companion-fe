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
    val bookPage: Int?
)

data class Sheet(
    val id: String,
    val setName: String,
    val subject: String,
    val branch: String,
    val itemCount: Int,
    val allotmentSeconds: Int,
    val key: List<Char>?,
    val tags: List<ItemTag>?
) {
    val published: Boolean get() = key != null && tags != null
    val title: String get() = "$branch: $setName"
}

data class Draft(
    val sheetId: String,
    val answers: List<Char?>,
    val flagged: Set<Int>,
    val itemSeconds: List<Int>,
    val elapsedSeconds: Int,
    val lastMarkSeconds: Int,
    val startedAt: Long
)

data class Attempt(
    val id: String,
    val sheetId: String,
    val answers: List<Char?>,
    val flagged: Set<Int>,
    val itemSeconds: List<Int>,
    val totalSeconds: Int,
    val submittedAt: Long
)

data class Profile(val nickname: String = "", val school: String = "", val course: String = "")

/** One topic's book-wide statistics as the review-plan optimizer needs them (FR21). */
data class TopicStat(
    val topicId: String,
    val subject: String,
    val branch: String,
    val topic: String,
    val itemsInBook: Int,
    val answered: Int,
    val correct: Int,
    val daysSincePractice: Int?,
    val refTimeSec: Int,
    val bookPageRef: Int?
)

/** FR21 optimizer outcome. UNKNOWN keeps a newer server from crashing an older build. */
enum class PlanStatus { OPTIMAL, OPTIMAL_RELAXED_Q, BUDGET_TOO_SMALL, EMPTY_INPUT, UNKNOWN }

data class ReviewPlanEntry(
    val rank: Int,
    val topicId: String,
    val costMin: Int,
    val value: Double,
    val mastery: Double,
    val weakness: Double,
    val staleness: Double,
    val strong: Boolean,
    val reliable: Boolean,
    val bookPageRef: Int?
)

/** One review plan from POST /api/v1/algorithm/solve (FR21). */
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
