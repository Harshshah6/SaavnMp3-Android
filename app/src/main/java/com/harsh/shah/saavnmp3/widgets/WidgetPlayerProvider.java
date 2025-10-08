package com.harsh.shah.saavnmp3.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.harsh.shah.saavnmp3.R;

public class WidgetPlayerProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_player);

        // Set up play/pause button
        Intent playIntent = new Intent(context, WidgetControlReceiver.class);
        playIntent.setAction("ACTION_TOGGLE_PLAY");
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(context, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_play_pause, playPendingIntent);

        // Set up next button
        Intent nextIntent = new Intent(context, WidgetControlReceiver.class);
        nextIntent.setAction("ACTION_NEXT");
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_next, nextPendingIntent);

        // Set up previous button
        Intent prevIntent = new Intent(context, WidgetControlReceiver.class);
        prevIntent.setAction("ACTION_PREV");
        PendingIntent prevPendingIntent = PendingIntent.getBroadcast(context, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.button_prev, prevPendingIntent);

        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}