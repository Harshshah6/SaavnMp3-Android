package com.harsh.shah.saavnmp3.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity

class MusicService : Service() {
    private val mBinder: IBinder = MyBinder()

    var actionPlaying: ActionPlaying? = null

    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    inner class MyBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.extras == null) return START_STICKY

        val actionName = intent.extras!!.getString("action", "")
        Log.d("MusicService", "onStartCommand called with action: $actionName")
        if (actionName != null) {
            when (actionName) {
                BaseApplicationClass.Companion.ACTION_NEXT -> {
                    // Handle next action
                    // Always use BaseApplicationClass as source of truth
                    (applicationContext as BaseApplicationClass).nextTrack()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.nextClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }

                BaseApplicationClass.Companion.ACTION_PREV -> {
                    // Handle previous action
                    (applicationContext as BaseApplicationClass).prevTrack()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.prevClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }

                BaseApplicationClass.Companion.ACTION_PLAY -> {
                    // Handle play/pause action
                    (getApplicationContext() as BaseApplicationClass).togglePlayPause()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.playClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }

                "action_click" -> startActivity(
                    Intent(this, MusicOverviewActivity::class.java).putExtra(
                        "id",
                        intent.getStringExtra("id")
                    )
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }

        return START_STICKY
    }

    fun setCallback(actionPlaying: ActionPlaying?) {
        this.actionPlaying = actionPlaying
    }
}
