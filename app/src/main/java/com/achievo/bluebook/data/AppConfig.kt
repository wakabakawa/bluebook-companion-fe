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

    const val API_TIMEOUT_SECONDS = 15L
}
