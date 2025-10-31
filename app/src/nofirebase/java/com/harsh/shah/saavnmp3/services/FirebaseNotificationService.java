package com.harsh.shah.saavnmp3.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FirebaseNotificationService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding
    }
}
