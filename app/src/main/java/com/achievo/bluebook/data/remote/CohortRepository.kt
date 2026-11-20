package com.achievo.bluebook.data.remote

import com.achievo.bluebook.data.AppConfig
import com.achievo.bluebook.data.CohortSlice
import com.achievo.bluebook.data.Supa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
private data class TopicStatsRow(
    val topic_id: String,
    val topic_label: String,
    val subject_label: String,
    val branch_label: String,
    val answered: Int,
    val correct: Int,
    val cohort_mastery: Double?,
    val reliable: Boolean
)

/** FR22: topic-level cohort comparison, read directly from v_topic_stats (already
 *  security_invoker + granted to authenticated - no new backend surface needed). Carries no
 *  other profile's identity; a missing cohort_mastery just means "not released yet" (BR09). */
object CohortRepository {
    suspend fun loadTopicStats(): List<CohortSlice> =
        Supa.client.postgrest.from("v_topic_stats").select().decodeList<TopicStatsRow>().map {
            CohortSlice(
                topicId = it.topic_id,
                topicLabel = it.topic_label,
                subjectLabel = it.subject_label,
                branchLabel = it.branch_label,
                ownAnswered = it.answered,
                ownCorrect = it.correct,
                cohortMasteryPercent = it.cohort_mastery?.let { m -> (m * 100).toInt() },
                reliable = it.reliable && it.answered >= AppConfig.MIN_ITEMS_FOR_PERCENT
            )
        }
}
