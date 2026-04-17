package edu.utap.demoproject_mrl.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import edu.utap.demoproject_mrl.R

class PhotoFullscreenFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_photo_fullscreen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uuids = arguments?.getStringArray("photo_uuids") ?: return
        val userUid = arguments?.getString("user_uid") ?: return
        val startIndex = arguments?.getInt("start_index", 0) ?: 0

        val storage = PhotoStorage()
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerFullscreen)
        viewPager.adapter = FullscreenPagerAdapter(uuids.toList(), userUid, storage)
        viewPager.setCurrentItem(startIndex, false)

        view.findViewById<ImageButton>(R.id.btnCloseFullscreen).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}

class FullscreenPagerAdapter(
    private val uuids: List<String>,
    private val userUid: String,
    private val storage: PhotoStorage
) : RecyclerView.Adapter<FullscreenPagerAdapter.FullscreenViewHolder>() {

    inner class FullscreenViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFullscreen: ImageView = view.findViewById(R.id.ivFullscreenPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FullscreenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_fullscreen, parent, false)
        return FullscreenViewHolder(view)
    }

    override fun onBindViewHolder(holder: FullscreenViewHolder, position: Int) {
        val storageRef = storage.uuid2StorageReference(userUid, uuids[position])
        storageRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(holder.ivFullscreen.context)
                .load(uri.toString())
                .fitCenter()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivFullscreen)
        }
    }

    override fun getItemCount() = uuids.size
}