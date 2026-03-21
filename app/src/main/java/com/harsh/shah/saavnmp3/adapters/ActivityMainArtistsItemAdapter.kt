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
import com.harsh.shah.saavnmp3.activities.ArtistProfileActivity
import com.harsh.shah.saavnmp3.adapters.ActivityMainArtistsItemAdapter.ActivityMainArtistsItemAdapterViewHolder
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.records.ArtistsSearch
import com.squareup.picasso.Picasso

class ActivityMainArtistsItemAdapter(private val data: MutableList<ArtistsSearch.Data.Results?>) :
    RecyclerView.Adapter<ActivityMainArtistsItemAdapterViewHolder?>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityMainArtistsItemAdapterViewHolder {
        val _v = View.inflate(
            parent.getContext(),
            if (viewType == 0) R.layout.activity_main_artists_item else R.layout.artists_item_shimmer,
            null
        )
        _v.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return ActivityMainArtistsItemAdapterViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ActivityMainArtistsItemAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        holder.itemView.findViewById<View?>(R.id.artist_name).setSelected(true)
        (holder.itemView.findViewById<View?>(R.id.artist_name) as TextView).setText(
            data.get(
                position
            )!!.name()
        )
        val imageView = holder.itemView.findViewById<ImageView?>(R.id.artist_img)
        val images = data[position]?.image
        val url = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (url.isNotEmpty()) {
            Picasso.get().load(Uri.parse(url)).into(imageView)
        }

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            val item = data[position] ?: return@OnClickListener
            val itemImages = item.image
            val itemUrl = if (itemImages.isNullOrEmpty()) "" else itemImages[itemImages.size - 1]?.url ?: ""
            v!!.context.startActivity(
                Intent(v.context, ArtistProfileActivity::class.java)
                    .putExtra(
                        "data",
                        Gson().toJson(
                            BasicDataRecord(
                                item.id,
                                item.name(),
                                "",
                                itemUrl
                            )
                        )
                    )
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.id == "<shimmer>") return 1
        else return 0
    }

    class ActivityMainArtistsItemAdapterViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView)
}
