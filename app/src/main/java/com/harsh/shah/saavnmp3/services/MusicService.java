package com.harsh.shah.saavnmp3.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.harsh.shah.saavnmp3.ApplicationClass;

public class MusicService extends Service {

    private final IBinder mBinder = new MyBinder();

    ActionPlaying actionPlaying;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String actionName = intent.getStringExtra("action");
        if (actionName != null) {
            switch (actionName) {
                case ApplicationClass.ACTION_NEXT:
                    // Handle next action
                    if (actionPlaying != null) {
                        actionPlaying.nextClicked();
                    }
                    break;
                case ApplicationClass.ACTION_PREV:
                    // Handle previous action
                    if (actionPlaying != null) {
                        actionPlaying.prevClicked();
                    }
                    break;
                case ApplicationClass.ACTION_PLAY:
                    // Handle play/pause action
                    if (actionPlaying != null) {
                        actionPlaying.playClicked();
                    }
                    break;
            }
        }

        return START_STICKY;
    }

    public void setCallback(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }
}
