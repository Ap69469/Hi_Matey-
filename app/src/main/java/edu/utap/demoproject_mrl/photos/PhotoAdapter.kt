package edu.utap.demoproject_mrl.photos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import edu.utap.demoproject_mrl.R

class PhotoAdapter(
    private val photoMetas: MutableList<PhotoMeta>,
    private val photoStorage: PhotoStorage,
    private val onDelete: (PhotoMeta, Int) -> Unit,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeletePhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val meta = photoMetas[position]
        val userUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ Click listeners outside the async callback
        holder.ivPhoto.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }

        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(meta, pos)
        }

        // ✅ Load via download URL
        photoStorage.getDownloadUrl(userUid, meta.uuid) { url ->
            Glide.with(holder.ivPhoto.context)
                .load(url)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivPhoto)
        }
    } // ✅ closes onBindViewHolder

    override fun getItemCount() = photoMetas.size
}