package com.harsh.shah.saavnmp3.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.harsh.shah.saavnmp3.R

class DeepLinkingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deep_linking)
        handleIntent(getIntent())
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        finish()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.getData()
        if (data == null) {
            Log.w(TAG, "No data in intent")
            return
        }

        val host = data.getHost()
        val path = data.getPath()
        Log.d(TAG, "Deep link host: " + host + " path: " + path)

        if (path == null) {
            openMainScreen()
            return
        }

        if (path.startsWith("/song")) {
            openPlayerForSongUrl(data.toString())
        } else if (path.startsWith("/album")) {
            openAlbumFromUrl(data.toString())
        } else if (path.startsWith("/featured")) {
            openPlaylistFromUrl(data.toString())
        } else if (path.startsWith("/artist")) {
            openArtistFromUrl(data.toString())
        }
    }

    private fun openPlayerForSongUrl(songUrl: String?) {
        val i = Intent(this, MusicOverviewActivity::class.java)
        i.putExtra("type", "clear").putExtra("id", songUrl)
        startActivity(i)
    }

    private fun openAlbumFromUrl(albumUrl: String?) {
        startActivity(
            Intent(this@DeepLinkingActivity, ListActivity::class.java)
                .putExtra("type", "album")
                .putExtra("id", albumUrl)
        )
    }

    private fun openPlaylistFromUrl(albumUrl: String?) {
        startActivity(
            Intent(this@DeepLinkingActivity, ListActivity::class.java)
                .putExtra("type", "playlist")
                .putExtra("id", albumUrl)
        )
    }

    private fun openArtistFromUrl(albumUrl: String?) {
        startActivity(
            Intent(this@DeepLinkingActivity, ArtistProfileActivity::class.java)
                .putExtra("data", albumUrl)
        )
    }

    private fun openMainScreen() {
        val i = Intent(this, MainActivity::class.java)
        startActivity(i)
    }

    companion object {
        private const val TAG = "DeepLinkingActivity"
    }
}
