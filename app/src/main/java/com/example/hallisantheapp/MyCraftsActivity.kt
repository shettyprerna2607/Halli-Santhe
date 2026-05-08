package com.example.hallisantheapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hallisantheapp.adapter.ProductAdapter
import com.example.hallisantheapp.databinding.ActivityMyCraftsBinding
import com.example.hallisantheapp.model.Product
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.io.Serializable

class MyCraftsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyCraftsBinding
    private lateinit var adapter: ProductAdapter
    private var myProducts = mutableListOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyCraftsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        checkSellerStatus()

        binding.btnStartSelling.setOnClickListener {
            startActivity(Intent(this, ArtisanUploadActivity::class.java))
        }

        binding.btnAddMore.setOnClickListener {
            startActivity(Intent(this, ArtisanUploadActivity::class.java))
        }
    }

    private fun checkSellerStatus() {
        val prefs = getSharedPreferences("halli_santhe_prefs", MODE_PRIVATE)
        val myPhone = prefs.getString("my_phone", null)

        if (myPhone != null) {
            binding.layoutSeller.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            fetchMyProducts(myPhone)
        } else {
            binding.layoutSeller.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(myProducts) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("product", product as Serializable)
            startActivity(intent)
        }
        binding.rvMyProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvMyProducts.adapter = adapter
    }

    private fun fetchMyProducts(phone: String) {
        lifecycleScope.launch {
            try {
                // First find artisan ID from phone
                val artisans = SupabaseClient.client.postgrest.from("artisans").select {
                    filter { eq("phone_number", phone) }
                }.decodeList<com.example.hallisantheapp.model.Artisan>()

                if (artisans.isNotEmpty()) {
                    val artisanId = artisans.first().id
                    val result = SupabaseClient.client.postgrest.from("products").select {
                        filter { eq("artisan_id", artisanId!!) }
                    }.decodeList<Product>()

                    myProducts.clear()
                    myProducts.addAll(result)
                    adapter.updateList(myProducts)
                    
                    if (myProducts.isEmpty()) {
                        binding.layoutSeller.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkSellerStatus()
    }
}
