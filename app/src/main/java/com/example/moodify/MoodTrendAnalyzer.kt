package com.example.moodify

import android.util.Log
import com.google.firebase.Timestamp
import kotlin.math.pow


class MoodTrendAnalyzer {

    internal val moodToYValueMap = mapOf(
        "Happy" to 5f,
        "Good" to 4f,
        "Okay" to 3f,
        "Sad" to 2f,
        "Anxious" to 1f,
        "Angry" to 1f,
        "Terrible" to 0f
    )
    private val defaultValue = 3f // Neutral

    fun getYPositionFor(moodEntry: MoodEntry): Float {
        return moodToYValueMap[moodEntry.mood] ?: defaultValue
    }

    fun getXDataFor(moodEntries: List<MoodEntry>): Triple<List<Float>, Timestamp?, Long> {
        if (moodEntries.isEmpty()) return Triple(emptyList(), null, 0L)

        val sortedEntries = moodEntries.sortedBy { it.timestamp }
        val earliestTimestamp = sortedEntries.first().timestamp ?: return Triple(emptyList(), null, 0L)
        val latestTimestamp = sortedEntries.last().timestamp ?: earliestTimestamp

        val earliestSeconds = earliestTimestamp.seconds
        val latestSeconds = latestTimestamp.seconds
        val diffSeconds = latestSeconds - earliestSeconds

        val xPositions = if (diffSeconds == 0L) {
            List(sortedEntries.size) { 0.5f }
        } else {
            sortedEntries.map {
                val time = it.timestamp?.seconds ?: earliestSeconds
                ((time - earliestSeconds).toFloat()) / diffSeconds.toFloat()
            }
        }

        return Triple(xPositions, earliestTimestamp, diffSeconds)
    }

    fun analyzeTrendAndComment(moodEntries: List<MoodEntry>): Pair<Float?, String> {
        if (moodEntries.count() <= 2) {
            return Pair(null, "Log more moods over a few days to see trends here!")
        }

        val yPositions = moodEntries.map { getYPositionFor(it) }
        val (xPositions, _, _) = getXDataFor(moodEntries)

        if (xPositions.size != yPositions.size || xPositions.size <= 1) {
            return Pair(null, "Not enough comparable data points for trend analysis.")
        }

        val xBar = xPositions.average().toFloat()
        val yBar = yPositions.average().toFloat()

        var numerator = 0f
        var denominator = 0f

        for (i in xPositions.indices) {
            val xDiff = xPositions[i] - xBar
            val yDiff = yPositions[i] - yBar
            numerator += xDiff * yDiff
            denominator += xDiff.pow(2)
        }

        val slope = if (denominator == 0f) 0f else numerator / denominator
        Log.i("MoodTrendAnalyzer", "Calculated Trend Slope: $slope")

        val comment = when {
            slope > 0.3 -> "Your overall mood trend appears to be improving during this period. 🙂"
            slope < -0.3 -> "Your overall mood trend seems to be declining during this period. 🙁"
            else -> "Your overall mood seems relatively stable during this period."
        }

        return Pair(slope, comment)
    }
}
