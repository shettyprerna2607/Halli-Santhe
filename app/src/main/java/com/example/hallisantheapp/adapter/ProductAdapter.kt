package com.example.hallisantheapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hallisantheapp.databinding.ItemProductBinding
import com.example.hallisantheapp.model.Product

class ProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.binding.apply {
            tvTitle.text = product.title
            tvPrice.text = "₹${product.price.toInt()}"
            tvArtisan.text = "by ${product.artisan_name ?: "Unknown Artisan"}"
            
            Glide.with(imgProduct.context)
                .load(product.image_url)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(imgProduct)

            root.setOnClickListener { onItemClick(product) }

            var isFavorite = false
            btnFavorite.setOnClickListener {
                isFavorite = !isFavorite
                if (isFavorite) {
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_on)
                    btnFavorite.setColorFilter(android.graphics.Color.RED)
                } else {
                    btnFavorite.setImageResource(android.R.drawable.btn_star_big_off)
                    btnFavorite.setColorFilter(android.graphics.Color.WHITE)
                }
            }
        }
    }

    override fun getItemCount() = products.size

    fun updateList(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }
}
