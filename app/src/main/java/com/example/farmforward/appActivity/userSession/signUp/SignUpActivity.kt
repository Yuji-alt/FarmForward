package com.example.farmforward.appActivity.userSession.signUp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.farmforward.R
import com.example.farmforward.appActivity.userSession.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity(), SignUpView {

    @Inject lateinit var controller: SignUpController

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signupview)

        // Bind the view
        controller.bindView(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        usernameInput = findViewById(R.id.user_name_input)
        passwordInput = findViewById(R.id.signUp_password)
        confirmPasswordInput = findViewById(R.id.inpt_confirm_password)
        signUpButton = findViewById(R.id.signUp)

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            val confirm = confirmPasswordInput.text.toString()
            controller.onSignUpClicked(username, password, confirm)
        }
    }

    override fun onDestroy() {
        controller.onDestroy()
        super.onDestroy()
    }

    override fun showToast(message: String, duration: Int) {
        Toast.makeText(this, message, duration).show()
    }

    override fun setSignUpButtonEnabled(isEnabled: Boolean) {
        signUpButton.isEnabled = isEnabled
    }

    override fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}