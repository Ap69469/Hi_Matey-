package edu.utap.demoproject_mrl.model

data class SharedTask(
    val id: String = "",
    val title: String = "",
    val assignedTo: String = "",     // user email
    val assignedToUid: String = "",  // user uid
    val reminderTime: String = "",   // e.g. "03:00 PM"
    val isCompleted: Boolean = false,
    val createdBy: String = "",
    val partnershipId: String = ""
)