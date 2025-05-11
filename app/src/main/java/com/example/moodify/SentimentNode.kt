package com.example.moodify

import com.google.firebase.Timestamp

data class SentimentNode(
    // Data from MoodEntry
    val entryId: String,
    val timestamp: Timestamp,
    val sentimentScore: Float, // Numerical score (-1.0 to 1.0 or similar)
    val originalMoodLabel: String?, // Keep original label if needed
    val noteSnippet: String?, // For potential display on tap

    // Simulation Properties
    var x: Float = 0f, // Current X position on canvas
    var y: Float = 0f, // Current Y position on canvas
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var forceX: Float = 0f,
    var forceY: Float = 0f
)