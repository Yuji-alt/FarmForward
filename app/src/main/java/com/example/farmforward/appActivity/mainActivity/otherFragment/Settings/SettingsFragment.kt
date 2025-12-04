package com.example.farmforward.appActivity.mainActivity.otherFragment.Settings

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.AppDatabase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    @Inject lateinit var session: SessionManager
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var firebaseUserRepo: FirebaseUserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_back_profile)
        btnBack.setOnClickListener {
            (activity as? MainActivity)?.controller?.onNavigationItemClicked(R.id.nav_home)
        }
        setupClickListeners(view)

        return view
    }

    private fun setupClickListeners(view: View) {
        val mainActivity = activity as? MainActivity ?: return

        view.findViewById<LinearLayout>(R.id.btn_edit_username).setOnClickListener {
            if (isNetworkAvailable()) showEditUsernameDialog() else showNoInternetToast()
        }

        view.findViewById<LinearLayout>(R.id.btn_change_password).setOnClickListener {
            if (isNetworkAvailable()) showChangePasswordDialog() else showNoInternetToast()
        }

        view.findViewById<LinearLayout>(R.id.btn_delete_account).setOnClickListener {
            if (isNetworkAvailable()) showDeleteAccountDialog() else showNoInternetToast()
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
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    private fun showNoInternetToast() {
        Toast.makeText(context, "Internet connection required for this action", Toast.LENGTH_SHORT).show()
    }
    private fun showEditUsernameDialog() {
        val currentName = session.getUserDetails()["name"] ?: ""
        val input = EditText(context)
        input.hint = "Enter new username"
        input.setText(currentName)
        input.setPadding(50, 40, 50, 40)
        AlertDialog.Builder(context)
            .setTitle("Edit Username")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUsername(newName)
                } else {
                    Toast.makeText(context, "Username cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun updateUsername(newName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val currentUsername = session.getUserName() ?: ""

            if (userId != -1 && currentUsername.isNotEmpty()) {
                db.userDao().updateUsername(userId, newName)

                val email = session.getUserDetails()["email"] ?: ""
                session.createLoginSession(userId, newName, email)

                try {
                    val user = db.userDao().getUserById(userId)
                    if (user != null) {
                        firebaseUserRepo.updateUsername(currentUsername, user)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Username updated to $newName", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Local update success, but Cloud sync failed.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showChangePasswordDialog() {
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 40)
        val oldPass = EditText(context)
        oldPass.hint = "Old Password"
        oldPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val newPass = EditText(context)
        newPass.hint = "New Password"
        newPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(oldPass)
        layout.addView(newPass)

        AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val oldP = oldPass.text.toString()
                val newP = newPass.text.toString()
                if (newP.length >= 6) verifyAndUpdatePassword(oldP, newP)
                else Toast.makeText(context, "New password must be at least 6 chars", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun verifyAndUpdatePassword(oldPass: String, newPass: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val user = db.userDao().getUserById(userId)
            val email = session.getUserDetails()["email"] ?: ""
            if (user != null && user.password == oldPass) {
                try {
                    firebaseUserRepo.updatePassword(email, oldPass, newPass)
                    db.userDao().updatePassword(userId, newPass)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Password changed securely!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Incorrect old password", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This will delete your account permanently from the cloud and this device.")
            .setPositiveButton("DELETE") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val userId = session.getUserId() ?: -1
                    val currentUsername = session.getUserName() ?: ""

                    if (userId != -1) {
                        // 1. Delete from Firebase
                        if (currentUsername.isNotEmpty()) {
                            try {
                                firebaseUserRepo.deleteUser(currentUsername)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        // 2. Delete Local Data
                        db.cropDao().deleteAllCropsForUser(userId)
                        val user = db.userDao().getUserById(userId)
                        if (user != null) {
                            db.userDao().delete(user)
                        }
                        session.clearSession()

                        withContext(Dispatchers.Main) {
                            (activity as? MainActivity)?.navigateToLogin()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showClearDataDialog() {
        AlertDialog.Builder(context)
            .setTitle("Clear All App Data")
            .setMessage("This will delete all local data and log you out. Cloud data remains safe.")
            .setPositiveButton("Delete") { _, _ ->
                performClearData()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun performClearData() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.clearAllTables()
            session.clearSession()
            withContext(Dispatchers.Main) {
                (activity as? MainActivity)?.navigateToLogin()
            }
        }
    }
}