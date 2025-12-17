package com.example.farmforward.appActivity.mainActivity.garden

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class GardenFragment : Fragment(), GardenView {

    @Inject lateinit var controller: GardenController
    private lateinit var cropViewModel: CropViewModel

    // UI Elements
    private lateinit var listContainer: LinearLayout
    private lateinit var tabLayout: TabLayout
    private lateinit var btnAdd: ImageButton
    private lateinit var searchInput: EditText
    private lateinit var menuButton: ImageButton
    private lateinit var searchButton: ImageButton
    private lateinit var appLogo: TextView

    // Data Holders
    private var growingList: List<CropEntity> = emptyList()
    private var readyList: List<CropEntity> = emptyList()
    private var harvestedList: List<CropEntity> = emptyList()

    private var isSearchOpen = false
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_garden, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        initViews(view)
        setupTabs()

        controller.bindView(this)
        controller.setupObserver(viewLifecycleOwner)

        setupListeners()
        setupSearchLogic()

        return view
    }
    //developer team is improving we use a function to separate the initiation of view
    private fun initViews(view: View) {
        listContainer = view.findViewById(R.id.listContainer)
        tabLayout = view.findViewById(R.id.tabLayout)
        btnAdd = view.findViewById(R.id.btnAdd)
        searchInput = view.findViewById(R.id.search_input)
        searchButton = view.findViewById(R.id.search_button)
        menuButton = view.findViewById(R.id.menu_button)
        appLogo = view.findViewById(R.id.app_logo_text)
    }
    //same with but this for Listener

    private fun setupListeners() {
        btnAdd.setOnClickListener { controller.onAddClicked() }
        menuButton.setOnClickListener { (activity as? MainActivity)?.openDrawer() }
    }
    //used in tabs for the seperation of active ,ready and harvested
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Active"))
        tabLayout.addTab(tabLayout.newTab().setText("Ready"))
        tabLayout.addTab(tabLayout.newTab().setText("Harvested"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                refreshList()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    //data Update from Controller
    override fun updateCropLists(
        growing: List<CropEntity>,
        ready: List<CropEntity>,
        harvested: List<CropEntity>
    ) {
        this.growingList = growing
        this.readyList = ready
        this.harvestedList = harvested
        refreshList()
    }
    //when the tan is empty this will be display
    private fun refreshList() {
        listContainer.removeAllViews()
        val currentTab = tabLayout.selectedTabPosition

        when (currentTab) {
            0 -> displayList(growingList, "Your garden is empty.\nTap '+' to plant your first crop! 🌱", isHarvested = false)
            1 -> displayList(readyList, "No crops are ready to harvest yet.\nKeep taking care of them! 🌾", isHarvested = false)
            2 -> displayList(harvestedList, "No harvests yet.\nHarvested crops will appear here.", isHarvested = true)
        }
    }
    //used to display the respective crops in tabs
    private fun displayList(crops: List<CropEntity>, emptyMessage: String, isHarvested: Boolean) {
        if (crops.isEmpty()) {
            showEmptyState(listContainer, emptyMessage)
            return
        }
        listContainer.gravity = Gravity.TOP or Gravity.START
        val inflater = LayoutInflater.from(requireContext())
        for (crop in crops) {
            val layoutId = if (isHarvested) R.layout.garden_frame else R.layout.active_status
            val itemView = inflater.inflate(layoutId, listContainer, false)

            bindCropItem(itemView, crop, isHarvested)
            listContainer.addView(itemView)
        }
    }
    // this is the setter of rectangle thins or the crops holder in the tabs
    private fun bindCropItem(itemView: View, crop: CropEntity, isHarvested: Boolean) {
        val tvCropName = itemView.findViewById<TextView>(R.id.tvCropName)
        val tvStatus = itemView.findViewById<TextView>(R.id.tvStatus)
        val imgCrop = itemView.findViewById<ImageView>(R.id.imgCrop)
        val tvDays = itemView.findViewById<TextView>(R.id.tvDays)

        imgCrop.setImageResource(CropImageHelper.getImageRes(crop.cropName))
        imgCrop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.moss_green))
        tvCropName.text = crop.cropName

        val today = System.currentTimeMillis()

        if (isHarvested) {
            val dateStr = crop.harvestedDate?.let { dateFormat.format(it) } ?: "N/A"
            tvStatus.text = "Harvested: $dateStr"
        } else if (crop.date > today) {
            // Scheduled to Plant
            val diff = crop.date - today
            val daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
            tvStatus.text = "Scheduled to Plant"
            tvDays.text = "$daysLeft DAYS"
        } else {
            // Already planted, active crops
            val minHarvest = crop.mindate ?: 0L

            if (today >= minHarvest) {
                tvStatus.text = "HARVEST NOW"
                tvDays.text = "READY"
                tvDays.setTextColor(ContextCompat.getColor(requireContext(), R.color.kombuGreen))
            } else {
                val diff = minHarvest - today
                val daysLeft = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)
                tvStatus.text = "Growing"
                tvDays.text = "$daysLeft DAYS"
            }
        }

        itemView.setOnClickListener {
            controller.onCropClicked(crop)
        }
    }

    // when the list is empty this will be display
    private fun showEmptyState(container: LinearLayout, message: String) {
        container.gravity = Gravity.CENTER
        val emptyTextView = TextView(requireContext())
        emptyTextView.text = message
        emptyTextView.textSize = 16f
        emptyTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.kombuGreen))
        emptyTextView.alpha = 0.6f
        emptyTextView.setTypeface(null, Typeface.ITALIC)
        emptyTextView.gravity = Gravity.CENTER

        val heightPx = (200 * resources.displayMetrics.density).toInt()
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            heightPx
        )
        params.gravity = Gravity.CENTER
        emptyTextView.layoutParams = params
        container.addView(emptyTextView)
    }
    //general search logic
    private fun setupSearchLogic() {
        searchButton.setOnClickListener { if (isSearchOpen) closeSearch() else openSearch() }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controller.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    //search -----------------------------
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
    //-----------------------
    //when user click any botton this will handle where they will go
    override fun selectCropForGrowth(crop: CropEntity) {
        cropViewModel.viewCropDetails(crop)
        cropViewModel.lastSourceId = R.id.nav_garden
    }
    override fun navigateToGrowth() {
        (requireActivity() as? MainActivity)?.controller?.onNavigationItemClicked(MainActivity.NAV_CROP_DETAILS)
    }
    override fun navigateToCalc() {
        (requireActivity() as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_calc)
    }
    //prevent data leaks
    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }
    //helper -------------------
    override fun getFragmentContext(): Context = requireContext()
    override fun getScope(): LifecycleCoroutineScope = lifecycleScope
    //------------------
}