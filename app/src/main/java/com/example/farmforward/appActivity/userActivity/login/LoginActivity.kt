package com.example.farmforward.appActivity.userActivity.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userActivity.session.SessionManager
import com.example.farmforward.appActivity.userActivity.signUp.SignUpActivity
import com.example.farmforward.database.firebaseDatabase.FirebaseSyncManager
import com.example.farmforward.utils.loadingUtils.LoadingDialogFragment
import com.example.farmforward.utils.otherUtils.NetworkUtils
import com.example.farmforward.utils.otherUtils.RetrofitClient
import com.example.farmforward.utils.weatherUtils.WeatherRepository
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.appcompat.app.AlertDialog
import android.text.InputType

@AndroidEntryPoint
class LoginActivity : AppCompatActivity(), LoginView {

    @Inject lateinit var controller: LoginController
    @Inject lateinit var weatherRepository: WeatherRepository
    @Inject lateinit var session: SessionManager
    @Inject lateinit var syncManager: FirebaseSyncManager

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var logInButton: Button
    private lateinit var offlineSwitch: Switch
    private lateinit var signUpButton: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("LoginActivity", "Location Permission granted")
        } else {
            Log.d("LoginActivity", "Location Permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (session.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.loginview)

        controller.bindView(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        usernameInput = findViewById(R.id.userName)
        passwordInput = findViewById(R.id.userPassword)
        logInButton = findViewById(R.id.logIn)
        offlineSwitch = findViewById(R.id.offline)
        signUpButton = findViewById(R.id.signUp_here)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        controller.onViewCreated()

        logInButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val isOffline = offlineSwitch.isChecked

            controller.onLoginClicked(username, password, isOffline)
        }

        signUpButton.setOnClickListener {
            controller.onSignUpClicked()
        }
        val forgotPasswordBtn = findViewById<TextView>(R.id.forgotPassword)
        forgotPasswordBtn.setOnClickListener {
            controller.onForgotPasswordClicked()
        }
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun showToast(message: String, isError: Boolean) {
        val context = this
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view

        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        params.topMargin = 60.dpToPx(context).toInt()
        params.leftMargin = 20.dpToPx(context).toInt()
        params.rightMargin = 20.dpToPx(context).toInt()
        snackbarView.layoutParams = params

        snackbarView.backgroundTintList = null
        val borderDrawable = GradientDrawable()
        borderDrawable.shape = GradientDrawable.RECTANGLE
        borderDrawable.cornerRadius = 12f.dpToPx(context)

        val bgColor = ContextCompat.getColor(context, R.color.tan)
        val strokeColor = ContextCompat.getColor(context, R.color.kombuGreen)

        borderDrawable.setColor(bgColor)
        borderDrawable.setStroke(4, strokeColor)
        snackbarView.background = borderDrawable

        snackbar.setTextColor(strokeColor)
        snackbar.setActionTextColor(strokeColor)
        snackbar.setAction("OK") { snackbar.dismiss() }

        snackbar.show()
    }

    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    override fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun startLoginSync() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(supportFragmentManager, "LoginLoadingDialog")

        val isOfflineMode = offlineSwitch.isChecked

        lifecycleScope.launch(Dispatchers.IO) {

            fun updateUi(progress: Int, msg: String) {
                launch(Dispatchers.Main) {
                    if (loadingDialog.isAdded) {
                        loadingDialog.updateProgress(progress, msg)
                    }
                }
            }
            var hasNetworkError = false
            try {
                updateUi(10, "Verifying credentials...")
                delay(300)

                if (!isOfflineMode) {
                    if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
                        throw IOException("No network connection available.")
                    }
                    updateUi(30, "Syncing User Profile...")
                    syncManager.syncUsers()

                    updateUi(50, "Downloading Crop Data...")
                    syncManager.syncCrops()

                } else {
                    updateUi(40, "Offline Mode: Using local data...")
                    delay(500)
                }
                updateUi(70, "Checking Local Weather...")
                val hasPermission = ContextCompat.checkSelfPermission(
                    this@LoginActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!isOfflineMode && hasPermission) {
                    try {
                        fetchWeatherSync()
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Weather sync failed: ${e.message}")
                    }
                } else {
                    val reason = if (isOfflineMode) "Offline Mode" else "No Permission"
                    Log.d("LoginActivity", "Skipping weather fetch: $reason")
                    updateUi(75, "Skipping weather ($reason)...")
                    delay(300)
                }

                updateUi(90, "Preparing Dashboard...")
                delay(300)
                updateUi(100, "Welcome Back!")
                delay(200)
            } catch (e: IOException) {
                Log.e("LoginActivity", "Sync/Download failed: ${e.message}")
                hasNetworkError = true
                updateUi(100, "Error: Network required for Online setup.")
                delay(1500)

            } catch (e: Exception) {
                Log.e("LoginActivity", "Sync failed: ${e.message}")
                hasNetworkError = true
                updateUi(100, "Error: Data synchronization failed.")
                delay(1500)
            }
            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                if (!hasNetworkError) {
                    navigateToMain()
                } else {
                    showToast("Sync failed. Switch to Offline Mode if network is poor.", isError = true)
                }
            }
        }
    }
    private suspend fun fetchWeatherSync() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val location = fusedLocationClient.lastLocation.await()
            if (location == null) {
                Log.e("LoginActivity", "Weather skipped: Location not yet available (GPS cold start).")
                return
            }
            var locName = "Unknown"
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                locName = if (!addresses.isNullOrEmpty()) addresses[0].locality ?: "Unknown" else "Unknown"
            } catch (e: Exception) {
                Log.e("LoginActivity", "Geocoder failed: ${e.message}")
            }
            val apiKey = BuildConfig.WEATHER_API_KEY
            val response = RetrofitClient.instance.getForecastByCoordinates(location.latitude, location.longitude, apiKey).awaitResponse()
            if (response.isSuccessful) {
                val forecasts = response.body()?.list
                if (forecasts != null) {
                    val upcomingForecasts = forecasts.take(8)
                    val format = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
                    val dateText = format.format(Date())
                    weatherRepository.saveWeatherData(upcomingForecasts, locName, dateText)
                    Log.d("LoginActivity", "Weather saved locally for offline use!")
                }
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Fetch failed: ${e.message}")
        }
    }
    override fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        builder.setMessage("Enter your email address to receive a reset link.")

        val input = EditText(this)
        input.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        input.hint = "email@example.com"
        builder.setView(input)
        builder.setPositiveButton("Send") { dialog, _ ->
            val email = input.text.toString().trim()
            controller.sendPasswordReset(email)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    override fun navigateToSignUp() {
        startActivity(Intent(this, SignUpActivity::class.java))
    }
    override fun setOfflineSwitch(isChecked: Boolean) {
        offlineSwitch.isChecked = isChecked
    }
    override fun enableSignUpButton(isEnabled: Boolean) {
        signUpButton.isEnabled = isEnabled
        signUpButton.alpha = if (isEnabled) 1.0f else 0.5f
    }
}