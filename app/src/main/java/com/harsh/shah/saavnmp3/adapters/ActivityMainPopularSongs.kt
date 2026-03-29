package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.squareup.picasso.Picasso
import androidx.core.net.toUri


class ActivityMainPopularSongs(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainPopularSongs.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_main_songs_item else R.layout.songs_item_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        (holder.itemView.findViewById<View?>(R.id.albumTitle) as TextView).text = data[position]!!.albumTitle()
        (holder.itemView.findViewById<View?>(R.id.albumSubTitle) as TextView).text = data[position]!!.albumSubTitle()

        holder.itemView.findViewById<View>(R.id.albumTitle).isSelected = true
        holder.itemView.findViewById<View>(R.id.albumSubTitle).isSelected = true

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        Picasso.get().load(data[position]!!.albumCover?.toUri()).into(coverImage)

        holder.itemView.setOnClickListener { v: View? ->
            MusicPlayerManager.trackQueue?.clear()
            Log.d(
                "AdapterDebug",
                "Click at pos: " + position + ". Populating queue with " + data.size + " items."
            )
            for (i in data.indices) {
                val id = data[i]!!.id
                Log.d(
                    "AdapterDebug",
                    "Queue[" + i + "]: " + id + " - " + data[i]!!.albumTitle()
                )
                MusicPlayerManager.trackQueue?.add(id)
            }
            MusicPlayerManager.track_position = position
            v!!.context.startActivity(
                Intent(v.context, MusicOverviewActivity::class.java).putExtra(
                    "id",
                    data[position]!!.id
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position]!!.albumTitle() == "<shimmer>") 1 else 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
