package com.harsh.shah.saavnmp3.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.harsh.shah.saavnmp3.utils.TrackDownloader

class DownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == TrackDownloader.ACTION_CANCEL_DOWNLOAD) {
            TrackDownloader.cancelDownload(context)
        }
    }
}

