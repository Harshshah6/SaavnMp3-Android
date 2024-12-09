package com.harsh.shah.saavnmp3.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.harsh.shah.saavnmp3.ApplicationClass;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        Intent intent1 = new Intent(context, MusicService.class);

        switch (intent.getAction()) {
            case ApplicationClass.ACTION_NEXT:
                // Handle next action
                //Toast.makeText(context, "next", Toast.LENGTH_SHORT).show();
                intent1.putExtra("action", intent.getAction());
                //context.startService(intent1);
                break;
            case ApplicationClass.ACTION_PREV:
                // Handle previous action
                //Toast.makeText(context, "prev", Toast.LENGTH_SHORT).show();
                intent1.putExtra("action", intent.getAction());
                //context.startService(intent1);
                break;
            case ApplicationClass.ACTION_PLAY:
                // Handle play/pause action
                //Toast.makeText(context, "play", Toast.LENGTH_SHORT).show();
                intent1.putExtra("action", intent.getAction());
                //context.startService(intent1);
                if (ApplicationClass.mediaPlayerUtil.isPlaying())
                    ApplicationClass.mediaPlayerUtil.pause();
                else
                    ApplicationClass.mediaPlayerUtil.start();
                break;
        }
    }
}
