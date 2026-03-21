package com.harsh.shah.saavnmp3.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.harsh.shah.saavnmp3.BaseApplicationClass

class WidgetControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.getAction()

        if (action == null) return

        val a = BaseApplicationClass()

        when (action) {
            "ACTION_TOGGLE_PLAY" -> {
                Log.d("MelotuneWidget", "Play/Pause pressed")
                a.togglePlayPause()
            }

            "ACTION_NEXT" -> {
                Log.d("MelotuneWidget", "Next pressed")
                a.nextTrack()
            }

            "ACTION_PREV" -> {
                Log.d("MelotuneWidget", "Previous pressed")
                a.prevTrack()
            }
        }
    }
}
