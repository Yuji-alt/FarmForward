package com.example.farmforward.appActivity.mainActivity.otherFragment.Settings

import android.app.AlertDialog
import android.content.Context
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
import android.widget.TextView
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.database.firebaseDatabase.FirebaseUserRepository
import com.example.farmforward.database.roomDatabase.AppDatabase
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Inject lateinit var session: SessionManager
    @Inject lateinit var db: AppDatabase
    @Inject lateinit var firebaseUserRepo: FirebaseUserRepository
    @Inject lateinit var firestore: FirebaseFirestore

    // -------------------------------------------------------------------------
    // Lifecycle Methods
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Setup & Listeners
    // -------------------------------------------------------------------------
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

    // -------------------------------------------------------------------------
    // Dialogs (UI Logic)
    // -------------------------------------------------------------------------
    private fun showEditUsernameDialog() {
        val currentName = session.getUserDetails()["name"] ?: ""

        // Setup Container
        val container = LinearLayout(context)
        container.orientation = LinearLayout.VERTICAL
        val padding = dpToPx(20)
        container.setPadding(padding, padding, padding, padding)

        // Setup Input
        val input = EditText(context).apply {
            hint = "Enter new username"
            setText(currentName)
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        container.addView(input)

        val builder = AlertDialog.Builder(context)
            .setTitle("Edit Username")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUsername(newName)
                } else {
                    showToast("Username cannot be empty", isError = true)
                }
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()

        val color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.kombuGreen)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
    }

    private fun showChangePasswordDialog() {
        // Setup Container
        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL
        val padding = dpToPx(20)
        layout.setPadding(padding, padding, padding, padding)

        // Setup Old Password Input
        val oldPass = EditText(context).apply {
            hint = "Old Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        // Setup New Password Input
        val newPass = EditText(context).apply {
            hint = "New Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setBackgroundResource(R.drawable.dialog_input)
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
        }

        // Add Margins
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 0, dpToPx(16))
        oldPass.layoutParams = params

        layout.addView(oldPass)
        layout.addView(newPass)

        val builder = AlertDialog.Builder(context)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val oldP = oldPass.text.toString()
                val newP = newPass.text.toString()

                if (newP.length >= 6) {
                    verifyAndUpdatePassword(oldP, newP)
                } else {
                    showToast("New password must be at least 6 chars", isError = true)
                }
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()

        val color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.kombuGreen)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
    }

    private fun showDeleteAccountDialog() {
        val builder = AlertDialog.Builder(context)
            .setTitle("Delete Account")
            .setMessage("Are you sure? This will delete your account permanently from the cloud and this device.")
            .setPositiveButton("DELETE") { _, _ ->
                performDeleteAccount()
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()

        val color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.kombuGreen)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
    }

    private fun showClearDataDialog() {
        val builder = AlertDialog.Builder(context)
            .setTitle("Clear All App Data")
            .setMessage("This will delete all local data and log you out. Cloud data remains safe.")
            .setPositiveButton("Delete") { _, _ ->
                performClearData()
            }
            .setNegativeButton("Cancel", null)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog)
        dialog.show()

        val color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.kombuGreen)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color)
    }

    // -------------------------------------------------------------------------
    // Backend / Database Operations
    // -------------------------------------------------------------------------
    private fun updateUsername(newName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = session.getUserId() ?: -1
            val currentUsername = session.getUserName() ?: ""

            if (userId == -1) return@launch
            if (newName.equals(currentUsername, ignoreCase = true)) {
                withContext(Dispatchers.Main) {
                    showToast("That is already your username.", isError = true)
                }
                return@launch
            }
            if (isNetworkAvailable()) {
                try {
                    val snapshot = firestore.collection("users")
                        .whereEqualTo("username", newName)
                        .get()
                        .await()

                    if (!snapshot.isEmpty) {
                        withContext(Dispatchers.Main) {
                            showToast("Username '$newName' is already taken.", isError = true)
                        }
                        return@launch
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        showToast("Error checking availability. Try again.", isError = true)
                    }
                    return@launch
                }
            } else {
                val localCount = db.userDao().checkUserExists(newName)
                if (localCount > 0) {
                    withContext(Dispatchers.Main) {
                        showToast("Username '$newName' exists on this device.", isError = true)
                    }
                    return@launch
                }
            }
            try {
                db.userDao().updateUsername(userId, newName)

                val email = session.getUserDetails()["email"] ?: ""
                session.createLoginSession(userId, newName, email)
                val user = db.userDao().getUserById(userId)
                if (user != null) {
                    firebaseUserRepo.updateUsername(currentUsername, user)
                }
                withContext(Dispatchers.Main) {
                    showToast("Username updated to $newName", isError = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Local update success, but Cloud sync failed.", isError = true)
                }
            }
        }
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
                        showToast("Password changed securely!", isError = false)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showToast("Failed: ${e.message}", isError = true)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    showToast("Incorrect old password", isError = true)
                }
            }
        }
    }

    private fun performDeleteAccount() {
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

    private fun performClearData() {
        lifecycleScope.launch(Dispatchers.IO) {
            db.clearAllTables()
            session.clearSession()
            withContext(Dispatchers.Main) {
                (activity as? MainActivity)?.navigateToLogin()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers (Toast, Network, Dimensions)
    // -------------------------------------------------------------------------
    private fun showToast(message: String, isError: Boolean = false) {
        (activity as? MainActivity)?.showToast(message, isError)
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

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}