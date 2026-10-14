package com.harsh.shah.saavnmp3.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.common.Player
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager

class WidgetControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action ?: return
        Log.i("MelotuneWidget", "Widget Action received: $action")

        if (context != null) {
            MusicPlayerManager.init(context.applicationContext)
        }

        when (action) {
            "ACTION_TOGGLE_PLAY" -> {
                MusicPlayerManager.togglePlayPause()
            }

            "ACTION_NEXT" -> {
                MusicPlayerManager.nextTrack()
            }

            "ACTION_PREV" -> {
                MusicPlayerManager.prevTrack()
            }

            "ACTION_SHUFFLE" -> {
                val p = MusicPlayerManager.player ?: return
                p.shuffleModeEnabled = !p.shuffleModeEnabled
                MusicPlayerManager.updateWidget()
            }

            "ACTION_REPEAT" -> {
                val p = MusicPlayerManager.player ?: return
                p.repeatMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                MusicPlayerManager.updateWidget()
            }
        }
    }
}
