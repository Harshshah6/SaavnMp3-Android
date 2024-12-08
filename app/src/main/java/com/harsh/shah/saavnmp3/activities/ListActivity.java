package com.harsh.shah.saavnmp3.activities;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.adapters.ActivityListSongsItemAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivityListBinding;

public class ListActivity extends AppCompatActivity {

    ActivityListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(new ActivityListSongsItemAdapter());
    }

    public void backPress(View view) {
        finish();
    }

}