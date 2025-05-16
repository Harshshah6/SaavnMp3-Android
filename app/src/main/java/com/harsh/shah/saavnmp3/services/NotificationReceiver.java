package com.harsh.shah.saavnmp3.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.harsh.shah.saavnmp3.ApplicationClass;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.activities.MusicOverviewActivity;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            Log.e(TAG, "Received null intent or action");
            return;
        }

        String action = intent.getAction();
        Log.i(TAG, "Received action: " + action);
        
        // Create intent for MusicService
        Intent serviceIntent = new Intent(context, MusicService.class);
        
        // Get ApplicationClass instance
        final ApplicationClass applicationClass = (ApplicationClass) context.getApplicationContext();

        switch (action) {
            case ApplicationClass.ACTION_NEXT:
                Log.i(TAG, "Processing NEXT action");
                try {
                    // First update via ApplicationClass for immediate effect
                    applicationClass.nextTrack();
                    
                    // Then notify service for UI updates
                    serviceIntent.putExtra("action", action);
                    context.startService(serviceIntent);
                    
                    // Show visual feedback
                    Toast.makeText(context, "Playing next track", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing next action", e);
                }
                break;
                
            case ApplicationClass.ACTION_PREV:
                Log.i(TAG, "Processing PREVIOUS action");
                try {
                    // First update via ApplicationClass for immediate effect
                    applicationClass.prevTrack();
                    
                    // Then notify service for UI updates
                    serviceIntent.putExtra("action", action);
                    context.startService(serviceIntent);
                    
                    // Show visual feedback
                    Toast.makeText(context, "Playing previous track", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error processing previous action", e);
                }
                break;
                
            case ApplicationClass.ACTION_PLAY:
                Log.i(TAG, "Processing PLAY/PAUSE action");
                try {
                    // Toggle playback
                    applicationClass.togglePlayPause();
                    
                    // Notify service
                    serviceIntent.putExtra("action", action);
                    serviceIntent.putExtra("fromNotification", true);
                    context.startService(serviceIntent);
                    
                    // Update notification with current state
                    boolean isPlaying = ApplicationClass.player.isPlaying();
                    applicationClass.showNotification(isPlaying ? 
                        R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
                    
                    // Show visual feedback
                    Toast.makeText(context, isPlaying ? "Playing" : "Paused", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error toggling playback", e);
                }
                break;
                
            case "action_click":
                Log.i(TAG, "Processing CLICK action");
                try {
                    // Launch activity for the current track
                    Intent activityIntent = new Intent(context, MusicOverviewActivity.class)
                        .putExtra("id", ApplicationClass.MUSIC_ID)
                        .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(activityIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Error launching activity", e);
                }
                break;
                
            default:
                Log.i(TAG, "Unknown action received: " + action);
                break;
        }
    }
}
