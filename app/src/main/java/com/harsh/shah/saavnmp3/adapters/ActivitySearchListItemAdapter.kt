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
import com.harsh.shah.saavnmp3.activities.ListActivity
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.model.SearchListItem
import com.squareup.picasso.Picasso

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

        holder.itemView.findViewById<View?>(R.id.title).isSelected = true
        holder.itemView.findViewById<View?>(R.id.artist).isSelected = true

        val item = data.get(position)

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).text = item.title()
        (holder.itemView.findViewById<View?>(R.id.artist) as TextView).text = item.subtitle()

        Picasso.get().load(Uri.parse(item.coverImage))
            .into((holder.itemView.findViewById<View?>(R.id.coverImage) as ImageView?))

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            val intent = Intent()
            intent.putExtra("id", item.id)
            when (item.type) {
                SearchListItem.Type.SONG -> {
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
        })
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position).id == "<shimmer>") return 1
        else return 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
