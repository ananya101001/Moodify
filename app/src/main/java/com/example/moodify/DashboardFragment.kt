package com.example.moodify

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.moodify.databinding.FragmentDashboardBinding // Import ViewBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.ScatterChart // Import ScatterChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry // Used by ScatterChart
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.data.ScatterDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter // Base class for custom formatters
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var barChart: BarChart
    private lateinit var scatterChart: ScatterChart // Add variable for ScatterChart

    // Instantiate your analyzer
    private val trendAnalyzer = MoodTrendAnalyzer()

    // Define the order for the BAR chart and color mapping
    // Make sure these match the moods saved from LogFragment
    private val orderedMoodsForBarChart = listOf("Happy", "Good", "Okay", "Sad", "Anxious")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = Firebase.auth

        // Initialize BOTH charts using ViewBinding
        barChart = binding.barChartMoods
        scatterChart = binding.scatterChartMoods // Initialize ScatterChart

        // Setup chart appearances
        setupBarChart()
        setupScatterChart()

        // Setup ChipGroup listener
        binding.chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chip7Days -> loadData(7)
                    R.id.chip30Days -> loadData(30)
                }
            } else {
                // Handle case where selection is cleared if needed, maybe default to 7
                if (!binding.chip7Days.isChecked && !binding.chip30Days.isChecked) {
                    binding.chip7Days.isChecked = true // Re-check default if none selected
                    loadData(7)
                }
            }
        }

        // Load initial data based on the initially checked chip
        if (binding.chip7Days.isChecked) loadData(7) else loadData(30)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clean up binding
    }

    // --- Setup functions for charts ---
    private fun setupBarChart() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false)
        barChart.setDrawBarShadow(false)
        barChart.setScaleEnabled(false)

        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.isGranularityEnabled = true
        // ValueFormatter set in update function

        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisLeft.granularity = 1f
        barChart.axisLeft.isGranularityEnabled = true
    }

    private fun setupScatterChart() {
        scatterChart.description.isEnabled = false
        scatterChart.legend.isEnabled = false
        scatterChart.setDrawGridBackground(false)
        scatterChart.setPinchZoom(true)
        scatterChart.setScaleEnabled(true)
        scatterChart.isDragEnabled = true

        // X-Axis (Time) - Use DateAxisValueFormatter
        val xAxis = scatterChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.valueFormatter = DateAxisValueFormatter() // Assign date formatter
        xAxis.labelRotationAngle = -45f
        xAxis.granularity = 1f // Avoid duplicate labels if points are very close
        xAxis.isGranularityEnabled = true

        // Y-Axis (Mood Value) - Use MoodValueFormatter
        val yAxisLeft = scatterChart.axisLeft
        // Use analyzer map to find min/max dynamically, add padding
        val moodValues = trendAnalyzer.moodToYValueMap.values
        yAxisLeft.axisMinimum = (moodValues.minOrNull() ?: 0f) - 0.5f
        yAxisLeft.axisMaximum = (moodValues.maxOrNull() ?: 5f) + 0.5f
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.granularity = 1f
        yAxisLeft.isGranularityEnabled = true
        yAxisLeft.valueFormatter = MoodValueFormatter(trendAnalyzer.moodToYValueMap) // Pass map

        scatterChart.axisRight.isEnabled = false
    }

    // --- Data Loading ---
    private fun loadData(daysAgo: Int) {
        Log.d("DashboardFragment", "Loading data for last $daysAgo days")
        setLoadingState(true)
        binding.textViewTrendComment.visibility = View.GONE // Hide comment while loading

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w("DashboardFragment", "User not logged in.")
            setEmptyState("Please log in to view dashboard.")
            setLoadingState(false)
            return
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0) // Start of day
        val startTimestamp = Timestamp(calendar.time)

        firestore.collection("moodEntries")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
            .orderBy("timestamp", Query.Direction.ASCENDING) // Ascending needed for trend/scatter X-axis
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DashboardFragment", "Firestore query successful, found ${documents.size()} documents.")
                val entries = documents.mapNotNull { it.toObject(MoodEntry::class.java) }

                if (entries.isEmpty()) { // Handle case where query returns empty
                    setEmptyState("No mood entries found for the last $daysAgo days.")
                } else {
                    // Process data for both charts and comment
                    processAndDisplayData(entries)
                }
                setLoadingState(false) // Turn off progress bar AFTER processing
            }
            .addOnFailureListener { exception ->
                Log.e("DashboardFragment", "Error getting documents for dashboard", exception)
                setEmptyState("Error loading mood data.")
                setLoadingState(false)
            }
    }

    // --- Central Data Processing ---
    private fun processAndDisplayData(entries: List<MoodEntry>) {
        Log.d("DashboardFragment", "Processing ${entries.size} entries for charts and comment...")
        var chartsHaveData = false // Track if any chart gets populated

        // --- 1. Process for Bar Chart (Counts) ---
        val moodCounts = mutableMapOf<String, Int>()
        orderedMoodsForBarChart.forEach { moodCounts[it] = 0 } // Use specific list for bar chart order
        entries.forEach { entry ->
            entry.mood?.let { mood ->
                if (orderedMoodsForBarChart.contains(mood)) {
                    moodCounts[mood] = (moodCounts[mood] ?: 0) + 1
                }
            }
        }
        Log.d("DashboardFragment", "Processed Bar Chart Mood Counts: $moodCounts")

        val barEntries = ArrayList<BarEntry>()
        val barLabels = ArrayList<String>()
        var barIndex = 0f
        orderedMoodsForBarChart.forEach { mood ->
            val count = moodCounts[mood]?.toFloat() ?: 0f
            barEntries.add(BarEntry(barIndex, count))
            barLabels.add(mood)
            barIndex++
        }

        if (barEntries.any { it.y > 0f }) {
            updateBarChartData(barEntries, barLabels)
            binding.cardViewBar.visibility = View.VISIBLE
            chartsHaveData = true
        } else {
            binding.cardViewBar.visibility = View.GONE // Hide card if no data
        }

        // --- 2. Process for Scatter Chart and Trend Analysis ---
        if (entries.size >= 2) { // Need at least 2 points for scatter/trend
            Log.d("DashboardFragment", "Analyzing trend for ${entries.size} entries...")
            val (trendSlope, trendComment) = trendAnalyzer.analyzeTrendAndComment(entries)
            val (xPositionsNormalized, earliestTimestamp, durationSeconds) = trendAnalyzer.getXDataFor(entries)

            val scatterEntries = ArrayList<Entry>()
            val moodScatterColors = ArrayList<Int>() // Colors for scatter points
            val context = requireContext()

            if (xPositionsNormalized.size == entries.size) {
                entries.forEachIndexed { index, entry ->
                    val yValue = trendAnalyzer.getYPositionFor(entry)
                    // Use relative timestamp for X-axis plotting with DateAxisValueFormatter
                    val timeSinceStartSeconds = (entry.timestamp!!.seconds - (earliestTimestamp?.seconds ?: 0L)).toFloat()
                    scatterEntries.add(Entry(timeSinceStartSeconds, yValue))
                    moodScatterColors.add(ContextCompat.getColor(context, getMoodColor(entry.mood)))
                }
            } else {
                Log.e("DashboardFragment", "Mismatch between X positions and entries count for scatter!")
            }

            if (scatterEntries.isNotEmpty()) {
                // Update scatter chart X-axis reference time *before* setting data
                (scatterChart.xAxis.valueFormatter as? DateAxisValueFormatter)?.setReferenceTimestamp(earliestTimestamp?.seconds ?: 0L)
                updateScatterChartData(scatterEntries, moodScatterColors)
                binding.cardViewScatter.visibility = View.VISIBLE
                binding.textViewTrendComment.text = "Trend Analysis: $trendComment" // Set comment
                binding.textViewTrendComment.visibility = View.VISIBLE
                chartsHaveData = true
            } else {
                binding.cardViewScatter.visibility = View.GONE // Hide card if no data
                binding.textViewTrendComment.visibility = View.GONE
            }
        } else {
            // Not enough data for scatter/trend
            Log.d("DashboardFragment", "Not enough entries (${entries.size}) for trend analysis.")
            binding.cardViewScatter.visibility = View.GONE
            binding.textViewTrendComment.visibility = View.GONE
        }

        // --- 3. Update Overall Empty State ---
        if (chartsHaveData) {
            binding.textViewDashboardEmpty.visibility = View.GONE
        } else {
            // If neither chart had data, show the empty message
            setEmptyState("Not enough data to display charts for this period.")
        }
    }


    // --- Update functions for each chart ---
    private fun updateBarChartData(barEntries: List<BarEntry>, labels: List<String>) {
        Log.d("DashboardFragment", "Updating Bar Chart")
        val dataSet = BarDataSet(barEntries, "Mood Distribution")

        val moodColors = ArrayList<Int>()
        val context = requireContext()
        orderedMoodsForBarChart.forEach { mood -> // Use bar chart's ordered list
            moodColors.add(ContextCompat.getColor(context, getMoodColor(mood)))
        }
        dataSet.colors = moodColors

        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f
        dataSet.setDrawValues(true)

        val barData = BarData(dataSet)
        barData.barWidth = 0.7f
        barData.setValueFormatter(object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value > 0) value.toInt().toString() else ""
            }
        })

        barChart.data = barData
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.labelCount = labels.size
        barChart.animateY(800)
        barChart.invalidate()
    }

    private fun updateScatterChartData(scatterEntries: List<Entry>, pointColors: List<Int>) {
        Log.d("DashboardFragment", "Updating Scatter Chart")
        val dataSet = ScatterDataSet(scatterEntries, "Mood Timeline")
        dataSet.setScatterShape(ScatterChart.ScatterShape.CIRCLE)
        dataSet.scatterShapeSize = 12f // Slightly larger points
        dataSet.colors = pointColors // Use specific color for each point based on mood
        dataSet.setDrawValues(false) // Don't show Y value on points

        val scatterData = ScatterData(dataSet)
        scatterChart.data = scatterData
        scatterChart.invalidate() // Refresh chart
    }


    // --- Helper Functions ---

    // Helper to get color resource ID based on mood string
    private fun getMoodColor(mood: String?): Int {
        return when (mood) {
            "Happy" -> R.color.chart_happy
            "Good" -> R.color.chart_content // Map Good to content color
            "Okay" -> R.color.chart_neutral
            "Sad" -> R.color.chart_sad
            "Anxious" -> R.color.chart_anxious
            "Angry" -> R.color.chart_angry // Add if used
            // Add if used
            else -> R.color.chart_default
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.progressBarDashboard.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            binding.cardViewScatter.visibility = View.INVISIBLE
            binding.cardViewBar.visibility = View.INVISIBLE
            binding.textViewDashboardEmpty.visibility = View.GONE
            binding.textViewTrendComment.visibility = View.GONE
        }
    }

    private fun setEmptyState(message: String) {
        binding.cardViewScatter.visibility = View.GONE // Hide both cards
        binding.cardViewBar.visibility = View.GONE
        binding.progressBarDashboard.visibility = View.GONE
        binding.textViewDashboardEmpty.visibility = View.VISIBLE
        binding.textViewDashboardEmpty.text = message
        binding.textViewTrendComment.visibility = View.GONE
    }

    // --- Custom ValueFormatters ---

    // For Scatter Chart X-Axis (Dates)
    inner class DateAxisValueFormatter(private var referenceTimestampSeconds: Long = 0L) : ValueFormatter() {
        // Use a shorter format for axis labels if space is tight
        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        fun setReferenceTimestamp(ref: Long) {
            referenceTimestampSeconds = ref
        }

        override fun getFormattedValue(value: Float): String {
            // Convert the float value (seconds relative to reference) back to a date
            val timestampSeconds = referenceTimestampSeconds + value.toLong()
            val date = Date(TimeUnit.SECONDS.toMillis(timestampSeconds))
            return dateFormat.format(date)
        }
    }

    // For Scatter Chart Y-Axis (Mood Names)
    inner class MoodValueFormatter(moodMap: Map<String, Float>) : ValueFormatter() {
        // Create reverse map for efficient lookup
        private val valueToMoodMap = moodMap.entries.associateBy({ it.value }) { it.key }

        override fun getFormattedValue(value: Float): String {
            // Find the mood name corresponding to the rounded Y value
            // Use roundToInt() to snap to the nearest defined mood level
            return valueToMoodMap[value.roundToInt().toFloat()] ?: ""
        }
    }

} // End of DashboardFragment class