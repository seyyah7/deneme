package com.example.deneme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import android.util.Log // Log eklemek için

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "E-posta ve şifre boş olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseAuth", "Giriş başarılı: ${auth.currentUser?.email}")
                        val intent = Intent(this, ProblemListActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.e("FirebaseAuth", "Giriş başarısız: ${task.exception?.message}")
                        Toast.makeText(this, "Giriş başarısız: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}