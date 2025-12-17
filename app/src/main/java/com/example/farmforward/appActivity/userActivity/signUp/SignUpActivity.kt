package com.example.farmforward.appActivity.userActivity.signUp

import android.content.Context
import android.content.Intent
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
import com.example.farmforward.R
import com.example.farmforward.appActivity.userActivity.login.LoginActivity
import com.example.farmforward.utils.loadingUtils.LoadingDialogFragment
import com.example.farmforward.utils.otherUtils.hideSystemUI
import com.example.farmforward.utils.otherUtils.handleKeyboardVisibility
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity(), SignUpView {

    // ---------------------------------------------------------------------------------------------
    // Dependencies & Variables
    // ---------------------------------------------------------------------------------------------
    @Inject lateinit var controller: SignUpController

    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button
    private lateinit var backButton: ImageButton
    private var loadingDialog: LoadingDialogFragment? = null

    // ---------------------------------------------------------------------------------------------
    // Lifecycle Methods
    // ---------------------------------------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.hideSystemUI()
        setContentView(R.layout.signupview)
        controller.bindView(this)
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
        val rootLayout = findViewById<ScrollView>(R.id.rootLayout)
        rootLayout.handleKeyboardVisibility()

        initViews()
        setupListeners()
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------------------
    // Initialization & Setup
    // ---------------------------------------------------------------------------------------------
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

    // ---------------------------------------------------------------------------------------------
    // Loading State Handling
    // ---------------------------------------------------------------------------------------------
    override fun showLoading() {
        loadingDialog = LoadingDialogFragment()
        loadingDialog?.isCancelable = false
        loadingDialog?.show(supportFragmentManager, "SignUpLoading")
    }

    override fun updateLoading(progress: Int, message: String) {
        runOnUiThread {
            if (loadingDialog?.isAdded == true) {
                loadingDialog?.updateProgress(progress, message)
            }
        }
    }

    override fun hideLoading() {
        runOnUiThread {
            if (loadingDialog?.isAdded == true) {
                loadingDialog?.dismiss()
            }
            loadingDialog = null
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Navigation & UI Feedback
    // ---------------------------------------------------------------------------------------------
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
}