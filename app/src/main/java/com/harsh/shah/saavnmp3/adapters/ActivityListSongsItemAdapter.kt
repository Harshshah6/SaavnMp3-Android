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
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.squareup.picasso.Picasso


class ActivityListSongsItemAdapter(private val data: MutableList<Song>) :
    RecyclerView.Adapter<ActivityListSongsItemAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.getContext(),
            if (viewType == 0) R.layout.activity_list_song_item else R.layout.activity_list_shimmer,
            null
        )
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        _v.setLayoutParams(layoutParams)
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        val song = data.get(position)

        holder.itemView.findViewById<View?>(R.id.title).setSelected(true)
        holder.itemView.findViewById<View?>(R.id.artist).setSelected(true)

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).setText(song.name())
        val artistsNames = StringBuilder()
        val artistsList = song.artists?.all ?: emptyList()
        for (i in artistsList.indices) {
            val artistName = artistsList[i]?.name() ?: continue
            if (artistsNames.toString().contains(artistName)) continue
            artistsNames.append(artistName)
            artistsNames.append(", ")
        }
        (holder.itemView.findViewById<View?>(R.id.artist) as TextView).setText(artistsNames.toString())

        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (imgUrl.isNotEmpty()) {
            Picasso.get().load(Uri.parse(imgUrl))
                .into((holder.itemView.findViewById<View?>(R.id.coverImage) as ImageView?))
        }

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            BaseApplicationClass.Companion.trackQueue?.clear()
            for (i in data.indices) {
                val id = data[i].id
                if (id != null) {
                    BaseApplicationClass.Companion.trackQueue?.add(id)
                }
            }
            BaseApplicationClass.Companion.track_position = holder.getBindingAdapterPosition()
            holder.itemView.getContext().startActivity(
                Intent(view!!.getContext(), MusicOverviewActivity::class.java).putExtra(
                    "id",
                    song.id
                )
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position).id == "<shimmer>") return 1
        else return 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
