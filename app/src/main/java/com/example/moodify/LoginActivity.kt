package com.example.moodify // Replace with your actual package name

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth // Import Firebase Auth KTX extension
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: Button
    private lateinit var textViewGoToSignup: TextView
    private lateinit var auth: FirebaseAuth // Declare Firebase Auth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth // Using the KTX extension

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        textViewGoToSignup = findViewById(R.id.textViewGoToSignup)

        // Set listener for Login button
        buttonLogin.setOnClickListener {
            loginUser()
        }

        // Set listener for "Go to Signup" text
        textViewGoToSignup.setOnClickListener {
            // Intent to navigate to SignupActivity (Create this Activity next)
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            // Optional: finish() // Decide if you want to close Login when going to Signup
        }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        // Basic Input Validation
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email.", Toast.LENGTH_SHORT).show()
            editTextEmail.error = "Email required" // Optional: Set error on field
            editTextEmail.requestFocus() // Optional: Focus the field
            return // Stop the function
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password.", Toast.LENGTH_SHORT).show()
            editTextPassword.error = "Password required" // Optional
            editTextPassword.requestFocus() // Optional
            return // Stop the function
        }

        // --- Firebase Authentication ---
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI or navigate to MainActivity
                    Toast.makeText(baseContext, "Login Successful.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    // Clear back stack: Make sure pressing back doesn't return to Login
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Close LoginActivity
                } else {
                    // If sign in fails, display a message to the user.
                    // Log.w(TAG, "signInWithEmail:failure", task.exception) // Optional: Log detailed error
                    Toast.makeText(baseContext, "Authentication failed. Check credentials.",
                        Toast.LENGTH_LONG).show()
                    // You could provide more specific errors based on task.exception if needed,
                    // but be careful not to reveal too much for security.
                }
            }
    }

    // Optional: Check if user is already logged in when activity starts
    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, navigate to MainActivity directly
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Close LoginActivity so user can't go back to it
        }
        // Else: No user logged in, stay on LoginActivity
    }
}