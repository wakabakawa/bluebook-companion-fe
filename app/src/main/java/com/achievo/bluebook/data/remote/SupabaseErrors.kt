package com.achievo.bluebook.data.remote

import io.github.jan.supabase.exceptions.RestException
import org.json.JSONObject

/**
 * [RestException.message] embeds the failed request's raw headers (including the bearer
 * token) for debug logging - never show it to the user. [RestException.error] is the actual
 * server-provided text: a problem+json body for plan-gateway, a plain Postgrest message
 * otherwise, so both are handled here instead of assuming one shape.
 */
fun Throwable.toUserMessage(fallback: String): String {
    val error = (this as? RestException)?.error ?: return fallback
    val fromProblemJson = runCatching { JSONObject(error) }.getOrNull()
        ?.let { it.optString("detail").ifBlank { it.optString("title") } }
        ?.ifBlank { null }
    return fromProblemJson ?: error.ifBlank { fallback }
}
