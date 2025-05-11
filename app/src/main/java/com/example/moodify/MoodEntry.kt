package com.example.moodify

import com.google.firebase.Timestamp // Import Firestore Timestamp
import com.google.firebase.firestore.DocumentId // To get the document ID easily

data class MoodEntry(
    @DocumentId var id: String? = null, // Stores the document ID from Firestore
    val userId: String? = null,
    val mood: String? = null,         // e.g., "Happy", "Sad"
    val note: String? = null,         // The journal text
    val timestamp: Timestamp? = null,
    val sentiment: String? = null ,
    val activities: List<String>? = null
) {
    // Add a no-argument constructor for Firestore deserialization
    constructor() : this(null, null, null, null, null, null,null)
}