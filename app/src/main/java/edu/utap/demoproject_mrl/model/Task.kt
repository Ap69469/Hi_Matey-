package edu.utap.demoproject_mrl.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val reminderTime: String = "",
    val isCompleted: Boolean = false,
    val lastCompletedDate: String = "",
    val streak: Int = 0
)