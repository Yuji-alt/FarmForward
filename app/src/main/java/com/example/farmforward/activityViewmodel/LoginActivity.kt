package com.example.farmforward.activityViewmodel
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.farmforward.R
import com.example.farmforward.activityController.LoginController
import com.example.farmforward.roomDatabase.AppDatabase
import com.example.farmforward.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

            controller.login(username, password) { success ->
                runOnUiThread {
                    if (success) {
                        // ✅ Save user session
                        CoroutineScope(Dispatchers.IO).launch {
                            val db = AppDatabase.getDatabase(this@LoginActivity)
                            val user = db.userDao().getUserByUsername(username)
                            user?.let {
                                val session = SessionManager(this@LoginActivity)
                                session.saveSession(it.id, it.username)
                            }
                        }

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Invalid credentials!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        findViewById<TextView>(R.id.signUp_here).setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }
}
