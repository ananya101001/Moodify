package com.example.moodify

import android.os.Bundle
import android.util.Log
import android.view.MenuItem // For handling toolbar item clicks (like back)
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.moodify.databinding.ActivityEntryDetailBinding // Import ViewBinding class
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class EntryDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEntryDetailBinding // ViewBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var entryId: String? = null

    // Define a key for the Intent extra
    companion object {
        const val EXTRA_ENTRY_ID = "com.example.moodify.ENTRY_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEntryDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // --- Setup Toolbar ---
        setSupportActionBar(binding.topAppBarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Entry Details" // Set title

        // --- Get Entry ID from Intent ---
        entryId = intent.getStringExtra(EXTRA_ENTRY_ID)

        if (entryId == null) {
            Log.e("EntryDetailActivity", "No entry ID provided in Intent.")
            Toast.makeText(this, "Error: Could not load entry details.", Toast.LENGTH_LONG).show()
            finish() // Close activity if no ID is provided
            return
        }

        // --- Load Data ---
        loadEntryDetails(entryId!!) // Safe non-null call due to check above
    }

    // Handle Toolbar Back Button Click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle arrow click here
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed() // Close this activity
            return true
        }
        return super.onOptionsItemSelected(item)
    }


    // --- Fetch data from Firestore ---
    private fun loadEntryDetails(id: String) {
        Log.d("EntryDetailActivity", "Loading details for entry ID: $id")
        // Optional: Show a loading indicator here
        // binding.progressBarDetail.visibility = View.VISIBLE

        firestore.collection("moodEntries").document(id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                // Optional: Hide loading indicator
                // binding.progressBarDetail.visibility = View.GONE

                if (documentSnapshot.exists()) {
                    Log.d("EntryDetailActivity", "Document found.")
                    try {
                        val entry = documentSnapshot.toObject(MoodEntry::class.java)
                        if (entry != null) {
                            // Check if entry belongs to current user (optional security check)
                            if (entry.userId == auth.currentUser?.uid) {
                                bindDataToViews(entry)
                            } else {
                                Log.w("EntryDetailActivity", "User mismatch: Attempted to load entry not belonging to current user.")
                                showErrorAndFinish("Error: Entry not found or access denied.")
                            }
                        } else {
                            Log.e("EntryDetailActivity", "Failed to convert snapshot to MoodEntry object.")
                            showErrorAndFinish("Error: Could not parse entry data.")
                        }
                    } catch (e: Exception) {
                        Log.e("EntryDetailActivity", "Error parsing document ${documentSnapshot.id}", e)
                        showErrorAndFinish("Error: Could not read entry data.")
                    }
                } else {
                    Log.w("EntryDetailActivity", "Document with ID $id does not exist.")
                    showErrorAndFinish("Error: Entry not found.")
                }
            }
            .addOnFailureListener { e ->
                // Optional: Hide loading indicator
                // binding.progressBarDetail.visibility = View.GONE
                Log.e("EntryDetailActivity", "Error fetching document $id", e)
                showErrorAndFinish("Error: Failed to load entry details.")
            }
    }

    // --- Update UI with fetched data ---
    private fun bindDataToViews(entry: MoodEntry) {
        Log.d("EntryDetailActivity", "Binding data: $entry")

        // Format Timestamp
        val timestampFormat = SimpleDateFormat("MMMM dd, yyyy, hh:mm a", Locale.getDefault())
        val dateString = entry.timestamp?.toDate()?.let { timestampFormat.format(it) } ?: "Date unknown"

        // Set Mood Text (consider adding emoji logic here too)
        binding.textViewDetailMood.text = entry.mood ?: "N/A"

        // Set Timestamp Text
        binding.textViewDetailTimestamp.text = dateString

        // Set Journal Note Text (handle null or empty)
        if (entry.note.isNullOrBlank()) {
            binding.textViewDetailNoteLabel.visibility = View.GONE // Hide label if no note
            binding.textViewDetailNoteContent.visibility = View.GONE // Hide content if no note
        } else {
            binding.textViewDetailNoteLabel.visibility = View.VISIBLE
            binding.textViewDetailNoteContent.visibility = View.VISIBLE
            binding.textViewDetailNoteContent.text = entry.note
        }

        // Set Sentiment Text (handle null or empty - will be populated later)
        if (entry.sentiment.isNullOrBlank()) {
            binding.textViewDetailSentimentLabel.visibility = View.GONE // Hide if no sentiment yet
            binding.textViewDetailSentimentResult.visibility = View.GONE
        } else {
            binding.textViewDetailSentimentLabel.visibility = View.VISIBLE
            binding.textViewDetailSentimentResult.visibility = View.VISIBLE
            binding.textViewDetailSentimentResult.text = entry.sentiment
        }
    }

    // Helper function to show error and close activity
    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}