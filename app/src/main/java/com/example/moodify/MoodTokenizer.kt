package com.example.moodify

import android.content.Context
import android.util.Log
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.InputStreamReader

class MoodTokenizer(context: Context) {

    private var wordIndex: Map<String, Int> = emptyMap()
    private var initialized = false

    companion object {
        private const val TOKENIZER_FILENAME = "tokenizer.json"
        private const val MAX_SEQUENCE_LENGTH = 15
        private const val OOV_TOKEN_INDEX = 1
    }

    init {
        try {
            val inputStream = context.assets.open(TOKENIZER_FILENAME)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.use { it.readText() }

            // Parse the JSON
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            // Get the word_index directly from the JSON object
            wordIndex = if (jsonObject.has("word_index")) {
                // Case 1: word_index is a direct object in the JSON
                jsonObject.getAsJsonObject("word_index").entrySet().associate {
                    it.key to it.value.asInt
                }
            } else {
                // Case 2: word_index might be inside config object
                val config = jsonObject.getAsJsonObject("config")
                config.getAsJsonObject("word_index").entrySet().associate {
                    it.key to it.value.asInt
                }
            }

            initialized = true
            Log.d("MoodTokenizer", "Tokenizer loaded. Words: ${wordIndex.size}")

        } catch (e: Exception) {
            Log.e("MoodTokenizer", "Failed to load tokenizer", e)
        }
    }

    fun isReady(): Boolean = initialized

    fun tokenizeAndPad(text: String): IntArray {
        if (!initialized) return IntArray(MAX_SEQUENCE_LENGTH) { 0 }

        val cleaned = text.lowercase()
            .replace(Regex("[^a-z ]"), " ")  // Keep only letters and spaces
            .replace(Regex("\\s+"), " ")     // Replace multiple spaces with single space
            .trim()

        val words = if (cleaned.isEmpty()) emptyList() else cleaned.split(" ")

        val tokens = words.map { wordIndex[it] ?: OOV_TOKEN_INDEX }
        val truncated = tokens.take(MAX_SEQUENCE_LENGTH)

        val padded = IntArray(MAX_SEQUENCE_LENGTH) { 0 }
        truncated.forEachIndexed { i, v -> padded[i] = v }

        return padded
    }
}