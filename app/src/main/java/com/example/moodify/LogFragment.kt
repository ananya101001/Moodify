package com.example.moodify

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.moodify.databinding.FragmentLogBinding
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private var selectedMood: String? = null
    private var selectedMoodImageView: ImageView? = null

    private var tfliteInterpreter: Interpreter? = null
    private lateinit var moodTokenizer: MoodTokenizer
    private var isTfLiteInitialized = false
    private val labels = listOf("Anxious", "Good", "Happy", "Okay", "Sad")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        firestore = FirebaseFirestore.getInstance()

        setupMoodClickListeners()
        binding.buttonSaveMood.setOnClickListener { saveMoodEntry() }

        initializeTfLiteComponents()
    }

    private fun initializeTfLiteComponents() {
        if (isTfLiteInitialized) return

        val safeContext = context ?: return

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelBuffer = loadModelFile(safeContext, "mood_model.tflite")
                tfliteInterpreter = Interpreter(modelBuffer)

                moodTokenizer = MoodTokenizer(safeContext)

                if (moodTokenizer.isReady()) {
                    isTfLiteInitialized = true
                } else {
                    throw IOException("Tokenizer not initialized.")
                }

            } catch (e: Exception) {
                Log.e("LogFragment", "Model init failed", e)
                tfliteInterpreter?.close()
                tfliteInterpreter = null
                isTfLiteInitialized = false
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(safeContext, "Error loading model", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun loadModelFile(context: Context, modelFile: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFile)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
    }

    private fun setupMoodClickListeners() {
        binding.imageMoodHappy.setOnClickListener { handleMoodSelection("Happy", it as ImageView) }
        binding.imageMoodGood.setOnClickListener { handleMoodSelection("Good", it as ImageView) }
        binding.imageMoodOkay.setOnClickListener { handleMoodSelection("Okay", it as ImageView) }
        binding.imageMoodSad.setOnClickListener { handleMoodSelection("Sad", it as ImageView) }
        binding.imageMoodAnxious.setOnClickListener { handleMoodSelection("Anxious", it as ImageView) }
    }

    private fun handleMoodSelection(mood: String, clickedImageView: ImageView) {
        selectedMoodImageView?.alpha = 0.7f
        clickedImageView.alpha = 1.0f
        selectedMood = mood
        selectedMoodImageView = clickedImageView
    }

    private fun saveMoodEntry() {
        val note = binding.editTextJournal.text.toString().trim()
        val currentSelectedMood = selectedMood

        val selectedActivities = mutableListOf<String>()
        binding.chipGroupActivities.checkedChipIds.forEach { chipId ->
            (binding.chipGroupActivities.findViewById(chipId) as? Chip)?.text?.toString()?.let {
                selectedActivities.add(it)
            }
        }

        if (currentSelectedMood == null) {
            Toast.makeText(context, "Please select a mood", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        binding.buttonSaveMood.isEnabled = false
        binding.buttonSaveMood.text = "Saving..."

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val sentiment = if (note.isNotBlank() && isTfLiteInitialized) {
                    analyzeSentiment(note)
                } else null

                val entry = MoodEntry(
                    userId = userId,
                    mood = currentSelectedMood,
                    note = if (note.isBlank()) null else note,
                    timestamp = Timestamp.now(),
                    sentiment = sentiment,
                    activities = if (selectedActivities.isEmpty()) null else selectedActivities
                )

                saveToFirestore(entry)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving mood: ${e.message}", Toast.LENGTH_LONG).show()
                    binding.buttonSaveMood.isEnabled = true
                    binding.buttonSaveMood.text = "Save Mood"
                }
            }
        }
    }

    private suspend fun analyzeSentiment(text: String): String? = withContext(Dispatchers.Default) {
        try {
            val tokens = moodTokenizer.tokenizeAndPad(text)
            val inputBuffer = ByteBuffer.allocateDirect(15 * 4).order(ByteOrder.nativeOrder()).apply {
                tokens.forEach { putFloat(it.toFloat()) }
                rewind()
            }

            val output = Array(1) { FloatArray(labels.size) }
            tfliteInterpreter?.run(inputBuffer, output)

            val maxIndex = output[0].indices.maxByOrNull { output[0][it] }
            maxIndex?.let { labels[it] }
        } catch (e: Exception) {
            Log.e("LogFragment", "Sentiment analysis failed", e)
            null
        }
    }

    private fun saveToFirestore(entry: MoodEntry) {
        firestore.collection("moodEntries")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(context, "Mood saved successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                binding.buttonSaveMood.isEnabled = true
                binding.buttonSaveMood.text = "Save Mood"
            }
    }

    private fun clearForm() {
        binding.editTextJournal.setText("")
        selectedMood = null
        selectedMoodImageView?.alpha = 0.7f
        selectedMoodImageView = null
        binding.chipGroupActivities.clearCheck()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        tfliteInterpreter?.close()
    }
}
