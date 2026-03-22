package com.harsh.shah.saavnmp3.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) {
            Log.e(TAG, "Received null intent or action")
            return
        }

        val action = intent.action
        Log.i(TAG, "Received action: $action")

        // Create intent for MusicService
        val serviceIntent = Intent(context, MusicService::class.java)

        // Get ApplicationClass instance
        // val baseApplicationClass = context.getApplicationContext() as BaseApplicationClass?

        when (action) {
            MusicPlayerManager.ACTION_NEXT -> {
                Log.i(TAG, "Processing NEXT action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            MusicPlayerManager.ACTION_PREV -> {
                Log.i(TAG, "Processing PREVIOUS action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            MusicPlayerManager.ACTION_PLAY -> {
                Log.i(TAG, "Processing PLAY/PAUSE action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            "action_click" -> {
                Log.i(TAG, "Processing CLICK action")
                try {
                    // Launch activity for the current track
                    val activityIntent: Intent = Intent(context, MusicOverviewActivity::class.java)
                        .putExtra("id", MusicPlayerManager.MUSIC_ID)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching activity", e)
                }
            }

            else -> Log.i(TAG, "Unknown action received: $action")
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
}
