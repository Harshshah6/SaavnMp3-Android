package com.harsh.shah.saavnmp3;

import static com.harsh.shah.saavnmp3.activities.MusicOverviewActivity.convertPlayCount;

import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.media.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.database.DatabaseProvider;
import androidx.media3.database.ExoDatabaseProvider;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.CacheEvictor;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.palette.graphics.Palette;

import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;
import com.harsh.shah.saavnmp3.activities.SettingsActivity;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.harsh.shah.saavnmp3.services.NotificationReceiver;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BaseApplicationClass extends Application {
    public static final String CHANNEL_ID_1 = "channel_1";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_PREV = "prev";
    public static final String ACTION_PLAY = "play";
    public static SongResponse CURRENT_TRACK = null;

    public static ExoPlayer player;
    public static String TRACK_QUALITY = "320kbps";
    public static boolean isTrackDownloaded = false;
    private MediaSessionCompat mediaSession;
    public static List<String> trackQueue = new ArrayList<>();
    public static String MUSIC_TITLE = "";
    public static String MUSIC_DESCRIPTION = "";
    public static String IMAGE_URL = "";
    public static String MUSIC_ID = "";
    public static String SONG_URL = "";
    public static int track_position = -1;
    public static SharedPreferenceManager sharedPreferenceManager;
    private final String TAG = "ApplicationClass";
    public static int IMAGE_BG_COLOR = Color.argb(255, 25, 20, 20);
    public static int TEXT_ON_IMAGE_COLOR = IMAGE_BG_COLOR ^ 0x00FFFFFF;
    public static int TEXT_ON_IMAGE_COLOR1 = IMAGE_BG_COLOR ^ 0x00FFFFFF;
    private static Activity currentActivity = null;

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(Activity activity) {
        currentActivity = activity;
    }

    public static void setTrackQuality(String string) {
        TRACK_QUALITY = string;
        sharedPreferenceManager.setTrackQuality(string);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onCreate() {
        super.onCreate();

        File cacheDir = new File(getCacheDir(), "audio_cache");

        // Step 2: Set up SimpleCache
        DatabaseProvider databaseProvider = new ExoDatabaseProvider(this);
        long cacheSize = 100 * 1024 * 1024; // 100 MB
        CacheEvictor cacheEvictor = new LeastRecentlyUsedCacheEvictor(cacheSize);
        final SimpleCache simpleCache = new SimpleCache(cacheDir, cacheEvictor, databaseProvider);

        // Step 3: Create CacheDataSourceFactory
        DataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(Util.getUserAgent(this, "AudioCachingApp"))
                .setConnectTimeoutMs(15000) // 15 seconds timeout
                .setReadTimeoutMs(15000)
                .setAllowCrossProtocolRedirects(true);

        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        // Initialize player with better configuration
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory))
                .setHandleAudioBecomingNoisy(true) // Handle audio focus automatically
                .setAudioAttributes(
                        new androidx.media3.common.AudioAttributes.Builder()
                                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build(),
                        true // Handle audio focus automatically
                )
                .build();

        // Add a global player listener for logging and reliability
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Log.i(TAG, "Player state changed to: " + getStateString(playbackState));

                // Auto-handle errors
                if (playbackState == Player.STATE_IDLE) {
                    Log.e(TAG, "Player idle state detected, might need recovery");
                    if (SONG_URL != null && !SONG_URL.isEmpty()) {
                        new Handler().postDelayed(() -> {
                            try {
                                // Try to reload the media
                                MediaItem mediaItem = MediaItem.fromUri(SONG_URL);
                                player.setMediaItem(mediaItem);
                                player.prepare();
                            } catch (Exception e) {
                                Log.e(TAG, "Error recovering from player error", e);
                            }
                        }, 2000);
                    }
                } else if (playbackState == Player.STATE_READY) {
                    // Now it's safe to play
                    Log.i(TAG, "Player ready, starting playback...");
                    player.play();

                } else if (playbackState == Player.STATE_ENDED) {
                    // Auto-play next track when current track ends
                    Log.i(TAG, "Track ended, auto-playing next track");
                    nextTrack();
                }

            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.i(TAG, "Player isPlaying changed to: " + isPlaying);

                // Update notification when playback state changes
                showNotification();
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());

                // Auto-recovery from common errors
                if (SONG_URL != null && !SONG_URL.isEmpty()) {
                    Log.i(TAG, "Attempting to recover from error");
                    new Handler().postDelayed(() -> {
                        try {
                            // Try to reload the media
                            MediaItem mediaItem = MediaItem.fromUri(SONG_URL);
                            player.setMediaItem(mediaItem);
                            player.prepare();
                            player.play();
                        } catch (Exception e) {
                            Log.e(TAG, "Error recovering from player error", e);
                        }
                    }, 2000);
                }
            }
        });

        // Properly initialize media session with metadata
        mediaSession = new MediaSessionCompat(this, "ApplicationClass");

        // Set flags to enable media buttons and transport controls for Android Auto
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set callback for media session
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                player.play();
                showNotification();
            }

            @Override
            public void onPause() {
                player.pause();
                showNotification();
            }

            @Override
            public void onSkipToNext() {
                nextTrack();
            }

            @Override
            public void onSkipToPrevious() {
                prevTrack();
            }

            @Override
            public void onSeekTo(long pos) {
                if (player != null) {
                    player.seekTo(pos);
                    showNotification();
                }
            }
        });

        mediaSession.setActive(true);
        createNotificationChannel();
        sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        TRACK_QUALITY = sharedPreferenceManager.getTrackQuality();

        sharedPreferenceManager.migrateFromOldPrefs(this, () -> sharedPreferenceManager.clearOldPrefsAsync(this, null));
    }

    public static void updateTheme() {
        SettingsActivity.SettingsSharedPrefManager settingsSharedPrefManager = new SettingsActivity.SettingsSharedPrefManager(
                getCurrentActivity());
        final String theme = settingsSharedPrefManager.getTheme();
        AppCompatDelegate.setDefaultNightMode(theme.equals("dark") ? AppCompatDelegate.MODE_NIGHT_YES
                : theme.equals("light") ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel1 = new NotificationChannel(CHANNEL_ID_1, "Media Controls",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel1.setDescription("Notifications for media playback");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel1);
        }
    }

    public void setMusicDetails(String image, String title, String description, String id) {
        if (image != null)
            IMAGE_URL = image;
        if (title != null)
            MUSIC_TITLE = title;
        if (description != null)
            MUSIC_DESCRIPTION = description;
        MUSIC_ID = id;
        Log.i(TAG, "setMusicDetails: " + MUSIC_TITLE + " - ID: " + MUSIC_ID);
    }

    public void setSongUrl(String songUrl) {
        SONG_URL = songUrl;
    }

    public void setTrackQueue(List<String> que) {
        track_position = -1;
        trackQueue = que;
    }

    public List<String> getTrackQueue() {
        return trackQueue;
    }

    public void showNotification(int playPauseButton) {
        try {
            Log.i(TAG, "showNotification: " + MUSIC_TITLE + " - ID: " + MUSIC_ID);

            if (MUSIC_ID == null || MUSIC_ID.isEmpty()) {
                Log.e(TAG, "Cannot show notification: Music ID is empty");
                return;
            }

            if (MUSIC_TITLE == null || MUSIC_TITLE.isEmpty() || IMAGE_URL == null || IMAGE_URL.isEmpty()) {
                Log.e(TAG, "Cannot show notification: Missing title or image");
                return;
            }

            // 🔹 Update playback state with actual position
            long position = player != null ? player.getCurrentPosition() : 0;
            PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                                    PlaybackStateCompat.ACTION_SEEK_TO)
                    .setState(
                            playPauseButton == R.drawable.play_arrow_24px
                                    ? PlaybackStateCompat.STATE_PAUSED
                                    : PlaybackStateCompat.STATE_PLAYING,
                            position,
                            1.0f)
                    .build();

            mediaSession.setPlaybackState(state);

            // 🔹 Update metadata (shows in widget)
            MediaMetadataCompat metadata = new MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MUSIC_DESCRIPTION)
                    .build();
            mediaSession.setMetadata(metadata);

            int reqCode = MUSIC_ID.hashCode();

            Intent intent = new Intent(this, MusicOverviewActivity.class);
            intent.putExtra("id", MUSIC_ID);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent contentIntent = PendingIntent.getActivity(this, reqCode, intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

            Intent prevIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREV);
            PendingIntent prevPendingIntent = PendingIntent.getBroadcast(this, 0, prevIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            Intent playIntent = new Intent(this, NotificationReceiver.class)
                    .setAction(BaseApplicationClass.ACTION_PLAY);
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            Intent nextIntent = new Intent(this, NotificationReceiver.class)
                    .setAction(BaseApplicationClass.ACTION_NEXT);
            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(this, 0, nextIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            // Create and show a simple notification as fallback in case image loading fails
            androidx.core.app.NotificationCompat.Builder notificationBuilder = new androidx.core.app.NotificationCompat.Builder(
                    BaseApplicationClass.this, CHANNEL_ID_1)
                    .setSmallIcon(R.drawable.headphone)
                    .setContentTitle(MUSIC_TITLE)
                    .setOngoing(playPauseButton != R.drawable.play_arrow_24px)
                    .setContentText(MUSIC_DESCRIPTION)
                    .setStyle(new NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2))
                    .addAction(new androidx.core.app.NotificationCompat.Action(R.drawable.skip_previous_24px, "prev",
                            prevPendingIntent))
                    .addAction(
                            new androidx.core.app.NotificationCompat.Action(playPauseButton, "play", playPendingIntent))
                    .addAction(new androidx.core.app.NotificationCompat.Action(R.drawable.skip_next_24px, "next",
                            nextPendingIntent))
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setContentIntent(contentIntent)
                    .setOnlyAlertOnce(true);

            try {
                Picasso.get()
                        .load(IMAGE_URL)
                        .into(new Target() {
                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                try {
                                    try {
                                        // Generate color palette from the bitmap
                                        Palette.from(bitmap).generate(palette -> {
                                            if (palette == null) {
                                                Log.i("ApplicationClass", "palette is null :(");
                                                return;
                                            }
                                            Palette.Swatch textSwatch = palette.getDominantSwatch();
                                            if (textSwatch == null) {
                                                Log.i("ApplicationClass", "Null swatch :(");
                                                return;
                                            }
                                            IMAGE_BG_COLOR = textSwatch.getRgb();
                                            TEXT_ON_IMAGE_COLOR = textSwatch.getTitleTextColor();
                                            TEXT_ON_IMAGE_COLOR1 = textSwatch.getBodyTextColor();
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error generating palette", e);
                                    }

                                    // 🔹 Update MediaMetadata with album art for Android Auto
                                    MediaMetadataCompat metadataWithArt = new MediaMetadataCompat.Builder()
                                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, MUSIC_TITLE)
                                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, MUSIC_DESCRIPTION)
                                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)
                                            .build();
                                    mediaSession.setMetadata(metadataWithArt);

                                    // Add bitmap to the notification
                                    notificationBuilder.setLargeIcon(bitmap);

                                    // Build and show the notification
                                    Notification notification = notificationBuilder.build();
                                    NotificationManager notificationManager = (NotificationManager) getSystemService(
                                            NOTIFICATION_SERVICE);
                                    notificationManager.notify(0, notification);

                                } catch (Exception e) {
                                    Log.e(TAG, "Error displaying notification with bitmap", e);
                                    showBasicNotification(notificationBuilder);
                                }
                            }

                            @Override
                            public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                                Log.e(TAG, "Error loading image for notification", e);
                                showBasicNotification(notificationBuilder);
                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        });
            } catch (Exception e) {
                Log.e(TAG, "Error loading image for notification", e);
                showBasicNotification(notificationBuilder);
            }

        } catch (Exception e) {
            Log.e("ApplicationClass", "showNotification error: ", e);
        }
    }

    private void showBasicNotification(androidx.core.app.NotificationCompat.Builder builder) {
        try {
            Notification notification = builder.build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(0, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error showing basic notification", e);
        }
    }

    public static void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getCurrentActivity()
                .getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }

    public void togglePlayPause() {
        try {
            if (player == null) {
                Log.e(TAG, "Player is null in togglePlayPause");
                return;
            }

            boolean wasPlaying = player.isPlaying();
            Log.i(TAG, "togglePlayPause: wasPlaying=" + wasPlaying);

            if (wasPlaying) {
                player.pause();
                Log.i(TAG, "Player paused, isPlaying=" + player.isPlaying());
            } else {
                // Check if player is in a state where it can play
                if (player.getPlaybackState() == Player.STATE_IDLE) {
                    Log.i(TAG, "Player in idle state, preparing");
                    prepareMediaPlayer();
                } else {
                    player.play();
                    Log.i(TAG, "Player started, isPlaying=" + player.isPlaying());
                }
            }

            // Wait a moment for player state to update before showing notification
            new Handler().postDelayed(() -> {
                showNotification();
                Log.i(TAG, "Updated notification, isPlaying=" + player.isPlaying());
            }, 100);
        } catch (Exception e) {
            Log.e(TAG, "Error in togglePlayPause", e);
        }
    }

    @UnstableApi
    public void prepareMediaPlayer() {
        try {
            // Stop currently playing media first
            if (player != null && player.isPlaying()) {
                player.stop();
            }

            // Reset player state
            if (player != null) {
                player.clearMediaItems();
            }

            // Check if we actually have a URL to play
            if (SONG_URL == null || SONG_URL.isEmpty()) {
                Log.e(TAG, "prepareMediaPlayer: No URL available to play");
                return;
            }

            // Try to convert HTTP URLs to HTTPS for better security
            String finalUrl = SONG_URL;
            if (finalUrl.startsWith("http:")) {
                String httpsUrl = finalUrl.replace("http:", "https:");
                Log.i(TAG, "Converting URL from HTTP to HTTPS: " + httpsUrl);
                finalUrl = httpsUrl;
            }

            MediaItem mediaItem = MediaItem.fromUri(finalUrl);
            isTrackDownloaded = false;

            if (currentActivity == null) {
                Log.e(TAG, "prepareMediaPlayer: No current activity set");
                return;
            }

            player.setMediaItem(mediaItem);

            // Prepare player but don't auto-play to prevent race conditions
            player.setPlayWhenReady(true);
            player.prepare();

            // Update notification
            showNotification();

            // Start playback after a short delay
            new Handler().postDelayed(() -> {
                try {
                    if (player != null && !player.isPlaying() &&
                            player.getPlaybackState() == Player.STATE_READY) {
                        Log.i(TAG, "Starting delayed playback");
                        player.play();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in delayed play", e);
                }
            }, 500);

        } catch (Exception e) {
            Log.e(TAG, "prepareMediaPlayer: ", e);
        }
    }

    public void nextTrack() {
        if (trackQueue.isEmpty()) {
            Log.i(TAG, "Cannot play next track: track queue is empty");
            return;
        }

        int repeatMode = player.getRepeatMode();
        boolean shuffleEnabled = player.getShuffleModeEnabled();

        if (track_position >= trackQueue.size() - 1) {
            // End of queue behavior depends on repeat mode
            switch (repeatMode) {
                case Player.REPEAT_MODE_ONE:
                    // When repeat one is enabled, we should stay on the current track
                    // Just restart the current track
                    if (player != null) {
                        player.seekTo(0);
                        player.play();
                        Log.i(TAG, "Repeat ONE mode - restarting current track");
                        return;
                    }
                    break;

                case Player.REPEAT_MODE_ALL:
                    // Loop back to the first track in queue
                    track_position = 0;
                    Log.i(TAG, "Repeat ALL mode - looping back to first track in queue");
                    break;

                case Player.REPEAT_MODE_OFF:
                default:
                    // In no-repeat mode, we've reached the end of the queue
                    // Just restart the current track instead of stopping completely
                    if (player != null) {
                        Log.i(TAG, "Repeat OFF mode");
                        return;
                    }
                    break;
            }
        } else {
            // Normal next track behavior when not at end of queue
            if (shuffleEnabled) {
                // For shuffle, pick any random track except the current one
                int newPosition;
                do {
                    newPosition = (int) (Math.random() * trackQueue.size());
                } while (newPosition == track_position && trackQueue.size() > 1);

                track_position = newPosition;
                Log.i(TAG, "Shuffle enabled, random next track position: " + track_position);
            } else {
                track_position++;
                Log.i(TAG, "Playing next track at position: " + track_position);
            }
        }

        // Get the track ID and play it
        try {
            MUSIC_ID = trackQueue.get(track_position);
            Log.i(TAG, "Playing next track: " + MUSIC_ID + " at position " + track_position);
            playTrack();
            showNotification();
        } catch (Exception e) {
            Log.e(TAG, "Error playing next track", e);
            // Try to recover by resetting track position
            if (!trackQueue.isEmpty()) {
                track_position = 0;
                MUSIC_ID = trackQueue.get(0);
                playTrack();
            }
        }
    }

    public void prevTrack() {
        if (trackQueue.isEmpty()) {
            Log.i(TAG, "Cannot play previous track: track queue is empty");
            return;
        }

        if (track_position <= 0) {
            if (player.getRepeatMode() == Player.REPEAT_MODE_ALL) {
                // Loop to the last track
                track_position = trackQueue.size() - 1;
                Log.i(TAG, "Looping to last track in queue (position " + track_position + ")");
            } else {
                // Just restart current track
                Log.i(TAG, "At first track, restarting current track");
                if (player != null) {
                    player.seekTo(0);
                    player.play();
                    return;
                }
            }
        } else {
            if (player.getShuffleModeEnabled()) {
                track_position = (int) (Math.random() * trackQueue.size());
                Log.i(TAG, "Shuffle enabled, random previous track position: " + track_position);
            } else {
                track_position--;
                Log.i(TAG, "Playing previous track at position: " + track_position);
            }
        }

        MUSIC_ID = trackQueue.get(track_position);
        Log.i(TAG, "Playing previous track: " + MUSIC_ID);
        playTrack();
        showNotification();
    }

    private String getStateString(int state) {
        return switch (state) {
            case Player.STATE_IDLE -> "IDLE";
            case Player.STATE_BUFFERING -> "BUFFERING";
            case Player.STATE_READY -> "READY";
            case Player.STATE_ENDED -> "ENDED";
            default -> "UNKNOWN";
        };
    }

    private void playTrack() {
        ApiManager apiManager = new ApiManager(getCurrentActivity());
        apiManager.retrieveSongById(MUSIC_ID, null, new RequestNetwork.RequestListener() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                SongResponse songResponse = new Gson().fromJson(response, SongResponse.class);
                if (songResponse.success()) {
                    MUSIC_TITLE = (songResponse.data().get(0).name());
                    MUSIC_DESCRIPTION = (String.format("%s plays | %s | %s",
                            convertPlayCount(songResponse.data().get(0).playCount()),
                            songResponse.data().get(0).year(),
                            songResponse.data().get(0).copyright()));
                    List<SongResponse.Image> image = songResponse.data().get(0).image();
                    IMAGE_URL = image.get(image.size() - 1).url();

                    List<SongResponse.DownloadUrl> downloadUrls = songResponse.data().get(0).downloadUrl();

                    SONG_URL = getDownloadUrl(downloadUrls);
                    setMusicDetails(IMAGE_URL, MUSIC_TITLE, MUSIC_DESCRIPTION, MUSIC_ID);
                    prepareMediaPlayer();
                    // showNotification();
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {

            }
        });
    }

    public static String getDownloadUrl(List<SongResponse.DownloadUrl> downloadUrlList) {
        if (downloadUrlList.isEmpty())
            return "";

        // Try to find HTTPS URL first
        String bestUrl = "";

        for (SongResponse.DownloadUrl downloadUrl : downloadUrlList) {
            String url = downloadUrl.url();

            // Always prefer HTTPS URLs
            if (url.startsWith("https:") && downloadUrl.quality().equals(TRACK_QUALITY)) {
                return url;
            }

            // Keep the best quality HTTP URL as fallback
            if (downloadUrl.quality().equals(TRACK_QUALITY)) {
                bestUrl = url;
            }
        }

        // If we found a matching quality, use it even if HTTP
        if (!bestUrl.isEmpty()) {
            // Try to convert to HTTPS if possible
            if (bestUrl.startsWith("http:")) {
                String httpsUrl = bestUrl.replace("http:", "https:");
                Log.i("ApplicationClass", "Converted HTTP to HTTPS URL: " + httpsUrl);
                return httpsUrl;
            }
            return bestUrl;
        }

        // Otherwise use the highest quality available
        String lastUrl = downloadUrlList.get(downloadUrlList.size() - 1).url();
        // Try to convert to HTTPS if needed
        if (lastUrl.startsWith("http:")) {
            String httpsUrl = lastUrl.replace("http:", "https:");
            Log.i("ApplicationClass", "Converted HTTP to HTTPS URL: " + httpsUrl);
            return httpsUrl;
        }
        return lastUrl;
    }

    public void showNotification() {
        // showNotification(mediaPlayerUtil.isPlaying() ? R.drawable.baseline_pause_24 :
        // R.drawable.play_arrow_24px);
        showNotification(player.isPlaying() ? R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
    }

}
