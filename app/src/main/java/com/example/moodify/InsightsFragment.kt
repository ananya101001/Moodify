package com.example.moodify

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodify.databinding.FragmentInsightsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import java.util.Calendar

class InsightsFragment : Fragment() {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var constellationView: SentimentConstellationView

    private lateinit var transitionRecyclerView: RecyclerView
    private lateinit var transitionAdapter: TransitionMatrixAdapter
    private val transitionMoodOrder = listOf("Happy", "Good", "Okay", "Sad", "Anxious")

    private val sentimentValueMap = mapOf(
        "Happy" to 1.0f,
        "Good" to 0.7f,
        "Okay" to 0.0f,
        "Neutral" to 0.0f,
        "Anxious" to -0.8f,
        "Sad" to -1.0f,
        "Terrible" to -1.0f,
        "Positive" to 1.0f,
        "Negative" to -1.0f
    )
    private val defaultSentimentScore = 0.0f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        Log.d("InsightsFragment", "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InsightsFragment", "onViewCreated")

        firestore = FirebaseFirestore.getInstance()
        auth = Firebase.auth
        constellationView = binding.sentimentConstellationView

        transitionRecyclerView = binding.recyclerViewTransitions
        setupTransitionRecyclerView()

        binding.chipGroupInsightsPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull()
            when (checkedId) {
                R.id.chipInsights7Days -> loadSentimentData(7)
                R.id.chipInsights30Days -> loadSentimentData(30)
                else -> {
                    Log.w("InsightsFragment", "Chip group cleared, defaulting to 7 days.")
                    loadSentimentData(7)
                    binding.chipInsights7Days.isChecked = true
                }
            }
        }

        when {
            binding.chipInsights7Days.isChecked -> loadSentimentData(7)
            binding.chipInsights30Days.isChecked -> loadSentimentData(30)
            else -> {
                Log.w("InsightsFragment", "No chip selected, defaulting to 7 days.")
                binding.chipInsights7Days.isChecked = true
                loadSentimentData(7)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("InsightsFragment", "onDestroyView")
        _binding = null
    }

    private fun setupTransitionRecyclerView() {
        val numColumns = transitionMoodOrder.size
        transitionAdapter = TransitionMatrixAdapter(requireContext(), transitionMoodOrder, emptyMap(), 0)
        transitionRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), numColumns)
            adapter = transitionAdapter
        }
    }

    private fun loadSentimentData(daysAgo: Int) {
        Log.d("InsightsFragment", "Loading sentiment data for last $daysAgo days")
        setLoadingState(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("InsightsFragment", "User not logged in.")
            setEmptyState("Please log in to view insights.")
            setLoadingState(false)
            return
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTimestamp = Timestamp(calendar.time)

        firestore.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                Log.d("InsightsFragment", "Received ${documents.size()} documents.")

                val entries = documents.mapNotNull { doc ->
                    try {
                        doc.toObject(MoodEntry::class.java)?.apply {
                            id = doc.id
                            if (this.mood != null && this.timestamp != null) this else null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                if (entries.size < 2) {
                    setEmptyState("Not enough mood entries for insights.")
                } else {
                    processAndDisplayConstellation(entries)
                    processAndDisplayTransitions(entries)
                    binding.textViewInsightsEmpty.visibility = View.GONE
                }

                setLoadingState(false)
            }
            .addOnFailureListener { exception ->
                Log.e("InsightsFragment", "Firestore query failed", exception)
                setEmptyState("Failed to load insights.")
                setLoadingState(false)
            }
    }

    private fun processAndDisplayConstellation(entries: List<MoodEntry>) {
        val nodes = entries.mapNotNull { entry ->
            val score = sentimentValueMap[entry.sentiment ?: entry.mood] ?: defaultSentimentScore
            if (entry.timestamp != null && entry.id != null) {
                SentimentNode(
                    entryId = entry.id!!,
                    timestamp = entry.timestamp,
                    sentimentScore = score,
                    originalMoodLabel = entry.mood,
                    noteSnippet = entry.note?.take(50)
                )
            } else null
        }

        if (nodes.isNotEmpty()) {
            binding.cardViewConstellation.visibility = View.VISIBLE
            constellationView.setData(nodes)
            binding.progressBarConstellation?.visibility = View.GONE
            binding.textViewConstellationEmpty?.visibility = View.GONE
        } else {
            binding.cardViewConstellation.visibility = View.GONE
        }
    }

    private fun processAndDisplayTransitions(entries: List<MoodEntry>) {
        Log.d("InsightsFragment", "Processing ${entries.size} entries for transitions.")
        val transitionCounts = mutableMapOf<Pair<String, String>, Int>()
        var maxCount = 0

        for (i in 0 until entries.size - 1) {
            val fromMood = entries[i].mood
            val toMood = entries[i + 1].mood

            if (fromMood != null && toMood != null &&
                transitionMoodOrder.contains(fromMood) &&
                transitionMoodOrder.contains(toMood)) {
                val pair = Pair(fromMood, toMood)
                val currentCount = transitionCounts.getOrDefault(pair, 0) + 1
                transitionCounts[pair] = currentCount
                if (currentCount > maxCount) maxCount = currentCount
            }
        }

        Log.d("InsightsFragment", "Transition Counts: $transitionCounts | Max: $maxCount")

        if (transitionCounts.isNotEmpty()) {
            transitionAdapter.updateData(transitionMoodOrder, transitionCounts, maxCount)
            binding.cardViewTransitionMatrix.visibility = View.VISIBLE
        } else {
            binding.cardViewTransitionMatrix.visibility = View.GONE
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarInsights.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.cardViewConstellation.visibility = View.INVISIBLE
            binding.cardViewTransitionMatrix.visibility = View.INVISIBLE
            binding.textViewInsightsEmpty.visibility = View.GONE
        }
    }

    private fun setEmptyState(message: String) {
        binding.cardViewConstellation.visibility = View.GONE
        binding.cardViewTransitionMatrix.visibility = View.GONE
        binding.progressBarInsights.visibility = View.GONE
        binding.textViewInsightsEmpty.visibility = View.VISIBLE
        binding.textViewInsightsEmpty.text = message
        constellationView.setData(emptyList())
        transitionAdapter.updateData(transitionMoodOrder, emptyMap(), 0)
    }

    private fun getMoodColorResource(mood: String?): Int {
        return when (mood) {
            "Happy" -> R.color.mood_color_happy
            "Good" -> R.color.mood_color_good
            "Okay" -> R.color.mood_color_okay
            "Sad" -> R.color.mood_color_sad
            "Anxious" -> R.color.mood_color_anxious
            else -> R.color.mood_color_okay
        }
    }
}
