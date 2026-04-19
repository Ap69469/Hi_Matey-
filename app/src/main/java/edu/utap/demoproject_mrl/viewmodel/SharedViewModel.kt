
package edu.utap.demoproject_mrl.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import edu.utap.demoproject_mrl.database.AppDatabase
import edu.utap.demoproject_mrl.model.Task
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val taskDao = AppDatabase.getDatabase(application).taskDao()
    val allTasks: LiveData<List<Task>> = taskDao.getAllTasks()

    fun addTask(title: String, reminderTime: String = "") {
        viewModelScope.launch {
            taskDao.insertTask(Task(title = title, reminderTime = reminderTime))
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val today = getTodayDate()
            val yesterday = getYesterdayDate()
            val newIsCompleted = !task.isCompleted

            val newStreak = when {

                newIsCompleted && task.lastCompletedDate == yesterday -> task.streak + 1
                newIsCompleted && task.lastCompletedDate != today -> 1
                newIsCompleted -> task.streak // already completed today


                else -> if (task.streak > 0) task.streak - 1 else 0
            }


            val newLastCompletedDate = if (newIsCompleted) today else ""

            taskDao.updateTask(task.copy(
                isCompleted = newIsCompleted,
                lastCompletedDate = newLastCompletedDate, // ✅ Fixed
                streak = newStreak
            ))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
        }
    }

    fun resetTasksIfNewDay() {
        viewModelScope.launch {
            taskDao.resetTasksForNewDay(
                getTodayDate(),
                    yesterday = getYesterdayDate()
            )
        }
    }

    fun updateTaskReminder(task: Task, reminderTime: String) {
        viewModelScope.launch {
            taskDao.updateTask(task.copy(reminderTime = reminderTime))
        }
    }

    private fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getYesterdayDate(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(cal.time)
    }
}