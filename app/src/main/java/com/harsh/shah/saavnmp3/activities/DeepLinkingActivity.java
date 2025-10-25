package com.harsh.shah.saavnmp3.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harsh.shah.saavnmp3.R;

public class DeepLinkingActivity extends AppCompatActivity {

    private static final String TAG = "DeepLinkingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deep_linking);
        handleIntent(getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        finish();
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        final Uri data = intent.getData();
        if (data == null) {
            Log.w(TAG, "No data in intent");
            return;
        }

        String host = data.getHost();
        String path = data.getPath();
        Log.d(TAG, "Deep link host: " + host + " path: " + path);

        if (path == null) {
            openMainScreen();
            return;
        }

        if (path.startsWith("/song")) {
            openPlayerForSongUrl(data.toString());
        } else if (path.startsWith("/album")) {
            openAlbumFromUrl(data.toString());
        } else if (path.startsWith("/featured")) {
            openPlaylistFromUrl(data.toString());
        } else if (path.startsWith("/artist")) {
            openArtistFromUrl(data.toString());
        }

    }

    private void openPlayerForSongUrl(String songUrl) {
        Intent i = new Intent(this, MusicOverviewActivity.class);
        i.putExtra("type", "clear").putExtra("id", songUrl);
        startActivity(i);
    }

    private void openAlbumFromUrl(String albumUrl) {
        startActivity(new Intent(DeepLinkingActivity.this, ListActivity.class)
                .putExtra("type", "album")
                .putExtra("id", albumUrl)
        );
    }

    private void openPlaylistFromUrl(String albumUrl) {
        startActivity(new Intent(DeepLinkingActivity.this, ListActivity.class)
                .putExtra("type", "playlist")
                .putExtra("id", albumUrl)
        );
    }

    private void openArtistFromUrl(String albumUrl) {
        startActivity(new Intent(DeepLinkingActivity.this, ArtistProfileActivity.class)
                .putExtra("data", albumUrl)
        );
    }

    private void openMainScreen() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}