package com.harsh.shah.saavnmp3.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.AlbumSearch;

import java.util.HashMap;

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
        Uri data = intent.getData();
        if (data == null) {
            Log.w(TAG, "No data in intent");
            return;
        }

        // Example incoming: https://www.jiosaavn.com/song/rebel-song-from-kantara-a-legend-chapter-1-hindi/NCoDVxp1fGs
        String host = data.getHost();                   // "www.jiosaavn.com"
        String path = data.getPath();                   // "/song/slug/ID"
        Log.d(TAG, "Deep link host: " + host + " path: " + path);

        if(path == null){
            openMainScreen();
            return;
        }

        if(path.startsWith("/song")){
            openPlayerForSongUrl(data.toString());
        }else if(path.startsWith("/album")) {
            openAlbumFromUrl(data.toString());
        }

    }

    private void openPlayerForSongUrl(String songUrl) {
        Intent i = new Intent(this, MusicOverviewActivity.class);
        i.putExtra("type", "clear").putExtra("id", songUrl);
        startActivity(i);
    }

    private void openAlbumFromUrl(String albumUrl){
        startActivity(new Intent(DeepLinkingActivity.this, ListActivity.class)
                .putExtra("type", "album")
                .putExtra("id", albumUrl)
        );
    }

    private void openMainScreen() {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}