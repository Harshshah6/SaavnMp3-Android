package com.harsh.shah.saavnmp3.activities

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.databinding.ActivityMusicOverviewBinding
import com.harsh.shah.saavnmp3.databinding.MusicOverviewMoreInfoBottomSheetBinding
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.harsh.shah.saavnmp3.services.ActionPlaying
import com.harsh.shah.saavnmp3.services.MusicService
import com.harsh.shah.saavnmp3.services.MusicService.MyBinder
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.harsh.shah.saavnmp3.utils.TrackDownloader
import com.harsh.shah.saavnmp3.utils.TrackDownloader.TrackDownloadListener
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.harsh.shah.saavnmp3.utils.ThemeManager
import com.harsh.shah.saavnmp3.databinding.RecommendationsBottomSheetBinding
import com.harsh.shah.saavnmp3.databinding.SleepTimerBottomSheetBinding
import com.harsh.shah.saavnmp3.adapters.ActivityListSongsItemAdapter
import com.harsh.shah.saavnmp3.utils.RecommendationManager
import com.harsh.shah.saavnmp3.utils.SleepTimerManager
import com.harsh.shah.saavnmp3.activities.SettingsActivity.SettingsSharedPrefManager
import android.graphics.Rect
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration

class MusicOverviewActivity : AppCompatActivity(), ActionPlaying, ServiceConnection {
    private val TAG = "MusicOverviewActivity"

    // private final MediaPlayer mediaPlayer = new MediaPlayer();
    private val handler = Handler()
    var binding: ActivityMusicOverviewBinding? = null
    private var SONG_URL = ""
    private var ID_FROM_EXTRA: String? = ""
    private var IMAGE_URL: String? = ""
    var musicService: MusicService? = null
    private var currentLyricsList: List<Pair<Long, String>>? = null
    private var lyricsAdapter: LyricsAdapter? = null
    private var currentLyricsRecyclerView: androidx.recyclerview.widget.RecyclerView? = null

    inner class LyricsAdapter(private val lyrics: List<Pair<Long, String>>) : androidx.recyclerview.widget.RecyclerView.Adapter<LyricsAdapter.ViewHolder>() {
        var activeIndex = -1

        inner class ViewHolder(val textView: android.widget.TextView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_lyric, parent, false) as android.widget.TextView
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val text = lyrics[position].second
            holder.textView.text = text
            
            if (position == activeIndex) {
                holder.textView.setTypeface(null, android.graphics.Typeface.BOLD)
                holder.textView.setTextColor(resources.getColor(R.color.textMain, null))
                holder.textView.animate().alpha(1f).scaleX(1.1f).scaleY(1.1f).setDuration(200).start()
            } else {
                holder.textView.setTypeface(null, android.graphics.Typeface.NORMAL)
                holder.textView.setTextColor(resources.getColor(R.color.textSec, null))
                holder.textView.animate().alpha(0.5f).scaleX(1f).scaleY(1f).setDuration(200).start()
            }
            
            holder.textView.setOnClickListener {
                MusicPlayerManager.player?.seekTo(lyrics[position].first)
            }
        }

        override fun getItemCount() = lyrics.size

