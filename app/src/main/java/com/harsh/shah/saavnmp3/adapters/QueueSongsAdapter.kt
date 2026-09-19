package com.harsh.shah.saavnmp3.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.bumptech.glide.Glide

class QueueSongsAdapter(
    val data: MutableList<Song>,
    private val onSongClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit,
    /** Called whenever a drag-reorder completes so the caller can sync MusicPlayerManager.trackQueue */
    private val onOrderChanged: ((fromIndex: Int, toIndex: Int) -> Unit)? = null
) : RecyclerView.Adapter<QueueSongsAdapter.ViewHolder>() {

    // ── Drag-to-reorder support ───────────────────────────────────────────────

    /** Attach an ItemTouchHelper that allows vertical drag-reordering */
    fun attachDragHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,  // drag directions
            0                                             // no swipe
        ) {
            private var dragFrom = -1
            private var dragTo   = -1

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to   = target.bindingAdapterPosition
                if (dragFrom == -1) dragFrom = from
                dragTo = to
                // Immediately animate the swap in the adapter
                val item = data.removeAt(from)
                data.add(to, item)
                notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // No swipe configured, nothing to do
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)
                // Notify caller only once the drag is fully released
                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {
                    onOrderChanged?.invoke(dragFrom, dragTo)
                }
                dragFrom = -1
                dragTo   = -1
                viewHolder.itemView.alpha = 1f
            }

            override fun onSelectedChanged(
                viewHolder: RecyclerView.ViewHolder?,
                actionState: Int
            ) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    // Slightly dim the dragged item so it feels "lifted"
                    viewHolder?.itemView?.alpha = 0.7f
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(recyclerView)
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = View.inflate(parent.context, R.layout.activity_list_song_item, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = data[position]
        
        val titleText  = holder.itemView.findViewById<TextView>(R.id.title)
        val artistText = holder.itemView.findViewById<TextView>(R.id.artist)
        val coverImage = holder.itemView.findViewById<ImageView>(R.id.coverImage)
        val moreIcon   = holder.itemView.findViewById<ImageView>(R.id.more)
        
        titleText.isSelected  = true
        artistText.isSelected = true
        
        titleText.text = song.name()
        
        val artistsNames = StringBuilder()
        val artistsList  = song.artists?.all ?: emptyList()
        for (i in artistsList.indices) {
            val artistName = artistsList[i]?.name() ?: continue
            if (artistsNames.toString().contains(artistName)) continue
            artistsNames.append(artistName)
            artistsNames.append(", ")
        }
        artistText.text = artistsNames.toString().removeSuffix(", ")
        
        val images = song.image
        val imgUrl = if (images.isNullOrEmpty()) "" else images[images.size - 1]?.url ?: ""
        if (imgUrl.isNotEmpty()) {
            Glide.with(coverImage).load(imgUrl).into(coverImage)
        }
        
        // Use the "more" icon as the remove button
        moreIcon.visibility = View.VISIBLE
        moreIcon.setImageResource(R.drawable.baseline_clear_24)
        moreIcon.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) onRemoveClick(pos)
        }
        
        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) onSongClick(pos)
        }
    }

    override fun getItemCount(): Int = data.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
