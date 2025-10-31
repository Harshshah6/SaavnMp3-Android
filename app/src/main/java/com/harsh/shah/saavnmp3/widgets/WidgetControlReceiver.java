package com.harsh.shah.saavnmp3.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.harsh.shah.saavnmp3.BaseApplicationClass;

public class WidgetControlReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) return;

        BaseApplicationClass a = new BaseApplicationClass();

        switch (action) {
            case "ACTION_TOGGLE_PLAY":
                Log.d("MelotuneWidget", "Play/Pause pressed");
                a.togglePlayPause();
                break;

            case "ACTION_NEXT":
                Log.d("MelotuneWidget", "Next pressed");
                a.nextTrack();
                break;

            case "ACTION_PREV":
                Log.d("MelotuneWidget", "Previous pressed");
                a.prevTrack();
                break;
        }
    }
}