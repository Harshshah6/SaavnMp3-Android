package com.harsh.shah.saavnmp3.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.harsh.shah.saavnmp3.BaseApplicationClass;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Received null intent or action");
            return;
        }

        String action = intent.getAction();
        Log.i(TAG, "Received action: " + action);

        // Create intent for MusicService
        Intent serviceIntent = new Intent(context, MusicService.class);

        // Get ApplicationClass instance
        final BaseApplicationClass baseApplicationClass = (BaseApplicationClass) context.getApplicationContext();

        switch (action) {
            case BaseApplicationClass.ACTION_NEXT:
                Log.i(TAG, "Processing NEXT action");
                serviceIntent.putExtra("action", action);
                context.startService(serviceIntent);
                break;

            case BaseApplicationClass.ACTION_PREV:
                Log.i(TAG, "Processing PREVIOUS action");
                serviceIntent.putExtra("action", action);
                context.startService(serviceIntent);
                break;

            case BaseApplicationClass.ACTION_PLAY:
                Log.i(TAG, "Processing PLAY/PAUSE action");
                serviceIntent.putExtra("action", action);
                context.startService(serviceIntent);
                break;

            case "action_click":
                Log.i(TAG, "Processing CLICK action");
                try {
                    // Launch activity for the current track
                    Intent activityIntent = new Intent(context, MusicOverviewActivity.class)
                            .putExtra("id", BaseApplicationClass.MUSIC_ID)
                            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(activityIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching activity", e);
                }
                break;

            default:
                Log.i(TAG, "Unknown action received: " + action);
                break;
        }
    }
}
