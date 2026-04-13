package edu.utap.demoproject_mrl.fitness

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.database.AppDatabase
import edu.utap.demoproject_mrl.model.WorkoutSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FitnessFragment : Fragment() {

    private lateinit var tvTimer: TextView
    private lateinit var tvWeekCalendar: TextView
    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0L
    private var isRunning = false
    private var photoUri: Uri? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            val elapsed = System.currentTimeMillis() - startTime
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 60000) % 60
            val hours = elapsed / 3600000
            tvTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_fitness_to_photos)
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(), "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_fitness, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTimer = view.findViewById(R.id.tvTimer)
        tvWeekCalendar = view.findViewById(R.id.tvWeekCalendar)

        // Load weekly calendar
        loadWeeklyCalendar()

        // Start button
        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!isRunning) {
                startTime = System.currentTimeMillis()
                isRunning = true
                handler.post(timerRunnable)
                Toast.makeText(requireContext(), "Workout started! 💪", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop button — saves workout to Room DB
        view.findViewById<Button>(R.id.btnStop).setOnClickListener {
            if (isRunning) {
                isRunning = false
                handler.removeCallbacks(timerRunnable)
                val durationSeconds = (System.currentTimeMillis() - startTime) / 1000

                // Save workout session
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val session = WorkoutSession(
                    date = today,
                    durationSeconds = durationSeconds
                )

                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(requireContext()).workoutDao().insertWorkout(session)
                    withContext(Dispatchers.Main) {
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        Toast.makeText(requireContext(),
                            "Workout saved! ${mins}m ${secs}s 🎉",
                            Toast.LENGTH_SHORT).show()
                        loadWeeklyCalendar() // refresh calendar
                    }
                }
            }
        }

        // Capture photo
        view.findViewById<Button>(R.id.btnCapturePhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }

        // Back button
        view.findViewById<Button>(R.id.btnBackFitness).setOnClickListener {
            findNavController().navigate(R.id.action_fitness_to_home)
        }
    }

    private fun loadWeeklyCalendar() {
        CoroutineScope(Dispatchers.IO).launch {
            val workoutDates = AppDatabase.getDatabase(requireContext())
                .workoutDao().getAllWorkoutDates().toSet()

            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            // Get this week's Monday
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            val weekDisplay = StringBuilder("This Week:\n")
            for (i in 0..6) {
                val dateStr = sdf.format(calendar.time)
                val dot = if (workoutDates.contains(dateStr)) "🟢" else "⬜"
                weekDisplay.append("${dayNames[i]} $dot  ")
                if (i == 3) weekDisplay.append("\n")
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            withContext(Dispatchers.Main) {
                tvWeekCalendar.text = weekDisplay.toString()
            }
        }
    }

    private fun launchCamera() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePicture.launch(photoUri)
    }

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        return File(storageDir, "IMG_${timestamp}.jpg")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(timerRunnable)
    }
}