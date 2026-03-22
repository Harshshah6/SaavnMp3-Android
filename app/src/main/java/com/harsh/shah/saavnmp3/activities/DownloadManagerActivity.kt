package com.harsh.shah.saavnmp3.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.harsh.shah.saavnmp3.adapters.ActivityDownloadManagerListAdapter
import com.harsh.shah.saavnmp3.databinding.ActivityDownloadManagerBinding
import com.harsh.shah.saavnmp3.utils.TrackDownloader

class DownloadManagerActivity : AppCompatActivity() {
    private var binding: ActivityDownloadManagerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadManagerBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
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
