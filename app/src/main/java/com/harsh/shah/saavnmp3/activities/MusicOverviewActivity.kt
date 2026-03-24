package com.harsh.shah.saavnmp3.activities

import android.app.ProgressDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
import com.squareup.picasso.Picasso
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

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
                    if (layoutManager != null && recyclerView != null) {
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

    private var artsitsList: MutableList<SongResponse.Artist> = ArrayList<SongResponse.Artist>()
    private val isDebugMode = false
    private val audioManager: AudioManager? = null

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showData()
    }

    // @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicOverviewBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        binding!!.title.isSelected = true
        binding!!.description.isSelected = true

        val toggleLyrics = {
            val parent = binding!!.root as android.view.ViewGroup
            val transition = android.transition.Fade()
            transition.duration = 300
            android.transition.TransitionManager.beginDelayedTransition(parent, transition)
            
            if (binding!!.lyricsRecycler?.visibility == View.VISIBLE) {
                binding!!.lyricsRecycler?.visibility = View.GONE
                binding!!.coverImageCard?.visibility = View.VISIBLE
                binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textSec, null))
            } else {
                binding!!.lyricsRecycler?.visibility = View.VISIBLE
                binding!!.coverImageCard?.visibility = View.GONE
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
        
        binding!!.coverImageCard?.setOnClickListener {
            if (currentLyricsList.isNullOrEmpty()) return@setOnClickListener
            toggleLyrics()
        }

        if ((MusicPlayerManager.trackQueue?.size ?: 0) <= 1) binding!!.shuffleIcon.visibility = View.INVISIBLE

        binding!!.playPauseImage.setOnClickListener(View.OnClickListener { view: View? ->
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
                if (MusicPlayerManager.player?.isPlaying == true) {
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

        binding!!.nextIcon.setOnClickListener(View.OnClickListener { view: View? ->
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

        binding!!.prevIcon.setOnClickListener(View.OnClickListener { view: View? ->
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

        binding!!.repeatIcon.setOnClickListener(View.OnClickListener { view: View? ->
            try {
                // Cycle through all three repeat modes
                val player = MusicPlayerManager.player
                if (player == null) return@OnClickListener
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

                Log.i(TAG, "Repeat mode changed to: " + newMode)
            } catch (e: Exception) {
                Log.e(TAG, "Error changing repeat mode", e)
                Toast.makeText(
                    this@MusicOverviewActivity,
                    "Error changing repeat mode",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        binding!!.shuffleIcon.setOnClickListener(View.OnClickListener { view: View? ->
            val player = MusicPlayerManager.player
            if (player != null) {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                if (player.shuffleModeEnabled) binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(getResources().getColor(R.color.spotify_green))
                else binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(
                    getResources().getColor(
                        R.color.textSec
                    )
                )
            }
            if (isDebugMode) Toast.makeText(
                this@MusicOverviewActivity,
                "Shuffle Mode Changed.",
                Toast.LENGTH_SHORT
            ).show()
        })

        binding!!.shareIcon.setOnClickListener(View.OnClickListener { view: View? ->
            if (SHARE_URL.isBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, SHARE_URL)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        binding!!.moreIcon.setOnClickListener(View.OnClickListener { view: View? ->
            val bottomSheetDialog = BottomSheetDialog(
                this@MusicOverviewActivity,
                R.style.MyBottomSheetDialogTheme
            )
            val _binding = MusicOverviewMoreInfoBottomSheetBinding
                .inflate(layoutInflater)
            _binding.albumTitle.text = binding!!.title.text.toString()
            _binding.albumSubTitle.text = binding!!.description.text.toString()
            Picasso.get().load(Uri.parse(IMAGE_URL)).into(_binding.coverImage)
            val linearLayout = _binding.main

            _binding.goToAlbum.setOnClickListener(View.OnClickListener { go_to_album: View? ->
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

            _binding.addToLibrary.setOnClickListener(View.OnClickListener { v: View? ->
                -1
                val sharedPreferenceManager: SharedPreferenceManager =
                    SharedPreferenceManager.getInstance(this@MusicOverviewActivity)
                var savedLibraries = sharedPreferenceManager.savedLibrariesData as? SavedLibraries
                if (savedLibraries == null) savedLibraries = SavedLibraries(ArrayList<Library?>())
                val lists = savedLibraries.lists ?: emptyList<Library?>()
                if (lists.isEmpty()) {
                    Snackbar.make(_binding.getRoot(), "No Libraries Found", Snackbar.LENGTH_SHORT)
                        .show()
                    return@OnClickListener
                }
                val userCreatedLibraries: MutableList<String?> = ArrayList<String?>()
                for (library in lists) {
                    if (library != null && library.isCreatedByUser) userCreatedLibraries.add(library.name)
                }

                val materialAlertDialogBuilder = getMaterialAlertDialogBuilder(
                    userCreatedLibraries, savedLibraries, sharedPreferenceManager
                )
                materialAlertDialogBuilder.show()
            })

            val song = mSongResponse!!.data?.get(0) ?: return@OnClickListener

            if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                _binding.download.titleTextView?.text = "Download Manager"
            }

            _binding.download.setOnClickListener(View.OnClickListener { v: View? ->
                if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                    startActivity(
                        Intent(
                            this@MusicOverviewActivity,
                            DownloadManagerActivity::class.java
                        )
                    )
                    return@OnClickListener
                }
                val progressDialog = ProgressDialog(this@MusicOverviewActivity)
                progressDialog.setMessage("Downloading...")
                progressDialog.setCancelable(false)
                progressDialog.setCanceledOnTouchOutside(false)
                TrackDownloader.downloadAndEmbedMetadata(
                    this@MusicOverviewActivity,
                    song,
                    object : TrackDownloadListener {
                        override fun onStarted() {
                            progressDialog.show()
                        }

                        override fun onFinished() {
                            progressDialog.dismiss()
                            if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                                Toast.makeText(
                                    this@MusicOverviewActivity, "Successfully Downloaded.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                _binding.download.titleTextView?.text = "Download Manager"
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Toast.makeText(
                                this@MusicOverviewActivity,
                                errorMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                            val alertDialogBuilder = MaterialAlertDialogBuilder(
                                this@MusicOverviewActivity
                            )
                            alertDialogBuilder.setTitle("Error")
                            alertDialogBuilder.setMessage(errorMessage)
                            alertDialogBuilder.setPositiveButton(
                                "OK",
                                DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int -> dialogInterface!!.dismiss() })
                            alertDialogBuilder.show()
                        }
                    })
            })

            for (artist in artsitsList) {
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
                    bottomSheetItemView.setOnClickListener(View.OnClickListener { view1: View? ->
                        Log.i(TAG, "BottomSheetItemView: onCLicked!")
                        startActivity(
                            Intent(this@MusicOverviewActivity, ArtistProfileActivity::class.java)
                                .putExtra(
                                    "data", Gson().toJson(
                                        BasicDataRecord(artist.id, artist.name(), "", imgUrl)
                                    )
                                )
                        )
                    })
                    linearLayout.addView(bottomSheetItemView)
                } catch (e: Exception) {
                    Log.e(TAG, "BottomSheetDialog: ", e)
                }
            }
            bottomSheetDialog.setContentView(_binding.getRoot())
            bottomSheetDialog.create()
            bottomSheetDialog.show()
        })

        binding!!.trackQuality.setOnClickListener(View.OnClickListener { view: View? ->
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
        })

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
        val finalSavedLibraries = savedLibraries
        materialAlertDialogBuilder.setAdapter(
            listAdapter,
            DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                // index = i;
                Log.i(TAG, "pickedLibrary: " + i)

                val song = mSongResponse!!.data?.get(0) ?: return@OnClickListener

                val songs = Library.Songs(
                    song.id,
                    song.name(),
                    binding!!.description.text.toString(),
                    IMAGE_URL
                )

                finalSavedLibraries.lists?.get(i)?.songs?.add(songs)
                sharedPreferenceManager.savedLibrariesData = finalSavedLibraries
                Toast.makeText(
                    this@MusicOverviewActivity, "Added to " + finalSavedLibraries.lists?.get(i)?.name,
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
        if (MusicPlayerManager.MUSIC_ID == ID) {
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
            MusicPlayerManager.trackQueue = ArrayList<String?>(mutableListOf<String?>(ID))
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
            Picasso.get().load(Uri.parse(IMAGE_URL)).into(binding!!.coverImage)
        }
        val downloadUrls = song.downloadUrl

        artsitsList = song.artists?.primary?.filterNotNull()?.toMutableList() ?: mutableListOf()

        SONG_URL = MusicPlayerManager.getDownloadUrl(downloadUrls)

        if ((MusicPlayerManager.MUSIC_ID != ID_FROM_EXTRA || forced)) {
            MusicPlayerManager.setMusicDetails(
                IMAGE_URL, binding!!.title.text.toString(),
                binding!!.description.text.toString(), ID_FROM_EXTRA
            )
            MusicPlayerManager.SONG_URL = SONG_URL
            prepareMediaPLayer()
        }

        binding!!.lyricsIcon.visibility = View.GONE
        binding!!.lyricsIcon.setColorFilter(resources.getColor(R.color.textSec, null))
        
        // Handle both possible IDs if naming was inconsistent, but we've standardized to lyrics_recycler
        val recView = binding!!.lyricsRecycler
        recView?.visibility = View.GONE
        binding!!.coverImageCard?.visibility = View.VISIBLE
        
        currentLyricsList = null
        lyricsAdapter = null
        currentLyricsRecyclerView = null
        
        val durationInSecs = (song.duration ?: -1.0).toInt()
        val artistName = if (!song.artists?.primary.isNullOrEmpty()) song.artists!!.primary!![0]?.name() ?: "" else ""
        
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
                        if (sentencesMap != null && sentencesMap.isNotEmpty()) {
                            currentLyricsList = sentencesMap.toList()
                            Log.i(TAG, "Fetched ${currentLyricsList?.size} lyric lines")
                            
                            lyricsAdapter = LyricsAdapter(currentLyricsList!!)
                            val rv = binding!!.lyricsRecycler
                            if (rv != null) {
                                rv.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MusicOverviewActivity)
                                rv.adapter = lyricsAdapter
                                currentLyricsRecyclerView = rv
                                binding!!.lyricsIcon.visibility = View.VISIBLE
                            } else {
                                Log.e(TAG, "lyrics_recycler View not found in current binding")
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
        if (binding!!.title.text
                .toString() != MusicPlayerManager.MUSIC_TITLE
        ) binding!!.title.text = MusicPlayerManager.MUSIC_TITLE
        if (binding!!.description.text
                .toString() != MusicPlayerManager.MUSIC_DESCRIPTION
        ) binding!!.description.text = MusicPlayerManager.MUSIC_DESCRIPTION
        Picasso.get().load(Uri.parse(MusicPlayerManager.IMAGE_URL))
            .into(binding!!.coverImage)
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

        if (p.shuffleModeEnabled) binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(getResources().getColor(R.color.spotify_green))
        else binding!!.shuffleIcon.imageTintList = ColorStateList.valueOf(getResources().getColor(R.color.textSec))

        mHandler.postDelayed(mUpdateTimeTask, 1000)
    }

    private fun updateRepeatButtonUI() {
        val tintColor: Int
        val p = MusicPlayerManager.player ?: return
        val repeatMode: Int = p.repeatMode

        when (repeatMode) {
            Player.REPEAT_MODE_ONE -> {
                tintColor = getResources().getColor(R.color.spotify_green)
                try {
                    binding!!.repeatIcon.setImageResource(R.drawable.repeat_one_24px)
                } catch (e: Exception) {
                    // Fallback to regular repeat icon if repeat_one_24px isn't available
                    Log.e(TAG, "Error setting repeat_one icon: " + e.message)
                    binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
                }
            }

            Player.REPEAT_MODE_ALL -> {
                tintColor = getResources().getColor(R.color.spotify_green)
                binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
            }

            Player.REPEAT_MODE_OFF -> {
                tintColor = getResources().getColor(R.color.textSec)
                binding!!.repeatIcon.setImageResource(R.drawable.repeat_24px)
            }

            else -> {
                tintColor = getResources().getColor(R.color.textSec)
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
            runOnUiThread(Runnable {
                binding!!.nextIcon.alpha = 0.5f
                binding!!.nextIcon.animate().alpha(1.0f).setDuration(200).start()

                // Update UI
                updateTrackInfo()
                updateSeekbar()

                if (MusicPlayerManager.player?.isPlaying == true) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
            })
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
            runOnUiThread(Runnable {
                binding!!.prevIcon.alpha = 0.5f
                binding!!.prevIcon.animate().alpha(1.0f).setDuration(200).start()

                // Update UI
                updateTrackInfo()
                updateSeekbar()

                if (MusicPlayerManager.player?.isPlaying == true) {
                    binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
                } else {
                    binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error in prevClicked", e)
        }
    }

    override fun playClicked() {
        Log.i(TAG, "playClicked called from service")
        runOnUiThread(Runnable {
            // Retrieve application class to toggle playback
            MusicPlayerManager.togglePlayPause()

            if (MusicPlayerManager.player?.isPlaying == true) {
                binding!!.playPauseImage.setImageResource(R.drawable.baseline_pause_24)
            } else {
                binding!!.playPauseImage.setImageResource(R.drawable.play_arrow_24px)
            }
        })
    }

    override fun onProgressChanged(progress: Int) {
    }

    fun showNotification(playPauseButton: Int) {
        MusicPlayerManager.showNotification()
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
                timeString = hours.toString() + ":"
            }
            if (seconds < 10) {
                secondString = "0" + seconds
            } else {
                secondString = "" + seconds
            }
            timeString = timeString + minutes + ":" + secondString
            return timeString
        }
    }
}
