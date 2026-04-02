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
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FitnessFragment : Fragment() {

    private lateinit var tvTimer: TextView
    private var handler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0L
    private var isRunning = false
    private var photoUri: Uri? = null

    // Timer runnable — UI display layer
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

    // Camera launcher
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            Toast.makeText(requireContext(), "Photo saved!", Toast.LENGTH_SHORT).show()
            // Auto-navigate to Photos Gallery
            findNavController().navigate(R.id.action_fitness_to_photos)
        }
    }

    // Permission launcher
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

        // Start button — saves Unix timestamp (core logic)
        view.findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (!isRunning) {
                startTime = System.currentTimeMillis() // core timer logic
                isRunning = true
                handler.post(timerRunnable) // UI display layer
                Toast.makeText(requireContext(), "Workout started!", Toast.LENGTH_SHORT).show()
            }
        }

        // Stop button
        view.findViewById<Button>(R.id.btnStop).setOnClickListener {
            if (isRunning) {
                isRunning = false
                handler.removeCallbacks(timerRunnable)
                val duration = System.currentTimeMillis() - startTime
                Toast.makeText(requireContext(),
                    "Workout saved! Duration: ${duration / 1000}s",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Capture photo button
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