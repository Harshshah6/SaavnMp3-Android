package com.harsh.shah.saavnmp3.activities;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harsh.shah.saavnmp3.databinding.ActivityDownloadManagerBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadManagerActivity extends AppCompatActivity {

    private ActivityDownloadManagerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDownloadManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    public void backPress(View view) {
        finish();
    }

    private List<DownloadManagerItem> getM4AFiles() {
        List<DownloadManagerItem> m4aFiles = new ArrayList<>();
        File musicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "/Melotune");
        if (musicDir.exists() && musicDir.isDirectory()) {
            File[] files = musicDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".m4a")) {

                    }
                }
            }
        }
        return m4aFiles;
    }

    record DownloadManagerItem(
            String title,
            String artist
    ) {
    }
}