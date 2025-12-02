package com.example.farmforward.appActivity.mainActivity.otherFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject lateinit var session: SessionManager
    @Inject lateinit var db: AppDatabase
    private lateinit var cropViewModel: CropViewModel


    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvActivePlants: TextView
    private lateinit var tvHarvested: TextView
    private lateinit var tvFavorite: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmail = view.findViewById(R.id.tvDatePlanted)
        tvActivePlants = view.findViewById(R.id.tvActiveplants)
        tvHarvested = view.findViewById(R.id.yvHarvestedCrops)
        tvFavorite = view.findViewById(R.id.tvFavorite)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profile)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onBackClicked(cropViewModel)
        }

        loadProfileData()
        return view
    }

    private fun loadProfileData() {
        tvUsername.text = session.getUserName()
        tvEmail.text = "Email: ${session.getUserEmail()}"

        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            if (userId != -1) {
                val activeCount = db.cropDao().getActivePlantCount(userId)
                val harvestedCount = db.cropDao().getHarvestedCount(userId)
                val favCrop = db.cropDao().getFavoriteCrop(userId) ?: "None"

                withContext(Dispatchers.Main) {
                    tvActivePlants.text = "Active Plants: $activeCount"
                    tvHarvested.text = "Total Harvests: $harvestedCount"
                    tvFavorite.text = "Frequently Crop: $favCrop"
                }
            }
        }
    }
}