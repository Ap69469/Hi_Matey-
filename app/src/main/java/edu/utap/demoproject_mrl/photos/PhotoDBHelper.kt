package edu.utap.demoproject_mrl.photos

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PhotoDBHelper {
    private val db = FirebaseFirestore.getInstance()
    private val rootCollection = "userPhotos"


    fun fetchPhotoMeta(
        userUid: String,
        resultListener: (List<PhotoMeta>) -> Unit
    ) {
        db.collection(rootCollection)
            .whereEqualTo("ownerUid", userUid)
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { result ->
                resultListener(result.documents.mapNotNull {
                    it.toObject(PhotoMeta::class.java)
                })
            }
            .addOnFailureListener {
                Log.d("PhotoDBHelper", "Fetch FAILED", it)
                resultListener(listOf())
            }
    }


    fun createPhotoMeta(
        photoMeta: PhotoMeta,
        resultListener: () -> Unit
    ) {
        db.collection(rootCollection)
            .add(photoMeta)
            .addOnSuccessListener { ref ->
                Log.d("PhotoDBHelper", "Created photo ${ref.id}")
                resultListener()
            }
            .addOnFailureListener {
                Log.d("PhotoDBHelper", "Create FAILED", it)
            }
    }


    fun removePhotoMeta(
        photoMeta: PhotoMeta,
        resultListener: () -> Unit
    ) {
        db.collection(rootCollection)
            .document(photoMeta.firestoreID)
            .delete()
            .addOnSuccessListener {
                Log.d("PhotoDBHelper", "Deleted photo ${photoMeta.firestoreID}")
                resultListener()
            }
            .addOnFailureListener {
                Log.d("PhotoDBHelper", "Delete FAILED", it)
            }
    }
}