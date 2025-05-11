package com.example.moodify // Replace with your actual package name

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.auth.ktx.auth    // Import KTX extension
import com.google.firebase.ktx.Firebase

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val splashTimeOut: Long = 2500
    private lateinit var auth: FirebaseAuth // Declare Firebase Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Firebase Auth
        auth = Firebase.auth

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is signed in (non-null)
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in, go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // No user is signed in, go to LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
            }

            // Close this activity
            finish()
        }, splashTimeOut)
    }
}