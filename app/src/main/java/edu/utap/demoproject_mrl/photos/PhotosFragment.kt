package edu.utap.demoproject_mrl.photos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import edu.utap.demoproject_mrl.R

class PhotosFragment : Fragment() {

    private lateinit var adapter: PhotoAdapter
    private val photoStorage = PhotoStorage()
    private val photoDBHelper = PhotoDBHelper()
    private val photoMetas = mutableListOf<PhotoMeta>()
    private lateinit var tvPhotoCount: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_photos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvPhotoCount = view.findViewById(R.id.tvPhotoCount)

        setupAdapter(view)
        loadPhotos()

        view.findViewById<Button>(R.id.btnBackPhotos).setOnClickListener {
            findNavController().navigate(R.id.action_photos_to_home)
        }
    }

    private fun setupAdapter(view: View) {
        adapter = PhotoAdapter(
            photoMetas = photoMetas,
            photoStorage = photoStorage,
            onDelete = { photoMeta, position ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure?")
                    .setPositiveButton("Delete") { _, _ ->
                        val userUid = FirebaseAuth.getInstance()
                            .currentUser?.uid ?: return@setPositiveButton

                        // ✅ Delete from Storage
                        photoStorage.deleteImage(userUid, photoMeta.uuid)

                        // ✅ Delete from Firestore
                        photoDBHelper.removePhotoMeta(photoMeta) {
                            requireActivity().runOnUiThread {
                                photoMetas.removeAt(position)
                                adapter.notifyItemRemoved(position)
                                updatePhotoCount()
                                Toast.makeText(requireContext(),
                                    "Photo deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onClick = { position ->
                val uuidStrings = photoMetas.map { it.uuid }.toTypedArray()
                val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return@PhotoAdapter
                findNavController().navigate(
                    R.id.action_photos_to_fullscreen,
                    bundleOf(
                        "photo_uuids" to uuidStrings,
                        "user_uid" to userUid,
                        "start_index" to position
                    )
                )
            }
        )

        view.findViewById<RecyclerView>(R.id.rvPhotos).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = this@PhotosFragment.adapter
        }
    }

    private fun loadPhotos() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        tvPhotoCount.text = "Loading..."

        photoDBHelper.fetchPhotoMeta(userUid) { metas ->
            requireActivity().runOnUiThread {
                photoMetas.clear()
                photoMetas.addAll(metas)
                adapter.notifyDataSetChanged()
                updatePhotoCount()
            }
        }
    }

    private fun updatePhotoCount() {
        tvPhotoCount.text = "${photoMetas.size} Workout Photo${if (photoMetas.size != 1) "s" else ""}"
    }
}