package com.example.hallisantheapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.hallisantheapp.databinding.ActivityArtisanUploadBinding
import com.example.hallisantheapp.model.Artisan
import com.example.hallisantheapp.model.Category
import com.example.hallisantheapp.model.Product
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.*

class ArtisanUploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtisanUploadBinding
    private var imageUri: Uri? = null
    
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            imageUri = result.data?.data
            binding.imgProduct.setImageURI(imageUri)
            binding.imgProduct.setPadding(0, 0, 0, 0)
            binding.imgProduct.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
    }
    
    private var categoryList: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtisanUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fetchCategories()

        binding.cardImage.setOnClickListener {
            openGallery()
        }

        // Force dropdown to show when clicked
        binding.spinnerCategory.setOnClickListener {
            binding.spinnerCategory.showDropDown()
        }

        binding.btnUpload.setOnClickListener {
            validateAndUpload()
        }
    }

    private fun fetchCategories() {
        lifecycleScope.launch {
            try {
                // Fetch from DB
                val dbCategories = SupabaseClient.client.postgrest.from("categories").select().decodeList<Category>()
                categoryList = dbCategories
                
                // Startup-level categories list
                val startupCategories = listOf(
                    "Pottery", "Handloom", "Jewelry", "Woodwork", 
                    "Organic Food", "Paintings", "Home Decor", 
                    "Bamboo Crafts", "Leather Work", "Terracotta"
                )
                
                // Combine and remove duplicates
                val allNames = (startupCategories + dbCategories.map { it.name }).distinct().sorted()
                
                val adapter = ArrayAdapter(this@ArtisanUploadActivity, android.R.layout.simple_dropdown_item_1line, allNames)
                binding.spinnerCategory.setAdapter(adapter)
            } catch (e: Exception) {
                // Fallback to local list if DB fails
                val localCategories = listOf("Pottery", "Handloom", "Jewelry", "Woodwork", "Organic Food")
                val adapter = ArrayAdapter(this@ArtisanUploadActivity, android.R.layout.simple_dropdown_item_1line, localCategories)
                binding.spinnerCategory.setAdapter(adapter)
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        pickImageLauncher.launch(Intent.createChooser(intent, "Select Product Image"))
    }

    private fun validateAndUpload() {
        val name = binding.etProductName.text.toString().trim()
        val priceStr = binding.etPrice.text.toString().trim()
        val categoryName = binding.spinnerCategory.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val sellerName = binding.etSellerName.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()

        if (name.isEmpty() || priceStr.isEmpty() || categoryName.isEmpty() || description.isEmpty() || sellerName.isEmpty() || contact.isEmpty() || imageUri == null) {
            Toast.makeText(this, "Please fill all fields and select an image", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull() ?: 0.0
        var category = categoryList.find { it.name == categoryName }
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpload.isEnabled = false

        lifecycleScope.launch {
            try {
                // Create a local copy for thread safety
                var finalCategory = category
                
                // If category doesn't exist in DB, create it
                if (finalCategory == null) {
                    val newCat = Category(name = categoryName)
                    val catResponse = SupabaseClient.client.postgrest.from("categories").insert(newCat) {
                        select()
                    }
                    finalCategory = catResponse.decodeSingle<Category>()
                }

                val artisanId = getOrCreateArtisan(sellerName, contact)
                val imageUrl = uploadImageToSupabase()
                
                val product = Product(
                    title = name,
                    description = description,
                    price = price,
                    image_url = imageUrl,
                    artisan_id = artisanId,
                    artisan_name = sellerName,
                    category_id = finalCategory.id!!
                )
                
                SupabaseClient.client.postgrest.from("products").insert(product)
                
                // Save seller identity for "My Crafts" tab
                val prefs = getSharedPreferences("halli_santhe_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("my_phone", contact).apply()
                
                Toast.makeText(this@ArtisanUploadActivity, "Product uploaded successfully!", Toast.LENGTH_LONG).show()
                finish()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnUpload.isEnabled = true
                Toast.makeText(this@ArtisanUploadActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getOrCreateArtisan(name: String, phone: String): String {
        val response = SupabaseClient.client.postgrest.from("artisans").select {
            filter {
                eq("phone_number", phone)
            }
        }
        
        val existing = response.decodeSingleOrNull<Artisan>()
        if (existing != null) return existing.id!!
        
        val newArtisan = Artisan(
            name = name,
            village_name = "Village",
            phone_number = phone,
            bio = "Artisan from our village"
        )
        
        val insertResponse = SupabaseClient.client.postgrest.from("artisans").insert(newArtisan) {
            select()
        }
        
        return insertResponse.decodeSingle<Artisan>().id!!
    }

    private suspend fun uploadImageToSupabase(): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        val bucket = SupabaseClient.client.storage.from("product-images")
        
        val inputStream = contentResolver.openInputStream(imageUri!!)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()
        
        bucket.upload(fileName, data)
        return bucket.publicUrl(fileName)
    }
}
