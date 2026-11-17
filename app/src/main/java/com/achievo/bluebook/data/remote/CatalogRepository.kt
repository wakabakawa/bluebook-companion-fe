package com.achievo.bluebook.data.remote

import com.achievo.bluebook.data.Difficulty
import com.achievo.bluebook.data.ItemTag
import com.achievo.bluebook.data.Sheet
import com.achievo.bluebook.data.Supa
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

@Serializable
private data class SheetRow(
    val sheet_id: String,
    val set_name: String,
    val subject: String,
    val branch: String,
    val item_count: Int,
    val allotment_seconds: Int,
    val published: Boolean
)

@Serializable
private data class ItemRow(
    val sheet_id: String,
    val item_no: Int,
    val topic: String,
    val key_concept: String?,
    val cognitive_skill: String?,
    val difficulty: String,
    val book_page: Int?
)

/** Replaces SampleData.kt's hardcoded catalogue (v_sheet_catalog / v_sheet_items, FR05). */
object CatalogRepository {
    suspend fun loadSheets(): List<Sheet> {
        val sheetRows = Supa.client.postgrest.from("v_sheet_catalog").select().decodeList<SheetRow>()
        val itemsBySheet = Supa.client.postgrest.from("v_sheet_items").select().decodeList<ItemRow>()
            .groupBy { it.sheet_id }

        return sheetRows.map { r ->
            Sheet(
                id = r.sheet_id,
                setName = r.set_name,
                subject = r.subject,
                branch = r.branch,
                itemCount = r.item_count,
                allotmentSeconds = r.allotment_seconds,
                tags = itemsBySheet[r.sheet_id]?.sortedBy { it.item_no }?.map { it.toItemTag(r.subject, r.branch) },
                published = r.published
            )
        }
    }
}

private fun ItemRow.toItemTag(subject: String, branch: String) = ItemTag(
    subject = subject,
    branch = branch,
    topic = topic,
    keyConcept = key_concept ?: "",
    cognitiveSkill = cognitive_skill ?: "",
    difficulty = Difficulty.entries.first { it.label == difficulty },
    bookPage = book_page
)
