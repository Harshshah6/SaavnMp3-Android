package com.harsh.shah.saavnmp3.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager

class WidgetControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action

        if (action == null) return

        when (action) {
            "ACTION_TOGGLE_PLAY" -> {
                Log.d("MelotuneWidget", "Play/Pause pressed")
                MusicPlayerManager.togglePlayPause()
            }

            "ACTION_NEXT" -> {
                Log.d("MelotuneWidget", "Next pressed")
                MusicPlayerManager.nextTrack()
            }

            "ACTION_PREV" -> {
                Log.d("MelotuneWidget", "Previous pressed")
                MusicPlayerManager.prevTrack()
            }
        }
    }
}
