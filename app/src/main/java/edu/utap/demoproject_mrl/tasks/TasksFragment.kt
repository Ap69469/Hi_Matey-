package edu.utap.demoproject_mrl.tasks

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

class TasksFragment : Fragment() {

    private val viewModel: SharedViewModel by activityViewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reset tasks if it's a new day
        viewModel.resetTasksIfNewDay()

        // Set up RecyclerView
        adapter = TaskAdapter(
            onToggle = { task -> viewModel.toggleTask(task) },
            onDelete = { task -> viewModel.deleteTask(task) }
        )

        val rvTasks = view.findViewById<RecyclerView>(R.id.rvTasks)
        rvTasks.layoutManager = LinearLayoutManager(requireContext())
        rvTasks.adapter = adapter

        // Observe tasks from Room DB
        viewModel.allTasks.observe(viewLifecycleOwner) { tasks ->
            adapter.submitList(tasks)
        }

        // Add task button
        val etNewTask = view.findViewById<EditText>(R.id.etNewTask)
        view.findViewById<Button>(R.id.btnAddTask).setOnClickListener {
            val title = etNewTask.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addTask(title)
            etNewTask.text.clear()
        }

        // Back button
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            findNavController().navigate(R.id.action_tasks_to_home)
        }
    }
}