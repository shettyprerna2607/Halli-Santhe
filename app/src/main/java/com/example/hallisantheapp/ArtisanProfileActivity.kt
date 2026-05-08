package com.example.hallisantheapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hallisantheapp.adapter.ProductAdapter
import com.example.hallisantheapp.databinding.ActivityArtisanProfileBinding
import com.example.hallisantheapp.model.Artisan
import com.example.hallisantheapp.model.Product
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.io.Serializable
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

import android.provider.MediaStore
import android.widget.Toast
import com.bumptech.glide.Glide
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class ArtisanProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtisanProfileBinding
    private lateinit var adapter: ProductAdapter
    private var products = mutableListOf<Product>()
    private var currentArtisan: Artisan? = null
    private var isEditMode = false
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult<String, Uri?>(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.imgProfile.visibility = View.VISIBLE
            binding.tvProfileInitial.visibility = View.GONE
            binding.imgProfile.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtisanProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        val artisan = intent.getSerializableExtra("artisan") as? Artisan
        if (artisan != null) {
            displayArtisan(artisan)
        } else {
            loadMyProfile()
        }

        setupButtons()
    }

    private fun loadMyProfile() {
        val user = SupabaseClient.client.auth.currentUserOrNull()
        if (user != null) {
            val name = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "Your Profile"
            val email = user.email ?: ""
            
            binding.apply {
                tvArtisanName.text = name
                tvBio.text = "Logged in as $email"
                tvProfileInitial.text = name.take(1).uppercase()
                btnEdit.visibility = View.VISIBLE
                
                // Fetch artisan details by user ID
                fetchMyArtisanProfile(user.id)
                
                val prefs = getSharedPreferences("halli_santhe_prefs", MODE_PRIVATE)
                val myPhone = prefs.getString("my_phone", null)
                if (myPhone != null) {
                    fetchArtisanProductsByPhone(myPhone)
                }
            }
        }
    }

    private fun fetchMyArtisanProfile(userId: String) {
        lifecycleScope.launch {
            try {
                // First try to find by User ID
                var artisans = SupabaseClient.client.postgrest.from("artisans").select {
                    filter { eq("id", userId) }
                }.decodeList<Artisan>()
                
                if (artisans.isEmpty()) {
                    // Fallback: try to find by phone number from SharedPreferences
                    val prefs = getSharedPreferences("halli_santhe_prefs", MODE_PRIVATE)
                    val myPhone = prefs.getString("my_phone", null)
                    if (myPhone != null) {
                        artisans = SupabaseClient.client.postgrest.from("artisans").select {
                            filter { eq("phone_number", myPhone) }
                        }.decodeList<Artisan>()
                    }
                }
                
                if (artisans.isNotEmpty()) {
                    currentArtisan = artisans.first()
                    updateProfileUI(currentArtisan!!)
                    fetchArtisanProducts(currentArtisan!!.id!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateProfileUI(artisan: Artisan) {
        binding.apply {
            tvArtisanName.text = artisan.name
            tvBio.text = artisan.bio ?: "Master artisan dedicated to village crafts."
            tvProfileInitial.text = artisan.name.take(1).uppercase()
            
            if (!artisan.profile_image_url.isNullOrEmpty()) {
                imgProfile.visibility = View.VISIBLE
                tvProfileInitial.visibility = View.GONE
                Glide.with(this@ArtisanProfileActivity)
                    .load(artisan.profile_image_url)
                    .circleCrop()
                    .into(imgProfile)
            }
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.btnLogout.setOnClickListener {
            lifecycleScope.launch {
                SupabaseClient.client.auth.signOut()
                val intent = Intent(this@ArtisanProfileActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }

        binding.btnEdit.setOnClickListener {
            toggleEditMode()
        }

        binding.profileImageCard.setOnClickListener {
            if (isEditMode) {
                pickImageLauncher.launch("image/*")
            }
        }

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        binding.btnCall.setOnClickListener {
            currentArtisan?.let { artisan ->
                val intent = Intent(Intent.ACTION_DIAL)
                intent.data = Uri.parse("tel:${artisan.phone_number}")
                startActivity(intent)
            }
        }

        binding.btnWhatsApp.setOnClickListener {
            currentArtisan?.let { artisan ->
                val url = "https://api.whatsapp.com/send?phone=${artisan.phone_number}&text=Hello ${artisan.name}, I saw your crafts on Halli-Santhe!"
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        binding.apply {
            if (isEditMode) {
                tvArtisanName.visibility = View.GONE
                etArtisanName.visibility = View.VISIBLE
                etArtisanName.setText(tvArtisanName.text)
                
                tvBio.visibility = View.GONE
                etBio.visibility = View.VISIBLE
                etBio.setText(if (currentArtisan?.bio != null) currentArtisan!!.bio else "")
                
                imgEditOverlay.visibility = View.VISIBLE
                icEditCamera.visibility = View.VISIBLE
                btnSaveProfile.visibility = View.VISIBLE
                btnEdit.alpha = 0.5f
            } else {
                tvArtisanName.visibility = View.VISIBLE
                etArtisanName.visibility = View.GONE
                
                tvBio.visibility = View.VISIBLE
                etBio.visibility = View.GONE
                
                imgEditOverlay.visibility = View.GONE
                icEditCamera.visibility = View.GONE
                btnSaveProfile.visibility = View.GONE
                btnEdit.alpha = 1.0f
            }
        }
    }

    private fun saveProfile() {
        val newName = binding.etArtisanName.text.toString()
        val newBio = binding.etBio.text.toString()
        
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            binding.btnSaveProfile.isEnabled = false
            binding.btnSaveProfile.text = "Saving..."
            
            try {
                val user = SupabaseClient.client.auth.currentUserOrNull() ?: throw Exception("Not logged in")
                val userId = user.id
                
                var imageUrl = currentArtisan?.profile_image_url
                
                if (selectedImageUri != null) {
                    try {
                        imageUrl = uploadProfileImage(selectedImageUri!!)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback to old image if upload fails, or continue without image
                    }
                }
                
                val phone = getSharedPreferences("halli_santhe_prefs", MODE_PRIVATE).getString("my_phone", "") ?: ""

                // Crucial fix: Use the existing artisan's ID if we found one (even if found by phone)
                // This prevents the "duplicate key" error because it will update the existing row
                val targetId = currentArtisan?.id ?: userId

                val updatedArtisan = Artisan(
                    id = targetId,
                    name = newName,
                    village_name = currentArtisan?.village_name ?: "Local Village",
                    phone_number = currentArtisan?.phone_number ?: phone,
                    bio = newBio,
                    profile_image_url = imageUrl
                )

                // Upsert will now correctly update the existing row based on targetId
                SupabaseClient.client.postgrest.from("artisans").upsert(updatedArtisan)

                currentArtisan = updatedArtisan
                updateProfileUI(updatedArtisan)
                toggleEditMode()
                Toast.makeText(this@ArtisanProfileActivity, "Profile updated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ArtisanProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnSaveProfile.isEnabled = true
                binding.btnSaveProfile.text = "Save Profile"
            }
        }
    }

    private suspend fun uploadProfileImage(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val bytes = contentResolver.openInputStream(uri)?.readBytes() ?: throw Exception("Failed to read image")
            val fileName = "profile_${UUID.randomUUID()}.jpg"
            val bucket = SupabaseClient.client.storage.from("avatars")
            bucket.upload(fileName, bytes)
            bucket.publicUrl(fileName)
        }
    }

    private fun displayArtisan(artisan: Artisan) {
        currentArtisan = artisan
        updateProfileUI(artisan)
        binding.btnLogout.visibility = View.GONE
        binding.btnEdit.visibility = View.GONE
        fetchArtisanProducts(artisan.id!!)
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(products) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("product", product as Serializable)
            startActivity(intent)
        }
        binding.rvArtisanProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvArtisanProducts.adapter = adapter
    }

    private fun fetchArtisanProducts(artisanId: String) {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.client.postgrest.from("products").select {
                    filter { eq("artisan_id", artisanId) }
                }.decodeList<Product>()
                
                updateProductList(result)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun fetchArtisanProductsByPhone(phone: String) {
        lifecycleScope.launch {
            try {
                val artisans = SupabaseClient.client.postgrest.from("artisans").select {
                    filter { eq("phone_number", phone) }
                }.decodeList<Artisan>()
                
                if (artisans.isNotEmpty()) {
                    currentArtisan = artisans.first()
                    updateProfileUI(currentArtisan!!)
                    fetchArtisanProducts(currentArtisan!!.id!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateProductList(result: List<Product>) {
        products.clear()
        products.addAll(result)
        adapter.updateList(products)
        binding.tvCraftCount.text = products.size.toString()
    }
}
