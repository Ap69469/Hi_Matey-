package edu.utap.demoproject_mrl.shared

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.SharedTask

class SharedTasksFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SharedTaskAdapter
    private var partnershipId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_shared_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser ?: return

        // Set up RecyclerView
        adapter = SharedTaskAdapter(
            currentUserEmail = currentUser.email ?: "",
            onToggle = { task -> toggleSharedTask(task) },
            onDelete = { task -> deleteSharedTask(task) }
        )

        view.findViewById<RecyclerView>(R.id.rvSharedTasks).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SharedTasksFragment.adapter
        }

        // Load partnership and listen for tasks
        loadPartnership(currentUser.uid, view)

        // Add task button
        val etTask = view.findViewById<EditText>(R.id.etNewSharedTask)
        val etAssign = view.findViewById<EditText>(R.id.etAssignTo)

        view.findViewById<Button>(R.id.btnAddSharedTask).setOnClickListener {
            val title = etTask.text.toString().trim()
            val assignTo = etAssign.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addSharedTask(title, assignTo.ifEmpty { currentUser.email ?: "" })
            etTask.text.clear()
            etAssign.text.clear()
        }

        // Back button
        view.findViewById<Button>(R.id.btnBackShared).setOnClickListener {
            findNavController().navigate(R.id.action_shared_to_home)
        }
    }

    private fun loadPartnership(uid: String, view: View) {
        db.collection("partnerships")
            .whereArrayContains("members", uid)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    partnershipId = docs.documents[0].id
                    listenForSharedTasks()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No partner yet. Invite one in Settings!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun listenForSharedTasks() {
        // Real-time Firestore listener — mutable shared state!
        db.collection("sharedTasks")
            .whereEqualTo("partnershipId", partnershipId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val tasks = snapshot.documents.mapNotNull {
                        it.toObject(SharedTask::class.java)
                    }
                    adapter.submitList(tasks)
                }
            }
    }

    private fun addSharedTask(title: String, assignTo: String) {
        if (partnershipId.isEmpty()) {
            Toast.makeText(requireContext(),
                "No active partnership!", Toast.LENGTH_SHORT).show()
            return
        }
        val task = SharedTask(
            id = db.collection("sharedTasks").document().id,
            title = title,
            assignedTo = assignTo,
            createdBy = auth.currentUser?.email ?: "",
            partnershipId = partnershipId
        )
        db.collection("sharedTasks").document(task.id).set(task)
    }

    private fun toggleSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id)
            .update("isCompleted", !task.isCompleted)
    }

    private fun deleteSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id).delete()
    }
}