        fun updateTime(timeInMillis: Long, recyclerView: androidx.recyclerview.widget.RecyclerView?) {
            var newIndex = -1
            // Use binary search for efficiency if list is large
            for (i in lyrics.indices) {
                if (lyrics[i].first <= timeInMillis) {
                    newIndex = i
                } else {
                    break
                }
            }
            
            if (newIndex != activeIndex) {
                val oldIndex = activeIndex
                activeIndex = newIndex
                
                if (oldIndex != -1) notifyItemChanged(oldIndex)
                if (activeIndex != -1) {
                    notifyItemChanged(activeIndex)
                    
                    // Keep the active item centered
                    val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
                    if (layoutManager != null) {
                        val smoothScroller = object : androidx.recyclerview.widget.LinearSmoothScroller(recyclerView.context) {
                            override fun calculateDtToFit(viewStart: Int, viewEnd: Int, boxStart: Int, boxEnd: Int, snapPreference: Int): Int {
                                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)
                            }
                            
                            override fun calculateSpeedPerPixel(displayMetrics: android.util.DisplayMetrics): Float {
                                return 100f / displayMetrics.densityDpi
                            }
                        }
                        smoothScroller.targetPosition = activeIndex
                        layoutManager.startSmoothScroll(smoothScroller)
                    }
                }
            }
        }
    }

    private var artistsList: MutableList<SongResponse.Artist> = ArrayList()
    private val isDebugMode = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showData()
    }

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isTouchOverSeekbar = false
    private var velocityTracker: VelocityTracker? = null
    private var touchSlop = 0
    private var isActivityRecreated = false

    // @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        isActivityRecreated = savedInstanceState != null
        binding = ActivityMusicOverviewBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        val primaryColor = ThemeManager.getPrimaryColor(this)
        val onPrimaryColor = ThemeManager.getOnPrimaryColor(this)
        binding!!.PlayPauseIconBg.setCardBackgroundColor(ColorStateList.valueOf(primaryColor))
        binding!!.playPauseImage.imageTintList = ColorStateList.valueOf(onPrimaryColor)
        binding!!.seekbar.progressTintList = ColorStateList.valueOf(primaryColor)
        binding!!.seekbar.thumbTintList = ColorStateList.valueOf(primaryColor)
        binding!!.trackQuality.setTextColor(primaryColor)

        binding!!.title.isSelected = true
        binding!!.description.isSelected = true

        val toggleLyrics = {
            val parent = binding!!.root as android.view.ViewGroup
            val transition = android.transition.Fade()
            transition.duration = 300
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)

            if (binding!!.lyricsRecycler.isVisible) {
                binding!!.lyricsRecycler.visibility = View.GONE
                binding!!.coverImageCard.visibility = View.VISIBLE
                binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textSec, null))
            } else {
                binding!!.lyricsRecycler.visibility = View.VISIBLE
                binding!!.coverImageCard.visibility = View.GONE
                binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textMain, null))

                val p = MusicPlayerManager.player
                if (p != null) {
                    lyricsAdapter?.updateTime(p.currentPosition, currentLyricsRecyclerView)
                }
            }
        }

        binding!!.lyricsIcon.setOnClickListener {
            if (currentLyricsList.isNullOrEmpty()) return@setOnClickListener
            toggleLyrics()
        }

        binding!!.coverImageCard.setOnClickListener {
            val tapLyricsEnabled = SettingsSharedPrefManager(this).tapAlbumLyricsToggle
            if (!tapLyricsEnabled) return@setOnClickListener
            if (currentLyricsList.isNullOrEmpty()) return@setOnClickListener
            toggleLyrics()
        }

        binding!!.queueIcon?.setOnClickListener {
            com.harsh.shah.saavnmp3.utils.MiniPlayerHelper.showQueueBottomSheet(this)
        }

        if ((MusicPlayerManager.trackQueue?.size ?: 0) <= 1) binding!!.shuffleIcon.visibility = View.INVISIBLE

        binding!!.playPauseImage.setOnClickListener(View.OnClickListener { _: View? ->
            try {
                if (MusicPlayerManager.player == null) {
                    Log.e(TAG, "Player is null, cannot toggle playback")
                    Toast.makeText(this, "Media player not ready. Try again.", Toast.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }

                // Toggle play/pause using MusicPlayerManager
                Log.i(TAG, "Play/Pause button clicked")
                MusicPlayerManager.togglePlayPause()

                // Update UI based on new state
                if (MusicPlayerManager.isPlayerPlaying()) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
                updateSeekbar()
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling playback", e)
                Toast.makeText(this, "Error controlling playback. Try again.", Toast.LENGTH_SHORT)
                    .show()
            }
        })

        binding!!.seekbar.max = 100

        binding!!.seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, i: Int, b: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val p = MusicPlayerManager.player
                if (p != null) {
                    val playPosition = ((p.duration / 100)
                            * binding!!.seekbar.progress).toInt()
                    p.seekTo(playPosition.toLong())
                    binding!!.elapsedDuration.text = convertDuration(p.currentPosition)
                }
            }
        })

        // val baseApplicationClass = getApplicationContext() as BaseApplicationClass

        binding!!.nextIcon.setOnClickListener(View.OnClickListener {
            try {
                Log.i(TAG, "Next button clicked")
                if (MusicPlayerManager.player == null) {
                    Log.e(TAG, "Player is null, cannot skip to next track")
                    Toast.makeText(
                        this@MusicOverviewActivity,
                        "Media player not ready",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }

                // Add visual feedback
                binding!!.nextIcon.alpha = 0.5f
                binding!!.nextIcon.animate().alpha(1.0f).setDuration(200).start()

                // Call next track method
                MusicPlayerManager.nextTrack()

                // Update UI state
                updateSeekbar()
                updateTrackInfo()

                // Make sure play icon reflects current state
                if (MusicPlayerManager.player?.isPlaying == true) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }

                if (isDebugMode) Toast.makeText(
                    this@MusicOverviewActivity,
                    "Playing next track",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping to next track", e)
                Toast.makeText(
                    this@MusicOverviewActivity,
                    "Error skipping to next track",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        binding!!.prevIcon.setOnClickListener(View.OnClickListener {
            try {
                Log.i(TAG, "Previous button clicked")
                if (MusicPlayerManager.player == null) {
                    Log.e(TAG, "Player is null, cannot skip to previous track")
                    Toast.makeText(
                        this@MusicOverviewActivity,
                        "Media player not ready",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@OnClickListener
                }

                // Add visual feedback
                binding!!.prevIcon.alpha = 0.5f
                binding!!.prevIcon.animate().alpha(1.0f).setDuration(200).start()

                // If we're already at the beginning of the track, go to previous track
                // Otherwise just restart the current track
                val player = MusicPlayerManager.player
                if (player != null && player.currentPosition > 3000) {
                    player.seekTo(0)
                    if (isDebugMode) Toast.makeText(
                        this@MusicOverviewActivity,
                        "Restarting current track",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                } else {
                    // Call previous track method
                    MusicPlayerManager.prevTrack()
                    if (isDebugMode) Toast.makeText(
                        this@MusicOverviewActivity,
                        "Playing previous track",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // Update UI state
                updateSeekbar()
                updateTrackInfo()

                // Make sure play icon reflects current state
                if (MusicPlayerManager.player?.isPlaying == true) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error going to previous track", e)
                Toast.makeText(
                    this@MusicOverviewActivity,
                    "Error going to previous track",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        binding!!.repeatIcon.setOnClickListener(View.OnClickListener {
            try {
                // Cycle through all three repeat modes
                val player = MusicPlayerManager.player ?: return@OnClickListener
                val currentMode: Int = player.repeatMode
                val newMode: Int
                val modeMessage: String?
                when (currentMode) {
                    Player.REPEAT_MODE_OFF -> {
                        newMode = Player.REPEAT_MODE_ONE
                        modeMessage = "Repeat One"
                    }

                    Player.REPEAT_MODE_ONE -> {
                        newMode = Player.REPEAT_MODE_ALL
                        modeMessage = "Repeat All"
                    }

                    Player.REPEAT_MODE_ALL -> {
                        newMode = Player.REPEAT_MODE_OFF
                        modeMessage = "Repeat Off"
                    }

                    else -> {
                        newMode = Player.REPEAT_MODE_OFF
                        modeMessage = "Repeat Off"
                    }
                }

                player.repeatMode = newMode

                // Update UI to reflect the current mode
                updateRepeatButtonUI()

                if (isDebugMode) Toast.makeText(
                    this@MusicOverviewActivity,
                    modeMessage,
                    Toast.LENGTH_SHORT
                ).show()

                Log.i(TAG, "Repeat mode changed to: $newMode")
            } catch (e: Exception) {
                Log.e(TAG, "Error changing repeat mode", e)
                Toast.makeText(
                    this@MusicOverviewActivity,
                    "Error changing repeat mode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        binding!!.shuffleIcon.setOnClickListener {
            val player = MusicPlayerManager.player
            val primaryColor = ThemeManager.getPrimaryColor(this)
            if (player != null) {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                if (player.shuffleModeEnabled) binding!!.shuffleIcon.imageTintList =
                    ColorStateList.valueOf(primaryColor)
                else binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(
                    resources.getColor(R.color.textSec, null)
                )
            }
            if (isDebugMode) Toast.makeText(
                this@MusicOverviewActivity,
                "Shuffle Mode Changed.",
                Toast.LENGTH_SHORT
            ).show()
        }


        binding!!.shareIcon.setOnClickListener(View.OnClickListener {
            if (SHARE_URL.isBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, SHARE_URL)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        binding!!.moreIcon.setOnClickListener(View.OnClickListener {
            val bottomSheetDialog = BottomSheetDialog(
                this@MusicOverviewActivity,
                R.style.MyBottomSheetDialogTheme
            )
            val _binding = MusicOverviewMoreInfoBottomSheetBinding
                .inflate(layoutInflater)
            _binding.albumTitle.text = binding!!.title.text.toString()
            _binding.albumSubTitle.text = binding!!.description.text.toString()
            Glide.with(_binding.coverImage)
                .load(IMAGE_URL)
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(_binding.coverImage)
            val linearLayout = _binding.main

            _binding.goToAlbum.setOnClickListener(View.OnClickListener {
                if (mSongResponse == null) return@OnClickListener
                val song = mSongResponse!!.data?.get(0)
                if (song?.album == null) return@OnClickListener
                val album = song.album
                startActivity(
                    Intent(this@MusicOverviewActivity, ListActivity::class.java)
                        .putExtra("type", "album")
                        .putExtra("id", album.id)
                        .putExtra("data", Gson().toJson(AlbumItem(album.name(), "", "", album.id)))
                )
            })

            _binding.addToLibrary.setOnClickListener(View.OnClickListener {
                val sharedPreferenceManager: SharedPreferenceManager =
                    SharedPreferenceManager.getInstance(this@MusicOverviewActivity)
                var savedLibraries = sharedPreferenceManager.savedLibrariesData
                if (savedLibraries == null) savedLibraries = SavedLibraries(ArrayList())
                val lists = savedLibraries.lists ?: emptyList()
                if (lists.isEmpty()) {
                    Snackbar.make(_binding.getRoot(), "No Libraries Found", Snackbar.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }
                val userCreatedLibraries: MutableList<String?> = ArrayList()
                for (library in lists) {
                    if (library != null && library.isCreatedByUser) userCreatedLibraries.add(library.name)
                }

                val materialAlertDialogBuilder = getMaterialAlertDialogBuilder(
                    userCreatedLibraries, savedLibraries, sharedPreferenceManager
                )
                materialAlertDialogBuilder.show()
            })

            val song = mSongResponse!!.data?.get(0) ?: return@OnClickListener

            val currentId = MusicPlayerManager.MUSIC_ID ?: ID_FROM_EXTRA
            val isLiked = if (!currentId.isNullOrEmpty()) {
                SharedPreferenceManager.getInstance(this@MusicOverviewActivity).isSongLiked(currentId)
            } else false
            val textMainColor = resources.getColor(R.color.textMain, null)

            if (isLiked) {
                _binding.favorite.titleTextView?.text = "Remove from Liked Songs"
                _binding.favorite.iconImageView?.setImageResource(R.drawable.favorite_24px)
                _binding.favorite.iconImageView?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.red, null))
            } else {
                _binding.favorite.titleTextView?.text = "Add to Liked Songs"
                _binding.favorite.iconImageView?.setImageResource(R.drawable.favorite_outline_24px)
                _binding.favorite.iconImageView?.imageTintList = ColorStateList.valueOf(textMainColor)
            }

            _binding.favorite.setOnClickListener {
                if (currentId.isNullOrEmpty()) return@setOnClickListener
                val title = song.name()
                val desc = song.artists?.all?.joinToString(", ") { it?.name() ?: "" } ?: binding!!.description.text.toString()
                val img = IMAGE_URL ?: ""
                val songItem = SavedLibraries.Library.Songs(currentId, title, desc, img)
                val nowLiked = SharedPreferenceManager.getInstance(this@MusicOverviewActivity).toggleLikedSong(songItem)

                if (nowLiked) {
                    _binding.favorite.titleTextView?.text = "Remove from Liked Songs"
                    _binding.favorite.iconImageView?.setImageResource(R.drawable.favorite_24px)
                    _binding.favorite.iconImageView?.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.red, null))
                } else {
                    _binding.favorite.titleTextView?.text = "Add to Liked Songs"
                    _binding.favorite.iconImageView?.setImageResource(R.drawable.favorite_outline_24px)
                    _binding.favorite.iconImageView?.imageTintList = ColorStateList.valueOf(textMainColor)
                }

                val msg = if (nowLiked) "Added to Liked Songs" else "Removed from Liked Songs"
                Snackbar.make(binding!!.root, msg, Snackbar.LENGTH_SHORT).show()
            }

            updateDownloadButtonUI(song.name(), _binding)

            _binding.download.setOnClickListener(View.OnClickListener { v: View? ->
                startDownloadTrack(song, _binding)
            })

            _binding.recommendations.setOnClickListener {
                bottomSheetDialog.dismiss()
                showRecommendationsBottomSheet()
            }

            _binding.sleepTimer.setOnClickListener {
                bottomSheetDialog.dismiss()
                showSleepTimerBottomSheet()
            }

            _binding.playbackSpeed.setOnClickListener {
                bottomSheetDialog.dismiss()
                showPlaybackSpeedDialog()
            }

            _binding.equalizer.setOnClickListener {
                bottomSheetDialog.dismiss()
                openEqualizer()
            }

            for (artist in artistsList) {
                try {
                    val images = artist.image
                    val imgUrl = if (images.isNullOrEmpty())
                        ""
                    else
                        images.get(images.size - 1)?.url ?: ""
                    val bottomSheetItemView = BottomSheetItemView(
                        this@MusicOverviewActivity,
                        artist.name(), imgUrl, artist.id
                    )
                    bottomSheetItemView.setFocusable(true)
                    bottomSheetItemView.isClickable = true
                    bottomSheetItemView.setOnClickListener {
                        Log.i(TAG, "BottomSheetItemView: onCLicked!")
                        startActivity(
                            Intent(this@MusicOverviewActivity, ArtistProfileActivity::class.java)
                                .putExtra(
                                    "data", Gson().toJson(
                                        BasicDataRecord(artist.id, artist.name(), "", imgUrl)
                                    )
                                )
                        )
                    }
                    linearLayout.addView(bottomSheetItemView)
                } catch (e: Exception) {
                    Log.e(TAG, "BottomSheetDialog: ", e)
                }
            }
            bottomSheetDialog.setContentView(_binding.getRoot())
            bottomSheetDialog.create()
            bottomSheetDialog.show()
        })

        binding!!.trackQuality.setOnClickListener { view: View? ->
            val popupMenu = PopupMenu(this@MusicOverviewActivity, view!!)
            popupMenu.menuInflater.inflate(R.menu.track_quality_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem: MenuItem? ->
                Toast.makeText(
                    this@MusicOverviewActivity,
                    menuItem!!.title,
                    Toast.LENGTH_SHORT
                ).show()
                // Objects.requireNonNull(menuItem.getTitle());
                MusicPlayerManager.setTrackQuality(menuItem.title.toString())
                onSongFetched(mSongResponse!!, true)
                prepareMediaPLayer()
                binding!!.trackQuality.text = MusicPlayerManager.TRACK_QUALITY
                true
            }
            popupMenu.show()
        }

        binding!!.trackQuality.text = MusicPlayerManager.TRACK_QUALITY

        showData()

        updateTrackInfo()
    }

    private fun getMaterialAlertDialogBuilder(
        userCreatedLibraries: MutableList<String?>,
        savedLibraries: SavedLibraries, sharedPreferenceManager: SharedPreferenceManager
    ): MaterialAlertDialogBuilder {
        val materialAlertDialogBuilder = MaterialAlertDialogBuilder(
            this@MusicOverviewActivity
        )
        val listAdapter: ListAdapter = ArrayAdapter<String?>(
            this@MusicOverviewActivity, android.R.layout.simple_list_item_1,
            userCreatedLibraries
        )
        materialAlertDialogBuilder.setAdapter(
            listAdapter,
            DialogInterface.OnClickListener { _: DialogInterface?, i: Int ->
                // index = i;
                Log.i(TAG, "pickedLibrary: $i")

                val song = mSongResponse!!.data?.get(0) ?: return@OnClickListener

                val songs = Library.Songs(
                    song.id,
                    song.name(),
                    binding!!.description.text.toString(),
                    IMAGE_URL
                )

                savedLibraries.lists?.get(i)?.songs?.add(songs)
                sharedPreferenceManager.savedLibrariesData = savedLibraries
                Toast.makeText(
                    this@MusicOverviewActivity, "Added to " + savedLibraries.lists?.get(i)?.name,
                    Toast.LENGTH_SHORT
                ).show()
            })

        materialAlertDialogBuilder.setTitle("Select Library")
        return materialAlertDialogBuilder
    }

    override fun onResume() {
        super.onResume()

        // Set as current activity in ApplicationClass
        BaseApplicationClass.currentActivity = this

        // Bind to the service
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)

        // Update UI with current playback state
        if (MusicPlayerManager.player != null) {
            updateTrackInfo()
            updateSeekbar()
        }
    }

    override fun onPause() {
        super.onPause()

        // Remove callbacks to prevent leaks
        handler.removeCallbacks(runnable)
        mHandler.removeCallbacks(mUpdateTimeTask)

        // Unbind from service
        try {
            unbindService(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding service", e)
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure we're not updating UI when activity is in background
        handler.removeCallbacks(runnable)
        mHandler.removeCallbacks(mUpdateTimeTask)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup
        handler.removeCallbacks(runnable)
        mHandler.removeCallbacks(mUpdateTimeTask)
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MyBinder
        musicService = binder.service
        musicService!!.setCallback(this@MusicOverviewActivity)
        Log.i(TAG, "onServiceConnected: ")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        Log.e(TAG, "onServiceDisconnected: ")
        musicService = null
    }

    private var SHARE_URL = ""

    fun showData() {
        if (intent.extras == null) return
        val apiManager = ApiManager(this)
        val ID = intent.extras!!.getString("id", "")
        ID_FROM_EXTRA = ID
        // ((ApplicationClass)getApplicationContext()).setMusicDetails(null,null,null,ID);
        if (MusicPlayerManager.currentlyPlayingTrackId == ID || MusicPlayerManager.MUSIC_ID == ID) {
            updateSeekbar()
            if (MusicPlayerManager.player?.isPlaying == true) binding!!.playPauseImage.setImageResource(
                R.drawable.baseline_pause_24
            )
            else binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
        }

        val requestListener: RequestNetwork.RequestListener =
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val songResponse =
                        Gson().fromJson<SongResponse>(response, SongResponse::class.java)
                    if (songResponse.success) {
                        onSongFetched(songResponse)
                        SharedPreferenceManager.getInstance(this@MusicOverviewActivity)
                            .setSongResponseById(
                                ID,
                                songResponse
                            )
                    } else {
                        val cached = SharedPreferenceManager.getInstance(this@MusicOverviewActivity).getSongResponseById(ID)
                        if (cached != null) {
                            onSongFetched(cached)
                        } else {
                            finish()
                        }
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    val cached = SharedPreferenceManager.getInstance(this@MusicOverviewActivity).getSongResponseById(ID)
                    if (cached != null) {
                        onSongFetched(cached)
                    } else {
                        Toast.makeText(this@MusicOverviewActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

        if (intent.extras!!.getString("type", "") == "clear") {
            MusicPlayerManager.trackQueue = ArrayList(mutableListOf<String?>(ID))
        }
        if ((ID.startsWith("http") || ID.startsWith("www")) && ID.contains("jiosaavn.com")) {
            apiManager.retrieveSongByLink(ID, requestListener)
        } else {
            val cached = SharedPreferenceManager.getInstance(this@MusicOverviewActivity).getSongResponseById(ID)
            if (cached != null) {
                onSongFetched(cached)
            } else {
                apiManager.retrieveSongById(ID, null, requestListener)
            }
        }
    }

    private var mSongResponse: SongResponse? = null

    private fun onSongFetched(songResponse: SongResponse, forced: Boolean = false) {
        mSongResponse = songResponse
        MusicPlayerManager.CURRENT_TRACK = mSongResponse
        val song = songResponse.data?.get(0) ?: return
        binding!!.title.text = song.name()
        binding!!.description.text = String.format(
            "%s plays | %s | %s",
            convertPlayCount(song.playCount ?: 0),
            song.year,
            song.copyright
        )
        val image = song.image
        IMAGE_URL = if (!image.isNullOrEmpty()) image[image.size - 1]?.url ?: "" else ""
        SHARE_URL = song.url ?: ""
        if (IMAGE_URL!!.isNotEmpty()) {
            Glide.with(binding!!.coverImage)
                .load(IMAGE_URL)
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(binding!!.coverImage)
        } else {
            binding!!.coverImage.setImageResource(R.drawable.baseline_album_24)
        }
        val downloadUrls = song.downloadUrl

        artistsList = song.artists?.primary?.filterNotNull()?.toMutableList() ?: mutableListOf()

        SONG_URL = MusicPlayerManager.getDownloadUrl(downloadUrls)

        val fromMiniPlayer = intent.getBooleanExtra("from_mini_player", false)
        val isActivelyPlaying = MusicPlayerManager.isPlayerPlaying()
        val isPlayerActive = MusicPlayerManager.player != null &&
                MusicPlayerManager.player?.playbackState != Player.STATE_IDLE &&
                MusicPlayerManager.player?.playbackState != Player.STATE_ENDED
        val isSameSong = (MusicPlayerManager.currentlyPlayingTrackId == ID_FROM_EXTRA || MusicPlayerManager.MUSIC_ID == ID_FROM_EXTRA)

        val shouldPlay: Boolean
        if (forced) {
            shouldPlay = true
        } else if (fromMiniPlayer || isActivityRecreated) {
            shouldPlay = false
        } else if (isSameSong && isPlayerActive) {
            if (!isActivelyPlaying) {
                MusicPlayerManager.player?.play()
                MusicPlayerManager.showNotification()
                MusicPlayerManager.updateWidget()
            }
            shouldPlay = false
        } else {
            shouldPlay = true
        }

        if (shouldPlay) {
            MusicPlayerManager.setMusicDetails(
                IMAGE_URL, binding!!.title.text.toString(),
                binding!!.description.text.toString(), ID_FROM_EXTRA
            )
            MusicPlayerManager.SONG_URL = SONG_URL
            prepareMediaPLayer()
        } else {
            val p = MusicPlayerManager.player
            if (p != null && p.duration > 0) {
                binding!!.totalDuration.text = convertDuration(p.duration)
            }
            if (MusicPlayerManager.isPlayerPlaying()) {
                binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
            }
            updateSeekbar()
        }

        updateDownloadButtonUI(song.name())
        fetchLyricsForCurrentSong(song)
    }

    private fun fetchLyricsForCurrentSong(song: SongResponse.Song) {
        val wasVisible = binding!!.lyricsRecycler.isVisible

        binding!!.lyricsIcon.visibility = View.GONE
        binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textSec, null))
        
        // Handle both possible IDs if naming was inconsistent, but we've standardized to lyrics_recycler
        val recView = binding!!.lyricsRecycler
        recView.visibility = View.GONE
        binding!!.coverImageCard.visibility = View.VISIBLE
        
        currentLyricsList = null
        lyricsAdapter = null
        currentLyricsRecyclerView = null
        
        val durationInSecs = (song.duration ?: -1.0).toInt()
        val artistName = if (!song.artists?.primary.isNullOrEmpty()) song.artists.primary[0]?.name() ?: "" else ""
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fetchedLyrics = com.samyak.lrclib.LrcLib.getLyrics(
                    title = song.name(),
                    artist = artistName,
                    duration = durationInSecs
                ).getOrNull()

                withContext(Dispatchers.Main) {
                    if (!fetchedLyrics.isNullOrEmpty()) {
                        val sentencesMap = com.samyak.lrclib.LrcLib.Lyrics(fetchedLyrics).sentences
                        if (!sentencesMap.isNullOrEmpty()) {
                            currentLyricsList = sentencesMap.toList()
                            Log.i(TAG, "Fetched ${currentLyricsList?.size} lyric lines")
                            
                            lyricsAdapter = LyricsAdapter(currentLyricsList!!)
                            val rv = binding!!.lyricsRecycler
                            run {
                                rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MusicOverviewActivity)
                                rv.adapter = lyricsAdapter
                                currentLyricsRecyclerView = rv
                                binding!!.lyricsIcon.visibility = View.VISIBLE

                                if (wasVisible) {
                                    binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textMain, null))
                                    rv.visibility = View.VISIBLE
                                    binding!!.coverImageCard.visibility = View.GONE
                                }
                            }
                        }
                    } else {
                        Log.i(TAG, "No lyrics found for this song")
                        // Optional: Toast or hint to user
                        // Toast.makeText(this@MusicOverviewActivity, "Lyrics not found", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch lyrics", e)
            }
        }
    }



    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val gesturesEnabled = SettingsSharedPrefManager(this).gesturesEnabled
        if (!gesturesEnabled) {
            return super.dispatchTouchEvent(ev)
        }

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.rawX
                touchDownY = ev.rawY

                val seekbar = binding?.seekbar
                if (seekbar != null && seekbar.isShown) {
                    val rect = Rect()
                    seekbar.getGlobalVisibleRect(rect)
                    rect.inset(0, -(16 * resources.displayMetrics.density).toInt())
                    if (rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        isTouchOverSeekbar = true
                        return super.dispatchTouchEvent(ev)
                    }
                }
                isTouchOverSeekbar = false

                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isTouchOverSeekbar) {
                    return super.dispatchTouchEvent(ev)
                }
                velocityTracker?.addMovement(ev)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTouchOverSeekbar) {
                    isTouchOverSeekbar = false
                    return super.dispatchTouchEvent(ev)
                }

                velocityTracker?.addMovement(ev)
                velocityTracker?.computeCurrentVelocity(1000)
                val velY = velocityTracker?.yVelocity ?: 0f
                val velX = velocityTracker?.xVelocity ?: 0f
                velocityTracker?.recycle()
                velocityTracker = null

                val deltaX = ev.rawX - touchDownX
                val deltaY = ev.rawY - touchDownY
                val absX = kotlin.math.abs(deltaX)
                val absY = kotlin.math.abs(deltaY)
                val swipeThreshold = 80 * resources.displayMetrics.density
                val velocityThreshold = 600f

                // Downward swipe → cleanly minimize player via onBackPressed
                if (deltaY > swipeThreshold && deltaY > absX * 1.5f && (deltaY > 100 || velY > velocityThreshold)) {
                    val lyricsRecycler = binding?.lyricsRecycler
                    val isLyricsVisible = lyricsRecycler?.isVisible == true
                    val canScrollLyricsUp = lyricsRecycler?.canScrollVertically(-1) == true

                    if (!isLyricsVisible || !canScrollLyricsUp) {
                        onBackPressedDispatcher.onBackPressed()
                        return true
                    }
                }

                // Horizontal swipe left → next track
                if (absX > absY * 1.5f && (deltaX < -swipeThreshold || velX < -velocityThreshold)) {
                    binding?.nextIcon?.let { icon ->
                        icon.alpha = 0.4f
                        icon.animate()?.alpha(1f)?.setDuration(200)?.start()
                    }
                    MusicPlayerManager.nextTrack()
                    return true
                }

                // Horizontal swipe right → previous track
                if (absX > absY * 1.5f && (deltaX > swipeThreshold || velX > velocityThreshold)) {
                    binding?.prevIcon?.let { icon ->
                        icon.alpha = 0.4f
                        icon.animate()?.alpha(1f)?.setDuration(200)?.start()
                    }
                    MusicPlayerManager.prevTrack()
                    return true
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    private fun updateDownloadButtonUI(songName: String?, sheetBinding: MusicOverviewMoreInfoBottomSheetBinding? = null) {
        val isDownloaded = TrackDownloader.isAlreadyDownloaded(songName)
        val primaryColor = ThemeManager.getPrimaryColor(this)
        val textMainColor = resources.getColor(R.color.textMain, null)

        if (sheetBinding != null) {
            if (isDownloaded) {
                sheetBinding.download.titleTextView?.text = "Downloaded"
                sheetBinding.download.iconImageView?.setImageResource(R.drawable.round_done_24)
                sheetBinding.download.iconImageView?.imageTintList = ColorStateList.valueOf(primaryColor)
            } else {
                sheetBinding.download.titleTextView?.text = "Download"
                sheetBinding.download.iconImageView?.setImageResource(R.drawable.baseline_download_for_offline_24)
                sheetBinding.download.iconImageView?.imageTintList = ColorStateList.valueOf(textMainColor)
            }
        }
    }

    private fun startDownloadTrack(song: SongResponse.Song, sheetBinding: MusicOverviewMoreInfoBottomSheetBinding? = null) {
        if (TrackDownloader.isAlreadyDownloaded(song.name())) {
            MaterialAlertDialogBuilder(this@MusicOverviewActivity)
                .setTitle("Downloaded Song")
                .setMessage("\"${song.name()}\" is downloaded on this device.")
                .setPositiveButton("Open Downloads") { _, _ ->
                    startActivity(
                        Intent(
                            this@MusicOverviewActivity,
                            DownloadManagerActivity::class.java
                        )
                    )
                }
                .setNegativeButton("Delete Song") { _, _ ->
                    if (TrackDownloader.deleteDownloadedSong(this@MusicOverviewActivity, song.name())) {
                        Toast.makeText(this@MusicOverviewActivity, "Song deleted from device", Toast.LENGTH_SHORT).show()
                        updateDownloadButtonUI(song.name(), sheetBinding)
                    } else {
                        Toast.makeText(this@MusicOverviewActivity, "Failed to delete song", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNeutralButton("Cancel", null)
                .show()
            return
        }

        val progressDialog = ProgressDialog(this@MusicOverviewActivity)
        progressDialog.setMessage("Downloading \"${song.name()}\"...")
        progressDialog.setCancelable(true)
        progressDialog.setCanceledOnTouchOutside(true)
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { _, _ ->
            TrackDownloader.cancelDownload(this@MusicOverviewActivity)
            Toast.makeText(this@MusicOverviewActivity, "Download cancelled", Toast.LENGTH_SHORT).show()
        }
        progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Download in Background") { dialog, _ ->
            dialog.dismiss()
            Snackbar.make(binding!!.root, "Downloading in background. Check notifications.", Snackbar.LENGTH_LONG)
                .setAction("Cancel") {
                    TrackDownloader.cancelDownload(this@MusicOverviewActivity)
                }.show()
        }
        progressDialog.setOnCancelListener {
            Snackbar.make(binding!!.root, "Downloading in background. Check notifications.", Snackbar.LENGTH_LONG)
                .setAction("Cancel") {
                    TrackDownloader.cancelDownload(this@MusicOverviewActivity)
                }.show()
        }

        val settingsMgr = SettingsSharedPrefManager(this@MusicOverviewActivity)
        val downloadFormat = TrackDownloader.DownloadFormat.fromExtension(
            settingsMgr.downloadFormat
        )
        TrackDownloader.downloadAndEmbedMetadata(
            this@MusicOverviewActivity,
            song,
            downloadFormat,
            object : TrackDownloadListener {
                override fun onStarted() {
                    if (!isFinishing && !isDestroyed) {
                        progressDialog.show()
                    }
                }

                override fun onFinished() {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                        Toast.makeText(
                            this@MusicOverviewActivity, "Successfully Downloaded.",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateDownloadButtonUI(song.name(), sheetBinding)
                    }
                }

                override fun onError(errorMessage: String?) {
                    if (progressDialog.isShowing) {
                        progressDialog.dismiss()
                    }
                    if (errorMessage != "Cancelled") {
                        Toast.makeText(
                            this@MusicOverviewActivity,
                            "Download error: $errorMessage",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    updateDownloadButtonUI(song.name(), sheetBinding)
                }
            }
        )
    }

    fun backPress(view: View?) {
        finish()
    }

    @OptIn(markerClass = [UnstableApi::class])
    fun prepareMediaPLayer() {
        try {
            MusicPlayerManager.prepareMediaPlayer()

            // Wait until player is actually ready
            val p = MusicPlayerManager.player
            if (p != null && p.duration > 0) {
                binding!!.totalDuration.text = convertDuration(p.duration)
            } else {
                // If duration is not yet available, set a default or retry
                binding!!.totalDuration.text = "00:00"
                // Schedule a retry to get the duration
                Handler(Looper.getMainLooper()).postDelayed({
                    val p2 = MusicPlayerManager.player
                    if (p2 != null && p2.duration > 0) {
                        binding!!.totalDuration.text = convertDuration(p2.duration)
                    }
                }, 500)
            }

            // Set play state
            if (MusicPlayerManager.player?.isPlaying == true) {
                binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
            }
            updateSeekbar()

            // Update notification
            // showNotification(BaseApplicationClass.player.isPlaying() ?
            // R.drawable.baseline_pause_24
            // : R.drawable.play_arrow_24px);
            // BaseApplicationClass handles notification updates
        } catch (e: Exception) {
            Log.e(TAG, "Error preparing media player", e)
            // Try to recover
            Toast.makeText(this, "Error playing track. Retrying...", Toast.LENGTH_SHORT).show()
            Handler().postDelayed(Runnable { this.prepareMediaPLayer() }, 1000)
        }
    }

    private val runnable = Runnable { this.updateSeekbar() }

    fun updateSeekbar() {
        handler.removeCallbacks(runnable)
        try {
            if (MusicPlayerManager.player == null) {
                Log.e(TAG, "Player is null in updateSeekbar")
                return
            }

            val p = MusicPlayerManager.player ?: return
            
            val duration: Long = p.duration
            val currentPosition: Long = p.currentPosition

            if (duration > 0) {
                val progress = ((currentPosition.toFloat() / duration) * 100).toInt()
                binding!!.seekbar.progress = progress
                binding!!.elapsedDuration.text = convertDuration(currentPosition)
                
                lyricsAdapter?.updateTime(currentPosition, currentLyricsRecyclerView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in updateSeekbar", e)
        }
        handler.postDelayed(runnable, 250)
    }

    private val mHandler = Handler()
    private val mUpdateTimeTask = Runnable { this.updateTrackInfo() }

    private fun updateTrackInfo() {
        mHandler.removeCallbacks(mUpdateTimeTask)
        
        val currentTitle = binding!!.title.text.toString()
        if (currentTitle != MusicPlayerManager.MUSIC_TITLE) {
            binding!!.title.text = MusicPlayerManager.MUSIC_TITLE
            
            val currentTrackData = MusicPlayerManager.CURRENT_TRACK?.data?.getOrNull(0)
            if (currentTrackData != null && currentTitle.isNotEmpty()) {
                fetchLyricsForCurrentSong(currentTrackData)
            }
        }
        
        if (binding!!.description.text.toString() != MusicPlayerManager.MUSIC_DESCRIPTION) {
            binding!!.description.text = MusicPlayerManager.MUSIC_DESCRIPTION
        }
        if (!MusicPlayerManager.IMAGE_URL.isNullOrBlank()) {
            Glide.with(binding!!.coverImage)
                .load(MusicPlayerManager.IMAGE_URL)
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(binding!!.coverImage)
        } else {
            binding!!.coverImage.setImageResource(R.drawable.baseline_album_24)
        }
        val p = MusicPlayerManager.player ?: return
        binding!!.seekbar.progress = ((p.currentPosition.toFloat() / p.duration) * 100).toInt()

        binding!!.seekbar.secondaryProgress = ((p.bufferedPosition.toFloat() / p.duration) * 100).toInt()

        val currentDuration: Long = p.currentPosition
        binding!!.elapsedDuration.text = convertDuration(currentDuration)

        if (binding!!.totalDuration.text.toString()
            != convertDuration(p.duration)
        ) binding!!.totalDuration.text = convertDuration(p.duration)

        if (p.isPlaying) binding!!.playPauseImage.setImageResource(
            R.drawable.baseline_pause_24
        )
        else binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)

        // ((ApplicationClass)getApplicationContext()).showNotification();

        // Update repeat and shuffle button UI
        updateRepeatButtonUI()

        val primaryColor = ThemeManager.getPrimaryColor(this)
        if (p.shuffleModeEnabled) binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(primaryColor)
        else binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(resources.getColor(R.color.textSec, null))


        mHandler.postDelayed(mUpdateTimeTask, 1000)
    }

    private fun updateRepeatButtonUI() {
        val tintColor: Int
        val p = MusicPlayerManager.player ?: return
        val repeatMode: Int = p.repeatMode
        val primaryColor = ThemeManager.getPrimaryColor(this)

        when (repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                tintColor = primaryColor
                try {
                    binding!!.repeatIcon.setImageResource(R.drawable.repeat_one_24px)
                } catch (e: Exception) {
                    // Fallback to regular repeat icon if repeat_one_24px isn't available
                    Log.e(TAG, "Error setting repeat_one icon: " + e.message)
                    binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
                }
            }

            Player.REPEAT_MODE_ALL -> {
                tintColor = primaryColor
                binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
            }

            Player.REPEAT_MODE_OFF -> {
                tintColor = resources.getColor(R.color.textSec, null)
                binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
            }

            else -> {
                tintColor = resources.getColor(R.color.textSec, null)
                binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
            }
        }

        binding!!.repeatIcon.imageTintList = ColorStateList.valueOf(tintColor)
    }

    override fun nextClicked() {
        Log.i(TAG, "nextClicked called from service")
        try {
            if (MusicPlayerManager.player == null) {
                Log.e(TAG, "Player is null in nextClicked")
                return
            }

            // Update UI to show active button state
            runOnUiThread {
                binding!!.nextIcon.alpha = 0.5f
                binding!!.nextIcon.animate().alpha(1.0f).setDuration(200).start()

                // Update UI
                updateTrackInfo()
                updateSeekbar()

                if (MusicPlayerManager.isPlayerPlaying()) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in nextClicked", e)
        }
    }

    override fun prevClicked() {
        Log.i(TAG, "prevClicked called from service")
        try {
            if (MusicPlayerManager.player == null) {
                Log.e(TAG, "Player is null in prevClicked")
                return
            }

            // Update UI to show active button state
            runOnUiThread {
                binding!!.prevIcon.alpha = 0.5f
                binding!!.prevIcon.animate().alpha(1.0f).setDuration(200).start()

                // Update UI
                updateTrackInfo()
                updateSeekbar()

                if (MusicPlayerManager.isPlayerPlaying()) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in prevClicked", e)
        }
    }

    override fun playClicked() {
        runOnUiThread {
            if (MusicPlayerManager.isPlayerPlaying()) {
                binding?.playPauseImage?.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding?.playPauseImage?.setImageResource(R.drawable.play_arrow_24px)
            }
            updateSeekbar()
        }
    }

    override fun onProgressChanged(progress: Int) {
    }

    fun showNotification(playPauseButton: Int) {
        MusicPlayerManager.showNotification()
    }


    private fun showRecommendationsBottomSheet() {
        val currentSong = mSongResponse?.data?.getOrNull(0)
        if (currentSong == null) {
            Toast.makeText(this, "Song details not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }

        val bottomSheet = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
        val sheetBinding = RecommendationsBottomSheetBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        sheetBinding.recSubtitle.text = "Similar to ${currentSong.name()}"
        sheetBinding.loadingProgress.visibility = View.VISIBLE
        sheetBinding.recRecyclerView.visibility = View.GONE
        sheetBinding.emptyText.visibility = View.GONE
        sheetBinding.playAllBtn.visibility = View.GONE

        RecommendationManager.getRecommendationsForSong(this, currentSong, limit = 15) { recommendations ->
            if (isFinishing || isDestroyed) return@getRecommendationsForSong
            sheetBinding.loadingProgress.visibility = View.GONE

            if (recommendations.isEmpty()) {
                sheetBinding.emptyText.visibility = View.VISIBLE
            } else {
                sheetBinding.recRecyclerView.visibility = View.VISIBLE
                sheetBinding.playAllBtn.visibility = View.VISIBLE

                val adapter = ActivityListSongsItemAdapter(
                    recommendations.toMutableList(),
                    currentSong.image?.lastOrNull()?.url
                )
                sheetBinding.recRecyclerView.adapter = adapter

                sheetBinding.playAllBtn.setOnClickListener {
                    MusicPlayerManager.trackQueue?.clear()
                    recommendations.mapNotNull { it.id }.forEach { id ->
                        MusicPlayerManager.trackQueue?.add(id)
                    }
                    if (!MusicPlayerManager.trackQueue.isNullOrEmpty()) {
                        MusicPlayerManager.track_position = 0
                        MusicPlayerManager.MUSIC_ID = MusicPlayerManager.trackQueue!![0]
                        MusicPlayerManager.playTrack()
                    }
                    bottomSheet.dismiss()
                }
            }
        }

        bottomSheet.show()
    }

    private fun showSleepTimerBottomSheet() {
        val bottomSheet = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
        val sheetBinding = SleepTimerBottomSheetBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        fun refreshTimerUI() {
            if (SleepTimerManager.isTimerActive) {
                sheetBinding.activeTimerCard.visibility = View.VISIBLE
                sheetBinding.remainingTimeText.text = SleepTimerManager.getFormattedRemainingTime()
            } else {
                sheetBinding.activeTimerCard.visibility = View.GONE
            }
        }

        refreshTimerUI()

        val timerListener = object : SleepTimerManager.SleepTimerListener {
            override fun onTimerTick(remainingSeconds: Long) {
                sheetBinding.remainingTimeText.text = SleepTimerManager.getFormattedRemainingTime()
            }

            override fun onTimerFinished() {
                refreshTimerUI()
            }

            override fun onTimerCancelled() {
                refreshTimerUI()
            }
        }

        SleepTimerManager.addListener(timerListener)
        bottomSheet.setOnDismissListener {
            SleepTimerManager.removeListener(timerListener)
        }

        sheetBinding.timerOption15.setOnClickListener {
            SleepTimerManager.startTimer(15)
            refreshTimerUI()
        }
        sheetBinding.timerOption30.setOnClickListener {
            SleepTimerManager.startTimer(30)
            refreshTimerUI()
        }
        sheetBinding.timerOption45.setOnClickListener {
            SleepTimerManager.startTimer(45)
            refreshTimerUI()
        }
        sheetBinding.timerOption60.setOnClickListener {
            SleepTimerManager.startTimer(60)
            refreshTimerUI()
        }
        sheetBinding.timerOptionEndOfTrack.setOnClickListener {
            SleepTimerManager.startEndOfTrackTimer()
            refreshTimerUI()
        }

        sheetBinding.btnAdd5.setOnClickListener {
            SleepTimerManager.addMinutes(5)
            refreshTimerUI()
        }

        sheetBinding.btnCancelTimer.setOnClickListener {
            SleepTimerManager.cancelTimer()
            refreshTimerUI()
            Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
        }

        bottomSheet.show()
    }

    private fun showPlaybackSpeedDialog() {
        val speeds = arrayOf("0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
        val speedValues = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        val currentSpeed = MusicPlayerManager.getPlaybackSpeed()

        var selectedIndex = 2 // default 1.0x
        for (i in speedValues.indices) {
            if (kotlin.math.abs(speedValues[i] - currentSpeed) < 0.05f) {
                selectedIndex = i
                break
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Playback Speed")
            .setSingleChoiceItems(speeds, selectedIndex) { dialog, which ->
                val selectedSpeed = speedValues[which]
                MusicPlayerManager.setPlaybackSpeed(selectedSpeed)
                Toast.makeText(this, "Speed set to ${speeds[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openEqualizer() {
        try {
            val sessionId = MusicPlayerManager.player?.audioSessionId ?: 0
            val intent = Intent(android.media.audiofx.AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra("android.media.extra.AUDIO_SESSION_ID", sessionId)
                putExtra("android.media.extra.PACKAGE_NAME", packageName)
                putExtra("android.media.extra.CONTENT_TYPE", 0)
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Equalizer")
                    .setMessage("No system audio equalizer found on this device. You can install an equalizer app like Wavelet or Poweramp Equalizer for system-wide DSP controls.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Equalizer not available: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun convertPlayCount(playCount: Int): String {
            if (playCount < 1000) return playCount.toString() + ""
            if (playCount < 1000000) return (playCount / 1000).toString() + "K"
            return (playCount / 1000000).toString() + "M"
        }

        fun convertDuration(duration: Long): String {
            var timeString = ""
            val secondString: String?

            val hours = (duration / (1000 * 60 * 60)).toInt()
            val minutes = (duration % (1000 * 60 * 60)).toInt() / (1000 * 60)
            val seconds = ((duration % (1000 * 60 * 60)) % (1000 * 60) / 1000).toInt()
            if (hours > 0) {
                timeString = "$hours:"
            }
            secondString = if (seconds < 10) {
                "0$seconds"
            } else {
                "" + seconds
            }
            timeString = "$timeString$minutes:$secondString"
            return timeString
        }
    }
}
