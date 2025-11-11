package com.example.farmforward.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.CropViewModel
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.fragmentController.GardenController
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GardenFragment : Fragment() {

    private lateinit var controller: GardenController
    private lateinit var cropContainer: LinearLayout
    private lateinit var btnAdd: ImageButton
    private var userId: Int? = null
    private var refreshJob: Job? = null

    private lateinit var cropViewModel: CropViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_garden, container, false)

        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]
        cropContainer = view.findViewById(R.id.cropListContainer)
        btnAdd = view.findViewById(R.id.btnBack)
        controller = GardenController(requireContext(), cropContainer)
        val session = SessionManager(requireContext())
        userId = session.getUserId()
        btnAdd.setOnClickListener {
            (requireActivity() as? MainActivity)?.controller?.switchFragment(R.id.nav_calc)
        }
        refreshData()
        return view
    }


    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        refreshJob?.cancel()
        val id = userId ?: return
        val db = AppDatabase.getDatabase(requireContext())

        refreshJob = lifecycleScope.launch(Dispatchers.IO) {
            val crops = db.cropDao().getCropsForUserList(id)
            withContext(Dispatchers.Main) {

                controller.displayCrops(crops) { crop ->

                    cropViewModel.viewCropDetails(crop)

                    (requireActivity() as? MainActivity)
                        ?.controller
                        ?.switchFragment(R.id.nav_growth)
                }
            }
        }
    }
}