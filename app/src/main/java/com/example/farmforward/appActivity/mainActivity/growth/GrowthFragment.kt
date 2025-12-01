package com.example.farmforward.appActivity.mainActivity.growth

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
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
import javax.inject.Inject

@AndroidEntryPoint
class GrowthFragment : Fragment(), GrowthView {

    @Inject lateinit var controller: GrowthController
    private lateinit var cropViewModel: CropViewModel

    private lateinit var gridContainer: GridLayout
    private lateinit var searchInput: EditText
    private lateinit var searchButton: ImageButton
    private lateinit var appLogo: ImageView
    private lateinit var menuButton: ImageButton
    private var isSearchOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_growth, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        gridContainer = view.findViewById(R.id.cropListContainer)
        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.search_button)
        appLogo = view.findViewById(R.id.app_logo)
        menuButton = view.findViewById(R.id.menu_button)

        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner)

        menuButton.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }

        setupSearchLogic()
        return view
    }

    override fun displayCrops(crops: List<CropEntity>) {
        gridContainer.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        // Grid calculation logic
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density
        val parentPaddingPx = (32 * density).toInt()
        val itemMarginPx = (6 * density).toInt()
        val totalMarginSpace = itemMarginPx * 4
        val availableWidth = screenWidth - parentPaddingPx - totalMarginSpace
        val itemSize = availableWidth / 2

        for (crop in crops) {
            val itemView = inflater.inflate(R.layout.item_crop_grid, gridContainer, false)

            val tvCropName = itemView.findViewById<TextView>(R.id.tvCropName)
            val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
            val imgCrop = itemView.findViewById<ImageView>(R.id.imgCrop)

            val params = GridLayout.LayoutParams()
            params.width = itemSize
            params.height = itemSize
            params.setMargins(itemMarginPx, itemMarginPx, itemMarginPx, itemMarginPx)
            itemView.layoutParams = params

            imgCrop.setImageResource(CropImageHelper.getImageRes(crop.cropName))
            imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.moss_green))
            tvCropName.text = crop.cropName

            val today = System.currentTimeMillis()
            val planted = crop.date
            val minHarvest = crop.mindate ?: 0L
            val status = when {
                today < planted -> "Scheduled"
                today >= minHarvest -> "Ready!"
                else -> "Growing"
            }
            tvStatus.text = status

            itemView.setOnClickListener {
                controller.onCropClicked(crop)
            }
            gridContainer.addView(itemView)
        }
    }

    override fun navigateToCropDetails(crop: CropEntity) {
        cropViewModel.viewCropDetails(crop)
        cropViewModel.lastSourceId = R.id.nav_growth
        (activity as? MainActivity)?.switchFragment(MainActivity.NAV_GROWTH_CROP_DETAILS)
    }

    private fun setupSearchLogic() {
        searchButton.setOnClickListener {
            if (isSearchOpen) closeSearch() else openSearch()
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

    override fun getFragmentContext(): Context = requireContext()
    override fun getScope(): LifecycleCoroutineScope = lifecycleScope
    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
}