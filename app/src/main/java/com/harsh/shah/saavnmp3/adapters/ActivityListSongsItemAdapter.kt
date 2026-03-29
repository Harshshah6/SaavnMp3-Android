package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.squareup.picasso.Picasso
import androidx.core.net.toUri


class ActivityListSongsItemAdapter(private val data: MutableList<Song>) :
    RecyclerView.Adapter<ActivityListSongsItemAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.activity_list_song_item else R.layout.activity_list_shimmer,
            null
        )
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        _v.layoutParams = layoutParams
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        val song = data[position]

        holder.itemView.findViewById<View>(R.id.title).isSelected = true
        holder.itemView.findViewById<View>(R.id.artist).isSelected = true

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).text = song.name()
        val artistsNames = StringBuilder()
        val artistsList = song.artists?.all ?: emptyList()
        for (i in artistsList.indices) {
            val artistName = artistsList[i]?.name() ?: continue
            if (artistsNames.toString().contains(artistName)) continue
            artistsNames.append(artistName)
            artistsNames.append(", ")
        }
        (holder.itemView.findViewById<View?>(R.id.artist) as TextView).text = artistsNames.toString()

        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (imgUrl.isNotEmpty()) {
            Picasso.get().load(imgUrl.toUri())
                .into((holder.itemView.findViewById<View?>(R.id.coverImage) as ImageView?))
        }

        holder.itemView.setOnClickListener { view: View? ->
            MusicPlayerManager.trackQueue?.clear()
            for (i in data.indices) {
                val id = data[i].id
                if (id != null) {
                    MusicPlayerManager.trackQueue?.add(id)
                }
            }
            MusicPlayerManager.track_position = holder.getBindingAdapterPosition()
            holder.itemView.context.startActivity(
                Intent(view!!.context, MusicOverviewActivity::class.java).putExtra(
                    "id",
                    song.id
                )
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (data[position].id == "<shimmer>") 1 else 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
