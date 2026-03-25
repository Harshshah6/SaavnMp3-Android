package com.harsh.shah.saavnmp3.utils

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
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.database.ExoDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.palette.graphics.Palette
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.services.NotificationReceiver
import com.harsh.shah.saavnmp3.widgets.WidgetPlayerProvider
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import java.io.File

object MusicPlayerManager {
    private const val TAG = "MusicPlayerManager"

    const val CHANNEL_ID_1: String = "channel_1"
    const val ACTION_NEXT: String = "next"
    const val ACTION_PREV: String = "prev"
    const val ACTION_PLAY: String = "play"

    var CURRENT_TRACK: SongResponse? = null
    var player: ExoPlayer? = null
    var musicService: com.harsh.shah.saavnmp3.services.MusicService? = null
    private var mediaSession: MediaSessionCompat? = null

    var TRACK_QUALITY: String? = "320kbps"
    var isTrackDownloaded: Boolean = false
    var trackQueue: MutableList<String?>? = ArrayList()
    var track_position: Int = -1

    var MUSIC_TITLE: String? = ""
    var MUSIC_DESCRIPTION: String? = ""
    var IMAGE_URL: String? = ""
    var MUSIC_ID: String? = ""
    var SONG_URL: String? = ""

    var IMAGE_BG_COLOR: Int = Color.argb(255, 25, 20, 20)
    var TEXT_ON_IMAGE_COLOR: Int = IMAGE_BG_COLOR xor 0x00FFFFFF
    var TEXT_ON_IMAGE_COLOR1: Int = IMAGE_BG_COLOR xor 0x00FFFFFF

    private var appContext: Context? = null
    private var simpleCache: SimpleCache? = null
    private var sharedPreferenceManager: SharedPreferenceManager? = null

    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        sharedPreferenceManager = SharedPreferenceManager.getInstance(appContext!!)

