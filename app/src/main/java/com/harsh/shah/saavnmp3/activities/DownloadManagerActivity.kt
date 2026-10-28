package com.harsh.shah.saavnmp3.activities

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.harsh.shah.saavnmp3.adapters.ActivityDownloadManagerListAdapter
import com.harsh.shah.saavnmp3.databinding.ActivityDownloadManagerBinding
import com.harsh.shah.saavnmp3.utils.ThemeManager
import com.harsh.shah.saavnmp3.utils.TrackDownloader
import com.harsh.shah.saavnmp3.utils.attachSnapHelper

class DownloadManagerActivity : AppCompatActivity() {
    private var binding: ActivityDownloadManagerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        val widthDp = resources.configuration.screenWidthDp
        val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || widthDp >= 600) {
            maxOf(2, if (widthDp > 0) widthDp / 320 else 2)
        } else {
            1
        }
        binding!!.recyclerView.setLayoutManager(GridLayoutManager(this, spanCount))
        val tracks = TrackDownloader.getDownloadedTracks(this)
        binding!!.recyclerView.setAdapter(
            ActivityDownloadManagerListAdapter(
                tracks.filterNotNull().toMutableList()
            )
        )
    }

    fun backPress(view: View?) {
        finish()
    }
}
