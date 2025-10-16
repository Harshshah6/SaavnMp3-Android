package com.harsh.shah.saavnmp3.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harsh.shah.saavnmp3.adapters.ActivityDownloadManagerListAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivityDownloadManagerBinding;
import com.harsh.shah.saavnmp3.utils.TrackDownloader;

public class DownloadManagerActivity extends AppCompatActivity {

    private ActivityDownloadManagerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(new ActivityDownloadManagerListAdapter(TrackDownloader.getDownloadedTracks(this)));
    }

    public void backPress(View view) {
        finish();
    }

}