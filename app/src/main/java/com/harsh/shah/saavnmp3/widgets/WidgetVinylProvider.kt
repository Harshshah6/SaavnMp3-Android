package com.harsh.shah.saavnmp3.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.RemoteViews
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.ThemeManager

class WidgetVinylProvider : AppWidgetProvider() {
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
        stopRotation()
    }

    companion object {
        private var cachedImageUrl: String? = null
        private var baseVinylBitmap: Bitmap? = null
        private var reusableBitmap: Bitmap? = null
        private var reusableCanvas: Canvas? = null
        private val matrix = Matrix()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        private var currentAngle = 0f
        private var isRotating = false
        private val rotationHandler = Handler(Looper.getMainLooper())
        private var savedContext: Context? = null

        private val rotationRunnable = object : Runnable {
            override fun run() {
                val ctx = savedContext ?: return
                val isPlaying = MusicPlayerManager.isPlayerPlaying()
                if (!isPlaying || baseVinylBitmap == null) {
                    isRotating = false
                    return
                }

                // Power optimization: Skip tick if screen is off
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                if (pm?.isInteractive == false) {
                    rotationHandler.postDelayed(this, 1000)
                    return
                }

                val appWidgetManager = AppWidgetManager.getInstance(ctx)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(ctx, WidgetVinylProvider::class.java)
                )
                if (appWidgetIds.isEmpty()) {
                    isRotating = false
                    return
                }

                // Advance angle smoothly: ~5.5 degrees per tick => ~5.5s per revolution
                currentAngle = (currentAngle + 5.5f) % 360f
                renderRotatedFrame(ctx, appWidgetManager, appWidgetIds)

                rotationHandler.postDelayed(this, 85)
            }
        }

        private fun startRotation(context: Context) {
            savedContext = context.applicationContext
            if (isRotating) return
            if (!MusicPlayerManager.isPlayerPlaying() || baseVinylBitmap == null) return
            isRotating = true
            rotationHandler.removeCallbacks(rotationRunnable)
            rotationHandler.post(rotationRunnable)
        }

        private fun stopRotation() {
            isRotating = false
            rotationHandler.removeCallbacks(rotationRunnable)
        }

        private fun renderRotatedFrame(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val bmp = baseVinylBitmap ?: return
            val w = bmp.width
            val h = bmp.height
            if (w <= 0 || h <= 0) return

            if (reusableBitmap == null || reusableBitmap?.width != w || reusableBitmap?.height != h) {
                reusableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                reusableCanvas = Canvas(reusableBitmap!!)
            }

            val targetBitmap = reusableBitmap ?: return
            val targetCanvas = reusableCanvas ?: return

            targetCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            matrix.reset()
            matrix.postRotate(currentAngle, w / 2f, h / 2f)
            targetCanvas.drawBitmap(bmp, matrix, paint)

            val views = RemoteViews(context.packageName, R.layout.widget_vinyl)
            views.setImageViewBitmap(R.id.widget_album_art, targetBitmap)
            for (id in appWidgetIds) {
                appWidgetManager.partiallyUpdateAppWidget(id, views)
            }
        }

        fun updateAllWidgets(context: Context) {
            savedContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, WidgetVinylProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            savedContext = context.applicationContext
            val views = RemoteViews(context.packageName, R.layout.widget_vinyl)

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
                context, 103, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.vinyl_disc, pendingIntent)
            views.setOnClickPendingIntent(R.id.text_container, pendingIntent)

            // Control Intents
            val togglePlayIntent = getPendingSelfIntent(context, "ACTION_TOGGLE_PLAY")
            views.setOnClickPendingIntent(R.id.button_play_pause, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_play_pause_bg, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_play_pause_container, togglePlayIntent)
            views.setOnClickPendingIntent(R.id.button_next, getPendingSelfIntent(context, "ACTION_NEXT"))
            views.setOnClickPendingIntent(R.id.button_prev, getPendingSelfIntent(context, "ACTION_PREV"))

            // Circular Album Art for Vinyl Center
            val imageUrl = MusicPlayerManager.IMAGE_URL
            if (!imageUrl.isNullOrEmpty()) {
                if (baseVinylBitmap != null && cachedImageUrl == imageUrl) {
                    val currentRotated = getOrCreateRotatedBitmap(baseVinylBitmap!!, currentAngle)
                    views.setImageViewBitmap(R.id.widget_album_art, currentRotated)
                    if (isPlaying) {
                        startRotation(context)
                    } else {
                        stopRotation()
                    }
                } else {
                    Glide.with(context.applicationContext)
                        .asBitmap()
                        .load(imageUrl)
                        .transform(CircleCrop())
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                baseVinylBitmap = bitmap
                                cachedImageUrl = imageUrl
                                val currentRotated = getOrCreateRotatedBitmap(bitmap, currentAngle)
                                views.setImageViewBitmap(R.id.widget_album_art, currentRotated)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                                if (MusicPlayerManager.isPlayerPlaying()) {
                                    startRotation(context)
                                } else {
                                    stopRotation()
                                }
                            }
                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        })
                }
            } else {
                baseVinylBitmap = null
                cachedImageUrl = null
                stopRotation()
                views.setImageViewResource(R.id.widget_album_art, R.mipmap.ic_launcher)
            }

            if (!isPlaying) {
                stopRotation()
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun getOrCreateRotatedBitmap(bmp: Bitmap, angle: Float): Bitmap {
            val w = bmp.width
            val h = bmp.height
            if (w <= 0 || h <= 0) return bmp

            if (reusableBitmap == null || reusableBitmap?.width != w || reusableBitmap?.height != h) {
                reusableBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                reusableCanvas = Canvas(reusableBitmap!!)
            }

            val targetBitmap = reusableBitmap ?: return bmp
            val targetCanvas = reusableCanvas ?: return bmp

            targetCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            matrix.reset()
            matrix.postRotate(angle, w / 2f, h / 2f)
            targetCanvas.drawBitmap(bmp, matrix, paint)
            return targetBitmap
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
