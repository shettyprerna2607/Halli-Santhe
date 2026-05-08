package com.example.hallisantheapp.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Category(
    val id: Int? = null,
    val name: String
) : JavaSerializable
