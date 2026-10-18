package com.harsh.shah.saavnmp3.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import androidx.media3.common.Player
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.ThemeManager

class WidgetCardProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopProgressUpdates()
    }

    companion object {
        private var cachedImageUrl: String? = null
        private var cachedBitmap: Bitmap? = null

        private var isProgressRunning = false
        private val progressHandler = Handler(Looper.getMainLooper())
        private var savedContext: Context? = null

        private val progressRunnable = object : Runnable {
            override fun run() {
                val ctx = savedContext ?: return
                val isPlaying = MusicPlayerManager.isPlayerPlaying()
                if (!isPlaying) {
                    isProgressRunning = false
                    return
                }

                // Power optimization: Skip tick if screen is off
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isInteractive == false) {
                    progressHandler.postDelayed(this, 2000)
                    return
                }

                val appWidgetManager = AppWidgetManager.getInstance(ctx)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(ctx, WidgetCardProvider::class.java)
                )
                if (appWidgetIds.isEmpty()) {
                    isProgressRunning = false
                    return
                }

                val p = MusicPlayerManager.player
                val duration = if (p != null && p.duration > 0) p.duration else 0L
                val position = if (p != null && p.currentPosition > 0) p.currentPosition else 0L
                val progress = if (duration > 0) ((position.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000) else 0

                val views = RemoteViews(ctx.packageName, R.layout.widget_card)
                views.setProgressBar(R.id.widget_progress_bar, 1000, progress, false)
                views.setTextViewText(R.id.widget_time_elapsed, formatDuration(position))
                if (duration > 0) {
                    views.setTextViewText(R.id.widget_time_total, formatDuration(duration))
                }

                for (id in appWidgetIds) {
                    appWidgetManager.partiallyUpdateAppWidget(id, views)
                }

                progressHandler.postDelayed(this, 2000)
            }
        }

        fun startProgressUpdates(context: Context) {
            savedContext = context.applicationContext
            if (isProgressRunning) return
            if (!MusicPlayerManager.isPlayerPlaying()) return
            isProgressRunning = true
            progressHandler.removeCallbacks(progressRunnable)
            progressHandler.postDelayed(progressRunnable, 2000)
        }

        fun stopProgressUpdates() {
            isProgressRunning = false
            progressHandler.removeCallbacks(progressRunnable)
        }

        fun updateAllWidgets(context: Context) {
            savedContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WidgetCardProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }

            if (MusicPlayerManager.isPlayerPlaying()) {
                startProgressUpdates(context)
            } else {
                stopProgressUpdates()
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            savedContext = context.applicationContext
            val views = RemoteViews(context.packageName, R.layout.widget_card)

            // Song Info
            val title = if (MusicPlayerManager.MUSIC_TITLE.isNullOrEmpty()) "Not Playing" else MusicPlayerManager.MUSIC_TITLE
            val description = MusicPlayerManager.MUSIC_DESCRIPTION ?: "Melotune"
            val artist = if (description.contains("|")) description.substringAfterLast("|").trim() else description

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)

            // Theme colors for play/pause FAB and active states
            val primaryColor = ThemeManager.getPrimaryColor(context)
            val onPrimaryColor = ThemeManager.getOnPrimaryColor(context)
            views.setInt(R.id.button_play_pause_bg, "setColorFilter", primaryColor)
            views.setInt(R.id.button_play_pause, "setColorFilter", onPrimaryColor)
            views.setTextColor(R.id.widget_badge_text, primaryColor)

            // Dynamic progress bar theme tinting
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setColorStateList(
                    R.id.widget_progress_bar,
                    "setProgressTintList",
                    ColorStateList.valueOf(primaryColor)
                )
            }

            val p = MusicPlayerManager.player
            val isPlaying = MusicPlayerManager.isPlayerPlaying()

            // Play/Pause Icon
            views.setImageViewResource(
                R.id.button_play_pause,
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px
            )

            // Progress Bar & Duration
            val duration = if (p != null && p.duration > 0) p.duration else 0L
            val position = if (p != null && p.currentPosition > 0) p.currentPosition else 0L
            val progress = if (duration > 0) ((position.toFloat() / duration) * 1000).toInt().coerceIn(0, 1000) else 0

            views.setProgressBar(R.id.widget_progress_bar, 1000, progress, false)
            views.setTextViewText(R.id.widget_time_elapsed, formatDuration(position))
            views.setTextViewText(R.id.widget_time_total, if (duration > 0) formatDuration(duration) else "0:00")

            // Shuffle & Repeat states
            val shuffleOn = p?.shuffleModeEnabled == true
            views.setInt(
                R.id.button_shuffle,
                "setColorFilter",
                if (shuffleOn) primaryColor else context.getColor(R.color.widget_text_subtitle)
            )

            val repeatMode = p?.repeatMode ?: Player.REPEAT_MODE_OFF
            val repeatRes = if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.repeat_one_24px else R.drawable.repeat_24px
            views.setImageViewResource(R.id.button_repeat, repeatRes)
            views.setInt(
                R.id.button_repeat,
                "setColorFilter",
                if (repeatMode != Player.REPEAT_MODE_OFF) primaryColor else context.getColor(R.color.widget_text_subtitle)
            )

            // Open App Intent
            val intent = Intent(context, MusicOverviewActivity::class.java).apply {
                putExtra("id", MusicPlayerManager.MUSIC_ID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 102, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_album_art, pendingIntent)
            views.setOnClickPendingIntent(R.id.text_container, pendingIntent)

            // Control Intents
            val togglePlayIntent = getPendingSelfIntent(context, "ACTION_TOGGLE_PLAY")
            views.setOnClickPendingIntent(R.id.button_play_pause, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_play_pause_bg, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_play_pause_container, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_next, getPendingSelfIntent(context, "ACTION_NEXT"))
            views.setOnClickPendingIntent(R.id.button_prev, getPendingSelfIntent(context, "ACTION_PREV"))
            views.setOnClickPendingIntent(R.id.button_shuffle, getPendingSelfIntent(context, "ACTION_SHUFFLE"))
            views.setOnClickPendingIntent(R.id.button_repeat, getPendingSelfIntent(context, "ACTION_REPEAT"))

            // Album Art with Rounded Corners
            val imageUrl = MusicPlayerManager.IMAGE_URL
            if (!imageUrl.isNullOrEmpty()) {
                if (cachedBitmap != null && cachedImageUrl == imageUrl) {
                    views.setImageViewBitmap(R.id.widget_album_art, cachedBitmap)
                } else {
                    val radius = (18 * context.resources.displayMetrics.density).toInt()
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(imageUrl)
                        .transform(RoundedCorners(radius))
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                cachedBitmap = bitmap
                                cachedImageUrl = imageUrl
                                views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        })
                }
            } else {
                cachedBitmap = null
                cachedImageUrl = null
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
            }

            if (isPlaying) {
                startProgressUpdates(context)
            } else {
                stopProgressUpdates()
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatDuration(millis: Long): String {
            val totalSeconds = (millis / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }

        private fun getPendingSelfIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, WidgetControlReceiver::class.java).apply {
                this.action = action
                `package` = context.packageName
            }
            return PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
