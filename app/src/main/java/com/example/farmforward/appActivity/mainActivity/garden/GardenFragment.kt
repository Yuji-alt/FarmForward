package com.example.farmforward.appActivity.mainActivity.garden

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.database.CropEntity
import com.example.farmforward.database.viewModel.CropViewModel
import com.example.farmforward.utils.CropImageHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class GardenFragment : Fragment(), GardenView {

    @Inject lateinit var controller: GardenController

    private lateinit var activeCropContainer: LinearLayout
    private lateinit var harvestedCropContainer: LinearLayout
    private lateinit var tvActiveNumber: TextView
    private lateinit var tvHarvestNumber: TextView
    private lateinit var btnAdd: ImageButton
    private lateinit var cropViewModel: CropViewModel

    private lateinit var searchInput: EditText
    private lateinit var menuButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var appLogo: ImageView
    private var isSearchOpen = false

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_garden, container, false)

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        activeCropContainer = view.findViewById(R.id.cropListContainer)
        harvestedCropContainer = view.findViewById(R.id.harvestedCropListContainer)
        tvActiveNumber = view.findViewById(R.id.tvActiveNumber)
        tvHarvestNumber = view.findViewById(R.id.tvHarvestNumber)
        btnAdd = view.findViewById(R.id.btnAdd)

        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.search_button)
        menuButton = view.findViewById(R.id.menu_button)
        appLogo = view.findViewById(R.id.app_logo)

        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner)

        btnAdd.setOnClickListener {
            controller.onAddClicked()
        }
        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        setupSearchLogic()

        return view
    }

    private fun setupSearchLogic() {
        searchButton.setOnClickListener {
            if (isSearchOpen) {
                closeSearch()
            } else {
                openSearch()
            }
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controller.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun openSearch() {
        isSearchOpen = true
        appLogo.visibility = View.GONE
        searchInput.visibility = View.VISIBLE
        searchButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        searchInput.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        isSearchOpen = false
        searchInput.setText("")
        appLogo.visibility = View.VISIBLE
        searchInput.visibility = View.GONE
        searchButton.setImageResource(android.R.drawable.ic_menu_search)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
    }

    override fun updateDashboardCounts(activeCount: Int, readyToHarvestCount: Int) {
        tvActiveNumber.text = activeCount.toString()
        tvHarvestNumber.text = readyToHarvestCount.toString()
    }

    @SuppressLint("SetTextI18n")
    override fun displayActiveCrops(crops: List<CropEntity>) {
        activeCropContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_grid, activeCropContainer, false)
            bindCropItem(itemView, crop, isHarvested = false)
            activeCropContainer.addView(itemView)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun displayHarvestedCrops(crops: List<CropEntity>) {
        harvestedCropContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.garden_frame, harvestedCropContainer, false)
            bindCropItem(itemView, crop, isHarvested = true)
            harvestedCropContainer.addView(itemView)
        }
    }

    private fun bindCropItem(itemView: View, crop: CropEntity, isHarvested: Boolean) {
        val tvCropName = itemView.findViewById<TextView>(R.id.tvCropName)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        val imgCrop = itemView.findViewById<ImageView>(R.id.imgCrop)

        imgCrop.setImageResource(CropImageHelper.getImageRes(crop.cropName))
        imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.moss_green))
        tvCropName.text = crop.cropName

        if (isHarvested) {
            val dateStr = crop.harvestedDate?.let { dateFormat.format(it) } ?: "N/A"
            tvStatus.text = "Harvested: $dateStr"
        } else {
            val today = System.currentTimeMillis()
            val planted = crop.date
            val minHarvest = crop.mindate ?: 0L

            val status = when {
                today < planted -> "Scheduled"
                today >= minHarvest -> "Ready to Harvest"
                else -> "Growing"
            }
            tvStatus.text = "Status: $status"
        }

        itemView.setOnClickListener {
            controller.onCropClicked(crop)
        }
    }

    override fun selectCropForGrowth(crop: CropEntity) {
        cropViewModel.viewCropDetails(crop)
        cropViewModel.lastSourceId = R.id.nav_garden
    }

    override fun navigateToGrowth() {
        (requireActivity() as? MainActivity)
            ?.controller
            ?.onNavigationItemClicked(MainActivity.NAV_CROP_DETAILS)
    }

    override fun navigateToCalc() {
        (requireActivity() as? MainActivity)
            ?.controller
            ?.onNavigationItemClicked(R.id.nav_calc)
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun getFragmentContext(): Context = requireContext()
    override fun getScope(): LifecycleCoroutineScope = lifecycleScope
}