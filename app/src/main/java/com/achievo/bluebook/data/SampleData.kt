package com.achievo.bluebook.data

/**
 * Stand-in for the backend. Only "Set A / Geometry" has an answer key and tags (sample set).
 * Everything else is unpublished so the "no published answer key" state (FR05) can be tested.
 * TODO: replace with Supabase once the backend contract is agreed (CC09, CC10 pending).
 */
object SampleData {
    val setNames = listOf("Set A", "Set B", "Set C")

    private val layout = listOf(
        "Mathematics" to listOf("Geometry" to 10, "Trigonometry" to 30, "Calculus" to 20),
        "Science" to listOf("Chemistry" to 10, "Biology" to 10),
        "Language Proficiency" to listOf("Grammar" to 10),
        "Reading Comprehension" to listOf("Passages" to 10)
    )

    private val geometryKey = "BDACBDAACD".toList()
    private val topics = listOf("Triangles", "Triangles", "Circles", "Polygons", "Circles", "Solids", "Polygons", "Triangles", "Solids", "Circles")
    private val concepts = listOf("Angle sum", "Pythagorean theorem", "Arc length", "Interior angles", "Chords", "Volume", "Perimeter", "Similarity", "Surface area", "Tangents")
    private val skills = listOf("Recall", "Application", "Analysis")
    private val diffs = listOf(
        Difficulty.EASY, Difficulty.EASY, Difficulty.AVERAGE, Difficulty.EASY, Difficulty.AVERAGE,
        Difficulty.HARD, Difficulty.EASY, Difficulty.AVERAGE, Difficulty.HARD, Difficulty.HARD
    )
    private val geometryTags = List(10) { i ->
        ItemTag("Mathematics", "Geometry", topics[i], concepts[i], skills[i % 3], diffs[i], 11 + i / 3)
    }

    val sheets: List<Sheet> = setNames.flatMap { set ->
        layout.flatMap { (subject, branches) ->
            branches.map { (branch, n) ->
                val sample = set == "Set A" && branch == "Geometry"
                Sheet(
                    id = "${set}-${branch}".replace(' ', '_'),
                    setName = set,
                    subject = subject,
                    branch = branch,
                    itemCount = n,
                    allotmentSeconds = n * 30,
                    key = if (sample) geometryKey else null,
                    tags = if (sample) geometryTags else null
                )
            }
        }
    }
}
