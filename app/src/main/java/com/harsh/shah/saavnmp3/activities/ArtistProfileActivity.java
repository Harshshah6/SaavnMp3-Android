package com.harsh.shah.saavnmp3.activities;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.adapters.ActivityArtistProfileTopAlbumsAdapter;
import com.harsh.shah.saavnmp3.adapters.ActivityArtistProfileTopSongsAdapter;
import com.harsh.shah.saavnmp3.adapters.ActivitySeeMoreListAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivityArtistProfileBinding;
import com.harsh.shah.saavnmp3.model.BasicDataRecord;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.AlbumsSearch;
import com.harsh.shah.saavnmp3.records.ArtistSearch;
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The `ArtistProfileActivity` class displays the profile information of an artist,
 * including their name, image, top songs, top albums, and singles.
 * It fetches the artist's data from a remote API and handles network connectivity changes.
 *
 * <p>
 * This activity uses a collapsing toolbar layout to provide a visually appealing
 * header that expands and collapses as the user scrolls.
 * It also utilizes Shimmer effect as placeholder while data is loading.
 * </p>
 */
public class ArtistProfileActivity extends AppCompatActivity {

    private final String TAG = "ArtistProfileActivity";
    ActivityArtistProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtistProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.collapsingToolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent));

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

//        binding.collapsingToolbarLayout.setTitle("Artist Name");

        binding.collapsingToolbarAppbarlayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if (verticalOffset == 0) {
            } else {
            }
        });

        binding.topSongsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.topAlbumsRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        binding.topSinglesRecyclerview.setLayoutManager(new LinearLayoutManager(this));

        binding.topSongsSeeMore.setOnClickListener(v -> {
            startActivity(new Intent(ArtistProfileActivity.this, SeeMoreActivity.class)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_SONGS.name())
                    .putExtra("artist_name", binding.artistName.getText().toString()));
        });
        binding.topAlbumsSeeMore.setOnClickListener(v -> {
            startActivity(new Intent(ArtistProfileActivity.this, SeeMoreActivity.class)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_ALBUMS.name())
                    .putExtra("artist_name", binding.artistName.getText().toString()));
        });
        binding.topSinglesSeeMore.setVisibility(View.GONE);
        //binding.topSinglesSeeMore.setOnClickListener(v -> startActivity(seeMoreIntent.putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_SINGLES.name())));

        showShimmerData();
        showData();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private String artistId = "9999";
    private ArtistSearch artistSearch;

    void showData() {
        if (getIntent().getExtras() == null) return;
        Log.i(TAG, "showData: " + getIntent().getExtras());
        final String artist = getIntent().getExtras().getString("data", "null");
        final ApiManager apiManager = new ApiManager(this);
        final RequestNetwork.RequestListener responseListener = new RequestNetwork.RequestListener() {
            final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(ArtistProfileActivity.this);

            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                try {
                    artistSearch = new Gson().fromJson(response, ArtistSearch.class);
                    Log.i(TAG, "onResponse: " + response);
                    sharedPreferenceManager.setArtistData(artistSearch.data().id(), artistSearch);
                    Log.i(TAG, "onResponse: " + sharedPreferenceManager.getArtistData(artistSearch.data().id()));
                    display();
                } catch (Exception e) {
                    Log.e(TAG, "onResponse: ", e);
                    finish();
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                Log.i(TAG, "onErrorResponse: " + message);
                if (!artistId.equals("9999")) {
                    ArtistSearch offlineData = sharedPreferenceManager.getArtistData(artistId);
                    if (offlineData != null) {
                        artistSearch = offlineData;
                        display();
                    }
                }
            }
        };

        if ((artist.startsWith("http") || artist.startsWith("www")) && artist.contains("jiosaavn.com")) {
            apiManager.retrieveArtistsByLink(artist, null, null, null, null, null, responseListener);
            return;
        }

        final BasicDataRecord artistItem = new Gson().fromJson(artist, BasicDataRecord.class);
        if (artistItem == null) {
            return;
        }
        artistId = artistItem.id();

        Picasso.get().load(Uri.parse(artistItem.image())).into(binding.artistImg);
        binding.artistName.setText(artistItem.title());
        binding.collapsingToolbarLayout.setTitle(artistItem.title());

        apiManager.retrieveArtistById(artistId, responseListener);
    }

    private void display() {
        Log.i(TAG, "display: " + artistSearch);
        if (artistSearch.success()) {
            Picasso.get().load(Uri.parse(artistSearch.data().image().get(artistSearch.data().image().size() - 1).url())).into(binding.artistImg);
            binding.artistName.setText(artistSearch.data().name());
            binding.collapsingToolbarLayout.setTitle(artistSearch.data().name());
            binding.topSongsRecyclerview.setAdapter(new ActivityArtistProfileTopSongsAdapter(artistSearch.data().topSongs()));
            binding.topAlbumsRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(artistSearch.data().topAlbums()));
            binding.topSinglesRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(artistSearch.data().singles()));
        }
    }

    @NonNull
    private static List<SongResponse.Song> getShimmerData() {
        final List<SongResponse.Song> shimmerData = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            shimmerData.add(new SongResponse.Song(
                    "<shimmer>",
                    "",
                    "",
                    "",
                    "",
                    0.0,
                    "",
                    false,
                    0,
                    "",
                    false,
                    "",
                    new SongResponse.Lyrics("", "", ""),
                    "",
                    "",
                    new SongResponse.Album("", "", ""),
                    new SongResponse.Artists(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                    new ArrayList<>(),
                    new ArrayList<>()
            ));
        }
        return shimmerData;
    }


    void showShimmerData() {
        final List<SongResponse.Song> shimmerData = getShimmerData();
        final List<AlbumsSearch.Data.Results> shimmerDataAlbum = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            shimmerDataAlbum.add(new AlbumsSearch.Data.Results(
                    "<shimmer>",
                    null,
                    null,
                    null,
                    0,
                    null,
                    0,
                    null,
                    false,
                    null,
                    null
            ));
        }

        binding.topSongsRecyclerview.setAdapter(new ActivityArtistProfileTopSongsAdapter(shimmerData));
        binding.topAlbumsRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(shimmerDataAlbum));
        binding.topSinglesRecyclerview.setAdapter(new ActivityArtistProfileTopAlbumsAdapter(shimmerDataAlbum));

        ArtistSearch offlineData = SharedPreferenceManager.getInstance(this).getArtistData(artistId);
        if (offlineData != null) {
            artistSearch = offlineData;
            display();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void backPress(View v){
        finish();
    }

}
