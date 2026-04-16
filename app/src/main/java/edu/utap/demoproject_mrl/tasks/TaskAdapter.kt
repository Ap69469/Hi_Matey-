package edu.utap.demoproject_mrl.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.Task
import java.util.Calendar

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
        val tvStreak: TextView = view.findViewById(R.id.tvStreak)
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

        if (task.isCompleted) {
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags =
                holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        if (task.streak > 0) {
            holder.tvStreak.text = "🔥 ${task.streak} day streak!"
            holder.tvStreak.visibility = View.VISIBLE
        } else {
            holder.tvStreak.visibility = View.GONE
        }

        holder.cbTask.setOnClickListener { onToggle(task) }
        holder.btnDelete.setOnClickListener { onDelete(task) }


        holder.btnSetReminder.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dialog = android.app.TimePickerDialog(
                holder.itemView.context,
                android.R.style.Theme_Holo_Light_Dialog,
                { _, hour, minute ->
                    onSetReminder(task, hour, minute)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            dialog.show()
        }
    }

    override fun getItemCount() = tasks.size
}