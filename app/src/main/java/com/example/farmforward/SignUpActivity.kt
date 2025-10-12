package com.example.farmforward

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.farmforward.database.AppDatabase
import com.example.farmforward.database.User
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signupview )

        db = AppDatabase.getDatabase(this)

        usernameInput = findViewById(R.id.user_name_input)
        passwordInput = findViewById(R.id.signUp_password)
        confirmPasswordInput = findViewById(R.id.inpt_confirm_password)
        signUpButton = findViewById(R.id.signUp)

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            val confirm = confirmPasswordInput.text.toString()

            if (username.isEmpty()) usernameInput.error = "Enter a username"
            if (password.isEmpty()) passwordInput.error = "Enter a password"
            if (confirm.isEmpty()) confirmPasswordInput.error = "Confirm your password"

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirm) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val existingUser = db.userDao().checkUserExists(username)
                if (existingUser == null) {
                    db.userDao().registerUser(User(username = username, password = password))
                    runOnUiThread {
                        Toast.makeText(this@SignUpActivity, "Sign Up successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SignUpActivity, "Username already exists!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}