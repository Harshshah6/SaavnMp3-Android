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

        (holder.itemView.findViewById<View?>(R.id.albumTitle) as TextView).text =
            data.get(position)!!.albumTitle()
        (holder.itemView.findViewById<View?>(R.id.albumSubTitle) as TextView).text = data.get(
            position
        )!!.albumSubTitle()

        holder.itemView.findViewById<View?>(R.id.albumTitle).isSelected = true
        holder.itemView.findViewById<View?>(R.id.albumSubTitle).isSelected = true

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        Picasso.get().load(Uri.parse(data.get(position)!!.albumCover)).into(coverImage)

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            MusicPlayerManager.trackQueue?.clear()
            Log.d(
                "AdapterDebug",
                "Click at pos: " + position + ". Populating queue with " + data.size + " items."
            )
            for (i in data.indices) {
                val id = data.get(i)!!.id
                Log.d(
                    "AdapterDebug",
                    "Queue[" + i + "]: " + id + " - " + data.get(i)!!.albumTitle()
                )
                MusicPlayerManager.trackQueue?.add(id)
            }
            MusicPlayerManager.track_position = position
            v!!.context.startActivity(
                Intent(v.context, MusicOverviewActivity::class.java).putExtra(
                    "id",
                    data.get(position)!!.id
                )
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.albumTitle() == "<shimmer>") return 1
        else return 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
