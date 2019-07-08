package org.fukutan.libs.example

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PictureListAdapter(private val context: Context, private val itemList: List<String>) : RecyclerView.Adapter<PictureListAdapter.ViewHolder>() {

    private val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val v = inflater.inflate(R.layout.selector_row_thumbnail, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val path = itemList[position]
        Glide
            .with(context)
            .load(path)
            .centerCrop()
            .into(holder.thumbnail)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val thumbnail: ImageView = itemView.findViewById(R.id.image)
    }
}