package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.adapters.ActivityMainPlaylistAdapter.PlaylistAdapterViewHolder
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class ActivityMainPlaylistAdapter(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<PlaylistAdapterViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistAdapterViewHolder {
        val _v = LayoutInflater.from(parent.context).inflate(
            if (viewType == 0) R.layout.activity_main_playlist_item else R.layout.main_playlist_item_shimmer,
            null,
            false
        )
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return PlaylistAdapterViewHolder(_v)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: PlaylistAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).text = data[position]!!.albumTitle()
        val imageView = holder.itemView.findViewById<ImageView?>(R.id.imageView)
        Picasso.get().load(data[position]!!.albumCover?.toUri()).into(imageView)

        holder.itemView.setOnClickListener { v: View? ->
            v!!.context.startActivity(
                Intent(v.context, ListActivity::class.java).putExtra(
                    "data",
                    Gson().toJson(data[position])
                )
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (data[position]!!.id == "<shimmer>") return 1
        return 0
    }

    class PlaylistAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
