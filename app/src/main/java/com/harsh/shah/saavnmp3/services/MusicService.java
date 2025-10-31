package com.harsh.shah.saavnmp3.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.harsh.shah.saavnmp3.BaseApplicationClass;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;

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
        if (intent == null || intent.getExtras() == null) return START_STICKY;

        String actionName = intent.getExtras().getString("action","");
        if (actionName != null) {
            switch (actionName) {
                case BaseApplicationClass.ACTION_NEXT:
                    // Handle next action
                    if (actionPlaying != null) {
                        actionPlaying.nextClicked();
                    }
                    break;
                case BaseApplicationClass.ACTION_PREV:
                    // Handle previous action
                    if (actionPlaying != null) {
                        actionPlaying.prevClicked();
                    }
                    break;
                case BaseApplicationClass.ACTION_PLAY:
                    // Handle play/pause action
                    if (actionPlaying != null) {
                        actionPlaying.playClicked();
                    }
                    break;

                case "action_click":
                    startActivity(new Intent(this, MusicOverviewActivity.class).putExtra("id", intent.getStringExtra("id")).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                    break;
            }
        }

        return START_STICKY;
    }

    public void setCallback(ActionPlaying actionPlaying) {
        this.actionPlaying = actionPlaying;
    }
}
