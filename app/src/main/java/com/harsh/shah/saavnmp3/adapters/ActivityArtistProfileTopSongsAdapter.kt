package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.databinding.ActivityArtistProfileViewTopSongsItemBinding
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class ActivityArtistProfileTopSongsAdapter(private val data: MutableList<Song?>) :
    RecyclerView.Adapter<ActivityArtistProfileTopSongsAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 1) R.layout.activity_artist_profile_view_top_songs_item else R.layout.artist_profile_view_top_songs_shimmer,
            null
        )
        _v.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        val itemView = ActivityArtistProfileViewTopSongsItemBinding.bind(holder.itemView)

        itemView.position.text = (position + 1).toString()
        itemView.coverTitle.text = data.get(position)!!.name()
        itemView.coverPlayed.text = String.format("%s | %s", data[position]!!.year, data[position]!!.label)
        val images = data[position]?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty()) {
            Picasso.get().load(url.toUri()).into(itemView.coverImage)
        }

        holder.itemView.setOnClickListener { view: View? ->
            view!!.context.startActivity(
                Intent(
                    view.context,
                    MusicOverviewActivity::class.java
                ).putExtra("id", data[position]!!.id)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.id == "<shimmer>") return 0
        else return 1
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
