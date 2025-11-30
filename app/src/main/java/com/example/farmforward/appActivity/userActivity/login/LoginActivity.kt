package com.example.farmforward.appActivity.userActivity.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.LocationManager
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
import android.view.LayoutInflater
import androidx.activity.result.IntentSenderRequest
import androidx.core.view.ViewCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

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

    private var locationContinuation: kotlinx.coroutines.CancellableContinuation<Boolean>? = null

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
        val rootLayout = findViewById<ScrollView>(R.id.rootLayout)

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomPadding = max(imeInsets.bottom, systemBars.bottom)
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bottomPadding)

            val isKeyboardVisible = imeInsets.bottom > 0
            if (isKeyboardVisible) {
                val focusedView = currentFocus
                if (focusedView != null) {
                    view.post {
                        val rect = Rect()
                        focusedView.getDrawingRect(rect)
                        rootLayout.offsetDescendantRectToMyCoords(focusedView, rect)
                        val scrollY = rect.top - 100
                        rootLayout.smoothScrollTo(0, scrollY)
                    }
                }
            }

            insets
        }

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

                    val isLocationOn = withContext(Dispatchers.Main) { ensureLocationOn() }

                    if (isLocationOn) {
                        updateUi(75, "Fetching Local Weather...")
                        try {
                            fetchWeatherSync()
                        } catch (e: Exception) {
                            Log.e("LoginActivity", "Weather sync failed: ${e.message}")
                        }
                    } else {
                        Log.d("LoginActivity", "Skipping weather: User refused to turn on GPS")
                        updateUi(75, "Skipping weather (GPS Off)...")
                        delay(500)
                    }
                } else {
                    // Logic for Offline or No Permission
                    val reason = if (isOfflineMode) "Offline" else "No Permission"
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
                Log.e("LoginActivity", "Weather skipped: Permission denied.")
                return
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            val cancellationTokenSource = CancellationTokenSource()

            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).await()

            if (location == null) {
                Log.e("LoginActivity", "Weather skipped: Could not determine current location.")
                return
            }

            var locName = "Unknown"
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
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
                    Log.d("LoginActivity", "Weather and Location updated successfully!")
                }
            } else {
                Log.e("LoginActivity", "API Error: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("LoginActivity", "Fetch failed: ${e.message}")
        }
    }
    override fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot_password, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val etEmail = dialogView.findViewById<EditText>(R.id.etResetEmail)
        val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            controller.sendPasswordReset(email)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("LoginActivity", "User enabled location")
            locationContinuation?.resume(true)
        } else {
            Log.d("LoginActivity", "User rejected location")
            locationContinuation?.resume(false)
        }
        locationContinuation = null
    }

    private suspend fun ensureLocationOn(): Boolean = suspendCancellableCoroutine { cont ->
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            if (cont.isActive) cont.resume(true)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    locationContinuation = cont

                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)

                } catch (sendEx: Exception) {
                    if (cont.isActive) cont.resume(false)
                }
            } else {
                if (cont.isActive) cont.resume(false)
            }
        }
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