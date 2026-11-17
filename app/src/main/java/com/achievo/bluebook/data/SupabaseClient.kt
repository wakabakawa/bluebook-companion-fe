package com.achievo.bluebook.data

import com.achievo.bluebook.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import kotlin.time.Duration.Companion.seconds

/** The app's one Supabase client. `by lazy` so construction never lands on the splash's critical path. */
object Supa {
    val client: SupabaseClient by lazy {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
            install(Functions)
            requestTimeout = AppConfig.API_TIMEOUT_SECONDS.seconds
        }
    }
}
