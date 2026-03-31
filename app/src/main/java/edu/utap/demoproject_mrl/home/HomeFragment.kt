package edu.utap.demoproject_mrl.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import edu.utap.demoproject_mrl.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Show greeting with user email
        val user = FirebaseAuth.getInstance().currentUser
        val tvProgress = view.findViewById<TextView>(R.id.tvProgress)
        tvProgress.text = "Welcome, ${user?.email ?: "User"}!"

        // Navigation buttons
        view.findViewById<Button>(R.id.btnDailyRoutine).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_tasks)
        }

        view.findViewById<Button>(R.id.btnSharedTasks).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_shared)
        }

        view.findViewById<Button>(R.id.btnFitness).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_fitness)
        }

        view.findViewById<Button>(R.id.btnPhotos).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_photos)
        }

        view.findViewById<Button>(R.id.btnSettings).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
    }
}