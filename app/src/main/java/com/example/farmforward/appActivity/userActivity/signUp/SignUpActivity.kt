package com.example.farmforward.appActivity.userActivity.signUp

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.login.LoginActivity
import com.example.farmforward.utils.otherUtils.handleKeyboardVisibility
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity(), SignUpView {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------
    @Inject lateinit var controller: SignUpController

    // -------------------------------------------------------------------------
    // UI Elements
    // -------------------------------------------------------------------------
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button
    private lateinit var backButton: ImageButton

    // -------------------------------------------------------------------------
    // Lifecycle Methods
    // -------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signupview)

        // 1. Setup Controller
        controller.bindView(this)

        // 2. Setup Window/System UI
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val rootLayout = findViewById<ScrollView>(R.id.rootLayout)
        rootLayout.handleKeyboardVisibility() // Safe Extension Function

        // 3. Initialize Views & Listeners
        initViews()
        setupListeners()
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Setup & Initialization
    // -------------------------------------------------------------------------
    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        usernameInput = findViewById(R.id.user_name_input)
        passwordInput = findViewById(R.id.signUp_password)
        confirmPasswordInput = findViewById(R.id.inpt_confirm_password)
        signUpButton = findViewById(R.id.signUp)
        backButton = findViewById(R.id.back)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            controller.onBackClicked()
        }

        signUpButton.setOnClickListener {
            val email = emailInput.text.toString()
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            val confirm = confirmPasswordInput.text.toString()

            controller.onSignUpClicked(email, username, password, confirm)
        }
    }

    // -------------------------------------------------------------------------
    // View Interface Implementation
    // -------------------------------------------------------------------------
    override fun setSignUpButtonEnabled(isEnabled: Boolean) {
        signUpButton.isEnabled = isEnabled
        signUpButton.alpha = if (isEnabled) 1.0f else 0.5f
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showToast(message: String, isError: Boolean) {
        val context = this
        val rootView = findViewById<View>(android.R.id.content)
        val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams

        // Positioning
        params.gravity = Gravity.TOP
        params.topMargin = 60.dpToPx(context).toInt()
        params.leftMargin = 20.dpToPx(context).toInt()
        params.rightMargin = 20.dpToPx(context).toInt()
        snackbarView.layoutParams = params

        // Styling
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

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
}