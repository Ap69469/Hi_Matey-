package edu.utap.demoproject_mrl.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String = "",        // "2026-04-09"
    val durationSeconds: Long = 0,
    val workoutType: String = "General"
)