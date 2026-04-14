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
    private var taskListener: ListenerRegistration? = null

    // ✅ Track previous tasks for notification comparison
    private var previousTasks = listOf<SharedTask>()
    private var isFirstLoad = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_shared_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser ?: return

        adapter = SharedTaskAdapter(
            onToggle = { task -> toggleSharedTask(task) },
            onDelete = { task -> deleteSharedTask(task) }
        )

        view.findViewById<RecyclerView>(R.id.rvSharedTasks).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SharedTasksFragment.adapter
        }

        createNotificationChannel()
        loadPartnership(currentUser.uid)

        val etTask = view.findViewById<EditText>(R.id.etNewSharedTask)
        val etAssign = view.findViewById<EditText>(R.id.etAssignTo)

        view.findViewById<Button>(R.id.btnAddSharedTask).setOnClickListener {
            val title = etTask.text.toString().trim()
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

    override fun onStop() {
        super.onStop()
        taskListener?.remove()
        taskListener = null
    }

    override fun onStart() {
        super.onStart()
        if (partnershipId.isNotEmpty() && taskListener == null) {
            isFirstLoad = true  // ✅ Reset on return
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
        taskListener?.remove()

        taskListener = db.collection("sharedTasks")
            .whereEqualTo("partnershipId", partnershipId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val tasks = snapshot.documents.mapNotNull {
                    it.toObject(SharedTask::class.java)
                }

                // ✅ Only fire notifications from server-confirmed snapshots
                if (!snapshot.metadata.hasPendingWrites()) {
                    if (!isFirstLoad) {
                        tasks.forEach { newTask ->
                            val oldTask = previousTasks.find { it.id == newTask.id }
                            if (newTask.isCompleted && oldTask?.isCompleted == false) {
                                showTaskClaimedNotification(newTask.assignedTo, newTask.title)
                            }
                        }
                    }
                    isFirstLoad = false
                    previousTasks = tasks
                }

                // ✅ Always update UI
                adapter.submitList(tasks)
            }
    }

    // ✅ No notification here — fires on Firestore snapshot instead
    private fun toggleSharedTask(task: SharedTask) {
        val newStatus = !task.isCompleted
        val updates = mapOf(
            "isCompleted" to newStatus,
            "completedAt" to if (newStatus) System.currentTimeMillis() else 0L
        )
        db.collection("sharedTasks").document(task.id).update(updates)
    }

    private fun addSharedTask(title: String, assignTo: String) {
        if (partnershipId.isEmpty()) return

        db.collection("partnerships").document(partnershipId).get()
            .addOnSuccessListener { doc ->
                val memberEmails = doc.get("memberEmails") as? List<String> ?: emptyList()
                val members = doc.get("members") as? List<String> ?: emptyList()
                val index = memberEmails.indexOf(assignTo)
                val assignedToUid = if (index >= 0) members[index] else ""

                val taskId = db.collection("sharedTasks").document().id
                val task = SharedTask(
                    id = taskId,
                    title = title,
                    assignedTo = assignTo,
                    assignedToUid = assignedToUid,
                    createdBy = auth.currentUser?.email ?: "",
                    partnershipId = partnershipId,
                    isCompleted = false
                )
                db.collection("sharedTasks").document(taskId).set(task)
            }
    }

    private fun showTaskClaimedNotification(userEmail: String, taskTitle: String) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(requireContext(), "himatey_shared")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hi Matey Update 🤝")
            .setContentText("$userEmail is handling: $taskTitle")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        // ✅ Unique ID per task so multiple notifications don't overwrite each other
        notificationManager.notify(taskTitle.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "himatey_shared",
            "Shared Task Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun deleteSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id).delete()
    }
}