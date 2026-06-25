package com.harsh.shah.saavnmp3.utils

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.squareup.picasso.Picasso
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.Toast
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork

object MiniPlayerHelper {
    private val handler = Handler(Looper.getMainLooper())
    private var activeActivity: AppCompatActivity? = null
    
    private val updateRunnable = object : Runnable {
        override fun run() {
            val act = activeActivity
            if (act != null && !act.isFinishing && !act.isDestroyed) {
                updatePlayBar(act)
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    fun initMiniPlayer(activity: AppCompatActivity) {
        val playBar = activity.findViewById<View>(R.id.play_bar_background) 
            ?: activity.findViewById<View>(R.id.mini_player) 
            ?: return
        
        playBar.setOnClickListener {
            if (!MusicPlayerManager.MUSIC_ID.isNullOrBlank()) {
                activity.startActivity(
                    Intent(activity, MusicOverviewActivity::class.java).apply {
                        putExtra("id", MusicPlayerManager.MUSIC_ID)
                    }
                )
            }
        }
        
        activity.findViewById<View>(R.id.play_bar_play_pause_icon)?.setOnClickListener {
            MusicPlayerManager.togglePlayPause()
            handler.postDelayed({ updatePlayBar(activity) }, 100)
        }
        
        activity.findViewById<View>(R.id.play_bar_prev_icon)?.setOnClickListener { view ->
            view.alpha = 0.5f
            view.animate().alpha(1.0f).setDuration(200).start()
            MusicPlayerManager.prevTrack()
            handler.postDelayed({ updatePlayBar(activity) }, 100)
        }
        
        activity.findViewById<View>(R.id.play_bar_next_icon)?.setOnClickListener { view ->
            view.alpha = 0.5f
            view.animate().alpha(1.0f).setDuration(200).start()
            MusicPlayerManager.nextTrack()
            handler.postDelayed({ updatePlayBar(activity) }, 100)
        }

        activity.findViewById<View>(R.id.play_bar_queue_icon)?.setOnClickListener {
            showQueueBottomSheet(activity)
        }
    }
    
    fun onActivityResume(activity: AppCompatActivity) {
        activeActivity = activity
        handler.removeCallbacks(updateRunnable)
        handler.post(updateRunnable)
    }
    
    fun onActivityPause(activity: AppCompatActivity) {
        if (activeActivity == activity) {
            activeActivity = null
            handler.removeCallbacks(updateRunnable)
        }
    }
    
    fun updatePlayBar(activity: AppCompatActivity) {
        val playBar = activity.findViewById<View>(R.id.play_bar_background) 
            ?: activity.findViewById<View>(R.id.mini_player) 
            ?: return
        
        if (MusicPlayerManager.MUSIC_ID.isNullOrBlank()) {
            playBar.visibility = View.GONE
            return
        } else {
            playBar.visibility = View.VISIBLE
        }
        
        val titleText = activity.findViewById<TextView>(R.id.play_bar_music_title)
        val descText = activity.findViewById<TextView>(R.id.play_bar_music_desc)
        val coverImage = activity.findViewById<ImageView>(R.id.play_bar_cover_image)
        val playPauseIcon = activity.findViewById<ImageView>(R.id.play_bar_play_pause_icon)
        val prevIcon = activity.findViewById<ImageView>(R.id.play_bar_prev_icon)
        val nextIcon = activity.findViewById<ImageView>(R.id.play_bar_next_icon)
        
        titleText?.text = MusicPlayerManager.MUSIC_TITLE
        descText?.text = MusicPlayerManager.MUSIC_DESCRIPTION
        
        if (coverImage != null && !MusicPlayerManager.IMAGE_URL.isNullOrBlank()) {
            try {
                Picasso.get().load(MusicPlayerManager.IMAGE_URL!!.toUri()).into(coverImage)
            } catch (e: Exception) {
                Log.e("MiniPlayerHelper", "Error loading image: ${e.message}")
            }
        }
        
        val p = MusicPlayerManager.player
        if (p != null && playPauseIcon != null) {
            playPauseIcon.setImageResource(
                if (p.isPlaying) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px
            )
        }
        
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(MusicPlayerManager.IMAGE_BG_COLOR)
        gradientDrawable.cornerRadius = 18f
        playBar.background = gradientDrawable
        
        titleText?.setTextColor(MusicPlayerManager.TEXT_ON_IMAGE_COLOR1)
        descText?.setTextColor(MusicPlayerManager.TEXT_ON_IMAGE_COLOR1)
        
        val tintList = ColorStateList.valueOf(MusicPlayerManager.TEXT_ON_IMAGE_COLOR)
        playPauseIcon?.imageTintList = tintList
        prevIcon?.imageTintList = tintList
        nextIcon?.imageTintList = tintList
        activity.findViewById<ImageView>(R.id.play_bar_queue_icon)?.imageTintList = tintList
    }

    fun showQueueBottomSheet(activity: AppCompatActivity) {
        val bottomSheetDialog = BottomSheetDialog(activity, R.style.MyBottomSheetDialogTheme)
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_queue, null)
        bottomSheetDialog.setContentView(dialogView)

        val nowPlayingCover = dialogView.findViewById<ImageView>(R.id.now_playing_cover)
        val nowPlayingTitle = dialogView.findViewById<TextView>(R.id.now_playing_title)
        val nowPlayingArtist = dialogView.findViewById<TextView>(R.id.now_playing_artist)
        val queueRecyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.queue_recycler_view)
        val queueProgressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.queue_progress_bar)
        val queueEmptyTv = dialogView.findViewById<TextView>(R.id.queue_empty_tv)

        // Setup Now Playing
        nowPlayingTitle.text = MusicPlayerManager.MUSIC_TITLE
        nowPlayingArtist.text = MusicPlayerManager.MUSIC_DESCRIPTION
        if (!MusicPlayerManager.IMAGE_URL.isNullOrBlank()) {
            try {
                Picasso.get().load(MusicPlayerManager.IMAGE_URL!!.toUri()).into(nowPlayingCover)
            } catch (e: Exception) {}
        }

        queueRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)

