package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
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
        holder.title.text = data[position]!!.name
        holder.artist.text = data[position]!!.description
        Picasso.get().load(data[position]!!.image?.toUri()).into(holder.coverImage)

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val albumItem = AlbumItem(
                data[position]!!.name,
                data[position]!!.description,
                data[position]!!.image,
                data[position]!!.id
            )
            if (data[position]!!.isCreatedByUser) {
                v!!.context.startActivity(
                    Intent(v.context, ListActivity::class.java)
                        .putExtra("id", data[position]!!.id)
                        .putExtra("data", Gson().toJson(albumItem))
                        .putExtra("type", "playlist")
                        .putExtra("createdByUser", true)
                )
                return@OnClickListener
            }
            v!!.context.startActivity(
                Intent(v.context, ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", if (data[position]!!.isAlbum) "album" else "playlist")
                    .putExtra("id", data[position]!!.id)
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
