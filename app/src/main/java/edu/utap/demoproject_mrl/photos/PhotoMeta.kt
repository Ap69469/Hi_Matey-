package edu.utap.demoproject_mrl.photos

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.Timestamp

data class PhotoMeta(
    var ownerUid: String = "",
    var uuid: String = "",
    var byteSize: Long = 0L,
    var pictureTitle: String = "",
    @ServerTimestamp val timeStamp: Timestamp? = null,
    @DocumentId var firestoreID: String = ""
)