package com.harsh.shah.saavnmp3.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.BuildConfig;
import com.harsh.shah.saavnmp3.databinding.ActivityAboutBinding;
import com.harsh.shah.saavnmp3.model.aboutus.Contributors;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.network.utility.RequestNetworkController;
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView;

import java.util.HashMap;

public class AboutActivity extends AppCompatActivity {

    ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> finish());

        binding.versionTxt.getTitleTextView().setText(BuildConfig.VERSION_NAME);

        binding.email.setOnClickListener(view -> openUrl("mailto:harshsandeep23@gmail.com"));
        binding.sourceCode.setOnClickListener(view -> openUrl("https://github.com/harshshah6/SaavnMp3-Android"));
        binding.discord.setOnClickListener(view -> Toast.makeText(AboutActivity.this, "Oops, No Discord Server found.", Toast.LENGTH_SHORT).show());
        binding.instagram.setOnClickListener(view -> openUrl("https://www.instagram.com/harsh_.s._shah/"));
        binding.telegram.setOnClickListener(view -> openUrl("https://t.me/legendary_streamer_official"));
        binding.rate.setOnClickListener(view -> openUrl("https://github.com/harshshah6/SaavnMp3-Android"));

        new RequestNetwork(this).startRequestNetwork(RequestNetworkController.GET, "https://androsketchui.vercel.app/api/github/harshshah6/saavnmp3-android/contributors", "", new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
//                final BottomSheetItemView bottomSheetItemView = new BottomSheetItemView(AboutActivity.this, "Harsh Shah", "https://avatars.githubusercontent.com/u/69447184?v=4", "");
//                bottomSheetItemView.setOnClickListener(view -> openUrl("https://github.com/harshshah6"));
//                binding.layoutContributors.addView(bottomSheetItemView);
                final Contributors contributors = new Gson().fromJson(response, Contributors.class);
                Log.i("AboutActivity", "contributors: " + contributors);
                for (Contributors.Contributor contributor : contributors.contributors()) {
                    final BottomSheetItemView item = new BottomSheetItemView(AboutActivity.this, contributor.login(), contributor.avatar_url(), "");
                    item.setOnClickListener(view -> openUrl(contributor.html_url()));
                    binding.layoutContributors.addView(item);
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {

            }
        });

    }

    private void openUrl(final String url) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_VIEW);
        sendIntent.setData(Uri.parse(url));
        startActivity(sendIntent);
    }
}