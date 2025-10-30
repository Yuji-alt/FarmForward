package com.example.farmforward.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.fragmentController.HomeController
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var controller: HomeController
    private lateinit var searchInput: EditText
    private lateinit var itemContainer: LinearLayout
    private var userId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        searchInput = view.findViewById(R.id.search_input)
        itemContainer = view.findViewById(R.id.itemContainer)
        controller = HomeController(requireContext(), itemContainer)

        val session = SessionManager(requireContext())
        userId = session.getUserId()
        refreshData()

        return view
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }


    fun refreshData() {
        val id = userId ?: return
        val db = AppDatabase.getDatabase(requireContext())

         lifecycleScope.launch(Dispatchers.IO) {
            val crops = db.cropDao().getCropsForUserList(id)
            withContext(Dispatchers.Main) {
                controller.displayCrops(crops)
            }
        }
    }
}
