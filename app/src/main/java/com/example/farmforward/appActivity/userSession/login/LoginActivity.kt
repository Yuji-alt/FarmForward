package com.example.farmforward.appActivity.userSession.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.BuildConfig
import com.example.farmforward.R
import com.example.farmforward.appActivity.mainActivity.MainActivity
import com.example.farmforward.appActivity.userSession.session.SessionManager
import com.example.farmforward.appActivity.userSession.signUp.SignUpActivity
import com.example.farmforward.firebase.FirebaseSyncManager
import com.example.farmforward.utils.LoadingDialogFragment
import com.example.farmforward.utils.RetrofitClient
import com.example.farmforward.utils.WeatherRepository
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.awaitResponse
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

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
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun showToast(message: String, duration: Int) {
        Toast.makeText(this, message, duration).show()
    }

    override fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- UPDATED LOADING LOGIC WITH WEATHER ---
    override fun startLoginSync() {
        val loadingDialog = LoadingDialogFragment()
        loadingDialog.show(supportFragmentManager, "LoginLoadingDialog")

        lifecycleScope.launch(Dispatchers.IO) {

            fun updateUi(progress: Int, msg: String) {
                launch(Dispatchers.Main) {
                    loadingDialog.updateProgress(progress, msg)
                }
            }

            updateUi(10, "Verifying credentials...")
            delay(300)

            updateUi(30, "Syncing User Profile...")
            syncManager.syncUsers()

            updateUi(50, "Updating Global Crop Data...")
            syncManager.syncCrops()

            // --- ADDED WEATHER FETCH HERE ---
            updateUi(70, "Getting Local Weather...")
            val hasPermission = ContextCompat.checkSelfPermission(
                this@LoginActivity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                try {
                    fetchWeatherSync()
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Weather sync failed: ${e.message}")
                }
            } else {
                Log.d("LoginActivity", "Skipping weather: Permission not granted yet.")
            }
            // --------------------------------

            updateUi(90, "Preparing Dashboard...")
            delay(300)

            updateUi(100, "Welcome Back!")
            delay(200)

            withContext(Dispatchers.Main) {
                loadingDialog.dismiss()
                navigateToMain()
            }
        }
    }

    private suspend fun fetchWeatherSync() {
        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val location = fusedLocationClient.lastLocation.await() ?: return

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