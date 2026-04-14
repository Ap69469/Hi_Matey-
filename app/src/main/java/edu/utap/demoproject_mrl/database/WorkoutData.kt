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
    // ✅ Count distinct workout days in a given month (e.g. "2026-04")
    @Query("SELECT COUNT(DISTINCT date) FROM workout_sessions WHERE date LIKE :monthPrefix || '%'")
    suspend fun getWorkoutDaysInMonth(monthPrefix: String): Int

    // ✅ Total workout count all time
    @Query("SELECT COUNT(*) FROM workout_sessions")
    suspend fun getTotalWorkouts(): Int
}