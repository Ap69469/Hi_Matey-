package edu.utap.demoproject_mrl.photos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.demoproject_mrl.R
import java.io.File

class PhotosFragment : Fragment() {

    private lateinit var adapter: PhotoAdapter
    private val photoFiles = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_photos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadPhotos(view)

        view.findViewById<Button>(R.id.btnBackPhotos).setOnClickListener {
            findNavController().navigate(R.id.action_photos_to_home)
        }
    }

    private fun loadPhotos(view: View) {
        photoFiles.clear()
        val files = requireContext()
            .getExternalFilesDir(null)
            ?.listFiles { file -> file.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        photoFiles.addAll(files)

        val photoUris = photoFiles.map { file ->
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
        }.toMutableList()

        // Update photo count
        view.findViewById<TextView>(R.id.tvPhotoCount).text =
            "${photoFiles.size} Workout Photo${if (photoFiles.size != 1) "s" else ""}"

        adapter = PhotoAdapter(
            photoUris = photoUris,
            onDelete = { _, position ->
                // ✅ Confirm before deleting
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure you want to delete this photo?")
                    .setPositiveButton("Delete") { _, _ ->
                        photoFiles[position].delete()
                        photoFiles.removeAt(position)
                        adapter.removeAt(position)
                        view.findViewById<TextView>(R.id.tvPhotoCount).text =
                            "${photoFiles.size} Workout Photo${if (photoFiles.size != 1) "s" else ""}"
                        Toast.makeText(requireContext(),
                            "Photo deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            },
            onClick = { position ->
                // ✅ Open fullscreen swipe view
                val uriStrings = photoUris.map { it.toString() }.toTypedArray()
                findNavController().navigate(
                    R.id.action_photos_to_fullscreen,
                    bundleOf(
                        "photo_uris" to uriStrings,
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
}