package edu.utap.demoproject_mrl.tasks

import androidx.lifecycle.LiveData
import androidx.room.*
import edu.utap.demoproject_mrl.model.Task

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY id ASC")
    fun getAllTasks(): LiveData<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("UPDATE tasks SET isCompleted = 0 WHERE lastCompletedDate != :today")
    suspend fun resetTasksForNewDay(today: String)
}