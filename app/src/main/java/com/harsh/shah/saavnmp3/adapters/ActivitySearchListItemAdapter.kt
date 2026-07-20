package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ArtistProfileActivity
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.model.SearchListItem
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.squareup.picasso.Picasso
import androidx.core.net.toUri

class ActivitySearchListItemAdapter(private val data: MutableList<SearchListItem>) :
    RecyclerView.Adapter<ActivitySearchListItemAdapter.ViewHolder?>() {
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

        holder.itemView.findViewById<View>(R.id.title).isSelected = true
        holder.itemView.findViewById<View>(R.id.artist).isSelected = true

        val item = data[position]

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).text = item.title()
        
        val typePrefix = when (item.type) {
            SearchListItem.Type.SONG -> "Song"
            SearchListItem.Type.ALBUM -> "Album"
            SearchListItem.Type.PLAYLIST -> "Playlist"
            SearchListItem.Type.ARTIST -> "Artist"
            else -> ""
        }
        val formattedSubtitle = if (typePrefix.isNotEmpty() && !item.subtitle().isNullOrEmpty()) {
            "$typePrefix • ${item.subtitle()}"
        } else if (typePrefix.isNotEmpty()) {
            typePrefix
        } else {
            item.subtitle()
        }
        (holder.itemView.findViewById<View?>(R.id.artist) as TextView).text = formattedSubtitle

        val coverCard = holder.itemView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.coverCard)
        if (coverCard != null) {
            val radiusRes = if (item.type == SearchListItem.Type.ARTIST) {
                com.intuit.sdp.R.dimen._20sdp
            } else {
                com.intuit.sdp.R.dimen._4sdp
            }
            coverCard.radius = coverCard.context.resources.getDimension(radiusRes)
        }

        val moreIcon = holder.itemView.findViewById<ImageView>(R.id.more)
        if (item.type == SearchListItem.Type.SONG) {
            moreIcon.visibility = View.VISIBLE
            moreIcon.setOnClickListener { v ->
                val popup = androidx.appcompat.widget.PopupMenu(v.context, v)
                popup.menu.add("Play Next")
                popup.menu.add("Add to Queue")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Play Next" -> {
                            MusicPlayerManager.playNext(item.id)
                            android.widget.Toast.makeText(v.context, "Song will play next", android.widget.Toast.LENGTH_SHORT).show()
                            true
                        }
                        "Add to Queue" -> {
                            MusicPlayerManager.addToQueue(item.id)
                            android.widget.Toast.makeText(v.context, "Song added to queue", android.widget.Toast.LENGTH_SHORT).show()
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        } else {
            moreIcon.visibility = View.GONE
        }

        Picasso.get().load(item.coverImage?.toUri())
            .into((holder.itemView.findViewById<View?>(R.id.coverImage) as ImageView?))

        holder.itemView.setOnClickListener {
            val intent = Intent()
            intent.putExtra("id", item.id)
            when (item.type) {
                SearchListItem.Type.SONG -> {
                    MusicPlayerManager.trackQueue?.clear()
                    var clickIndex = 0
                    var validIndex = 0
                    for (i in data.indices) {
                        val searchItem = data[i]
                        if (searchItem.type == SearchListItem.Type.SONG && !searchItem.id.isNullOrEmpty() && searchItem.id != "<shimmer>") {
                            MusicPlayerManager.trackQueue?.add(searchItem.id)
                            if (i == position) {
                                clickIndex = validIndex
                            }
                            validIndex++
                        }
                    }
                    MusicPlayerManager.track_position = clickIndex
                    intent.setClass(holder.itemView.context, MusicOverviewActivity::class.java)
                }

                SearchListItem.Type.ALBUM -> {
                    val albumItem =
                        AlbumItem(item.title(), item.subtitle(), item.coverImage, item.id)
                    intent.putExtra("data", Gson().toJson(albumItem))
                    intent.putExtra("type", "album")
                    intent.setClass(holder.itemView.context, ListActivity::class.java)
                }

                SearchListItem.Type.PLAYLIST -> {
                    val albumItem =
                        AlbumItem(item.title(), item.subtitle(), item.coverImage, item.id)
                    intent.putExtra("data", Gson().toJson(albumItem))
                    intent.setClass(holder.itemView.context, ListActivity::class.java)
                }

                SearchListItem.Type.ARTIST -> {
                    intent.setClass(holder.itemView.context, ArtistProfileActivity::class.java)
                    intent.putExtra(
                        "data",
                        Gson().toJson(BasicDataRecord(item.id, item.title(), "", item.coverImage))
                    )
                }

                else -> {}
            }
            holder.itemView.context.startActivity(intent)
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
