package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.databinding.ActivityArtistProfileViewTopSongsItemBinding
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.records.AlbumsSearch
import com.squareup.picasso.Picasso

class ActivityArtistProfileTopAlbumsAdapter(private val data: MutableList<AlbumsSearch.Data.Results?>) :
    RecyclerView.Adapter<ActivityArtistProfileTopAlbumsAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.getContext(),
            if (viewType == 1) R.layout.activity_artist_profile_view_top_songs_item else R.layout.artist_profile_view_top_songs_shimmer,
            null
        )
        _v.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return ViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == 0) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        val itemView = ActivityArtistProfileViewTopSongsItemBinding.bind(holder.itemView)

        itemView.position.setText((position + 1).toString())
        itemView.coverTitle.setText(data.get(position)!!.name())
        itemView.coverPlayed.setText(
            String.format("%s | %s", data.get(position)!!.year, data.get(position)!!.language)
        )
        val images = data[position]?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty()) {
            Picasso.get().load(Uri.parse(url)).into(itemView.coverImage)
        }

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            val item = data[position] ?: return@OnClickListener
            val itemImages = item.image
            val itemUrl = if (itemImages.isNullOrEmpty()) "" else itemImages[itemImages.size - 1]?.url ?: ""
            val albumItem = AlbumItem(
                item.id,
                item.name(),
                itemUrl,
                item.id
            )
            holder.itemView.getContext().startActivity(
                Intent(holder.itemView.getContext(), ListActivity::class.java)
                    .putExtra("data", Gson().toJson(albumItem))
                    .putExtra("type", "album")
                    .putExtra("id", data.get(position)!!.id)
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.id == "<shimmer>") return 0
        else return 1
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
