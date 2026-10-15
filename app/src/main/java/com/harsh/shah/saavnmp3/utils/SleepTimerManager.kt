package com.harsh.shah.saavnmp3.utils

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.media3.common.Player
import com.harsh.shah.saavnmp3.BaseApplicationClass
import java.util.concurrent.CopyOnWriteArraySet

object SleepTimerManager {
    interface SleepTimerListener {
        fun onTimerTick(remainingSeconds: Long)
        fun onTimerFinished()
        fun onTimerCancelled()
    }

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<SleepTimerListener>()

    var isTimerActive: Boolean = false
        private set

    var remainingSeconds: Long = 0L
        private set

    var isEndOfTrack: Boolean = false
        private set

    private var initialTrackIdAtStart: String? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isTimerActive) return

            if (isEndOfTrack) {
                // Check if current track has finished or changed
                val player = MusicPlayerManager.player
                val currentId = MusicPlayerManager.MUSIC_ID
                if (player == null || player.playbackState == Player.STATE_ENDED || (initialTrackIdAtStart != null && currentId != initialTrackIdAtStart)) {
                    triggerFinish()
                    return
                }
                handler.postDelayed(this, 1000)
                return
            }

            remainingSeconds--
            notifyTick(remainingSeconds)

            if (remainingSeconds <= 0) {
                triggerFinish()
            } else {
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun addListener(listener: SleepTimerListener) {
        listeners.add(listener)
        if (isTimerActive) {
            listener.onTimerTick(remainingSeconds)
        }
    }

    fun removeListener(listener: SleepTimerListener) {
        listeners.remove(listener)
    }

    fun startTimer(minutes: Int) {
        cancelTimer()
        if (minutes <= 0) return

        isTimerActive = true
        isEndOfTrack = false
        remainingSeconds = minutes * 60L

        notifyTick(remainingSeconds)
        handler.postDelayed(tickRunnable, 1000)

        val ctx = BaseApplicationClass.currentActivity
        if (ctx != null) {
            Toast.makeText(ctx, "Sleep timer set for $minutes minutes", Toast.LENGTH_SHORT).show()
        }
    }

    fun startEndOfTrackTimer() {
        cancelTimer()
        isTimerActive = true
        isEndOfTrack = true
        initialTrackIdAtStart = MusicPlayerManager.MUSIC_ID

        val player = MusicPlayerManager.player
        remainingSeconds = if (player != null && player.duration > 0 && player.currentPosition >= 0) {
            ((player.duration - player.currentPosition) / 1000L).coerceAtLeast(0L)
        } else {
            0L
        }

        notifyTick(remainingSeconds)
        handler.postDelayed(tickRunnable, 1000)

        val ctx = BaseApplicationClass.currentActivity
        if (ctx != null) {
            Toast.makeText(ctx, "Music will stop at the end of this track", Toast.LENGTH_SHORT).show()
        }
    }

    fun addMinutes(minutes: Int) {
        if (!isTimerActive || isEndOfTrack) {
            startTimer(minutes)
            return
        }
        remainingSeconds += minutes * 60L
        notifyTick(remainingSeconds)
        val ctx = BaseApplicationClass.currentActivity
        if (ctx != null) {
            Toast.makeText(ctx, "Added $minutes minutes to sleep timer", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelTimer() {
        handler.removeCallbacks(tickRunnable)
        val wasActive = isTimerActive
        isTimerActive = false
        isEndOfTrack = false
        remainingSeconds = 0L
        initialTrackIdAtStart = null

        if (wasActive) {
            for (listener in listeners) {
                try {
                    listener.onTimerCancelled()
                } catch (_: Exception) {}
            }
        }
    }

    private fun triggerFinish() {
        handler.removeCallbacks(tickRunnable)
        isTimerActive = false
        isEndOfTrack = false
        remainingSeconds = 0L

        // Pause playback safely
        try {
            val player = MusicPlayerManager.player
            if (player != null && player.isPlaying) {
                player.pause()
            }
            MusicPlayerManager.showNotification()
            MusicPlayerManager.updateWidget()
        } catch (_: Exception) {}

        val ctx = BaseApplicationClass.currentActivity
        if (ctx != null) {
            Toast.makeText(ctx, "Sleep timer completed. Playback paused.", Toast.LENGTH_LONG).show()
        }

        for (listener in listeners) {
            try {
                listener.onTimerFinished()
            } catch (_: Exception) {}
        }
    }

    private fun notifyTick(seconds: Long) {
        for (listener in listeners) {
            try {
                listener.onTimerTick(seconds)
            } catch (_: Exception) {}
        }
    }

    fun getFormattedRemainingTime(): String {
        if (!isTimerActive) return "Off"
        if (isEndOfTrack) return "End of Track"
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return String.format("%02d:%02d", m, s)
    }
}