        val queue = MusicPlayerManager.trackQueue ?: ArrayList()
        val pos = MusicPlayerManager.track_position
        val upcomingIds = if (pos >= 0 && pos < queue.size - 1) {
            queue.subList(pos + 1, queue.size).filterNotNull()
        } else {
            ArrayList()
        }

        if (upcomingIds.isEmpty()) {
            queueEmptyTv.visibility = View.VISIBLE
            queueRecyclerView.visibility = View.GONE
            queueProgressBar.visibility = View.GONE
        } else {
            queueEmptyTv.visibility = View.GONE
            queueRecyclerView.visibility = View.GONE
            queueProgressBar.visibility = View.VISIBLE

            val idsString = upcomingIds.joinToString(",")
            ApiManager(activity).retrieveSongsByIds(idsString, object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val songResponse = Gson().fromJson(response, SongResponse::class.java)
                        if (songResponse.success && !songResponse.data.isNullOrEmpty()) {
                            val songList = songResponse.data.filterNotNull().toMutableList()
                            
                            queueProgressBar.visibility = View.GONE
                            queueRecyclerView.visibility = View.VISIBLE
                            
                            val adapter = com.harsh.shah.saavnmp3.adapters.QueueSongsAdapter(
                                songList,
                                onSongClick = { itemIndex ->
                                    val absoluteIndex = pos + 1 + itemIndex
                                    if (absoluteIndex < queue.size) {
                                        MusicPlayerManager.track_position = absoluteIndex
                                        MusicPlayerManager.MUSIC_ID = queue[absoluteIndex]
                                        MusicPlayerManager.playTrack()
                                        bottomSheetDialog.dismiss()
                                    }
                                },
                                onRemoveClick = { itemIndex ->
                                    val absoluteIndex = pos + 1 + itemIndex
                                    if (absoluteIndex < queue.size) {
                                        queue.removeAt(absoluteIndex)
                                        songList.removeAt(itemIndex)
                                        queueRecyclerView.adapter?.notifyItemRemoved(itemIndex)
                                        if (songList.isEmpty()) {
                                            queueEmptyTv.visibility = View.VISIBLE
                                            queueRecyclerView.visibility = View.GONE
                                        }
                                        Toast.makeText(activity, "Removed from queue", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            queueRecyclerView.adapter = adapter
                        } else {
                            queueProgressBar.visibility = View.GONE
                            queueEmptyTv.visibility = View.VISIBLE
                        }
                    } catch (e: Exception) {
                        Log.e("MiniPlayerHelper", "Error parsing queue response", e)
                        queueProgressBar.visibility = View.GONE
                        queueEmptyTv.visibility = View.VISIBLE
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    queueProgressBar.visibility = View.GONE
                    queueEmptyTv.visibility = View.VISIBLE
                }
            })
        }

        bottomSheetDialog.show()
    }
}
