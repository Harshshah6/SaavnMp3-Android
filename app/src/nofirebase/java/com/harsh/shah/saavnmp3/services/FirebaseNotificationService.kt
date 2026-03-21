package com.harsh.shah.saavnmp3.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class FirebaseNotificationService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding
    }
}
