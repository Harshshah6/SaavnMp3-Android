package com.harsh.shah.saavnmp3.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.harsh.shah.saavnmp3.R

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
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_player)

            // Set up play/pause button
            val playIntent = Intent(context, WidgetControlReceiver::class.java)
            playIntent.action = "ACTION_TOGGLE_PLAY"
            val playPendingIntent =
                PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.button_play_pause, playPendingIntent)

            // Set up next button
            val nextIntent = Intent(context, WidgetControlReceiver::class.java)
            nextIntent.action = "ACTION_NEXT"
            val nextPendingIntent =
                PendingIntent.getBroadcast(context, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.button_next, nextPendingIntent)

            // Set up previous button
            val prevIntent = Intent(context, WidgetControlReceiver::class.java)
            prevIntent.action = "ACTION_PREV"
            val prevPendingIntent =
                PendingIntent.getBroadcast(context, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.button_prev, prevPendingIntent)

            // Update widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
