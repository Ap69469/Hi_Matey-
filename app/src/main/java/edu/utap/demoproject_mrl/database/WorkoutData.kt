package edu.utap.demoproject_mrl.database

import androidx.lifecycle.LiveData
import androidx.room.*
import edu.utap.demoproject_mrl.model.WorkoutSession

@Dao
interface WorkoutData {

    @Insert
    suspend fun insertWorkout(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions ORDER BY id DESC")
    fun getAllWorkouts(): LiveData<List<WorkoutSession>>

    @Query("SELECT date FROM workout_sessions")
    suspend fun getAllWorkoutDates(): List<String>
}