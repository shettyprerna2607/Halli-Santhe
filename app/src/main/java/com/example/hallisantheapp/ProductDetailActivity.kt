package com.example.hallisantheapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hallisantheapp.databinding.ActivityProductDetailBinding
import com.example.hallisantheapp.model.Artisan
import com.example.hallisantheapp.model.Product
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.launch
import java.io.Serializable

class ProductDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val product = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("product", Product::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("product") as? Product
        }

        product?.let { displayProduct(it) }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun displayProduct(product: Product) {
        binding.apply {
            tvName.text = product.title
            tvPrice.text = "₹${product.price}"
            tvDescription.text = product.description
            tvCategory.text = "Handmade Authentic Craft"
            
            // Fetch Artisan Details
            product.artisan_id?.let { artisanId ->
                lifecycleScope.launch {
                    try {
                        val response = SupabaseClient.client.postgrest.from("artisans").select {
                            filter {
                                eq("id", artisanId)
                            }
                        }
                        
                        val artisan = response.decodeSingleOrNull<Artisan>()
                            
                        artisan?.let { art ->
                            tvSellerName.text = art.name
                            tvSellerContact.text = art.phone_number
                            tvInitial.text = art.name.take(1).uppercase()
                            
                            cardArtisan.setOnClickListener {
                                val intent = Intent(this@ProductDetailActivity, ArtisanProfileActivity::class.java)
                                intent.putExtra("artisan", art as Serializable)
                                startActivity(intent)
                            }

                            btnContact.setOnClickListener {
                                contactSeller(art.phone_number, product.title)
                            }

                            btnCall.setOnClickListener {
                                callArtisan(art.phone_number)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Glide.with(this@ProductDetailActivity)
                .load(product.image_url)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(imgProductLarge)
        }
    }

    private fun callArtisan(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    private fun contactSeller(contact: String, productName: String) {
        val message = "Hello, I am interested in your product: $productName on Halli-Santhe!"
        val url = "https://api.whatsapp.com/send?phone=$contact&text=${Uri.encode(message)}"
        
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("smsto:$contact")
            smsIntent.putExtra("sms_body", message)
            startActivity(smsIntent)
        }
    }
}
