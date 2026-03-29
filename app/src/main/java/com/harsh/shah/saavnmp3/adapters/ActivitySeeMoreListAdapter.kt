package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class ActivitySeeMoreListAdapter : RecyclerView.Adapter<ActivitySeeMoreListAdapter.ViewHolder?> {
    private val data: MutableList<Song?>?

    constructor(data: MutableList<Song?>?) {
        this.data = data
    }

    constructor() {
        this.data = ArrayList<Song?>()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 1) R.layout.activity_artist_profile_view_top_songs_item else R.layout.progress_bar_layout,
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
            //((ShimmerFrameLayout) holder.itemView.findViewById(R.id.shimmer)).startShimmer();
            return
        }

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        val coverTitle = holder.itemView.findViewById<TextView>(R.id.coverTitle)
        val coverPlayed = holder.itemView.findViewById<TextView>(R.id.coverPlayed)
        val positionTextView = holder.itemView.findViewById<TextView>(R.id.position)
        holder.itemView.findViewById<ImageView?>(R.id.more)

        positionTextView.text = (position + 1).toString()
        coverTitle.text = data!![position]!!.name()
        coverPlayed.text = String.format("%s | %s", data[position]!!.year, data[position]!!.label)
        val images = data[position]?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty()) {
            Picasso.get().load(url.toUri()).into(coverImage)
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
        return data?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    fun add(da: Song?) {
        data!!.add(da)
        notifyItemInserted(data.size - 1)
    }

    fun addAll(moveResults: MutableList<Song?>) {
        for (result in moveResults) {
            add(result)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    enum class Mode {
        TOP_SONGS,
        TOP_ALBUMS,
        TOP_SINGLES
    }

    companion object {
        private const val LOADING = 0
        private const val ITEM = 1
    }
}
