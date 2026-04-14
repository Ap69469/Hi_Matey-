package edu.utap.demoproject_mrl.shared

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.utap.demoproject_mrl.R
import edu.utap.demoproject_mrl.model.SharedTask

class SharedTaskAdapter(
    private val onToggle: (SharedTask) -> Unit,
    private val onDelete: (SharedTask) -> Unit
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
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteShared)
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
            holder.tvCompletedAt.text = "✓ committed at $time"
            holder.tvCompletedAt.visibility = View.VISIBLE
        } else {
            holder.tvCompletedAt.visibility = View.GONE
        }

        holder.cbTask.setOnCheckedChangeListener { _, _ ->
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onToggle(tasks[pos])
            }
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(tasks[pos])
            }
        }
    }

    override fun getItemCount() = tasks.size
}