package com.example.hallisantheapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hallisantheapp.databinding.ActivityExploreBinding

class ExploreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExploreBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardPottery.setOnClickListener { selectCategory("Pottery") }
        binding.cardHandloom.setOnClickListener { selectCategory("Handloom") }
        binding.cardJewelry.setOnClickListener { selectCategory("Jewelry") }
        binding.cardWoodwork.setOnClickListener { selectCategory("Woodwork") }
        binding.cardOrganic.setOnClickListener { selectCategory("Organic Food") }
        binding.cardPaintings.setOnClickListener { selectCategory("Paintings") }
        binding.cardDecor.setOnClickListener { selectCategory("Home Decor") }
        binding.cardAll.setOnClickListener { selectCategory("All") }
    }

    private fun selectCategory(category: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("selected_category", category)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
