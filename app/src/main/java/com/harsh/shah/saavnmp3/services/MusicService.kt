package com.harsh.shah.saavnmp3.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager

class MusicService : Service() {
    private val mBinder: IBinder = MyBinder()

    var actionPlaying: ActionPlaying? = null

    override fun onCreate() {
        super.onCreate()
        MusicPlayerManager.musicService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (MusicPlayerManager.musicService == this) {
            MusicPlayerManager.musicService = null
        }
    }

    override fun onBind(intent: Intent?): IBinder {
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
                MusicPlayerManager.ACTION_NEXT -> {
                    // Handle next action
                    MusicPlayerManager.nextTrack()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.nextClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }

                MusicPlayerManager.ACTION_PREV -> {
                    // Handle previous action
                    MusicPlayerManager.prevTrack()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.prevClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }

                MusicPlayerManager.ACTION_PLAY -> {
                    // Handle play/pause action
                    MusicPlayerManager.togglePlayPause()
                    if (actionPlaying != null) {
                        try {
                            actionPlaying!!.playClicked()
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error in callback", e)
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    fun setCallback(actionPlaying: ActionPlaying?) {
        this.actionPlaying = actionPlaying
    }
}
