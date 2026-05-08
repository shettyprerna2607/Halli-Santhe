package com.example.hallisantheapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://setnnnzkmxmzurogwtcf.supabase.co",
        supabaseKey = "sb_publishable_o2dCP_lO5pz8OYzddk5U1A_IvQJfFE2"
    ) {
        install(Postgrest)
        install(Storage)
        install(Auth)
    }
}
