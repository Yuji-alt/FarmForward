package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.farmforward.R
import com.example.farmforward.activityController.SignUpController

class SignUpActivity : AppCompatActivity() {

    private lateinit var controller: SignUpController
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signupview)
        controller = SignUpController(this)

        usernameInput = findViewById(R.id.user_name_input)
        passwordInput = findViewById(R.id.signUp_password)
        confirmPasswordInput = findViewById(R.id.inpt_confirm_password)
        signUpButton = findViewById(R.id.signUp)

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirm = confirmPasswordInput.text.toString().trim()

            controller.signUp(username, password, confirm) { success, message ->
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                if (success) finish()
            }
            signUpButton.isEnabled = false
            controller.signUp(username, password, confirm) { success, message ->
                runOnUiThread {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    signUpButton.isEnabled = true
                    if (success) {
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }
            }

        }

    }
}
