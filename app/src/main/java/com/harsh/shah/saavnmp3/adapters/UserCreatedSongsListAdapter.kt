package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.databinding.ActivityListSongItemBinding
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.squareup.picasso.Picasso

class UserCreatedSongsListAdapter(private val data: MutableList<Library.Songs?>) :
    RecyclerView.Adapter<UserCreatedSongsListAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(parent.getContext(), R.layout.activity_list_song_item, null)
        _v.setLayoutParams(
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return ViewHolder(_v)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.title.setText(data.get(position)!!.title)
        holder.binding.artist.setText(data.get(position)!!.description)
        val imageUrl = data.get(position)?.image
        if (imageUrl?.isNotBlank() == true) Picasso.get()
            .load(Uri.parse(imageUrl)).into(holder.binding.coverImage)

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            if (BaseApplicationClass.Companion.trackQueue?.contains(data.get(position)?.id) == true) {
                BaseApplicationClass.Companion.track_position = holder.getBindingAdapterPosition()
            }
            holder.itemView.getContext().startActivity(
                Intent(
                    view!!.getContext(),
                    MusicOverviewActivity::class.java
                ).putExtra("id", data.get(position)!!.id)
            )
        })
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding: ActivityListSongItemBinding

        init {
            binding = ActivityListSongItemBinding.bind(itemView)
        }
    }
}
