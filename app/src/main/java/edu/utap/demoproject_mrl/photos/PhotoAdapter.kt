package edu.utap.demoproject_mrl.photos

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import edu.utap.demoproject_mrl.R

class PhotoAdapter(
    private val photoUris: MutableList<Uri>,
    private val onDelete: (Uri, Int) -> Unit,
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
        val uri = photoUris[position]

        Glide.with(holder.ivPhoto.context)
            .load(uri)
            .centerCrop()
            .into(holder.ivPhoto)

        // ✅ Click photo to expand
        holder.ivPhoto.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onClick(pos)
        }

        // ✅ Delete photo
        holder.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) onDelete(uri, pos)
        }
    }

    fun removeAt(position: Int) {
        photoUris.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemCount() = photoUris.size
}