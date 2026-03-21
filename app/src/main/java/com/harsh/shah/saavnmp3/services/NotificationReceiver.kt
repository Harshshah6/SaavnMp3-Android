package com.harsh.shah.saavnmp3.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Received null intent or action")
            return
        }

        val action = intent.getAction()
        Log.i(TAG, "Received action: " + action)

        // Create intent for MusicService
        val serviceIntent = Intent(context, MusicService::class.java)

        // Get ApplicationClass instance
        val baseApplicationClass = context.getApplicationContext() as BaseApplicationClass?

        when (action) {
            BaseApplicationClass.Companion.ACTION_NEXT -> {
                Log.i(TAG, "Processing NEXT action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            BaseApplicationClass.Companion.ACTION_PREV -> {
                Log.i(TAG, "Processing PREVIOUS action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            BaseApplicationClass.Companion.ACTION_PLAY -> {
                Log.i(TAG, "Processing PLAY/PAUSE action")
                serviceIntent.putExtra("action", action)
                context.startService(serviceIntent)
            }

            "action_click" -> {
                Log.i(TAG, "Processing CLICK action")
                try {
                    // Launch activity for the current track
                    val activityIntent: Intent = Intent(context, MusicOverviewActivity::class.java)
                        .putExtra("id", BaseApplicationClass.Companion.MUSIC_ID)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching activity", e)
                }
            }

            else -> Log.i(TAG, "Unknown action received: " + action)
        }
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
}
