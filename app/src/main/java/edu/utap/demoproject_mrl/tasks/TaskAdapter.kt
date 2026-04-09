package edu.utap.demoproject_mrl.tasks

import android.app.TimePickerDialog
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.Task
import java.util.Calendar
import android.widget.Button

class TaskAdapter(
    private val onToggle: (Task) -> Unit,
    private val onDelete: (Task) -> Unit,
    private val onSetReminder: (Task, Int, Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks = listOf<Task>()

    fun submitList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbTask: CheckBox = view.findViewById(R.id.cbTask)
        val tvTitle: TextView = view.findViewById(R.id.tvTaskTitle)
        val tvReminder: TextView = view.findViewById(R.id.tvReminderTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val btnSetReminder: Button = view.findViewById(R.id.btnSetReminder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvReminder.text = if (task.reminderTime.isNotEmpty())
            "⏰ ${task.reminderTime}" else ""

        holder.cbTask.isChecked = task.isCompleted

        // Strike through when completed
        if (task.isCompleted) {
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cbTask.setOnClickListener { onToggle(task) }
        holder.btnDelete.setOnClickListener { onDelete(task) }

        // Set reminder for THIS specific task
        holder.btnSetReminder.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                holder.itemView.context,
                { _, hour, minute ->
                    onSetReminder(task, hour, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    override fun getItemCount() = tasks.size
}