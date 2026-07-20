package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.squareup.picasso.Picasso
import androidx.core.net.toUri
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import android.widget.Toast

class SavedLibrariesAdapter(private val data: MutableList<Library?>) :
    RecyclerView.Adapter<SavedLibrariesAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        _v.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val library = data[position]!!
        holder.title.text = library.name
        holder.artist.text = library.description

        // Use first song's image if library cover is blank
        val coverUrl = library.image?.takeIf { it.isNotBlank() }
            ?: library.songs?.firstOrNull { it?.image?.isNotBlank() == true }?.image
        if (!coverUrl.isNullOrBlank()) {
            Picasso.get().load(coverUrl.toUri()).into(holder.coverImage)
        }

        // 3-dots more menu
        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        moreIcon?.setOnClickListener { v ->
            val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
            popup.menu.add("Play All")
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "Play All" -> {
                        val songIds = library.songs?.mapNotNull { it?.id } ?: emptyList()
                        if (songIds.isNotEmpty()) {
                            MusicPlayerManager.trackQueue = ArrayList(songIds)
                            MusicPlayerManager.track_position = 0
                            val intent = Intent(v.context, com.harsh.shah.saavnmp3.activities.MusicOverviewActivity::class.java)
                                .putExtra("id", songIds[0])
                            v.context.startActivity(intent)
                        } else {
                            Toast.makeText(v.context, "No songs in this library", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val albumItem = AlbumItem(
                library.name,
                library.description,
                coverUrl,
                library.id
            )
            if (library.isCreatedByUser) {
                v!!.context.startActivity(
                    Intent(v.context, ListActivity::class.java)
                        .putExtra("id", library.id)
                        .putExtra("data", Gson().toJson(albumItem))
                        .putExtra("type", "playlist")
                        .putExtra("createdByUser", true)
                )
                return@OnClickListener
            }
            v!!.context.startActivity(
                Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", if (library.isAlbum) "album" else "playlist")
                    .putExtra("id", library.id)
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView? = itemView.findViewById(R.id.coverImage)
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.artist)
    }
}
