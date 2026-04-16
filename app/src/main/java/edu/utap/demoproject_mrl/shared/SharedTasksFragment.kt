package edu.utap.demoproject_mrl.shared

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.utap.demoproject_mrl.MainActivity
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.SharedTask
import edu.utap.demoproject_mrl.tasks.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SharedTasksFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SharedTaskAdapter
    private var partnershipId: String = ""
    private var taskListener: ListenerRegistration? = null
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
            onDelete = { task -> deleteSharedTask(task) },

            onSetReminder = { task, hour, minute ->
                val reminderTime = String.format("%02d:%02d", hour, minute)
                db.collection("sharedTasks").document(task.id)
                    .update("reminderTime", reminderTime)

                scheduleSharedReminder(task.id, task.title, hour, minute)
                Toast.makeText(requireContext(),
                    "Reminder set for ${task.title} at $reminderTime ⏰",
                    Toast.LENGTH_SHORT).show()
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        taskListener?.remove()
        taskListener = null
    }

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
        taskListener?.remove()

        taskListener = db.collection("sharedTasks")
            .whereEqualTo("partnershipId", partnershipId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val tasks = snapshot.documents.mapNotNull {
                    it.toObject(SharedTask::class.java)
                }

                if (!snapshot.metadata.hasPendingWrites()) {
                    if (!isFirstLoad) {
                        tasks.forEach { newTask ->
                            val oldTask = previousTasks.find { it.id == newTask.id }
                            if (newTask.isCompleted && oldTask?.isCompleted == false) {
                                showTaskClaimedNotification(
                                    newTask.assignedTo,
                                    newTask.title,
                                    newTask.id
                                )
                            }
                        }
                    }
                    isFirstLoad = false
                    previousTasks = tasks
                }

                adapter.submitList(tasks)
            }
    }


    private fun scheduleSharedReminder(taskId: String, title: String, hour: Int, minute: Int) {
        val now = Calendar.getInstance()
        val reminderTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (reminderTime.before(now)) {
            reminderTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val delay = reminderTime.timeInMillis - now.timeInMillis

        val data = Data.Builder()
            .putString("task_title", title)
            .build()


        val workName = "shared_reminder_$taskId"

        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workName)
            .build()


        WorkManager.getInstance(requireContext().applicationContext)
            .enqueueUniqueWork(
                workName,
                ExistingWorkPolicy.REPLACE,
                reminderRequest
            )
    }

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

    private fun showTaskClaimedNotification(
        userEmail: String,
        taskTitle: String,
        taskId: String
    ) {
        val appContext = requireContext().applicationContext

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, "himatey_shared")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hi Matey Update 🤝")
            .setContentText("$userEmail is handling: $taskTitle")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$userEmail is handling: $taskTitle"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(taskId.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "himatey_shared",
            "Shared Task Updates",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = requireContext().applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun deleteSharedTask(task: SharedTask) {
        db.collection("sharedTasks").document(task.id).delete()
    }
}