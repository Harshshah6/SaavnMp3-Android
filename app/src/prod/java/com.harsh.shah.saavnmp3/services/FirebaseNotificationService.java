package com.harsh.shah.saavnmp3.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.activities.MainActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class FirebaseNotificationService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "6969";
    private static final String CHANNEL_NAME = "Melotune-Firebase-Cloud-Messaging";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "Notification";
        String body = "";
        String clickUrl = null;
        String imageUrl = null;

        // Handle data payload
        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            title = data.get("title");
            body = data.get("body");
            clickUrl = data.get("click_url");
            imageUrl = data.get("image");
        }

        // Handle notification payload (optional)
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
            if (clickUrl == null) clickUrl = remoteMessage.getNotification().getClickAction();
            if (imageUrl == null) imageUrl = String.valueOf(remoteMessage.getNotification().getImageUrl());
        }

        showNotification(title, body, clickUrl, imageUrl);
    }

    private void showNotification(String title, String message, String clickUrl, String imageUrl) {
        Intent intent;
        if (clickUrl != null && clickUrl.startsWith("http")) {
            // Open browser or WebView
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl));
        } else {
            // Default to opening app
            intent = new Intent(this, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                : PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Replace with your app's icon
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Load and add image if available
        if (imageUrl != null && imageUrl.startsWith("http")) {
            Bitmap bitmap = getBitmapFromURL(imageUrl);
            if (bitmap != null) {
                builder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setSummaryText(message));
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
            }
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("App Notifications");
            notificationManager.createNotificationChannel(channel);
        }

        // Show notification
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // Helper method to load image from URL
    private Bitmap getBitmapFromURL(String strUrl) {
        try {
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e("FCM", "getBitmapFromURL: ", e);
            return null;
        }
    }
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // You can send the token to your server if needed
    }
}
