package com.achievo.bluebook.data

import kotlin.math.roundToInt

data class ScoreResult(val correct: Int, val wrong: Int, val blank: Int, val flagged: Int, val total: Int) {
    val percent: Int get() = if (total == 0) 0 else (correct * 100f / total).roundToInt()
}

/** One bar/row of analysis. [note] carries extra text such as a book page. */
data class Slice(val label: String, val correct: Int, val answered: Int, val note: String? = null) {
    val percent: Int get() = if (answered == 0) 0 else (correct * 100f / answered).roundToInt()
    val reliable: Boolean get() = answered >= AppConfig.MIN_ITEMS_FOR_PERCENT
}

data class SpeedBucket(val label: String, val count: Int, val avgSeconds: Int)
data class SpeedResult(val buckets: List<SpeedBucket>, val answered: Int, val avgSeconds: Int)

/** FR10 / BR05: blank is scored incorrect but reported separately. */
fun scoreOf(sheet: Sheet, a: Attempt): ScoreResult {
    val key = sheet.key ?: return ScoreResult(0, 0, a.answers.size, a.flagged.size, a.answers.size)
    var c = 0; var w = 0; var b = 0
    a.answers.forEachIndexed { i, ans ->
        when {
            ans == null -> b++
            ans == key[i] -> c++
            else -> w++
        }
    }
    return ScoreResult(c, w, b, a.flagged.size, a.answers.size)
}

/** FR16: percent correct per difficulty, computed from answered items only. */
fun difficultySlices(sheet: Sheet, a: Attempt): List<Slice> {
    val key = sheet.key ?: return emptyList()
    val tags = sheet.tags ?: return emptyList()
    return Difficulty.values().map { d ->
        var c = 0; var n = 0
        a.answers.forEachIndexed { i, ans ->
            if (ans != null && tags[i].difficulty == d) {
                n++
                if (ans == key[i]) c++
            }
        }
        Slice(d.label, c, n)
    }
}

/** Weak/strong topics of one attempt, with the book page so the student knows where to go back (FR18, FR21). */
fun topicSlices(sheet: Sheet, a: Attempt): List<Slice> {
    val key = sheet.key ?: return emptyList()
    val tags = sheet.tags ?: return emptyList()
    return tags.indices.groupBy { tags[it].topic }.map { (topic, idx) ->
        val answeredIdx = idx.filter { a.answers[it] != null }
        Slice(
            label = topic,
            correct = answeredIdx.count { a.answers[it] == key[it] },
            answered = answeredIdx.size,
            note = tags[idx.first()].bookPage?.let { "Pg $it" }
        )
    }
}

/** FR13: mastery per branch across ALL attempts of a subject. */
fun branchMastery(attempts: List<Attempt>, subject: String): List<Slice> {
    val branches = SampleData.sheets.filter { it.subject == subject }.map { it.branch }.distinct()
    return branches.map { br ->
        var c = 0; var n = 0
        attempts.forEach { a ->
            val s = SampleData.sheets.firstOrNull { it.id == a.sheetId }
            val key = s?.key
            if (s != null && key != null && s.subject == subject && s.branch == br) {
                a.answers.forEachIndexed { i, ans ->
                    if (ans != null) {
                        n++
                        if (ans == key[i]) c++
                    }
                }
            }
        }
        Slice(br, c, n)
    }
}

/** The PDF's proposed per-difficulty review budget (distinct from AppConfig.REFERENCE_SECONDS). */
fun refSecondsOf(d: Difficulty): Int = when (d) {
    Difficulty.EASY -> AppConfig.EASY_REF_SECONDS
    Difficulty.AVERAGE -> AppConfig.AVERAGE_REF_SECONDS
    Difficulty.HARD -> AppConfig.HARD_REF_SECONDS
}

fun topicIdOf(t: ItemTag): String = "${t.subject}|${t.branch}|${t.topic}"

/** FR21: book-wide per-topic stats across ALL attempts, for the review-plan optimizer. */
fun topicStats(attempts: List<Attempt>, now: Long = System.currentTimeMillis()): List<TopicStat> {
    val tagsByTopic = SampleData.sheets
        .filter { it.published }
        .flatMap { it.tags!! }
        .groupBy { topicIdOf(it) }

    return tagsByTopic.map { (topicId, tags) ->
        val first = tags.first()
        val itemsInBook = tags.size
        val refTimeSec = tags.map { refSecondsOf(it.difficulty) }.average().roundToInt().coerceAtLeast(1)
        val bookPageRef = tags.mapNotNull { it.bookPage }.minOrNull()

        var answered = 0
        var correct = 0
        var lastPracticedAt: Long? = null

        attempts.forEach { a ->
            val sheet = SampleData.sheets.firstOrNull { it.id == a.sheetId }
            val key = sheet?.key
            val sheetTags = sheet?.tags
            if (sheet != null && key != null && sheetTags != null) {
                var touchedThisAttempt = false
                a.answers.forEachIndexed { i, ans ->
                    if (ans != null && i < sheetTags.size && i < key.size && topicIdOf(sheetTags[i]) == topicId) {
                        answered++
                        if (ans == key[i]) correct++
                        touchedThisAttempt = true
                    }
                }
                if (touchedThisAttempt) {
                    lastPracticedAt = maxOf(lastPracticedAt ?: a.submittedAt, a.submittedAt)
                }
            }
        }

        val daysSincePractice = lastPracticedAt?.let { ((now - it) / 86_400_000L).toInt().coerceAtLeast(0) }

        TopicStat(
            topicId = topicId,
            subject = first.subject,
            branch = first.branch,
            topic = first.topic,
            itemsInBook = itemsInBook,
            answered = answered,
            correct = correct,
            daysSincePractice = daysSincePractice,
            refTimeSec = refTimeSec,
            bookPageRef = bookPageRef
        )
    }
}

/** FR17: fast/slow x correct/wrong against the reference time (default from AppConfig, CC03). */
fun speedOf(sheet: Sheet, a: Attempt, ref: Int = AppConfig.REFERENCE_SECONDS): SpeedResult {
    val key = sheet.key ?: return SpeedResult(emptyList(), 0, 0)
    val groups = linkedMapOf(
        "Fast & Correct" to mutableListOf<Int>(),
        "Slow & Correct" to mutableListOf<Int>(),
        "Fast & Wrong" to mutableListOf<Int>(),
        "Slow & Wrong" to mutableListOf<Int>()
    )
    a.answers.forEachIndexed { i, ans ->
        if (ans != null) {
            val t = a.itemSeconds[i]
            val k = (if (t <= ref) "Fast" else "Slow") + " & " + (if (ans == key[i]) "Correct" else "Wrong")
            groups.getValue(k).add(t)
        }
    }
    val n = groups.values.sumOf { it.size }
    val total = groups.values.sumOf { it.sum() }
    return SpeedResult(
        groups.map { (k, v) -> SpeedBucket(k, v.size, if (v.isEmpty()) 0 else v.sum() / v.size) },
        n,
        if (n == 0) 0 else total / n
    )
}
