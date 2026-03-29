package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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

        holder.itemView.setOnClickListener { view: View? ->
            if (MusicPlayerManager.trackQueue?.contains(data[position]?.id) == true) {
                MusicPlayerManager.track_position = holder.getBindingAdapterPosition()
            }
            holder.itemView.context.startActivity(
                Intent(
                    view!!.context,
                    MusicOverviewActivity::class.java
                ).putExtra("id", data[position]!!.id)
            )
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ActivityListSongItemBinding = ActivityListSongItemBinding.bind(itemView)
    }
}
