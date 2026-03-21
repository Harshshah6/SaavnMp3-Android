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

class SavedLibrariesAdapter(private val data: MutableList<Library?>) :
    RecyclerView.Adapter<SavedLibrariesAdapter.ViewHolder>() {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.setText(data.get(position)!!.name)
        holder.artist.setText(data.get(position)!!.description)
        Picasso.get().load(Uri.parse(data.get(position)!!.image)).into(holder.coverImage)

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val albumItem = AlbumItem(
                data.get(position)!!.name,
                data.get(position)!!.description,
                data.get(position)!!.image,
                data.get(position)!!.id
            )
            if (data.get(position)!!.isCreatedByUser) {
                v!!.getContext().startActivity(
                    Intent(v.getContext(), ListActivity::class.java)
                        .putExtra("id", data.get(position)!!.id)
                        .putExtra("data", Gson().toJson(albumItem))
                        .putExtra("type", "playlist")
                        .putExtra("createdByUser", true)
                )
                return@OnClickListener
            }
            v!!.getContext().startActivity(
                Intent(v.getContext(), ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", if (data.get(position)!!.isAlbum) "album" else "playlist")
                    .putExtra("id", data.get(position)!!.id)
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImage: ImageView?
        val title: TextView
        val artist: TextView

        init {
            coverImage = itemView.findViewById<ImageView?>(R.id.coverImage)
            title = itemView.findViewById<TextView>(R.id.title)
            artist = itemView.findViewById<TextView>(R.id.artist)
        }
    }
}
