package com.harsh.shah.saavnmp3.adapters

import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.records.AlbumsSearch
import com.squareup.picasso.Picasso

class ActivitySeeMoreSinglesListAdapter :
    RecyclerView.Adapter<ActivitySeeMoreSinglesListAdapter.ViewHolder?> {
    private val data: MutableList<AlbumsSearch.Data.Results?>?

    constructor(data: MutableList<AlbumsSearch.Data.Results?>?) {
        this.data = data
    }

    constructor() {
        this.data = ArrayList<AlbumsSearch.Data.Results?>()
    }

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

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        val coverTitle = holder.itemView.findViewById<TextView>(R.id.coverTitle)
        val coverPlayed = holder.itemView.findViewById<TextView>(R.id.coverPlayed)
        val positionTextView = holder.itemView.findViewById<TextView>(R.id.position)
        val moreImage = holder.itemView.findViewById<ImageView?>(R.id.more)

        positionTextView.setText((position + 1).toString())
        coverTitle.setText(data!!.get(position)!!.name())
        coverPlayed.setText(
            String.format("%s | %s", data.get(position)!!.year, data.get(position)!!.language)
        )
        val images = data?.get(position)?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty()) {
            Picasso.get().load(Uri.parse(url)).into(coverImage)
        }

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            val item = data?.get(position) ?: return@OnClickListener
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
        return if (data == null) 0 else data.size
    }

    override fun getItemViewType(position: Int): Int {
        return 1
    }

    fun add(da: AlbumsSearch.Data.Results?) {
        data!!.add(da)
        notifyItemInserted(data.size - 1)
    }

    fun addAll(moveResults: MutableList<AlbumsSearch.Data.Results?>) {
        for (result in moveResults) {
            add(result)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
