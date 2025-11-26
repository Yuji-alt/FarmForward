package com.example.farmforward.database.staticData

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

// Data Models
data class CropData(
    val name: String,
    val baseYield: Double,
    val minDays: Int,
    val maxDays: Int,
    val category: String
)

data class FactorData(
    val category: String,
    val factorType: String,
    val option: String,
    val effect: Double
)

@Singleton
class CropRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cropsList: List<CropData> = emptyList()
    private var factorsList: List<FactorData> = emptyList()

    fun loadData() {
        if (cropsList.isNotEmpty()) return // Already loaded

        cropsList = parseCropsCsv()
        factorsList = parseFactorsCsv()
    }

    fun getAllCropNames(): List<String> {
        return cropsList.map { it.name }.sorted()
    }

    fun getCropData(name: String): CropData? {
        return cropsList.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getFactorEffect(category: String, factorType: String, option: String): Double {
        val factor = factorsList.find {
            it.category.equals(category, ignoreCase = true) &&
                    it.factorType.equals(factorType, ignoreCase = true) &&
                    it.option.equals(option, ignoreCase = true)
        }
        return factor?.effect ?: 0.0
    }

    private fun parseCropsCsv(): List<CropData> {
        val list = mutableListOf<CropData>()
        try {
            Log.d("CropRepo", "Attempting to open crops_data.csv...")

            val listFiles = context.assets.list("")
            Log.d("CropRepo", "Files in assets: ${listFiles?.joinToString()}")

            val inputStream = context.assets.open("crop.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val header = reader.readLine()
            Log.d("CropRepo", "Header read: $header")

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                if (tokens.size >= 5) {
                    val name = tokens[0].trim()
                    val category = tokens[4].trim()
                    val value = tokens[1].toDoubleOrNull() ?: 0.0
                    val minDays = tokens[2].toIntOrNull() ?: 0
                    val maxDays = tokens[3].toIntOrNull() ?: 0

                    list.add(CropData(name, value, minDays, maxDays, category))
                } else {
                    Log.e("CropRepo", "Skipping invalid line: $line")
                }
            }
            reader.close()
        } catch (e: Exception) {
            Log.e("CropRepo", "Error parsing CSV: ${e.message}")
            e.printStackTrace()
        }

        Log.d("CropRepo", "Total crops loaded: ${list.size}")
        return list
    }

    private fun parseFactorsCsv(): List<FactorData> {
        val list = mutableListOf<FactorData>()
        try {
            val inputStream = context.assets.open("crop_factors.csv")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readLine() // Skip Header

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                if (tokens.size >= 4) {
                    val category = tokens[0].trim()
                    val factor = tokens[1].trim()
                    val option = tokens[2].trim()
                    val effect = tokens[3].toDoubleOrNull() ?: 0.0

                    list.add(FactorData(category, factor, option, effect))
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}