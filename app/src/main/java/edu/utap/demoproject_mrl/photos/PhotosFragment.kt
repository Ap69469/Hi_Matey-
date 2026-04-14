package edu.utap.demoproject_mrl.photos

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.utap.demoproject_mrl.R

class PhotosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_photos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoFiles = requireContext()
            .getExternalFilesDir(null)
            ?.listFiles { file -> file.extension == "jpg" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val photoUris = photoFiles.map { Uri.fromFile(it) }

        val adapter = PhotoAdapter(photoUris)
        view.findViewById<RecyclerView>(R.id.rvPhotos).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            this.adapter = adapter
        }

        view.findViewById<Button>(R.id.btnBackPhotos).setOnClickListener {
            findNavController().navigate(R.id.action_photos_to_home)
        }
    }
}