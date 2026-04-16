package edu.utap.demoproject_mrl.photos

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import java.io.File

class PhotoStorage {
    private val photoStorage: StorageReference =
        FirebaseStorage.getInstance().reference.child("images")


    fun uploadImage(
        localFile: File,
        userUid: String,
        uuid: String,
        uploadSuccess: (Long) -> Unit
    ) {
        val fileUri = Uri.fromFile(localFile)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpg")
            .build()

        val uploadTask = photoStorage
            .child(userUid)
            .child(uuid)
            .putFile(fileUri, metadata)

        uploadTask
            .addOnFailureListener {
                Log.d("PhotoStorage", "Upload FAILED $uuid")
            }
            .addOnSuccessListener {
                val sizeBytes = it.metadata?.sizeBytes ?: -1
                uploadSuccess(sizeBytes)
                Log.d("PhotoStorage", "Upload succeeded $uuid size=$sizeBytes")
            }
    }
    fun getDownloadUrl(userUid: String, uuid: String, onSuccess: (String) -> Unit) {
        photoStorage.child(userUid).child(uuid).downloadUrl
            .addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
            .addOnFailureListener {
                Log.d("PhotoStorage", "Failed to get download URL: ${it.message}")
            }
    }

    fun deleteImage(userUid: String, uuid: String) {
        photoStorage.child(userUid).child(uuid).delete()
            .addOnSuccessListener {
                Log.d("PhotoStorage", "Deleted image $uuid")
            }
            .addOnFailureListener {
                Log.d("PhotoStorage", "Failed to delete image $uuid")
            }
    }


    fun uuid2StorageReference(userUid: String, uuid: String): StorageReference {
        return photoStorage.child(userUid).child(uuid)
    }
}