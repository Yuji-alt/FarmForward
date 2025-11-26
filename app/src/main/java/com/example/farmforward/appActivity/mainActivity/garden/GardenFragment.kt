package com.example.farmforward.appActivity.mainActivity.garden

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.roomDatabase.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class GardenFragment : Fragment(), GardenView {

    @Inject lateinit var controller: GardenController

    private lateinit var cropContainer: LinearLayout
    private lateinit var btnAdd: ImageButton
    private lateinit var cropViewModel: CropViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_garden, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        cropContainer = view.findViewById(R.id.cropListContainer)
        btnAdd = view.findViewById(R.id.btnBack)
        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner)
        btnAdd.setOnClickListener {
            controller.onAddClicked()
        }

        return view
    }
    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
    override fun getFragmentContext(): Context = requireContext()
    override fun getScope(): LifecycleCoroutineScope = lifecycleScope
    @SuppressLint("SetTextI18n")
    override fun displayCrops(crops: List<CropEntity>) {
        cropContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.garden_frame, cropContainer, false)
            val tvCropName = itemView.findViewById<TextView>(R.id.tvCropName)
            val tvCropDetails = itemView.findViewById<TextView>(R.id.tvCropDetails)
            tvCropName.text = crop.cropName
            val details = buildString {
                appendLine("Expected yield: ${String.format("%.2f", crop.expectedYield)} kg")
            }
            tvCropDetails.text = details.trim()

            itemView.setOnClickListener {
                controller.onCropClicked(crop)
            }
            cropContainer.addView(itemView)
        }
    }
    override fun selectCropForGrowth(crop: CropEntity) {
        cropViewModel.viewCropDetails(crop)
    }
    override fun navigateToGrowth() {
        (requireActivity() as? MainActivity)
            ?.controller
            ?.onNavigationItemClicked(R.id.nav_growth)
    }
    override fun navigateToCalc() {
        (requireActivity() as? MainActivity)
            ?.controller
            ?.onNavigationItemClicked(R.id.nav_calc)
    }
}