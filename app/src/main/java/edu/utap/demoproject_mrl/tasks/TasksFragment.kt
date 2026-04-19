package edu.utap.demoproject_mrl.tasks

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
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
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.viewmodel.SharedViewModel
import java.util.Calendar

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

        // REMOVED: viewModel.resetTasksIfNewDay() — MainActivity handles this now

        adapter = TaskAdapter(
            onToggle = { task -> viewModel.toggleTask(task) },
            onDelete = { task ->
                cancelReminder(task.title)
                viewModel.deleteTask(task)
            },
            onSetReminder = { task, hour, minute ->
                val reminderTime = String.format("%02d:%02d", hour, minute)
                viewModel.updateTaskReminder(task, reminderTime)
                scheduleReminder(task.title, hour, minute)
                Toast.makeText(
                    requireContext(),
                    "Reminder set for ${task.title} at $reminderTime ⏰",
                    Toast.LENGTH_SHORT
                ).show()
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
            set(Calendar.MILLISECOND, 0)
        }

        if (reminderTime.before(now)) {
            reminderTime.add(Calendar.DAY_OF_MONTH, 1)
        }

        val intent = Intent(requireContext(), ReminderReceiver::class.java).apply {
            putExtra("task_title", title)
        }

        val requestCode = title.trim().lowercase().hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.timeInMillis,
                    pendingIntent
                )
            } else {
                // Permission not granted — open system settings so user can allow it
                Toast.makeText(
                    requireContext(),
                    "Please allow exact alarms in Settings for accurate reminders",
                    Toast.LENGTH_LONG
                ).show()
                val settingsIntent = Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                )
                startActivity(settingsIntent)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminderTime.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelReminder(title: String) {
        val intent = Intent(requireContext(), ReminderReceiver::class.java)
        val requestCode = title.trim().lowercase().hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(), requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager =
            requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}