        val cacheDir = File(appContext!!.cacheDir, "audio_cache")
        val databaseProvider = ExoDatabaseProvider(appContext!!)
        val cacheSize = (100 * 1024 * 1024).toLong() // 100 MB
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        simpleCache = SimpleCache(cacheDir, cacheEvictor, databaseProvider)

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(Util.getUserAgent(appContext!!, "AudioCachingApp"))
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)
            .setAllowCrossProtocolRedirects(true)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache!!)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(appContext!!)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setHandleAudioBecomingNoisy(true)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .build()

        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.i(TAG, "Player state changed to: ${getStateString(playbackState)}")
                when (playbackState) {
                    Player.STATE_READY -> {
                        player?.play()
                        updateWidget()
                    }
                    Player.STATE_ENDED -> {
                        nextTrack()
                    }
                    else -> {
                        updateWidget()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.i(TAG, "Player isPlaying changed to: $isPlaying")
                showNotification()
                updateWidget()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}")
            }
        })

        mediaSession = MediaSessionCompat(appContext!!, TAG).apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() { player?.play(); showNotification() }
                override fun onPause() { player?.pause(); showNotification() }
                override fun onSkipToNext() { nextTrack() }
                override fun onSkipToPrevious() { prevTrack() }
                override fun onSeekTo(pos: Long) {
                    player?.seekTo(pos)
                    showNotification()
                }
            })
            isActive = true
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_1, "Media Controls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for media playback" }
            val notificationManager = appContext?.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun setTrackQuality(quality: String?, prefs: SharedPreferenceManager? = sharedPreferenceManager) {
        TRACK_QUALITY = quality
        prefs?.trackQuality = quality
    }

    fun setMusicDetails(image: String?, title: String?, description: String?, id: String?) {
        image?.let { IMAGE_URL = it }
        title?.let { MUSIC_TITLE = it }
        description?.let { MUSIC_DESCRIPTION = it }
        MUSIC_ID = id
        Log.i(TAG, "setMusicDetails: $MUSIC_TITLE - ID: $MUSIC_ID")
        updateWidget()
    }

    fun cancelNotification() {
        val notificationManager = appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(1)
    }

    private fun updateWidget() {
        appContext?.let { WidgetPlayerProvider.updateAllWidgets(it) }
    }

    fun getDownloadUrl(downloadUrlList: MutableList<SongResponse.DownloadUrl?>?): String {
        if (downloadUrlList.isNullOrEmpty()) return ""
        var bestUrl = ""
        for (downloadUrl in downloadUrlList) {
            val url = downloadUrl?.url ?: continue
            if (url.startsWith("https:") && downloadUrl.quality == TRACK_QUALITY) return url
            if (downloadUrl.quality == TRACK_QUALITY) bestUrl = url
        }
        if (bestUrl.isNotEmpty()) {
            return if (bestUrl.startsWith("http:")) bestUrl.replace("http:", "https:") else bestUrl
        }
        val lastUrl = downloadUrlList.lastOrNull()?.url ?: return ""
        return if (lastUrl.startsWith("http:")) lastUrl.replace("http:", "https:") else lastUrl
    }

    fun showNotification(playPauseButton: Int = if (player?.isPlaying == true) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px) {
        val ctx = appContext ?: return
        if (MUSIC_ID.isNullOrEmpty() || MUSIC_TITLE.isNullOrEmpty() || IMAGE_URL.isNullOrEmpty()) return

        val position = player?.currentPosition ?: 0
        val state = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO)
            .setState(if (playPauseButton == R.drawable.play_arrow_24px) PlaybackStateCompat.STATE_PAUSED else PlaybackStateCompat.STATE_PLAYING, position, 1.0f)
            .build()
        mediaSession?.setPlaybackState(state)

        val duration = if (player?.duration != null && player!!.duration > 0) player!!.duration else -1L
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MUSIC_DESCRIPTION)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
            .build()
        mediaSession?.setMetadata(metadata)

        val reqCode = MUSIC_ID.hashCode()
        val intent = Intent(ctx, MusicOverviewActivity::class.java).apply {
            putExtra("id", MUSIC_ID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(ctx, reqCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val prevIntent = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, NotificationReceiver::class.java).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE)
        val playIntent = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, NotificationReceiver::class.java).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE)
        val nextIntent = PendingIntent.getBroadcast(ctx, 0, Intent(ctx, NotificationReceiver::class.java).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(ctx, CHANNEL_ID_1)
            .setSmallIcon(R.drawable.headphone)
            .setContentTitle(MUSIC_TITLE)
            .setOngoing(playPauseButton != R.drawable.play_arrow_24px)
            .setContentText(MUSIC_DESCRIPTION)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .addAction(R.drawable.skip_previous_24px, "prev", prevIntent)
            .addAction(playPauseButton, "play", playIntent)
            .addAction(R.drawable.skip_next_24px, "next", nextIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)

        try {
            Picasso.get().load(IMAGE_URL).into(object : Target {
                override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
                    try {
                        Palette.from(bitmap).generate { palette ->
                            val textSwatch = palette?.dominantSwatch
                            if (textSwatch != null) {
                                IMAGE_BG_COLOR = textSwatch.rgb
                                TEXT_ON_IMAGE_COLOR = textSwatch.titleTextColor
                                TEXT_ON_IMAGE_COLOR1 = textSwatch.bodyTextColor
                            }
                        }
                        val metadataWithArt = MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MUSIC_DESCRIPTION)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
                            .build()
                        mediaSession?.setMetadata(metadataWithArt)

                        builder.setLargeIcon(bitmap)
                        showBasicNotification(builder, playPauseButton != R.drawable.play_arrow_24px)
                    } catch (e: Exception) {
                        showBasicNotification(builder, playPauseButton != R.drawable.play_arrow_24px)
                    }
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) { showBasicNotification(builder, playPauseButton != R.drawable.play_arrow_24px) }
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
        } catch (e: Exception) {
            showBasicNotification(builder, playPauseButton != R.drawable.play_arrow_24px)
        }
    }

    private fun showBasicNotification(builder: NotificationCompat.Builder, isPlaying: Boolean) {
        val notification = builder.build()
        if (isPlaying) {
            try {
                androidx.core.content.ContextCompat.startForegroundService(
                    appContext!!,
                    Intent(appContext, com.harsh.shah.saavnmp3.services.MusicService::class.java)
                )
                musicService?.startForeground(1, notification)
            } catch (e: Exception) {
                val manager = appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(1, notification)
            }
        } else {
            try { musicService?.stopForeground(false) } catch (e: Exception) {}
            val manager = appContext?.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(1, notification)
        }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) p.pause()
        else if (p.playbackState == Player.STATE_IDLE) prepareMediaPlayer()
        else p.play()

        Handler(Looper.getMainLooper()).postDelayed({ 
            showNotification() 
            updateWidget()
        }, 100)
    }

    fun prepareMediaPlayer() {
        val p = player ?: return
        if (p.isPlaying) p.stop()
        p.clearMediaItems()
        if (SONG_URL.isNullOrEmpty()) return

        var finalUrl = SONG_URL ?: ""
        if (finalUrl.startsWith("http:")) finalUrl = finalUrl.replace("http:", "https:")

        val mediaItem = MediaItem.Builder().setUri(finalUrl).setMediaId(SONG_URL ?: "").build()
        isTrackDownloaded = false
        p.setMediaItem(mediaItem)
        p.playWhenReady = false
        p.prepare()
        showNotification()
        updateWidget()
    }

    fun playTrack() {
        val ctx = BaseApplicationClass.currentActivity ?: appContext ?: return
        ApiManager(ctx).retrieveSongById(MUSIC_ID!!, null, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                val songResponse = Gson().fromJson(response, SongResponse::class.java)
                if (songResponse.success && !songResponse.data.isNullOrEmpty()) {
                    val firstSong = songResponse.data[0] ?: return
                    MUSIC_TITLE = firstSong.name()
                    MUSIC_DESCRIPTION = "${MusicOverviewActivity.convertPlayCount(firstSong.playCount ?: 0)} plays | ${firstSong.year} | ${firstSong.copyright}"
                    firstSong.image?.lastOrNull()?.url?.let { IMAGE_URL = it }
                    SONG_URL = getDownloadUrl(firstSong.downloadUrl)
                    setMusicDetails(IMAGE_URL, MUSIC_TITLE, MUSIC_DESCRIPTION, MUSIC_ID)
                    prepareMediaPlayer()
                }
            }
            override fun onErrorResponse(tag: String?, message: String?) {}
        })
    }

    fun nextTrack() {
        if (trackQueue.isNullOrEmpty()) return
        val p = player ?: return
        
        if (trackQueue!!.isEmpty()) return
        if (track_position >= trackQueue!!.size - 1) {
            when (p.repeatMode) {
                Player.REPEAT_MODE_ONE -> { p.seekTo(0); p.play(); return }
                Player.REPEAT_MODE_ALL -> { track_position = 0 }
                else -> { return }
            }
        } else {
            if (p.shuffleModeEnabled && trackQueue!!.size > 1) {
                var newPos: Int
                do { newPos = (Math.random() * trackQueue!!.size).toInt() } while (newPos == track_position)
                track_position = newPos
            } else { track_position++ }
        }
        
        try {
            MUSIC_ID = trackQueue!![track_position]
            playTrack()
            showNotification()
            updateWidget()
        } catch (e: Exception) {
            if (trackQueue!!.isNotEmpty()) {
                track_position = 0
                MUSIC_ID = trackQueue!![0]
                playTrack()
            }
        }
    }

    fun prevTrack() {
        if (trackQueue.isNullOrEmpty()) return
        val p = player ?: return

        if (track_position <= 0) {
            if (p.repeatMode == Player.REPEAT_MODE_ALL) track_position = trackQueue!!.size - 1
            else { p.seekTo(0); p.play(); return }
        } else {
            if (p.shuffleModeEnabled && trackQueue!!.size > 1) {
                track_position = (Math.random() * trackQueue!!.size).toInt()
            } else { track_position-- }
        }
        
        MUSIC_ID = trackQueue!![track_position]
        playTrack()
        showNotification()
        updateWidget()
    }

    private fun getStateString(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> "UNKNOWN"
    }
}
