package com.example.moodify

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList // Import for tinting
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat // Import for background tinting on Views
import androidx.recyclerview.widget.RecyclerView
// Import the CORRECT ViewBinding class for the new layout
import com.example.moodify.databinding.ListItemJournalCardTimelineBinding // *** UPDATE BINDING CLASS NAME ***
import java.text.SimpleDateFormat
import java.util.*

class JournalAdapter(
    private val context: Context,
    private var entries: List<MoodEntry>
) : RecyclerView.Adapter<JournalAdapter.JournalCardTimelineViewHolder>() { // *** RENAME ViewHolder ***

    // Keep the detailed date formatter for this view
    private val detailedDateFormatter = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalCardTimelineViewHolder {
        // Inflate the NEW layout using its binding class
        val binding = ListItemJournalCardTimelineBinding.inflate( // *** Use new Binding Class ***
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JournalCardTimelineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalCardTimelineViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry, position, itemCount) // Pass position info

        // Handle click listener (Option 1 implementation)
        holder.itemView.setOnClickListener {
            if (entry.id == null) {
                Log.e("JournalAdapter", "Clicked card timeline item has null ID: $entry")
                Toast.makeText(context, "Error opening entry.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d("JournalAdapter", "Card Timeline Item clicked, starting DetailActivity for ID: ${entry.id}")
            val intent = Intent(context, EntryDetailActivity::class.java).apply {
                putExtra(EntryDetailActivity.EXTRA_ENTRY_ID, entry.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = entries.size

    fun updateData(newEntries: List<MoodEntry>) {
        entries = newEntries
        notifyDataSetChanged() // Consider DiffUtil
    }

    // *** RENAME ViewHolder and update logic ***
    inner class JournalCardTimelineViewHolder(
        private val binding: ListItemJournalCardTimelineBinding // *** Use new Binding Class ***
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: MoodEntry, position: Int, totalItems: Int) {
            // --- Set Mood Text ---
            binding.textViewCardMood.text = entry.mood ?: "Unknown"

            // --- Set Date and Time ---
            val dateString = entry.timestamp?.toDate()?.let { detailedDateFormatter.format(it) } ?: "Date unknown"
            binding.textViewCardTimestamp.text = dateString

            // --- Set Note Snippet ---
            if (!entry.note.isNullOrBlank()) {
                binding.textViewCardNoteSnippet.text = entry.note
                binding.textViewCardNoteSnippet.visibility = View.VISIBLE
            } else {
                binding.textViewCardNoteSnippet.visibility = View.GONE
            }

            // --- Set Mood Indicator Node Color ---
            val moodColorResId = getMoodColorResource(entry.mood)
            val moodColor = ContextCompat.getColor(context, moodColorResId)
            // Use backgroundTintList for compatibility
            binding.viewMoodIndicatorNode.backgroundTintList = ColorStateList.valueOf(moodColor)

            // --- Handle Timeline Line Visibility ---
            binding.viewTimelineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            binding.viewTimelineBottom.visibility = if (position == totalItems - 1) View.INVISIBLE else View.VISIBLE

            // --- Optional: Color timeline lines to match node ---
            binding.viewTimelineTop.setBackgroundColor(moodColor)
            binding.viewTimelineBottom.setBackgroundColor(moodColor)
            // Or keep them as default divider color:
            // binding.viewTimelineTop.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            // binding.viewTimelineBottom.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        // Helper function to get color resource based on mood string (ensure colors defined)
        private fun getMoodColorResource(mood: String?): Int {
            return when (mood) {
                "Happy" -> R.color.mood_color_happy
                "Good" -> R.color.mood_color_good
                "Okay" -> R.color.mood_color_okay
                "Sad" -> R.color.mood_color_terrible
                "Anxious" -> R.color.mood_color_anxious
                "Terrible" -> R.color.mood_color_terrible
                "Angry" -> R.color.mood_color_terrible
                else -> R.color.mood_color_okay
            }
        }
    }
}