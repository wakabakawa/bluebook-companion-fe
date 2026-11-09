package com.achievo.bluebook.data

/**
 * Values the client has NOT confirmed yet. Keep them here, never inside screens (NFR09).
 * CC01 readiness levels, CC03 reference time, CC04 minimum answered items.
 */
object AppConfig {
    const val MIN_ITEMS_FOR_PERCENT = 5      // CC04 placeholder
    const val REFERENCE_SECONDS = 30         // CC03 placeholder (default per item)
}
