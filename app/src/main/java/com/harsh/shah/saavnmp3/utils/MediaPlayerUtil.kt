package com.harsh.shah.saavnmp3.utils

import android.media.MediaPlayer

class MediaPlayerUtil private constructor() : MediaPlayer() {
    private val TAG = "MediaPlayerUtil"

    companion object {
        val instance: MediaPlayerUtil? = null
            get() {
                if (field != null) return field
                return MediaPlayerUtil()
            }
    }
}
