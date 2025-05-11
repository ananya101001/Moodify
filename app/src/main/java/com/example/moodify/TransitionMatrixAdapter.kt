package com.example.moodify

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodify.databinding.ListItemTransitionCellBinding // Import binding
import kotlin.math.log10
import kotlin.math.max
import android.view.View


class TransitionMatrixAdapter(
    private val context: Context,
    private var moodLabels: List<String>, // Order defines rows/columns
    private var transitionCounts: Map<Pair<String, String>, Int>, // Map of (From -> To) -> Count
    private var maxCount: Int // Max count found for scaling color intensity
) : RecyclerView.Adapter<TransitionMatrixAdapter.TransitionViewHolder>() {

    private val numMoods = moodLabels.size
    private val heatmapColor = ContextCompat.getColor(context, R.color.primary_calm_blue) // Base color for heatmap

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransitionViewHolder {
        val binding = ListItemTransitionCellBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransitionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransitionViewHolder, position: Int) {
        // Calculate row (From Mood) and column (To Mood) based on position
        val fromIndex = position / numMoods
        val toIndex = position % numMoods

        val fromMood = moodLabels.getOrNull(fromIndex)
        val toMood = moodLabels.getOrNull(toIndex)

        if (fromMood != null && toMood != null) {
            val count = transitionCounts[Pair(fromMood, toMood)] ?: 0 // Get count, default 0
            holder.bind(count)

            // Set background color intensity based on count (log scale often looks better)
            val intensityFraction = if (maxCount > 0) {
                // Logarithmic scale for better visual difference at lower counts
                (log10(count.toDouble() + 1) / log10(maxCount.toDouble() + 1)).toFloat()
                // Linear scale: count.toFloat() / maxCount.toFloat()
            } else {
                0f // Avoid division by zero if maxCount is 0
            }
            // Alpha ranges from ~30 (for 0 count) to 255 (for max count)
            val alpha = (30 + intensityFraction * (255 - 30)).toInt().coerceIn(0, 255)
            val backgroundColor = Color.argb(alpha, Color.red(heatmapColor), Color.green(heatmapColor), Color.blue(heatmapColor))
            holder.binding.cellContainer.setBackgroundColor(backgroundColor)

        } else {
            // Should not happen if position is calculated correctly
            holder.bind(0) // Show 0 if indices are out of bounds
            holder.binding.cellContainer.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    override fun getItemCount(): Int = numMoods * numMoods // Total cells in the grid

    // Function to update data
    fun updateData(
        newLabels: List<String>,
        newCounts: Map<Pair<String, String>, Int>,
        newMaxCount: Int
    ) {
        moodLabels = newLabels
        transitionCounts = newCounts
        maxCount = max(1, newMaxCount) // Ensure maxCount is at least 1 for scaling
        notifyDataSetChanged()
    }


    inner class TransitionViewHolder(val binding: ListItemTransitionCellBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(count: Int) {
            if (count > 0) {
                binding.textViewTransitionCount.text = count.toString()
                binding.textViewTransitionCount.visibility = View.VISIBLE
            } else {
                binding.textViewTransitionCount.visibility = View.INVISIBLE // Hide text for zero counts
            }
        }
    }
}