package com.harsh.shah.saavnmp3.adapters

import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.databinding.DownloadManagerMoreViewBinding
import com.harsh.shah.saavnmp3.utils.TrackDownloader.DownloadedTrack

class ActivityDownloadManagerListAdapter(private val data: MutableList<DownloadedTrack>) :
    RecyclerView.Adapter<ActivityDownloadManagerListAdapter.ViewHolder?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val _v = View.inflate(
            parent.context,
            if (viewType == 0) R.layout.download_manager_list_item else R.layout.activity_list_shimmer,
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

        (holder.itemView.findViewById<View?>(R.id.title) as TextView).text = item.title
        (holder.itemView.findViewById<View?>(R.id.artist) as TextView).text = item.artist

        if (item.coverImage != null) {
            (holder.itemView.findViewById<View?>(R.id.coverImage) as ImageView).setImageBitmap(item.coverImage)
        }

        holder.itemView.setOnClickListener(View.OnClickListener { view: View? ->
            showDialog(item, view!!)
        })
    }

    private fun showDialog(track: DownloadedTrack, view: View) {
        val bottomSheetDialog =
            BottomSheetDialog(view.context, R.style.MyBottomSheetDialogTheme)
        val _binding =
            DownloadManagerMoreViewBinding.inflate((view.context as Activity).layoutInflater)
        _binding.songTitle.text = track.title
        _binding.songSubTitle.text = track.artist
        _binding.coverImage.setImageBitmap(track.coverImage)
        _binding.albumTitle.text = track.album
        _binding.songYear.text = track.year
        _binding.bitrate.text = track.bitrate + " kbps"
        _binding.duration.text = track.trackLength + " Seconds"
        if (track.trackUID == null || track.trackUID.isEmpty()) _binding.button.visibility = View.GONE
        _binding.button.setOnClickListener(View.OnClickListener { v: View? ->
            v!!.context.startActivity(
                Intent(v.context, MusicOverviewActivity::class.java).putExtra(
                    "type",
                    "clear"
                ).putExtra("id", track.trackUID)
            )
            bottomSheetDialog.dismiss()
        })
        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.show()
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        if (data.get(position).title == "<shimmer>") return 1
        else return 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
