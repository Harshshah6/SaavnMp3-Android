package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.adapters.ActivityMainAlbumItemAdapter.ActivityMainAlbumItemAdapterViewHolder
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class ActivityMainAlbumItemAdapter(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainAlbumItemAdapterViewHolder?>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityMainAlbumItemAdapterViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_main_songs_item else R.layout.songs_item_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ActivityMainAlbumItemAdapterViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ActivityMainAlbumItemAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        (holder.itemView.findViewById<View?>(R.id.albumTitle) as TextView).text =
            data[position]!!.albumTitle()
        (holder.itemView.findViewById<View?>(R.id.albumSubTitle) as TextView).text = data[position]!!.albumSubTitle()

        holder.itemView.findViewById<View>(R.id.albumTitle).isSelected = true
        holder.itemView.findViewById<View>(R.id.albumSubTitle).isSelected = true

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        Picasso.get().load(data[position]!!.albumCover?.toUri()).into(coverImage)

        holder.itemView.setOnClickListener { v: View? ->
            v!!.context.startActivity(
                Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(data[position]))
                    .putExtra("type", "album")
                    .putExtra("id", data[position]!!.id)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data[position]!!.albumTitle() == "<shimmer>") return 1
        else return 0
    }

    class ActivityMainAlbumItemAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
