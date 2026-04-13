package edu.utap.demoproject_mrl.shared

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.SharedTask

class SharedTasksFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SharedTaskAdapter
    private var partnershipId: String = ""
    private var previousTasks = listOf<SharedTask>()
    private var isFirstLoad = true

    // ✅ Store listener to properly manage lifecycle
    private var taskListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_shared_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser ?: return

        adapter = SharedTaskAdapter(
            onToggle = { task -> toggleSharedTask(task) },  // ✅ wired up
            onDelete = { task -> deleteSharedTask(task) }
        )

        view.findViewById<RecyclerView>(R.id.rvSharedTasks).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SharedTasksFragment.adapter
        }

        loadPartnership(currentUser.uid)

        val etTask   = view.findViewById<EditText>(R.id.etNewSharedTask)
        val etAssign = view.findViewById<EditText>(R.id.etAssignTo)

        view.findViewById<Button>(R.id.btnAddSharedTask).setOnClickListener {
            val title    = etTask.text.toString().trim()
            val assignTo = etAssign.text.toString().trim()
                .ifEmpty { currentUser.email ?: "" }
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            addSharedTask(title, assignTo)
            etTask.text.clear()
            etAssign.text.clear()
        }

        view.findViewById<Button>(R.id.btnBackShared).setOnClickListener {
            findNavController().navigate(R.id.action_shared_to_home)
        }
    }

    // ✅ Remove listener when off-screen
    override fun onStop() {
        super.onStop()
        taskListener?.remove()
        taskListener = null
    }

    // ✅ Re-attach when returning to screen
    override fun onStart() {
        super.onStart()
        if (partnershipId.isNotEmpty() && taskListener == null) {
            isFirstLoad = true
            listenForSharedTasks()
        }
    }

    private fun loadPartnership(uid: String) {
        db.collection("partnerships")
            .whereArrayContains("members", uid)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(),
                        "No partner yet. Invite one in Settings!",
                        Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                partnershipId = docs.documents[0].id
                isFirstLoad = true
                listenForSharedTasks()
            }
    }

    private fun listenForSharedTasks() {
        taskListener?.remove()  // ✅ Clean up before re-registering

        taskListener = db.collection("sharedTasks")
            .whereEqualTo("partnershipId", partnershipId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val tasks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(SharedTask::class.java)
                }

                val currentUserEmail = auth.currentUser?.email ?: ""

                // ✅ Local notification when partner completes a task
                // (only works while app is open — no Blaze plan)
                if (!isFirstLoad) {
                    tasks.forEach { newTask ->
                        val oldTask = previousTasks.find { it.id == newTask.id }
                        if (newTask.isCompleted &&
                            oldTask?.isCompleted == false &&
                            newTask.assignedTo != currentUserEmail
                        ) {
                            showTaskClaimedNotification(newTask.assignedTo, newTask.title)
                        }
                    }
                }

                isFirstLoad = false
                previousTasks = tasks

                // ✅ No runOnUiThread — Firestore already on main thread
                adapter.submitList(ArrayList(tasks))
            }
    }

    private fun toggleSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id)
            .update("isCompleted", !task.isCompleted)
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addSharedTask(title: String, assignTo: String) {
        if (partnershipId.isEmpty()) {
            Toast.makeText(requireContext(),
                "No active partnership!", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("partnerships").document(partnershipId).get()
            .addOnSuccessListener { doc ->
                val memberEmails  = doc.get("memberEmails") as? List<String> ?: emptyList()
                val members       = doc.get("members")      as? List<String> ?: emptyList()
                val index         = memberEmails.indexOf(assignTo)
                val assignedToUid = if (index >= 0) members[index] else ""

                val taskId = db.collection("sharedTasks").document().id
                val task   = SharedTask(
                    id            = taskId,
                    title         = title,
                    assignedTo    = assignTo,
                    assignedToUid = assignedToUid,
                    createdBy     = auth.currentUser?.email ?: "",
                    partnershipId = partnershipId,
                    isCompleted   = false
                )
                db.collection("sharedTasks").document(taskId).set(task)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            "Task added! ✅", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun showTaskClaimedNotification(userEmail: String, taskTitle: String) {
        val channelId = "himatey_shared"
        val notificationManager = requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(channelId, "Shared Task Updates",
                NotificationManager.IMPORTANCE_HIGH)
        )

        val notification = NotificationCompat.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hi Matey Update 🤝")
            .setContentText("$userEmail is handling: $taskTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun deleteSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id).delete()
    }
}