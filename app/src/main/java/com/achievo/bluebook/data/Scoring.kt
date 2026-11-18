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
fun scoreOf(a: Attempt): ScoreResult {
    val outcomes = a.outcomes ?: return ScoreResult(0, 0, a.answers.size, a.flagged.size, a.answers.size)
    var c = 0; var w = 0; var b = 0
    outcomes.forEach { o ->
        when (o) {
            ItemOutcome.CORRECT -> c++
            ItemOutcome.WRONG -> w++
            ItemOutcome.BLANK -> b++
        }
    }
    return ScoreResult(c, w, b, a.flagged.size, a.answers.size)
}

/** FR16: percent correct per difficulty, computed from answered items only. */
fun difficultySlices(sheet: Sheet, a: Attempt): List<Slice> {
    val outcomes = a.outcomes ?: return emptyList()
    val tags = sheet.tags ?: return emptyList()
    return Difficulty.values().map { d ->
        var c = 0; var n = 0
        outcomes.forEachIndexed { i, o ->
            if (o != ItemOutcome.BLANK && i < tags.size && tags[i].difficulty == d) {
                n++
                if (o == ItemOutcome.CORRECT) c++
            }
        }
        Slice(d.label, c, n)
    }
}

/** FR15: percent correct per cognitive skill, computed from answered items only. Cognitive skill
 *  is open-ended classification data (not a fixed enum like Difficulty), so slices are built from
 *  whichever distinct values are actually present on the sheet, rather than a fixed set. */
fun cognitiveSkillSlices(sheet: Sheet, a: Attempt): List<Slice> {
    val outcomes = a.outcomes ?: return emptyList()
    val tags = sheet.tags ?: return emptyList()
    val skills = tags.map { it.cognitiveSkill }.filter { it.isNotBlank() }.distinct()
    return skills.map { skill ->
        var c = 0; var n = 0
        outcomes.forEachIndexed { i, o ->
            if (o != ItemOutcome.BLANK && i < tags.size && tags[i].cognitiveSkill == skill) {
                n++
                if (o == ItemOutcome.CORRECT) c++
            }
        }
        Slice(skill, c, n)
    }
}

/** Weak/strong topics of one attempt, with the book page so the student knows where to go back (FR18, FR21). */
fun topicSlices(sheet: Sheet, a: Attempt): List<Slice> {
    val outcomes = a.outcomes ?: return emptyList()
    val tags = sheet.tags ?: return emptyList()
    return tags.indices.groupBy { tags[it].topic }.map { (topic, idx) ->
        val answeredIdx = idx.filter { it < outcomes.size && outcomes[it] != ItemOutcome.BLANK }
        Slice(
            label = topic,
            correct = answeredIdx.count { outcomes[it] == ItemOutcome.CORRECT },
            answered = answeredIdx.size,
            note = tags[idx.first()].bookPage?.let { "Pg $it" }
        )
    }
}

/** FR13: mastery per key concept across ALL attempts of a subject (finest classification level). */
fun keyConceptMastery(sheets: List<Sheet>, attempts: List<Attempt>, subject: String): List<Slice> {
    val concepts = sheets.filter { it.subject == subject }
        .flatMap { it.tags.orEmpty() }.map { it.keyConcept }.filter { it.isNotBlank() }.distinct()
    return concepts.map { kc ->
        var c = 0; var n = 0
        attempts.forEach { a ->
            val s = sheets.firstOrNull { it.id == a.sheetId }
            val tags = s?.tags
            val outcomes = a.outcomes
            if (s != null && tags != null && outcomes != null && s.subject == subject) {
                tags.indices.forEach { i ->
                    if (i < outcomes.size && tags[i].keyConcept == kc && outcomes[i] != ItemOutcome.BLANK) {
                        n++
                        if (outcomes[i] == ItemOutcome.CORRECT) c++
                    }
                }
            }
        }
        Slice(kc, c, n)
    }
}

/** FR13: single mastery % for an entire subject across ALL attempts (coarsest granularity). */
fun subjectMastery(sheets: List<Sheet>, attempts: List<Attempt>, subject: String): Slice {
    var c = 0; var n = 0
    attempts.forEach { a ->
        val s = sheets.firstOrNull { it.id == a.sheetId }
        val outcomes = a.outcomes
        if (s != null && outcomes != null && s.subject == subject) {
            outcomes.forEach { o ->
                if (o != ItemOutcome.BLANK) {
                    n++
                    if (o == ItemOutcome.CORRECT) c++
                }
            }
        }
    }
    return Slice(subject, c, n)
}

/** Overall mastery % across every attempt the student has made, any subject (FR20's estimate input). */
fun overallMastery(attempts: List<Attempt>): Slice {
    var c = 0; var n = 0
    attempts.forEach { a ->
        a.outcomes?.forEach { o -> if (o != ItemOutcome.BLANK) { n++; if (o == ItemOutcome.CORRECT) c++ } }
    }
    return Slice("Overall", c, n)
}

/** One attempt's result restricted to a single topic (FR19). */
data class ProgressPoint(val submittedAt: Long, val slice: Slice)

/** FR19: time-ordered mastery trend for one topic, across ALL attempts (including retakes and
 *  different sets/sheets that happen to share the topic). One point per attempt that actually
 *  touched the topic; attempts that didn't are skipped, never shown as a fake 0%. */
