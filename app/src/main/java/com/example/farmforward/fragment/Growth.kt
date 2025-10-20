package com.example.farmforward.fragment

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.farmforward.R
import com.example.farmforward.database.CropViewModel

class GrowthFragment : Fragment(R.layout.fragment_growth) {

    private lateinit var cropViewModel: CropViewModel

    override fun onStart() {
        super.onStart()

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        val userId = getCurrentUserId()
        if (userId != null) {
            cropViewModel.getUserCrops(userId).observe(viewLifecycleOwner) { crops ->
                if (crops.isNotEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Loaded ${crops.size} crops for user $userId 🌱",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(requireContext(), "No crops found yet.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentUserId(): Int? {
        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", -1)
        return if (userId != -1) userId else null
    }
}
