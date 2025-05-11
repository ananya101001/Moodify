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
import com.google.firebase.auth.ktx.auth // Import KTX extension
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonSignup: Button
    private lateinit var textViewGoToLogin: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup) // Make sure this layout file exists!

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Initialize UI elements
        editTextEmail = findViewById(R.id.editTextSignupEmail) // Use IDs from activity_signup.xml
        editTextPassword = findViewById(R.id.editTextSignupPassword)
        editTextConfirmPassword = findViewById(R.id.editTextSignupConfirmPassword)
        buttonSignup = findViewById(R.id.buttonSignup)
        textViewGoToLogin = findViewById(R.id.textViewGoToLogin)

        // Set listener for Signup button
        buttonSignup.setOnClickListener {
            registerUser()
        }

        // Set listener for "Go to Login" text
        textViewGoToLogin.setOnClickListener {
            // Intent to navigate back to LoginActivity
            // Option 1: Simply finish this activity if Login is always in the back stack
            finish()
            // Option 2: Start LoginActivity explicitly (safer if back stack isn't guaranteed)
            // val intent = Intent(this, LoginActivity::class.java)
            // startActivity(intent)
            // finish() // Optional: close Signup when going back to Login
        }
    }

    private fun registerUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        // --- Input Validation ---
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter email.", Toast.LENGTH_SHORT).show()
            editTextEmail.error = "Email required"
            editTextEmail.requestFocus()
            return
        }
        // Optional: Add email format validation if desired

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter password.", Toast.LENGTH_SHORT).show()
            editTextPassword.error = "Password required"
            editTextPassword.requestFocus()
            return
        }

        if (password.length < 6) { // Firebase requires password >= 6 chars
            Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
            editTextPassword.error = "Password too short"
            editTextPassword.requestFocus()
            return
        }


        if (TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "Please confirm password.", Toast.LENGTH_SHORT).show()
            editTextConfirmPassword.error = "Confirmation required"
            editTextConfirmPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            editTextConfirmPassword.error = "Passwords must match"
            editTextConfirmPassword.requestFocus()
            // Clear the confirm password field maybe?
            // editTextConfirmPassword.text = null
            return
        }
        // --- End Validation ---


        // --- Firebase User Creation ---
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success (user is also automatically signed in after creation)
                    // Navigate to MainActivity
                    Toast.makeText(baseContext, "Registration Successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    // Clear back stack
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish() // Close SignupActivity
                } else {
                    // If sign in fails, display a message to the user.
                    // Common errors: email already in use, weak password (if rules enabled)
                    Toast.makeText(baseContext, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show() // Show specific Firebase error message
                }
            }
    }

    // Optional: Prevent access if already logged in
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is already logged in, shouldn't be here
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}