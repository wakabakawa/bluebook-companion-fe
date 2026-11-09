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
