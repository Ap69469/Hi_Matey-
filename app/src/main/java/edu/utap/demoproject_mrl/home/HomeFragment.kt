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
        // Daily affirmations
        val affirmations = listOf(
            "\"Arise, awake and stop not till the goal is reached.\" - Swami Vivekananda",
            "\"It does not matter how slowly you go as long as you do not stop.\" - Confucius",
            "\"The secret of getting ahead is getting started.\" - Mark Twain",
            "\"Don't watch the clock; do what it does. Keep going.\" - Sam Levenson",
            "\"Success is the sum of small efforts repeated day in and day out.\" - Robert Collier",
            "\"Believe you can and you're halfway there.\" - Theodore Roosevelt",
            "\"The harder you work, the luckier you get.\" - Gary Player",
            "\"Push yourself because no one else is going to do it for you.\"",
            "\"Great things never come from comfort zones.\"",
            "\"Dream it. Wish it. Do it.\""
        )

        val randomAffirmation = affirmations.random()
        view.findViewById<TextView>(R.id.tvAffirmation).text = randomAffirmation

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