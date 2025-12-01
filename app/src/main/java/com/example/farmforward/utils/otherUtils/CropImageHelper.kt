package com.example.farmforward.utils

import com.example.farmforward.R

object CropImageHelper {

    fun getImageRes(cropName: String): Int {
        // Normalize the name (lowercase, remove extra spaces)
        val name = cropName.lowercase().trim()

        return when {
            // Match based on your list
            name.contains("banana") -> R.drawable.ic_bananas
            name.contains("broccoli") -> R.drawable.ic_broccoli
            name.contains("carrot") -> R.drawable.ic_carrots
            name.contains("cauliflower") -> R.drawable.ic_cauliflower
            name.contains("chicory") -> R.drawable.ic_chicory_roots
            name.contains("chili") -> R.drawable.ic_chili
            name.contains("corn") -> R.drawable.ic_corn
            name.contains("cow pea") -> R.drawable.ic_cow_peas
            name.contains("cucumber") -> R.drawable.ic_cucumbers
            name.contains("eggplant") -> R.drawable.ic_eggplants
            name.contains("ginger") -> R.drawable.ic_ginger
            name.contains("grape") -> R.drawable.ic_grapes
            name.contains("lettuce") -> R.drawable.ic_lettuce
            name.contains("mustard") -> R.drawable.ic_plant
            name.contains("okra") -> R.drawable.ic_okra
            name.contains("onion") -> R.drawable.ic_onions
            name.contains("papaya") -> R.drawable.ic_papaya
            name.contains("pechay") -> R.drawable.ic_pechay
            name.contains("pepper") -> R.drawable.ic_pepper
            name.contains("pineapple") -> R.drawable.ic_pineapple
            name.contains("potato") && !name.contains("sweet") -> R.drawable.ic_potatoes
            name.contains("pumpkin") -> R.drawable.ic_pumpkin
            name.contains("radish") -> R.drawable.ic_radish
            name.contains("shallot") -> R.drawable.ic_shallots
            name.contains("spinach") -> R.drawable.ic_spinach
            name.contains("strawberr") -> R.drawable.ic_strawberries
            name.contains("sweet potato") -> R.drawable.ic_sweet_potatoes
            name.contains("taro") -> R.drawable.ic_taro
            name.contains("tomato") -> R.drawable.ic_tomatoes
            name.contains("watermelon") -> R.drawable.ic_watermelons
            name.contains("yam") -> R.drawable.ic_yams

            else -> R.drawable.ic_plant
        }
    }
}