package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.databinding.ActivityListSongItemBinding
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class UserCreatedSongsListAdapter(private val data: MutableList<Library.Songs?>) :
    RecyclerView.Adapter<UserCreatedSongsListAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.title.text = data[position]!!.title
        holder.binding.artist.text = data[position]!!.description
        val imageUrl = data[position]?.image
        if (imageUrl?.isNotBlank() == true) Picasso.get()
            .load(imageUrl.toUri()).into(holder.binding.coverImage)

        // Wire up the 3-dots menu
        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        moreIcon?.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.menu.add("Play Next")
            popup.menu.add("Add to Queue")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Play Next" -> {
                        MusicPlayerManager.playNext(data[position]?.id)
                        Toast.makeText(v.context, "Song will play next", Toast.LENGTH_SHORT).show()
                        true
                    }
                    "Add to Queue" -> {
                        MusicPlayerManager.addToQueue(data[position]?.id)
                        Toast.makeText(v.context, "Song added to queue", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        holder.itemView.setOnClickListener { view: View? ->
            // Build full queue from list and set correct position
            MusicPlayerManager.trackQueue?.clear()
            var clickIndex = 0
            var validIndex = 0
            for (i in data.indices) {
                val song = data[i]
                if (song != null && !song.id.isNullOrEmpty()) {
                    MusicPlayerManager.trackQueue?.add(song.id)
                    if (i == position) clickIndex = validIndex
                    validIndex++
                }
            }
            MusicPlayerManager.track_position = clickIndex
            holder.itemView.context.startActivity(
                Intent(view!!.context, MusicOverviewActivity::class.java)
                    .putExtra("id", data[position]!!.id)
            )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ActivityListSongItemBinding = ActivityListSongItemBinding.bind(itemView)
    }
}
