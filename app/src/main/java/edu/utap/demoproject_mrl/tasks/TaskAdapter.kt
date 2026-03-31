package edu.utap.demoproject_mrl.tasks

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

class TaskAdapter(
    private val onToggle: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvReminder.text = task.reminderTime
        holder.cbTask.isChecked = task.isCompleted

        // Strike through text when completed
        if (task.isCompleted) {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.tvTitle.paintFlags = holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.cbTask.setOnClickListener { onToggle(task) }
        holder.btnDelete.setOnClickListener { onDelete(task) }
    }

    override fun getItemCount() = tasks.size
}