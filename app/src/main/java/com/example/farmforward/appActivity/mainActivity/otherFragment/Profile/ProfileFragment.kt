package com.example.farmforward.appActivity.mainActivity.otherFragment

import android.app.Dialog
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    // ---------------------------------------------------------------------------------------------
    // Dependencies & Variables
    // ---------------------------------------------------------------------------------------------
    @Inject lateinit var session: SessionManager
    @Inject lateinit var db: AppDatabase
    private lateinit var cropViewModel: CropViewModel

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvActivePlants: TextView
    private lateinit var tvHarvested: TextView
    private lateinit var tvFavorite: TextView
    private lateinit var imgProfile: ImageView
    private lateinit var btnEditPic: ImageView
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            saveProfilePictureLocally(uri)
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Methods
    // ---------------------------------------------------------------------------------------------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        cropViewModel = ViewModelProvider(requireActivity())[CropViewModel::class.java]

        initViews(view)
        setupListeners(view)
        loadProfileData()
        loadLocalProfilePicture()

        return view
    }

    // ---------------------------------------------------------------------------------------------
    // Setup & Initialization
    // ---------------------------------------------------------------------------------------------
    private fun initViews(view: View) {
        tvUsername = view.findViewById(R.id.tvUsername)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvActivePlants = view.findViewById(R.id.tvActiveplants)
        tvHarvested = view.findViewById(R.id.yvHarvestedCrops)
        tvFavorite = view.findViewById(R.id.tvFavorite)
        imgProfile = view.findViewById(R.id.imgProfile)
        btnEditPic = view.findViewById(R.id.btnEditProfilePic)
    }

    private fun setupListeners(view: View) {
        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profile)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onBackClicked(cropViewModel)
        }
        btnEditPic.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        imgProfile.setOnClickListener {
            showFullScreenImage()
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Image Viewer Logic
    // ---------------------------------------------------------------------------------------------
    private fun showFullScreenImage() {
        val userId = session.getUserId() ?: return
        val fileName = "profile_pic_$userId.jpg"
        val file = File(requireContext().filesDir, fileName)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val imageView = ImageView(requireContext())
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        val color = ContextCompat.getColor(requireContext(), R.color.tan)
        imageView.setBackgroundColor(color)

        if (file.exists()) {
            imageView.setImageURI(Uri.fromFile(file))
            imageView.imageTintList = null
        } else {
            imageView.setImageResource(R.drawable.ic_profile)
            val colorKombu = ContextCompat.getColor(requireContext(), R.color.kombuGreen)
            imageView.imageTintList = ColorStateList.valueOf(colorKombu)
        }
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    // ---------------------------------------------------------------------------------------------
    // Data Loading Logic (Stats)
    // ---------------------------------------------------------------------------------------------
    private fun loadProfileData() {
        tvUsername.text = session.getUserName()
        tvEmail.text = session.getUserEmail()

        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            if (userId != -1) {
                val activeCount = db.cropDao().getActivePlantCount(userId)
                val harvestedCount = db.cropDao().getHarvestedCount(userId)
                val favCrop = db.cropDao().getFavoriteCrop(userId) ?: "None"

                withContext(Dispatchers.Main) {
                    tvActivePlants.text = "Active Plants: $activeCount"
                    tvHarvested.text = "Total Harvests: $harvestedCount"
                    tvFavorite.text = "Frequent Crop: $favCrop"
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Local Image Logic (Save & Load)
    // ---------------------------------------------------------------------------------------------
    private fun saveProfilePictureLocally(sourceUri: Uri) {
        val userId = session.getUserId() ?: return
        val context = requireContext()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "profile_pic_$userId.jpg"
                val file = File(context.filesDir, fileName)

                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val outputStream = FileOutputStream(file)

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    imgProfile.setImageURI(null)
                    imgProfile.setImageURI(Uri.fromFile(file))
                    imgProfile.imageTintList = null
                    (activity as? MainActivity)?.showToast("Profile picture updated locally.", isError = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    (activity as? MainActivity)?.showToast("Failed to save image.", isError = true)
                }
            }
        }
    }

    private fun loadLocalProfilePicture() {
        val userId = session.getUserId() ?: return
        val fileName = "profile_pic_$userId.jpg"
        val file = File(requireContext().filesDir, fileName)

        if (file.exists()) {
            imgProfile.setImageURI(Uri.fromFile(file))
            imgProfile.imageTintList = null
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile)
            val color = ContextCompat.getColor(requireContext(), R.color.kombuGreen)
            imgProfile.imageTintList = ColorStateList.valueOf(color)
        }
    }
}