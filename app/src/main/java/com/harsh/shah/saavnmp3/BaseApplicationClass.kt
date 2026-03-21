package com.harsh.shah.saavnmp3

import android.app.Activity
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.palette.graphics.Palette
import androidx.palette.graphics.Palette.PaletteAsyncListener
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.activities.SettingsActivity.SettingsSharedPrefManager
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.SongResponse.DownloadUrl
import com.harsh.shah.saavnmp3.services.NotificationReceiver
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.io.File

open class BaseApplicationClass : Application() {
    // Debounce variables for prepareMediaPlayer
    private val lastPrepareTime: Long = 0
    private val lastPreparedId = ""
    private var mediaSession: MediaSessionCompat? = null
    private val TAG = "ApplicationClass"


    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val cacheDir = File(getCacheDir(), "audio_cache")

        // Step 2: Set up SimpleCache
        val databaseProvider: DatabaseProvider = ExoDatabaseProvider(this)
        val cacheSize = (100 * 1024 * 1024).toLong() // 100 MB
        val cacheEvictor: CacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        val simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        // Step 3: Create CacheDataSourceFactory
        val httpDataSourceFactory: DataSource.Factory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(this, "AudioCachingApp"))
            .setConnectTimeoutMs(15000) // 15 seconds timeout
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Initialize player with better configuration
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setHandleAudioBecomingNoisy(true) // Handle audio focus automatically
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true // Handle audio focus automatically
            )
            .build()

        // Add a global player listener for logging and reliability
        player!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.i(TAG, "Player state changed to: " + getStateString(playbackState))

                // Auto-handle errors
                when (playbackState) {
                    Player.STATE_IDLE -> {
                        Log.e(TAG, "Player idle state detected, might need recovery")
                        //                    if (SONG_URL != null && !SONG_URL.isEmpty()) {
            //                        new Handler().postDelayed(() -> {
            //                            try {
            //                                // Try to reload the media
            //                                MediaItem mediaItem = MediaItem.fromUri(SONG_URL);
            //                                player.setMediaItem(mediaItem);
            //                                player.prepare();
            //                            } catch (Exception e) {
            //                                Log.e(TAG, "Error recovering from player error", e);
            //                            }
            //                        }, 2000);
            //                    }
                    }
                    Player.STATE_READY -> {
                        // Now it's safe to play
                        Log.i(TAG, "Player ready, starting playback...")
                        player!!.play()
                    }
                    Player.STATE_ENDED -> {
                        // Auto-play next track when current track ends
                        Log.i(TAG, "Track ended, auto-playing next track")
                        nextTrack()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i(TAG, "Player isPlaying changed to: " + isPlaying)

                // Update notification when playback state changes
                showNotification()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: " + error.message)

                // Auto-recovery from common errors
//                if (SONG_URL != null && !SONG_URL.isEmpty()) {
//                    Log.i(TAG, "Attempting to recover from error");
//                    new Handler().postDelayed(() -> {
//                        try {
//                            // Try to reload the media
//                            MediaItem mediaItem = MediaItem.fromUri(SONG_URL);
//                            player.setMediaItem(mediaItem);
//                            player.prepare();
//                            player.play();
//                        } catch (Exception e) {
//                            Log.e(TAG, "Error recovering from player error", e);
//                        }
//                    }, 2000);
//                }
            }
        })

        // Properly initialize media session with metadata
        mediaSession = MediaSessionCompat(this, "ApplicationClass")

        // Set flags to enable media buttons and transport controls for Android Auto
        mediaSession!!.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        // Set callback for media session
        mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                player!!.play()
                showNotification()
            }

            override fun onPause() {
                player!!.pause()
                showNotification()
            }

            override fun onSkipToNext() {
                nextTrack()
            }

            override fun onSkipToPrevious() {
                prevTrack()
            }

            override fun onSeekTo(pos: Long) {
                if (player != null) {
                    player!!.seekTo(pos)
                    showNotification()
                }
            }
        })

        mediaSession!!.setActive(true)
        createNotificationChannel()
        sharedPreferenceManager = SharedPreferenceManager.getInstance(this)
        TRACK_QUALITY = sharedPreferenceManager!!.trackQuality

        sharedPreferenceManager!!.migrateFromOldPrefs(
            this,
            Runnable { sharedPreferenceManager!!.clearOldPrefsAsync(this, null) })
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val notificationChannel1 = NotificationChannel(
                CHANNEL_ID_1, "Media Controls",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel1.setDescription("Notifications for media playback")

            val notificationManager =
                getSystemService<NotificationManager>(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel1)
        }
    }

    fun setMusicDetails(image: String?, title: String?, description: String?, id: String?) {
        if (image != null) IMAGE_URL = image
        if (title != null) MUSIC_TITLE = title
        if (description != null) MUSIC_DESCRIPTION = description
        MUSIC_ID = id
        Log.i(TAG, "setMusicDetails: " + MUSIC_TITLE + " - ID: " + MUSIC_ID)
    }

    fun setSongUrl(songUrl: String?) {
        SONG_URL = songUrl
    }

    var trackQueue: MutableList<String?>?
        get() = Companion.trackQueue
        set(que) {
            track_position = -1
            Companion.trackQueue = que
        }

    @JvmOverloads
    fun showNotification(playPauseButton: Int = if (player!!.isPlaying()) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px) {
        try {
            Log.i(TAG, "showNotification: " + MUSIC_TITLE + " - ID: " + MUSIC_ID)

            if (MUSIC_ID == null || MUSIC_ID!!.isEmpty()) {
                Log.e(TAG, "Cannot show notification: Music ID is empty")
                return
            }

            if (MUSIC_TITLE == null || MUSIC_TITLE!!.isEmpty() || IMAGE_URL == null || IMAGE_URL!!.isEmpty()) {
                Log.e(TAG, "Cannot show notification: Missing title or image")
                return
            }

            // 🔹 Update playback state with actual position
            val position: Long = if (player != null) player!!.getCurrentPosition() else 0
            val state = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    if (playPauseButton == R.drawable.play_arrow_24px)
                        PlaybackStateCompat.STATE_PAUSED
                    else
                        PlaybackStateCompat.STATE_PLAYING,
                    position,
                    1.0f
                )
                .build()

            mediaSession!!.setPlaybackState(state)

            // 🔹 Update metadata (shows in widget)
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MUSIC_DESCRIPTION)
                .build()
            mediaSession!!.setMetadata(metadata)

            val reqCode = MUSIC_ID.hashCode()

            val intent = Intent(this, MusicOverviewActivity::class.java)
            intent.putExtra("id", MUSIC_ID)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val contentIntent = PendingIntent.getActivity(
                this, reqCode, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val prevIntent = Intent(this, NotificationReceiver::class.java).setAction(ACTION_PREV)
            val prevPendingIntent = PendingIntent.getBroadcast(
                this, 0, prevIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val playIntent = Intent(this, NotificationReceiver::class.java)
                .setAction(ACTION_PLAY)
            val playPendingIntent = PendingIntent.getBroadcast(
                this, 0, playIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val nextIntent = Intent(this, NotificationReceiver::class.java)
                .setAction(ACTION_NEXT)
            val nextPendingIntent = PendingIntent.getBroadcast(
                this, 0, nextIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            // Create and show a simple notification as fallback in case image loading fails
            val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
                this@BaseApplicationClass, CHANNEL_ID_1
            )
                .setSmallIcon(R.drawable.headphone)
                .setContentTitle(MUSIC_TITLE)
                .setOngoing(playPauseButton != R.drawable.play_arrow_24px)
                .setContentText(MUSIC_DESCRIPTION)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession!!.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.skip_previous_24px, "prev",
                        prevPendingIntent
                    )
                )
                .addAction(
                    NotificationCompat.Action(playPauseButton, "play", playPendingIntent)
                )
                .addAction(
                    NotificationCompat.Action(
                        R.drawable.skip_next_24px, "next",
                        nextPendingIntent
                    )
                )
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)

            try {
                Picasso.get()
                    .load(IMAGE_URL)
                    .into(object : Target {
                        override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom?) {
                            try {
                                try {
                                    // Generate color palette from the bitmap
                                    Palette.from(bitmap)
                                        .generate { palette: Palette? ->
                                            if (palette != null) {
                                                val textSwatch = palette.dominantSwatch
                                                if (textSwatch != null) {
                                                    IMAGE_BG_COLOR = textSwatch.rgb
                                                    TEXT_ON_IMAGE_COLOR = textSwatch.titleTextColor
                                                    TEXT_ON_IMAGE_COLOR1 = textSwatch.bodyTextColor
                                                } else {
                                                    Log.i("ApplicationClass", "Null swatch :(")
                                                }
                                            } else {
                                                Log.i("ApplicationClass", "palette is null :(")
                                            }
                                        }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error generating palette", e)
                                }

                                // 🔹 Update MediaMetadata with album art for Android Auto
                                val metadataWithArt = MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
                                    .putString(
                                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                                        MUSIC_DESCRIPTION
                                    )
                                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                    .putBitmap(
                                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
                                        bitmap
                                    )
                                    .build()
                                mediaSession!!.setMetadata(metadataWithArt)

                                // Add bitmap to the notification
                                notificationBuilder.setLargeIcon(bitmap)

                                // Build and show the notification
                                val notification = notificationBuilder.build()
                                val notificationManager = getSystemService(
                                    NOTIFICATION_SERVICE
                                ) as NotificationManager
                                notificationManager.notify(0, notification)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error displaying notification with bitmap", e)
                                showBasicNotification(notificationBuilder)
                            }
                        }

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                            Log.e(TAG, "Error loading image for notification", e)
                            showBasicNotification(notificationBuilder)
                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                        }
                    })
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image for notification", e)
                showBasicNotification(notificationBuilder)
            }
        } catch (e: Exception) {
            Log.e("ApplicationClass", "showNotification error: ", e)
        }
    }

    private fun showBasicNotification(builder: NotificationCompat.Builder) {
        try {
            val notification = builder.build()
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing basic notification", e)
        }
    }

    fun togglePlayPause() {
        try {
            if (player == null) {
                Log.e(TAG, "Player is null in togglePlayPause")
                return
            }

            val wasPlaying: Boolean = player!!.isPlaying()
            Log.i(TAG, "togglePlayPause: wasPlaying=" + wasPlaying)

            if (wasPlaying) {
                player!!.pause()
                Log.i(TAG, "Player paused, isPlaying=" + player!!.isPlaying())
            } else {
                // Check if player is in a state where it can play
                if (player!!.getPlaybackState() == Player.STATE_IDLE) {
                    Log.i(TAG, "Player in idle state, preparing")
                    prepareMediaPlayer()
                } else {
                    player!!.play()
                    Log.i(TAG, "Player started, isPlaying=" + player!!.isPlaying())
                }
            }

            // Wait a moment for player state to update before showing notification
            Handler().postDelayed(Runnable {
                showNotification()
                Log.i(TAG, "Updated notification, isPlaying=" + player!!.isPlaying())
            }, 100)
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause", e)
        }
    }

    fun prepareMediaPlayer() {
        try {
            // Debounce: If same song is requested within 2 seconds, ignore it.
//            long currentTime = System.currentTimeMillis();
//            if (MUSIC_ID.equals(lastPreparedId) && (currentTime - lastPrepareTime) < 2000) {
//                Log.i(TAG, "prepareMediaPlayer: Debouncing duplicate request for " + MUSIC_ID);
//                // If player is not playing (e.g. buffering or paused), ensure we Resume if
//                // intention was to play
//                if (player != null && !player.isPlaying() && player.getPlaybackState() != Player.STATE_ENDED) {
//                    player.play();
//                }
//                return;
//            }
//
//            // Allow processing and update debounce trackers
//            lastPrepareTime = currentTime;
//            lastPreparedId = MUSIC_ID;

            // Stop currently playing media first

            if (player != null && player!!.isPlaying()) {
                player!!.stop()
            }

            // Reset player state
            if (player != null) {
                player!!.clearMediaItems()
            }

            // Check if we actually have a URL to play
            if (SONG_URL == null || SONG_URL!!.isEmpty()) {
                Log.e(TAG, "prepareMediaPlayer: No URL available to play")
                return
            }

            // Try to convert HTTP URLs to HTTPS for better security
            var finalUrl: String = SONG_URL ?: ""
            if (finalUrl.startsWith("http:")) {
                val httpsUrl = finalUrl.replace("http:", "https:")
                Log.i(TAG, "Converting URL from HTTP to HTTPS: " + httpsUrl)
                finalUrl = httpsUrl
            }

            val mediaItem = MediaItem.Builder()
                .setUri(finalUrl)
                .setMediaId(SONG_URL ?: "") // Set ID for comparison checks
                .build()
            isTrackDownloaded = false

            if (currentActivity == null) {
                Log.e(TAG, "prepareMediaPlayer: No current activity set")
                return
            }

            player!!.setMediaItem(mediaItem)

            // Prepare player but don't auto-play to prevent race conditions
            player!!.setPlayWhenReady(false)
            player!!.prepare()

            // Update notification
            showNotification()

            // // Start playback after a short delay
            // new Handler().postDelayed(() -> {
            // try {
            // if (player != null && !player.isPlaying() &&
            // player.getPlaybackState() == Player.STATE_READY) {
            // Log.i(TAG, "Starting delayed playback");
            // player.play();
            // }
            // } catch (Exception e) {
            // Log.e(TAG, "Error in delayed play", e);
            // }
            // }, 500);
        } catch (e: Exception) {
            Log.e(TAG, "prepareMediaPlayer: ", e)
        }
    }

    fun nextTrack() {
        Log.d(
            TAG, ("nextTrack() called. Current pos: " + track_position + ", Queue size: "
                    + (if (Companion.trackQueue != null) Companion.trackQueue!!.size else "null"))
        )
        if (Companion.trackQueue!!.isEmpty()) {
            Log.i(TAG, "Cannot play next track: track queue is empty")
            return
        }

        val repeatMode: Int = player!!.getRepeatMode()
        val shuffleEnabled: Boolean = player!!.getShuffleModeEnabled()

        if (track_position >= Companion.trackQueue!!.size - 1) {
            // End of queue behavior depends on repeat mode
            when (repeatMode) {
                Player.REPEAT_MODE_ONE ->                     // When repeat one is enabled, we should stay on the current track
                    // Just restart the current track
                    if (player != null) {
                        player!!.seekTo(0)
                        player!!.play()
                        Log.i(TAG, "Repeat ONE mode - restarting current track")
                        return
                    }

                Player.REPEAT_MODE_ALL -> {
                    // Loop back to the first track in queue
                    track_position = 0
                    Log.i(TAG, "Repeat ALL mode - looping back to first track in queue")
                }

                Player.REPEAT_MODE_OFF ->                     // In no-repeat mode, we've reached the end of the queue
                    // Just restart the current track instead of stopping completely
                    if (player != null) {
                        Log.i(TAG, "Repeat OFF mode")
                        return
                    }

                else ->
                    if (player != null) {
                        Log.i(TAG, "Repeat OFF mode")
                        return
                    }
            }
        } else {
            // Normal next track behavior when not at end of queue
            if (shuffleEnabled) {
                // For shuffle, pick any random track except the current one
                var newPosition: Int
                do {
                    newPosition = (Math.random() * Companion.trackQueue!!.size).toInt()
                } while (newPosition == track_position && Companion.trackQueue!!.size > 1)

                track_position = newPosition
                Log.i(TAG, "Shuffle enabled, random next track position: " + track_position)
            } else {
                track_position++
                Log.i(TAG, "Playing next track at position: " + track_position)
            }
        }

        // Get the track ID and play it
        try {
            MUSIC_ID = Companion.trackQueue!!.get(track_position)
            Log.i(TAG, "Playing next track: " + MUSIC_ID + " at position " + track_position)
            playTrack()
            showNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing next track", e)
            // Try to recover by resetting track position
            if (!Companion.trackQueue!!.isEmpty()) {
                track_position = 0
                MUSIC_ID = Companion.trackQueue!!.get(0)
                playTrack()
            }
        }
    }

    fun prevTrack() {
        if (Companion.trackQueue!!.isEmpty()) {
            Log.i(TAG, "Cannot play previous track: track queue is empty")
            return
        }

        // if (player != null && player.getCurrentPosition() > 3000) {
        //     player.seekTo(0);
        //     player.play();
        //     return;
        // }
        if (track_position <= 0) {
            if (player!!.getRepeatMode() == Player.REPEAT_MODE_ALL) {
                // Loop to the last track
                track_position = Companion.trackQueue!!.size - 1
                Log.i(TAG, "Looping to last track in queue (position " + track_position + ")")
            } else {
                // Just restart current track
                Log.i(TAG, "At first track, restarting current track")
                if (player != null) {
                    player!!.seekTo(0)
                    player!!.play()
                    return
                }
            }
        } else {
            if (player!!.getShuffleModeEnabled()) {
                track_position = (Math.random() * Companion.trackQueue!!.size).toInt()
                Log.i(TAG, "Shuffle enabled, random previous track position: " + track_position)
            } else {
                track_position--
                Log.i(TAG, "Playing previous track at position: " + track_position)
            }
        }

        MUSIC_ID = Companion.trackQueue!!.get(track_position)
        Log.i(TAG, "Playing previous track: " + MUSIC_ID)
        playTrack()
        showNotification()
    }

    private fun getStateString(state: Int): String {
        return when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN"
        }
    }

    private fun playTrack() {
        var context: Context? = currentActivity
        if (context == null) {
            context = getApplicationContext()
        }
        val apiManager = ApiManager(context)
        apiManager.retrieveSongById(MUSIC_ID!!, null, object : RequestNetwork.RequestListener {
            @OptIn(UnstableApi::class)
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val songResponse = Gson().fromJson<SongResponse>(response, SongResponse::class.java)
                if (songResponse.success && !songResponse.data.isNullOrEmpty()) {
                    val firstSong = songResponse.data!![0]!!
                    MUSIC_TITLE = firstSong.name()
                    MUSIC_DESCRIPTION = String.format(
                        "%s plays | %s | %s",
                        MusicOverviewActivity.convertPlayCount(firstSong.playCount ?: 0),
                        firstSong.year,
                        firstSong.copyright
                    )
                    val image = firstSong.image
                    if (!image.isNullOrEmpty()) {
                        IMAGE_URL = image!![image.size - 1]!!.url ?: ""
                    }
                    val downloadUrls = firstSong.downloadUrl
                    if (downloadUrls != null) {
                        SONG_URL = getDownloadUrl(downloadUrls)
                    }
                    setMusicDetails(IMAGE_URL, MUSIC_TITLE, MUSIC_DESCRIPTION, MUSIC_ID)
                    prepareMediaPlayer()
                    // showNotification();
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
            }
        })
    }

    companion object {
        const val CHANNEL_ID_1: String = "channel_1"
        const val ACTION_NEXT: String = "next"
        const val ACTION_PREV: String = "prev"
        const val ACTION_PLAY: String = "play"
        var CURRENT_TRACK: SongResponse? = null

        var player: ExoPlayer? = null

        var TRACK_QUALITY: String? = "320kbps"
        var isTrackDownloaded: Boolean = false
        var trackQueue: MutableList<String?>? = ArrayList<String?>()
        var MUSIC_TITLE: String? = ""
        var MUSIC_DESCRIPTION: String? = ""
        var IMAGE_URL: String? = ""
        var MUSIC_ID: String? = ""
        var SONG_URL: String? = ""
        var track_position: Int = -1
        var sharedPreferenceManager: SharedPreferenceManager? = null
        var IMAGE_BG_COLOR: Int = Color.argb(255, 25, 20, 20)
        var TEXT_ON_IMAGE_COLOR: Int = IMAGE_BG_COLOR xor 0x00FFFFFF
        var TEXT_ON_IMAGE_COLOR1: Int = IMAGE_BG_COLOR xor 0x00FFFFFF
        var currentActivity: Activity? = null

        fun setTrackQuality(string: String?) {
            TRACK_QUALITY = string
            sharedPreferenceManager!!.trackQuality = string
        }

        fun updateTheme() {
            val settingsSharedPrefManager = SettingsSharedPrefManager(
                currentActivity!!
            )
            val theme = settingsSharedPrefManager.theme
            AppCompatDelegate.setDefaultNightMode(
                if (theme == "dark")
                    AppCompatDelegate.MODE_NIGHT_YES
                else
                    if (theme == "light") AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        }

        fun cancelNotification() {
            val notificationManager = currentActivity!!
                .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(0)
        }

        fun getDownloadUrl(downloadUrlList: MutableList<SongResponse.DownloadUrl?>?): String {
            if (downloadUrlList.isNullOrEmpty()) return ""

            // Try to find HTTPS URL first
            var bestUrl = ""

            for (downloadUrl in downloadUrlList) {
                if (downloadUrl == null) continue
                val url = downloadUrl.url ?: continue

                // Always prefer HTTPS URLs
                if (url.startsWith("https:") && downloadUrl.quality == TRACK_QUALITY) {
                    return url
                }

                // Keep the best quality HTTP URL as fallback
                if (downloadUrl.quality == TRACK_QUALITY) {
                    bestUrl = url
                }
            }

            // If we found a matching quality, use it even if HTTP
            if (!bestUrl.isEmpty()) {
                // Try to convert to HTTPS if possible
                if (bestUrl.startsWith("http:")) {
                    val httpsUrl = bestUrl.replace("http:", "https:")
                    Log.i("ApplicationClass", "Converted HTTP to HTTPS URL: " + httpsUrl)
                    return httpsUrl
                }
                return bestUrl
            }

            // Otherwise use the highest quality available
            val lastUrl = downloadUrlList.get(downloadUrlList.size - 1)?.url ?: ""
            // Try to convert to HTTPS if needed
            if (lastUrl.startsWith("http:")) {
                val httpsUrl = lastUrl.replace("http:", "https:")
                Log.i("ApplicationClass", "Converted HTTP to HTTPS URL: " + httpsUrl)
                return httpsUrl
            }
            return lastUrl
        }
    }
}
