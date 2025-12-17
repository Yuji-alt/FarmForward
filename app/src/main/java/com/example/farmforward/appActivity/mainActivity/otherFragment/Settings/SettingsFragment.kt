package com.example.farmforward.appActivity.mainActivity.otherFragment.Settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.example.farmforward.utils.loadingUtils.LoadingDialogFragment
import com.example.farmforward.utils.otherUtils.HashUtils
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject lateinit var session: SessionManager
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var firebaseUserRepo: FirebaseUserRepository
    @Inject lateinit var firestore: FirebaseFirestore

    @Inject lateinit var syncManager: FirebaseSyncManager

    private var loadingDialog: LoadingDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profile)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_home)
        }

        val switchOffline = view.findViewById<MaterialSwitch>(R.id.switch_keep_offline)
        val userId = session.getUserId() ?: -1
        val prefKey = "keep_data_offline_$userId"
        val prefs = requireContext().getSharedPreferences("FarmForwardConfig", Context.MODE_PRIVATE)
        switchOffline.isChecked = prefs.getBoolean(prefKey, true)

        switchOffline.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit { putBoolean(prefKey, isChecked) }

            if (isChecked) showToast("Data will be kept on device after logout.")
            else showToast("Data will be cleared on logout.")
        }

        setupClickListeners(view)
        return view
    }

    private fun setupClickListeners(view: View) {
        val mainActivity = activity as? MainActivity ?: return

        view.findViewById<LinearLayout>(R.id.btn_edit_username).setOnClickListener {
            if (isNetworkAvailable()) showEditUsernameDialog() else showToast("Internet connection required", isError = true)
        }
        view.findViewById<LinearLayout>(R.id.btn_change_password).setOnClickListener {
            if (isNetworkAvailable()) showChangePasswordDialog() else showToast("Internet connection required", isError = true)
        }
        view.findViewById<LinearLayout>(R.id.btn_delete_account).setOnClickListener {
            if (isNetworkAvailable()) showDeleteAccountDialog() else showToast("Internet connection required", isError = true)
        }
        view.findViewById<LinearLayout>(R.id.btn_location_permission).setOnClickListener {
            checkAndRequestLocation(mainActivity)
        }
        view.findViewById<LinearLayout>(R.id.btn_clear_data).setOnClickListener {
            showClearDataDialog()
        }
        view.findViewById<LinearLayout>(R.id.btn_terms).setOnClickListener {
            mainActivity.controller.onNavigationItemClicked(MainActivity.NAV_TERMS)
        }
        view.findViewById<LinearLayout>(R.id.btn_privacy).setOnClickListener {
            mainActivity.controller.onNavigationItemClicked(MainActivity.NAV_PRIVACY)
        }
        view.findViewById<LinearLayout>(R.id.btn_tutorial).setOnClickListener {
            val intent = Intent(
                requireContext(),
                com.example.farmforward.utils.onBoarding.OnboardingActivity::class.java
            )
            intent.putExtra("FROM_SETTINGS", true)
            startActivity(intent)
        }
    }
    private fun checkAndRequestLocation(activity: MainActivity) {
        if (activity.controller.hasLocationPermission()) {
            showToast("Location access is already enabled!", isError = false)
        } else {
            activity.controller.requestSystemPermission(activity)
        }
    }
    private fun showEditUsernameDialog() {
        val currentName = session.getUserDetails()["name"] ?: ""
        val input = EditText(context).apply {
            hint = "Enter new username"
            setText(currentName)
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
            addView(input)
        }

        val builder = AlertDialog.Builder(context)
            .setTitle("Edit Username")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) updateUsername(newName)
                else showToast("Username cannot be empty", isError = true)
            }
            .setNegativeButton("Cancel", null)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))
        }
        val oldPass = EditText(context).apply {
            hint = "Old Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }
        val newPass = EditText(context).apply {
            hint = "New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dpToPx(16)) }
        oldPass.layoutParams = params
        layout.addView(oldPass)
        layout.addView(newPass)
        val builder = AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val oldP = oldPass.text.toString()
                val newP = newPass.text.toString()
                if (newP.length >= 6) verifyAndUpdatePassword(oldP, newP)
                else showToast("New password must be at least 6 chars", isError = true)
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }
    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This will delete your account permanently.")
            .setPositiveButton("DELETE") { _, _ -> performDeleteAccount() }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }
    private fun showClearDataDialog() {
        val builder =  AlertDialog.Builder(context)
            .setTitle("Clear All App Data")
            .setMessage("This will delete all local data and log you out.")
            .setPositiveButton("Delete") { _, _ -> performClearData() }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()
    }

    private fun showLoading() {
        loadingDialog = LoadingDialogFragment()
        loadingDialog?.isCancelable = false
        loadingDialog?.show(parentFragmentManager, "SettingsLoading")
    }
    private fun updateLoading(progress: Int, message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            if (loadingDialog?.isAdded == true) {
                loadingDialog?.updateProgress(progress, message)
            }
        }
    }
    private fun hideLoading() {
        if (loadingDialog?.isAdded == true) {
            loadingDialog?.dismiss()
        }
        loadingDialog = null
    }

    private fun updateUsername(newName: String) {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            updateLoading(10, "Checking availability...")
            delay(500)

            val userId = session.getUserId() ?: -1
            val currentDocId = session.getUserName()?.lowercase() ?: ""
            val newDocId = newName.lowercase()

            try {
                // Check if new username is different
                if (newDocId != currentDocId) {
                    val userDoc = firestore.collection("users").document(newDocId).get().await()
                    if (userDoc.exists()) {
                        withContext(Dispatchers.Main) {
                            hideLoading()
                            showToast("Username '$newName' is already taken.", isError = true)
                        }
                        return@launch
                    }
                    updateLoading(50, "Migrating profile...")
                    performFullMigration(userId, currentDocId, newName)
                }
                else {
                    updateLoading(50, "Updating display name...")
                    // Update Local
                    db.userDao().updateUsername(userId, newName)
                    val email = session.getUserDetails()["email"] ?: ""
                    session.createLoginSession(userId, newName, email)

                    // Update Cloud (MATCHING SIGNUP LOGIC: Include lastUpdated)
                    val updates = mapOf(
                        "username" to newName,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    firestore.collection("users").document(currentDocId)
                        .update(updates)
                        .await()

                    updateLoading(100, "Done!")
                    delay(300)
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        showToast("Display name updated to $newName", isError = false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Error: ${e.message}", isError = true)
                }
            }
        }
    }

    private suspend fun performFullMigration(userId: Int, currentDocId: String, newName: String) {
        // 1. Update Local Data first (UI responsiveness)
        db.userDao().updateUsername(userId, newName)
        val email = session.getUserDetails()["email"] ?: ""
        session.createLoginSession(userId, newName, email)

        // Mark crops as unsynced locally so they get pushed to the new ID later
        val userCrops = db.cropDao().getCropsForUserList(userId)
        for (crop in userCrops) {
            db.cropDao().updateCrop(crop.copy(isSynced = 0))
        }

        // 2. Perform Cloud Migration via Repository (SAFER)
        try {
            val user = db.userDao().getUserById(userId)
            if (user != null) {
                // This uses the Transaction logic in your FirebaseUserRepository
                firebaseUserRepo.updateUsername(currentDocId, user, newName)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("Cloud migration warning: ${e.message}", isError = true)
            }
        }

        // 3. Sync Crops to new ID
        val explicitNewId = newName.lowercase()
        updateLoading(80, "Moving crops to new ID...")

        delay(1000)
        syncManager.syncCrops(explicitNewId)

        withContext(Dispatchers.Main) {
            hideLoading()
            showToast("Username updated to $newName", isError = false)
        }
    }

    private fun verifyAndUpdatePassword(oldPass: String, newPass: String) {
        if (oldPass == newPass) {
            showToast("New password cannot be the same as old password", isError = true)
            return
        }
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            updateLoading(10, "Verifying credentials...")
            val email = session.getUserDetails()["email"] ?: ""
            val userId = session.getUserId() ?: -1

            if (userId == -1 || email.isEmpty()) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Error: Invalid Session. Relogin required.", isError = true)
                }
                return@launch
            }

            try {
                // 1. Authenticate with Firebase
                val user = FirebaseAuth.getInstance().currentUser
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)
                if (user == null) throw Exception("No user logged in.")
                // Re-auth forces check of old password
                user.reauthenticate(credential).await()
                updateLoading(40, "Credential valid. Updating Cloud...")
                user.updatePassword(newPass).await()
                updateLoading(70, "Hashing & Saving locally...")

                // 3. Update Local Database
                val hashedNewPass = HashUtils.hashPassword(newPass)
                db.userDao().updatePassword(userId, hashedNewPass)
                // 4. Sync
                syncManager.syncUsers()
                updateLoading(100, "Success!")
                delay(300)
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Password updated successfully!", isError = false)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showToast("Failed: ${e.message}", isError = true)
                }
            }
        }
    }

    private fun performDeleteAccount() {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            updateLoading(10, "Preparing deletion...")
            val userId = session.getUserId() ?: -1
            val currentUsername = session.getUserName() ?: ""
            if (userId != -1) {
                try {
                    if (currentUsername.isNotEmpty()) {
                        updateLoading(40, "Deleting cloud data...")
                        firebaseUserRepo.deleteUser(currentUsername)
                    }
                    updateLoading(70, "Wiping local data...")
                    db.cropDao().deleteAllCropsForUser(userId)
                    val user = db.userDao().getUserById(userId)
                    if (user != null) db.userDao().delete(user)
                    session.clearSession()
                    updateLoading(100, "Goodbye.")
                    delay(500)
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        (activity as? MainActivity)?.navigateToLogin()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        hideLoading()
                        showToast("Deletion Failed: ${e.message}", isError = true)
                    }
                }
            }
        }
    }
    private fun performClearData() {
        showLoading()
        lifecycleScope.launch(Dispatchers.IO) {
            updateLoading(50, "Clearing tables...")
            db.clearAllTables()
            session.clearSession()
            updateLoading(100, "Done.")
            delay(300)
            withContext(Dispatchers.Main) {
                hideLoading()
                (activity as? MainActivity)?.navigateToLogin()
            }
        }
    }
    private fun showToast(message: String, isError: Boolean = false) {
        (activity as? MainActivity)?.showToast(message, isError)
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}