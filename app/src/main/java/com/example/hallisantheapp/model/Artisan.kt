package com.example.hallisantheapp.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Artisan(
    val id: String? = null,
    val name: String,
    val village_name: String,
    val phone_number: String,
    val bio: String? = null,
    val profile_image_url: String? = null
) : JavaSerializable
