package com.example.hallisantheapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hallisantheapp.databinding.ItemProductHorizontalBinding
import com.example.hallisantheapp.model.Product

class HorizontalProductAdapter(
    private var products: List<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<HorizontalProductAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemProductHorizontalBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]
        holder.binding.apply {
            tvTitle.text = product.title
            tvPrice.text = "₹${product.price.toInt()}"
            
            Glide.with(imgProduct.context)
                .load(product.image_url)
                .centerCrop()
                .into(imgProduct)

            root.setOnClickListener { onItemClick(product) }
        }
    }

    override fun getItemCount() = products.size

    fun updateList(newList: List<Product>) {
        products = newList
        notifyDataSetChanged()
    }
}
