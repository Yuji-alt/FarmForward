import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.activityViewmodel.MainActivity
import com.example.farmforward.fragment.CalcFragment
import com.example.farmforward.fragment.GrowthFragment
import com.example.farmforward.fragmentController.GardenController
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GardenFragment : Fragment() {

    private lateinit var controller: GardenController
    private lateinit var cropContainer: LinearLayout
    private lateinit var btnAdd: ImageButton
    private var userId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_garden, container, false)

        cropContainer = view.findViewById(R.id.cropListContainer)
        btnAdd = view.findViewById(R.id.btnBack)
        controller = GardenController(requireContext(), cropContainer)

        val session = SessionManager(requireContext())
        userId = session.getUserId()

        parentFragmentManager.setFragmentResultListener("newCropAdded", viewLifecycleOwner) { _, _ ->
            refreshData()
        }

        btnAdd.setOnClickListener {
            val calcFragment = CalcFragment()
            parentFragmentManager.beginTransaction()
                .hide(this@GardenFragment)
                .add(R.id.fragment_container, calcFragment)
                .addToBackStack(null)
                .commit()
            (requireActivity() as? MainActivity)?.controller?.setActiveMenu(R.id.nav_calc)
        }

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
                controller.displayCrops(crops) { crop ->
                    val bundle = Bundle().apply {
                        putString("cropName", crop.cropName)
                        putDouble("area", crop.area)
                        putDouble("expectedYield", crop.expectedYield)
                        putLong("datePlanted", crop.date)
                        putLong("minHarvestDate", crop.mindate ?: 0L)
                        putLong("maxHarvestDate", crop.maxdate ?: 0L)
                    }

                    val growthFragment = GrowthFragment().apply { arguments = bundle }
                    parentFragmentManager.beginTransaction()
                        .hide(this@GardenFragment)
                        .add(R.id.fragment_container, growthFragment)
                        .addToBackStack(null)
                        .commit()
                    (requireActivity() as? MainActivity)?.controller?.setActiveMenu(R.id.nav_growth)
                }
            }
        }
    }
}
