
package edu.utap.demoproject_mrl.photos

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import edu.utap.demoproject_mrl.R

class PhotoFullscreenFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_photo_fullscreen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uriStrings = arguments?.getStringArray("photo_uris") ?: return
        val startIndex = arguments?.getInt("start_index", 0) ?: 0
        val uris = uriStrings.map { Uri.parse(it) }

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerFullscreen)
        viewPager.adapter = FullscreenPagerAdapter(uris)
        viewPager.setCurrentItem(startIndex, false)

        view.findViewById<ImageButton>(R.id.btnCloseFullscreen).setOnClickListener {
            findNavController().popBackStack()
        }
    }
}

class FullscreenPagerAdapter(
    private val uris: List<Uri>
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
        Glide.with(holder.ivFullscreen.context)
            .load(uris[position])
            .fitCenter()
            .into(holder.ivFullscreen)
    }

    override fun getItemCount() = uris.size
}