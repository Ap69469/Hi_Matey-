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
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.database.AppDatabase
import edu.utap.demoproject_mrl.model.WorkoutSession
import edu.utap.demoproject_mrl.photos.PhotoDBHelper
import edu.utap.demoproject_mrl.photos.PhotoMeta
import edu.utap.demoproject_mrl.photos.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FitnessFragment : Fragment() {

    private lateinit var tvTimer: TextView
    private lateinit var tvWeekCalendar: TextView
    private lateinit var tvThisMonth: TextView
    private lateinit var tvLastMonth: TextView
    private lateinit var tvTotalWorkouts: TextView
    private lateinit var spinnerWorkoutType: Spinner

    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0L
    private var isRunning = false
    private var photoUri: Uri? = null

    // ✅ Firebase Storage helpers
    private val photoStorage = PhotoStorage()
    private val photoDBHelper = PhotoDBHelper()
    private var currentUUID: String = ""
    private var currentPhotoFile: File? = null

    private val workoutTypes = listOf(
        "🏃 Running",
        "🚴 Cycling",
        "🏊 Swimming",
        "🏋️ Weight Training",
        "🧘 Yoga",
        "🚶 Walking",
        "⚽ Sports",
        "💪 General"
    )

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

    // ✅ Updated takePicture — uploads to Firebase Storage
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            val userUid = FirebaseAuth.getInstance().currentUser?.uid
            val localFile = currentPhotoFile

            if (userUid == null || localFile == null) {
                Toast.makeText(requireContext(),
                    "Upload failed — not logged in", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            Toast.makeText(requireContext(),
                "Uploading photo... ☁️", Toast.LENGTH_SHORT).show()

            // ✅ Upload to Firebase Storage
            photoStorage.uploadImage(localFile, userUid, currentUUID) { byteSize ->

                // ✅ Save metadata to Firestore
                val photoMeta = PhotoMeta(
                    ownerUid = userUid,
                    uuid = currentUUID,
                    byteSize = byteSize,
                    pictureTitle = "Workout Photo"
                )

                photoDBHelper.createPhotoMeta(photoMeta) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(),
                            "Photo saved to cloud! ✅",
                            Toast.LENGTH_SHORT).show()
                        // ✅ Reset after use
                        currentUUID = ""
                        currentPhotoFile = null
                        findNavController().navigate(R.id.action_fitness_to_photos)
                    }
                }
            }
        }
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(requireContext(),
            "Camera permission needed", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_fitness, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvTimer = view.findViewById(R.id.tvTimer)
        tvWeekCalendar = view.findViewById(R.id.tvWeekCalendar)
        tvThisMonth = view.findViewById(R.id.tvThisMonth)
        tvLastMonth = view.findViewById(R.id.tvLastMonth)
        tvTotalWorkouts = view.findViewById(R.id.tvTotalWorkouts)
        spinnerWorkoutType = view.findViewById(R.id.spinnerWorkoutType)

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            workoutTypes
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerWorkoutType.adapter = spinnerAdapter

        loadWeeklyCalendar()
        loadMonthlyStats()

        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!isRunning) {
                startTime = System.currentTimeMillis()
                isRunning = true
                handler.post(timerRunnable)
                Toast.makeText(requireContext(),
                    "Workout started! 💪", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<Button>(R.id.btnStop).setOnClickListener {
            if (isRunning) {
                isRunning = false
                handler.removeCallbacks(timerRunnable)
                val durationSeconds = (System.currentTimeMillis() - startTime) / 1000
                val selectedType = spinnerWorkoutType.selectedItem.toString()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val session = WorkoutSession(
                    date = today,
                    durationSeconds = durationSeconds,
                    workoutType = selectedType
                )

                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    AppDatabase.getDatabase(requireContext())
                        .workoutDao().insertWorkout(session)
                    withContext(Dispatchers.Main) {
                        val mins = durationSeconds / 60
                        val secs = durationSeconds % 60
                        Toast.makeText(requireContext(),
                            "Workout saved! $selectedType ${mins}m ${secs}s 🎉",
                            Toast.LENGTH_SHORT).show()
                        loadWeeklyCalendar()
                        loadMonthlyStats()
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btnCapturePhoto).setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
                launchCamera()
            } else {
                requestPermission.launch(Manifest.permission.CAMERA)
            }
        }

        view.findViewById<Button>(R.id.btnBackFitness).setOnClickListener {
            findNavController().navigate(R.id.action_fitness_to_home)
        }
    }

    private fun loadMonthlyStats() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val cal = Calendar.getInstance()
            val thisMonthPrefix = sdf.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            val lastMonthPrefix = sdf.format(cal.time)
            cal.add(Calendar.MONTH, 1)
            val daysInThisMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.add(Calendar.MONTH, -1)
            val daysInLastMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            val dao = AppDatabase.getDatabase(requireContext()).workoutDao()
            val thisMonthDays = dao.getWorkoutDaysInMonth(thisMonthPrefix)
            val lastMonthDays = dao.getWorkoutDaysInMonth(lastMonthPrefix)
            val totalWorkouts = dao.getTotalWorkouts()

            withContext(Dispatchers.Main) {
                tvThisMonth.text = "📅 This Month: $thisMonthDays / $daysInThisMonth days"
                tvLastMonth.text = "📅 Last Month: $lastMonthDays / $daysInLastMonth days"
                tvTotalWorkouts.text = "🏆 Total Workouts: $totalWorkouts"
            }
        }
    }




    private fun launchCamera() {
        val photoFile = createImageFile()
        currentPhotoFile = photoFile
        currentUUID = UUID.randomUUID().toString() // ✅ Generate UUID

        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePicture.launch(photoUri)
    }
    private fun loadWeeklyCalendar() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val workoutDates = AppDatabase.getDatabase(requireContext())
                .workoutDao().getAllWorkoutDates().toSet()

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            // Locale-safe Monday calculation
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = (dayOfWeek - Calendar.MONDAY + 7) % 7
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

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