package com.harsh.shah.saavnmp3.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.harsh.shah.saavnmp3.BaseApplicationClass;
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
        final BaseApplicationClass baseApplicationClass = (BaseApplicationClass) context.getApplicationContext();

        switch (action) {
            case BaseApplicationClass.ACTION_NEXT:
                Log.i(TAG, "Processing NEXT action");
                try {
                    // First check if player exists
                    if (BaseApplicationClass.player == null) {
                        Log.e(TAG, "Player is null in notification receiver - cannot process NEXT action");
                        return;
                    }
                    
                    // Call nextTrack through ApplicationClass for immediate effect
                    baseApplicationClass.nextTrack();
                    
                    // Then notify the service for UI updates
                    serviceIntent.putExtra("action", action);
                    context.startService(serviceIntent);
                    
                    // Visual feedback is already handled in the activity when it receives the service update
                } catch (Exception e) {
                    Log.e(TAG, "Error processing next action: " + e.getMessage(), e);
                    Toast.makeText(context, "Error playing next track", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case BaseApplicationClass.ACTION_PREV:
                Log.i(TAG, "Processing PREVIOUS action");
                try {
                    // First check if player exists
                    if (BaseApplicationClass.player == null) {
                        Log.e(TAG, "Player is null in notification receiver - cannot process PREV action");
                        return;
                    }
                    
                    // If we're already at the beginning of the track, go to previous track
                    // Otherwise just restart the current track (standard music player behavior)
                    if (BaseApplicationClass.player.getCurrentPosition() > 3000) {
                        BaseApplicationClass.player.seekTo(0);
                        BaseApplicationClass.player.play();
                    } else {
                        // Call prevTrack for track change
                        baseApplicationClass.prevTrack();
                    }
                    
                    // Then notify service for UI updates
                    serviceIntent.putExtra("action", action);
                    context.startService(serviceIntent);
                    
                    // Visual feedback is already handled in the activity when it receives the service update
                } catch (Exception e) {
                    Log.e(TAG, "Error processing previous action: " + e.getMessage(), e);
                    Toast.makeText(context, "Error playing previous track", Toast.LENGTH_SHORT).show();
                }
                break;
                
            case BaseApplicationClass.ACTION_PLAY:
                Log.i(TAG, "Processing PLAY/PAUSE action");
                try {
                    // Toggle playback
                    baseApplicationClass.togglePlayPause();
                    
                    // Notify service
                    serviceIntent.putExtra("action", action);
                    serviceIntent.putExtra("fromNotification", true);
                    context.startService(serviceIntent);
                    
                    // Update notification with current state
                    boolean isPlaying = BaseApplicationClass.player.isPlaying();
                    baseApplicationClass.showNotification(isPlaying ?
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
                        .putExtra("id", BaseApplicationClass.MUSIC_ID)
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
