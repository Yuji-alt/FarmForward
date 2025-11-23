package com.example.farmforward.appActivity.home

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.roomDatabase.CropEntity

class HomeController(private val context: Context, private val container: LinearLayout) {

    @SuppressLint("SetTextI18n")
    fun displayCrops(crops: List<CropEntity>, onItemClick: (CropEntity) -> Unit) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(context)

        val addView = inflater.inflate(R.layout.item_add_crop, container, false)
        val addButton = addView.findViewById<ImageButton>(R.id.itemImage)

        addButton.setOnClickListener {
            (context as? MainActivity)?.controller?.switchFragment(R.id.nav_calc)
        }
        container.addView(addView)

        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_card, container, false)

            val itemTitle = itemView.findViewById<TextView>(R.id.itemTitle)
            val itemDesc = itemView.findViewById<TextView>(R.id.itemDesc)
            val itemArea = itemView.findViewById<TextView>(R.id.itemArea)

            itemTitle.text = crop.cropName
            itemDesc.text = "Expected Yield: ${crop.expectedYield} kg"
            itemArea.text = "Area: ${crop.area} sqr. meter"

            itemView.setOnClickListener {
                onItemClick(crop)
            }

            container.addView(itemView)
        }
    }
}