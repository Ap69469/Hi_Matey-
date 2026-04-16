package edu.utap.demoproject_mrl.shared

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
import edu.utap.demoproject_mrl.model.SharedTask
import java.util.Calendar

class SharedTaskAdapter(
    private val onToggle: (SharedTask) -> Unit,
    private val onDelete: (SharedTask) -> Unit,
    private val onSetReminder: (SharedTask, Int, Int) -> Unit
) : RecyclerView.Adapter<SharedTaskAdapter.ViewHolder>() {

    private var tasks = listOf<SharedTask>()

    fun submitList(newTasks: List<SharedTask>) {
        tasks = newTasks.toList()
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbTask: CheckBox = view.findViewById(R.id.cbSharedTask)
        val tvTitle: TextView = view.findViewById(R.id.tvSharedTaskTitle)
        val tvAssigned: TextView = view.findViewById(R.id.tvAssignedTo)
        val tvReminderTime: TextView = view.findViewById(R.id.tvSharedReminderTime)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteShared)
        val btnReminder: Button = view.findViewById(R.id.btnSharedReminder)
        val tvCompletedAt: TextView = view.findViewById(R.id.tvCompletedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTitle.text = task.title
        holder.tvAssigned.text = "→ ${task.assignedTo}"

        if (task.reminderTime.isNotEmpty()) {
            holder.tvReminderTime.text = "⏰ ${task.reminderTime}"
            holder.tvReminderTime.visibility = View.VISIBLE
        } else {
            holder.tvReminderTime.visibility = View.GONE
        }

        holder.cbTask.setOnCheckedChangeListener(null)
        holder.cbTask.isChecked = task.isCompleted

        holder.tvTitle.paintFlags =
            if (task.isCompleted)
                holder.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else
                holder.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

        if (task.isCompleted && task.completedAt > 0L) {
            val time = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                .format(java.util.Date(task.completedAt))
            val name = task.assignedTo.substringBefore("@")
            holder.tvCompletedAt.text = "✓ $name finished at $time"
            holder.tvCompletedAt.visibility = View.VISIBLE
        } else {
            holder.tvCompletedAt.visibility = View.GONE
        }

        holder.cbTask.setOnCheckedChangeListener { _, _ ->
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onToggle(tasks[pos])
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(tasks[pos])
        }

        holder.btnReminder.setOnClickListener {
            val calendar = Calendar.getInstance()
            val dialog = android.app.TimePickerDialog(
                holder.itemView.context,
                android.R.style.Theme_Holo_Light_Dialog,
                { _, hour, minute ->
                    val pos = holder.bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onSetReminder(tasks[pos], hour, minute)
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            )
            dialog.show()
        }
    } // ✅ closes onBindViewHolder

    override fun getItemCount() = tasks.size
} // ✅ closes class