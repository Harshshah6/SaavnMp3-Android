package com.harsh.shah.saavnmp3.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.squareup.picasso.Picasso

class QueueSongsAdapter(
    private val data: MutableList<Song>,
    private val onSongClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<QueueSongsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = data[position]
        
        val titleText = holder.itemView.findViewById<TextView>(R.id.title)
        val artistText = holder.itemView.findViewById<TextView>(R.id.artist)
        val coverImage = holder.itemView.findViewById<ImageView>(R.id.coverImage)
        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        
        titleText.isSelected = true
        artistText.isSelected = true
        
        titleText.text = song.name()
        
        val artistsNames = StringBuilder()
        val artistsList = song.artists?.all ?: emptyList()
        for (i in artistsList.indices) {
            val artistName = artistsList[i]?.name() ?: continue
            if (artistsNames.toString().contains(artistName)) continue
            artistsNames.append(artistName)
            artistsNames.append(", ")
        }
        artistText.text = artistsNames.toString().removeSuffix(", ")
        
        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (imgUrl.isNotEmpty()) {
            Picasso.get().load(imgUrl.toUri()).into(coverImage)
        }
        
        // Use the "more" ImageView as a remove/delete button in the queue list
        moreIcon.visibility = View.VISIBLE
        moreIcon.setImageResource(R.drawable.baseline_clear_24)
        moreIcon.setOnClickListener {
            onRemoveClick(position)
        }
        
        holder.itemView.setOnClickListener {
            onSongClick(position)
        }
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
