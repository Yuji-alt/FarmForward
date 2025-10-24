package com.example.farmforward.activityViewmodel

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.farmforward.R
import com.example.farmforward.activityController.LoginController
import com.example.farmforward.session.SessionManager

class LoginActivity : AppCompatActivity() {

    private lateinit var controller: LoginController
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var logInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.loginview)

        controller = LoginController(this)

        usernameInput = findViewById(R.id.userName)
        passwordInput = findViewById(R.id.userPassword)
        logInButton = findViewById(R.id.logIn)

        logInButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            controller.login(username, password) { user ->
                if (user != null) {
                    val session = SessionManager(this)
                    session.saveSession(user.id, user.username)

                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

                    // Small delay prevents DeadObjectException in Toast
                    Handler(mainLooper).postDelayed({
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }, 400)
                } else {
                    Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<TextView>(R.id.signUp_here).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}
