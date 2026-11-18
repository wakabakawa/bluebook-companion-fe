package com.achievo.bluebook.data

/**
 * Values the client has NOT confirmed yet. Keep them here, never inside screens (NFR09).
 * CC01 readiness levels, CC03 reference time, CC04 minimum answered items.
 */
object AppConfig {
    const val MIN_ITEMS_FOR_PERCENT = 5      // CC04 placeholder
    const val REFERENCE_SECONDS = 30         // CC03 placeholder (default per item), used by speedOf()

    const val BOOK_EDITION_ID = 1            // CC10 pending
    const val DEFAULT_BUDGET_MIN = 30
    const val MIN_BUDGET_MIN = 10
    const val MAX_BUDGET_MIN = 240           // server accepts up to 600
    const val BUDGET_STEP_MIN = 10
    const val DEFAULT_MIN_STRONG = 1

    // CC11 placeholders — pending Achievo's real passing-score cutoffs per program/exam (FR20).
    // A fixed display band and target cutoff, never presented as authoritative.
    const val CC11_SCORE_BAND_LOW = 40
    const val CC11_SCORE_BAND_HIGH = 95
    const val CC11_TARGET_CUTOFF = 75

    // Tolerance for "near average" in the Cohort Comparison standing label (FR22) — no client
    // input exists for this either; a placeholder like the above.
    const val COHORT_NEAR_BAND_PERCENT = 5

    const val API_TIMEOUT_SECONDS = 15L
}
