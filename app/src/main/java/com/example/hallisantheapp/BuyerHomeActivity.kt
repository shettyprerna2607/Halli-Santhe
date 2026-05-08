package com.example.hallisantheapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.hallisantheapp.adapter.ProductAdapter
import com.example.hallisantheapp.databinding.ActivityBuyerHomeBinding
import com.example.hallisantheapp.model.Product
import com.google.android.material.chip.Chip
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.io.Serializable

class BuyerHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuyerHomeBinding
    private lateinit var adapter: ProductAdapter
    private var allProducts = mutableListOf<Product>()
    private var currentCategory = "All"

    private val exploreLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedCategory = result.data?.getStringExtra("selected_category")
            selectedCategory?.let {
                currentCategory = it
                filterProducts(binding.etSearch.text.toString())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuyerHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        fetchProducts()

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterProducts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = findViewById<Chip>(checkedIds[0])
                currentCategory = if (chip.text == "All Crafts") "All" else chip.text.toString()
                filterProducts(binding.etSearch.text.toString())
            }
        }

        binding.fabAdd.setOnClickListener {
            startActivity(Intent(this, ArtisanUploadActivity::class.java))
        }

        setupNavigation()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    currentCategory = "All"
                    filterProducts("")
                    true
                }
                R.id.nav_categories -> {
                    val intent = Intent(this, ExploreActivity::class.java)
                    exploreLauncher.launch(intent)
                    true
                }
                R.id.nav_my_crafts -> {
                    startActivity(Intent(this, MyCraftsActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ArtisanProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(mutableListOf()) { product ->
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("product", product as Serializable)
            startActivity(intent)
        }
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)
        binding.rvProducts.adapter = adapter
    }

    private fun fetchProducts() {
        lifecycleScope.launch {
            try {
                val result = SupabaseClient.client.postgrest.from("products").select().decodeList<Product>()
                allProducts.clear()
                allProducts.addAll(result)
                filterProducts(binding.etSearch.text.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun filterProducts(query: String) {
        val filteredList = allProducts.filter { product ->
            val matchesSearch = product.title.contains(query, ignoreCase = true) ||
                                product.description.contains(query, ignoreCase = true)
            val matchesCategory = currentCategory == "All" || product.category_id.toString().contains(currentCategory) 
            // Note: Since we used names in chips, we'll simplify this or match by category name if available
            matchesSearch && matchesCategory
        }
        adapter.updateList(filteredList)
        binding.emptyState.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }
}
