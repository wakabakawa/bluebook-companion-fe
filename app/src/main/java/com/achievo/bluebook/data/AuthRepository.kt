package com.achievo.bluebook.data

import io.github.jan.supabase.auth.auth

/**
 * Anonymous sign-in only (BR04/NFR01: a random Supabase-issued user id, not derived from the
 * device or the person). Must only be called after the user has accepted the privacy notice
 * (FR04) — signing in creates a durable row in Supabase's auth.users table.
 */
object AuthRepository {
    suspend fun ensureSession(store: LocalStore): Result<String> = runCatching {
        val auth = Supa.client.auth
        auth.awaitInitialization()
        if (auth.currentSessionOrNull() == null) {
            auth.signInAnonymously()
        }
        val id = auth.currentUserOrNull()?.id ?: error("no session after sign-in")
        store.saveCachedUserId(id)
        id
    }
}
