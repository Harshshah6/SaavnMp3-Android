package com.harsh.shah.saavnmp3.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.ThemeManager

class WidgetPlayerProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        private var cachedImageUrl: String? = null
        private var cachedBitmap: Bitmap? = null

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WidgetPlayerProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_player)

            // Song Info
            val title = if (MusicPlayerManager.MUSIC_TITLE.isNullOrEmpty()) "Not Playing" else MusicPlayerManager.MUSIC_TITLE
            val description = MusicPlayerManager.MUSIC_DESCRIPTION ?: "Melotune"
            val artist = if (description.contains("|")) description.substringAfterLast("|").trim() else description

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_artist_name, artist)

            // Theme colors for play/pause FAB
            val primaryColor = ThemeManager.getPrimaryColor(context)
            val onPrimaryColor = ThemeManager.getOnPrimaryColor(context)
            views.setInt(R.id.button_play_pause_bg, "setColorFilter", primaryColor)
            views.setInt(R.id.button_play_pause, "setColorFilter", onPrimaryColor)

            // Play/Pause Icon
            val isPlaying = MusicPlayerManager.isPlayerPlaying()
            views.setImageViewResource(
                R.id.button_play_pause,
                if (isPlaying) R.drawable.baseline_pause_24 else R.drawable.play_arrow_24px
            )

            // Open App Intent
            val intent = Intent(context, MusicOverviewActivity::class.java).apply {
                putExtra("id", MusicPlayerManager.MUSIC_ID)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 101, intent,
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

            // Circular Album Art
            val imageUrl = MusicPlayerManager.IMAGE_URL
            if (!imageUrl.isNullOrEmpty()) {
                if (cachedBitmap != null && cachedImageUrl == imageUrl) {
                    views.setImageViewBitmap(R.id.widget_album_art, cachedBitmap)
                } else {
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(imageUrl)
                        .transform(CircleCrop())
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                cachedBitmap = bitmap
                                cachedImageUrl = imageUrl
                                views.setImageViewBitmap(R.id.widget_album_art, bitmap)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher_round)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        })
                }
            } else {
                cachedBitmap = null
                cachedImageUrl = null
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher_round)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
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
