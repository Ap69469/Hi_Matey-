package edu.utap.demoproject_mrl.tasks

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.viewmodel.SharedViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit

class TasksFragment : Fragment() {

    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: TaskAdapter
    private var selectedReminderTime: String = ""
    private var selectedHour: Int = -1
    private var selectedMinute: Int = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tasks, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.resetTasksIfNewDay()

        adapter = TaskAdapter(
            onToggle = { task -> viewModel.toggleTask(task) },
            onDelete = { task -> viewModel.deleteTask(task) },
            onSetReminder = { task, hour, minute ->
                val reminderTime = String.format("%02d:%02d", hour, minute)
                viewModel.updateTaskReminder(task, reminderTime)
                scheduleReminder(task.title, hour, minute)
                Toast.makeText(requireContext(),
                    "Reminder set for ${task.title} at $reminderTime ⏰",
                    Toast.LENGTH_SHORT).show()
            }
        )

        view.findViewById<RecyclerView>(R.id.rvTasks).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TasksFragment.adapter
        }

        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }

        val etNewTask = view.findViewById<EditText>(R.id.etNewTask)

        etNewTask.setOnLongClickListener {
            showTimePicker()
            true
        }

        view.findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val title = etNewTask.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Please enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addTask(title, selectedReminderTime)

            if (selectedHour >= 0 && selectedMinute >= 0) {
                scheduleReminder(title, selectedHour, selectedMinute)
                Toast.makeText(requireContext(),
                    "Task added with reminder at $selectedReminderTime ⏰",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Task added!", Toast.LENGTH_SHORT).show()
            }

            etNewTask.text.clear()
            selectedReminderTime = ""
            selectedHour = -1
            selectedMinute = -1
        }

        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.action_tasks_to_home)
        }
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                selectedReminderTime = String.format("%02d:%02d", hour, minute)
                Toast.makeText(requireContext(),
                    "Reminder set for $selectedReminderTime ⏰",
                    Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun scheduleReminder(title: String, hour: Int, minute: Int) {
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

        // ✅ Use stable tag per task
        val tag = "reminder_$title"

        // ✅ Cancel previous reminder for this task
        WorkManager.getInstance(requireContext().applicationContext)
            .cancelAllWorkByTag(tag)

        // ✅ Schedule with tag
        val reminderRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag) // ✅ Tag now actually added
            .build()

        WorkManager.getInstance(requireContext().applicationContext)
            .enqueue(reminderRequest)
    }
}