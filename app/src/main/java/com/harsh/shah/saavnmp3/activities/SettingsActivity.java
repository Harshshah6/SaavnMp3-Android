package com.harsh.shah.saavnmp3.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.harsh.shah.saavnmp3.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        final SettingsSharedPrefManager settingsSharedPrefManager = new SettingsSharedPrefManager(this);

        binding.downloadOverCellular.setOnCheckChangeListener(settingsSharedPrefManager::setDownloadOverCellular);
        binding.highQualityTrack.setOnCheckChangeListener(settingsSharedPrefManager::setHighQualityTrack);
        binding.storeInCache.setOnCheckChangeListener(settingsSharedPrefManager::setStoreInCache);
        binding.explicit.setOnCheckChangeListener(settingsSharedPrefManager::setExplicit);

        binding.downloadOverCellular.setChecked(settingsSharedPrefManager.getDownloadOverCellular());
        binding.highQualityTrack.setChecked(settingsSharedPrefManager.getHighQualityTrack());
        binding.storeInCache.setChecked(settingsSharedPrefManager.getStoreInCache());
        binding.explicit.setChecked(settingsSharedPrefManager.getExplicit());

        //TODO: Theme Switcher
            // Switch to Dark Mode
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);


        // folder night changing names


            // Switch to Light Mode
            //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

    }

    public void backPress(View view) {
        finish();
    }

    public static final class SettingsSharedPrefManager {
        SharedPreferences sharedPreferences;
        public SettingsSharedPrefManager(Context context){
            sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        }

        public void setDownloadOverCellular(boolean value){
            sharedPreferences.edit().putBoolean("download_over_cellular", value).apply();
        }
        public boolean getDownloadOverCellular(){
            return sharedPreferences.getBoolean("download_over_cellular", true);
        }

        public void setHighQualityTrack(boolean value){
            sharedPreferences.edit().putBoolean("high_quality_track", value).apply();
        }
        public boolean getHighQualityTrack(){
            return sharedPreferences.getBoolean("high_quality_track", true);
        }

        public void setStoreInCache(boolean value){
            sharedPreferences.edit().putBoolean("store_in_cache", value).apply();
        }
        public boolean getStoreInCache(){
            return sharedPreferences.getBoolean("store_in_cache", true);
        }

        public void setExplicit(boolean value){
            sharedPreferences.edit().putBoolean("explicit", value).apply();
        }
        public boolean getExplicit() {
            return sharedPreferences.getBoolean("explicit", true);
        }
    }
}