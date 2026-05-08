package com.example.hallisantheapp.model

import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable

@Serializable
data class Product(
    val id: String? = null,
    val title: String,
    val description: String,
    val price: Double,
    val image_url: String? = null,
    val artisan_id: String? = null,
    val artisan_name: String? = null,
    val category_id: Int? = null,
    val stock_quantity: Int = 1,
    val is_available: Boolean = true
) : JavaSerializable
