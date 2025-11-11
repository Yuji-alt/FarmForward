package com.example.farmforward.fragmentController

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.roomDatabase.CropEntity

class HomeController(private val context: Context, private val container: LinearLayout) {

    @SuppressLint("SetTextI18n")
    fun displayCrops(crops: List<CropEntity>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val addView = inflater.inflate(R.layout.item_add_crop, container, false)
        val addButton = addView.findViewById<ImageButton>(R.id.itemImage)
        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_card, container, false)

            val itemImage = itemView.findViewById<ImageView>(R.id.itemImage)
            val itemTitle = itemView.findViewById<TextView>(R.id.itemTitle)
            val itemDesc = itemView.findViewById<TextView>(R.id.itemDesc)
            val itemArea = itemView.findViewById<TextView>(R.id.itemArea)

            itemTitle.text = crop.cropName
            itemDesc.text = "Expected Yield: ${crop.expectedYield} kg"
            itemArea.text = "Area: ${crop.area} sqr. meter"
            container.addView(itemView)
        }
        container.addView(addView)
        addButton.setOnClickListener {
            (context as? MainActivity)?.controller?.switchFragment(R.id.nav_calc)
        }
    }
}