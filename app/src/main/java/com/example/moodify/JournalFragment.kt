package com.example.moodify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodify.databinding.FragmentJournalBinding // Import ViewBinding class
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class JournalFragment : Fragment() {

    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!! // Non-null accessor

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var journalAdapter: JournalAdapter
    private val entriesList = mutableListOf<MoodEntry>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase instances
        firestore = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // Setup RecyclerView
        setupRecyclerView()

        // Load data
        loadJournalEntries()
    }

    // Inside JournalFragment.kt - setupRecyclerView method

    private fun setupRecyclerView() {
        // Instantiate the CORRECT adapter
        journalAdapter = JournalAdapter(requireContext(), entriesList)

        binding.recyclerViewJournal.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = journalAdapter // Set the adapter
        }
    }


    private fun loadJournalEntries() {
        binding.progressBarJournal.visibility = View.VISIBLE
        binding.recyclerViewJournal.visibility = View.GONE
        binding.textViewJournalEmpty.visibility = View.GONE

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("JournalFragment", "User not logged in.")
            Toast.makeText(context, "Error: User not found.", Toast.LENGTH_SHORT).show()
            binding.progressBarJournal.visibility = View.GONE
            binding.textViewJournalEmpty.text = "Please log in to view journal."
            binding.textViewJournalEmpty.visibility = View.VISIBLE
            return
        }

        firestore.collection("moodEntries") // Your collection name
            .whereEqualTo("userId", userId) // Filter by current user
            // .whereNotEqualTo("note", null) // Optional: Only show entries WITH notes
            // .whereGreaterThan("note", "") // Optional: Alternative filter for non-empty notes
            .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest first
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBarJournal.visibility = View.GONE
                entriesList.clear() // Clear previous data
                if (documents.isEmpty) {
                    binding.textViewJournalEmpty.text = "No journal entries found."
                    binding.textViewJournalEmpty.visibility = View.VISIBLE
                    binding.recyclerViewJournal.visibility = View.GONE
                } else {
                    for (document in documents) {
                        try {
                            val entry = document.toObject(MoodEntry::class.java)
                            // Ensure entry has a note if you filtered for it, or handle nulls
                            entriesList.add(entry)
                        } catch (e: Exception) {
                            Log.e("JournalFragment", "Error converting document: ${document.id}", e)
                        }
                    }
                    journalAdapter.updateData(entriesList) // Update adapter with new data

                    // Show/Hide based on final list content after potential filtering
                    if (entriesList.isEmpty()) {
                        binding.textViewJournalEmpty.text = "No entries with notes found." // Adjust message if filtering
                        binding.textViewJournalEmpty.visibility = View.VISIBLE
                        binding.recyclerViewJournal.visibility = View.GONE
                    } else {
                        binding.textViewJournalEmpty.visibility = View.GONE
                        binding.recyclerViewJournal.visibility = View.VISIBLE
                    }
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBarJournal.visibility = View.GONE
                binding.textViewJournalEmpty.text = "Error loading entries."
                binding.textViewJournalEmpty.visibility = View.VISIBLE
                binding.recyclerViewJournal.visibility = View.GONE
                Log.e("JournalFragment", "Error getting documents: ", exception)
                Toast.makeText(context, "Failed to load journal entries.", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear binding reference to prevent memory leaks
    }


}