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
import com.harsh.shah.saavnmp3.adapters.ActivityMainAlbumItemAdapter.ActivityMainAlbumItemAdapterViewHolder
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.squareup.picasso.Picasso

class ActivityMainAlbumItemAdapter(private val data: MutableList<AlbumItem?>) :
    RecyclerView.Adapter<ActivityMainAlbumItemAdapterViewHolder?>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ActivityMainAlbumItemAdapterViewHolder {
        val _v = View.inflate(
            parent.getContext(),
            if (viewType == 0) R.layout.activity_main_songs_item else R.layout.songs_item_shimmer,
            null
        )
        _v.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        return ActivityMainAlbumItemAdapterViewHolder(_v)
    }

    override fun onBindViewHolder(holder: ActivityMainAlbumItemAdapterViewHolder, position: Int) {
        if (getItemViewType(position) == 1) {
            (holder.itemView.findViewById<View?>(R.id.shimmer) as ShimmerFrameLayout).startShimmer()
            return
        }

        (holder.itemView.findViewById<View?>(R.id.albumTitle) as TextView).setText(
            data.get(position)!!.albumTitle()
        )
        (holder.itemView.findViewById<View?>(R.id.albumSubTitle) as TextView).setText(
            data.get(
                position
            )!!.albumSubTitle()
        )

        holder.itemView.findViewById<View?>(R.id.albumTitle).setSelected(true)
        holder.itemView.findViewById<View?>(R.id.albumSubTitle).setSelected(true)

        val coverImage = holder.itemView.findViewById<ImageView?>(R.id.coverImage)
        Picasso.get().load(Uri.parse(data.get(position)!!.albumCover)).into(coverImage)

        holder.itemView.setOnClickListener(View.OnClickListener { v: View? ->
            v!!.getContext().startActivity(
                Intent(v.getContext(), ListActivity::class.java)
                    .putExtra("data", Gson().toJson(data.get(position)))
                    .putExtra("type", "album")
                    .putExtra("id", data.get(position)!!.id)
            )
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position)!!.albumTitle() == "<shimmer>") return 1
        else return 0
    }

    class ActivityMainAlbumItemAdapterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
