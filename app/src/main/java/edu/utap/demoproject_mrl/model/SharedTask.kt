package edu.utap.demoproject_mrl.model

import com.google.firebase.firestore.PropertyName

data class SharedTask(
    val id: String = "",
    val title: String = "",
    val assignedTo: String = "",
    val assignedToUid: String = "",
    val createdBy: String = "",
    val partnershipId: String = "",
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    val completedAt: Long = 0L,
    val completedBy: String = "",
    val reminderTime: String = ""
)