fun topicProgress(sheets: List<Sheet>, attempts: List<Attempt>, topic: String): List<ProgressPoint> =
    attempts.sortedBy { it.submittedAt }.mapNotNull { a ->
        val s = sheets.firstOrNull { it.id == a.sheetId } ?: return@mapNotNull null
        val tags = s.tags ?: return@mapNotNull null
        val outcomes = a.outcomes ?: return@mapNotNull null
        var c = 0; var n = 0
        outcomes.forEachIndexed { i, o ->
            if (o != ItemOutcome.BLANK && i < tags.size && tags[i].topic == topic) {
                n++
                if (o == ItemOutcome.CORRECT) c++
            }
        }
        if (n == 0) null else ProgressPoint(a.submittedAt, Slice(topic, c, n))
    }

/** FR13: mastery per branch across ALL attempts of a subject. */
fun branchMastery(sheets: List<Sheet>, attempts: List<Attempt>, subject: String): List<Slice> {
    val branches = sheets.filter { it.subject == subject }.map { it.branch }.distinct()
    return branches.map { br ->
        var c = 0; var n = 0
        attempts.forEach { a ->
            val s = sheets.firstOrNull { it.id == a.sheetId }
            val outcomes = a.outcomes
            if (s != null && outcomes != null && s.subject == subject && s.branch == br) {
                outcomes.forEach { o ->
                    if (o != ItemOutcome.BLANK) {
                        n++
                        if (o == ItemOutcome.CORRECT) c++
                    }
                }
            }
        }
        Slice(br, c, n)
    }
}

/** FR17: fast/slow x correct/wrong against each item's OWN reference time (per-item/per-difficulty,
 *  CC03), falling back to [fallbackRef] only when [tags] is unavailable (e.g. a cached attempt from
 *  before per-item reference seconds existed). */
fun speedOf(a: Attempt, tags: List<ItemTag>? = null, fallbackRef: Int = AppConfig.REFERENCE_SECONDS): SpeedResult {
    val outcomes = a.outcomes ?: return SpeedResult(emptyList(), 0, 0)
    val groups = linkedMapOf(
        "Fast & Correct" to mutableListOf<Int>(),
        "Slow & Correct" to mutableListOf<Int>(),
        "Fast & Wrong" to mutableListOf<Int>(),
        "Slow & Wrong" to mutableListOf<Int>()
    )
    outcomes.forEachIndexed { i, o ->
        if (o != ItemOutcome.BLANK) {
            val t = a.itemSeconds[i]
            val ref = tags?.getOrNull(i)?.refSeconds ?: fallbackRef
            val k = (if (t <= ref) "Fast" else "Slow") + " & " + (if (o == ItemOutcome.CORRECT) "Correct" else "Wrong")
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

data class ScoreEstimate(val low: Int, val high: Int, val gap: Int)

/** FR20: maps overall mastery % onto a fixed placeholder score band (CC11 — pending Achievo's
 *  real passing-score cutoffs per program/exam). Linear, not a real psychometric model: an
 *  honest placeholder, not fabricated precision — never present this as authoritative. */
fun estimatedScoreRange(
    overallMasteryPercent: Int,
    bandLow: Int = AppConfig.CC11_SCORE_BAND_LOW,
    bandHigh: Int = AppConfig.CC11_SCORE_BAND_HIGH,
    targetCutoff: Int = AppConfig.CC11_TARGET_CUTOFF
): ScoreEstimate {
    val mid = bandLow + (bandHigh - bandLow) * overallMasteryPercent / 100
    val spread = (bandHigh - bandLow) / 10
    val low = (mid - spread).coerceIn(bandLow, bandHigh)
    val high = (mid + spread).coerceIn(bandLow, bandHigh)
    return ScoreEstimate(low, high, (targetCutoff - mid).coerceAtLeast(0))
}

/** FR22: one topic's own mastery next to the cohort's (v_topic_stats.cohort_mastery), for the
 *  Anonymous Cohort Comparison screen. Carries no other profile's identity. */
data class CohortSlice(
    val topicId: String,
    val topicLabel: String,
    val subjectLabel: String,
    val branchLabel: String,
    val ownAnswered: Int,
    val ownCorrect: Int,
    val cohortMasteryPercent: Int?,
    val reliable: Boolean
) {
    val ownPercent: Int get() = if (ownAnswered == 0) 0 else (ownCorrect * 100f / ownAnswered).roundToInt()
}

enum class CohortStanding { BELOW, NEAR, ABOVE, UNKNOWN }

/** FR22: a relative-standing label, not a fabricated percentile — the schema only has a cohort
 *  MEAN (cohort_mastery), no distribution. UNKNOWN when either side isn't reliable/released yet. */
fun cohortStanding(slice: CohortSlice, nearBand: Int = AppConfig.COHORT_NEAR_BAND_PERCENT): CohortStanding {
    val cohortPercent = slice.cohortMasteryPercent
    if (!slice.reliable || cohortPercent == null) return CohortStanding.UNKNOWN
    val diff = slice.ownPercent - cohortPercent
    return when {
        diff > nearBand -> CohortStanding.ABOVE
        diff < -nearBand -> CohortStanding.BELOW
        else -> CohortStanding.NEAR
    }
}
