package com.harsh.shah.saavnmp3.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.BaseApplicationClass;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.adapters.ActivityListSongsItemAdapter;
import com.harsh.shah.saavnmp3.adapters.UserCreatedSongsListAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivityListBinding;
import com.harsh.shah.saavnmp3.databinding.ActivityListMoreInfoBottomSheetBinding;
import com.harsh.shah.saavnmp3.databinding.UserCreatedListActivityMoreBottomSheetBinding;
import com.harsh.shah.saavnmp3.model.AlbumItem;
import com.harsh.shah.saavnmp3.model.BasicDataRecord;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.AlbumSearch;
import com.harsh.shah.saavnmp3.records.PlaylistSearch;
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    ActivityListBinding binding;

    private final List<String> trackQueue = new ArrayList<>();

    private static final String TAG = "ListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.addMoreSongs.setVisibility(View.GONE);

        Log.i("ListActivity", "onCreate: reached ListActivity");

        showShimmerData();

        binding.playAllBtn.setOnClickListener(view -> {
            if (!trackQueue.isEmpty()) {
                ((BaseApplicationClass) getApplicationContext()).setTrackQueue(trackQueue);
                ((BaseApplicationClass) getApplicationContext()).nextTrack();
                Log.i(TAG, "trackQueueSet: " + BaseApplicationClass.trackQueue + " With POS " + BaseApplicationClass.track_position);
                startActivity(new Intent(ListActivity.this, MusicOverviewActivity.class).putExtra("id", trackQueue.get(0)));
            }
        });
        final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(ListActivity.this);

        binding.addToLibrary.setOnClickListener(view -> {
            if (albumItem == null) return;

            if (isAlbumInLibrary(albumItem, sharedPreferenceManager.getSavedLibrariesData())) {

                new MaterialAlertDialogBuilder(ListActivity.this)
                        .setTitle("Are you sure?")
                        .setMessage("Do you want to remove this album from your library?")
                        .setPositiveButton("Yes", (dialogInterface, i) -> {
                            int index = getAlbumIndexInLibrary(albumItem, sharedPreferenceManager.getSavedLibrariesData());
                            if (index == -1) return;
                            sharedPreferenceManager.removeLibraryFromSavedLibraries(index);
                            Snackbar.make(binding.getRoot(), "Removed from Library", Snackbar.LENGTH_SHORT).show();
                            updateAlbumInLibraryStatus();
                            finish();
                        })
                        .setNegativeButton("No", (dialogInterface, i) -> {

                        })
                        .show();
            } else {
                SavedLibraries.Library library = new SavedLibraries.Library(
                        albumItem.id(),
                        false,
                        isAlbum,
                        binding.albumTitle.getText().toString(),
                        albumItem.albumCover(),
                        binding.albumSubTitle.getText().toString(),
                        new ArrayList<>()
                );
                sharedPreferenceManager.addLibraryToSavedLibraries(library);
                Snackbar.make(binding.getRoot(), "Added to Library", Snackbar.LENGTH_SHORT).show();
            }

            updateAlbumInLibraryStatus();
        });

        binding.addMoreSongs.setOnClickListener(view -> {
            startActivity(new Intent(ListActivity.this, SearchActivity.class));
        });

        binding.moreIcon.setOnClickListener(view -> onMoreIconClicked());

        showData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getExtras() != null && getIntent().getExtras().getBoolean("createdByUser", false)) {
            onUserCreatedFetch();
        }
    }

    private void onMoreIconClicked() {
        if (albumItem == null) return;

        if (isUserCreated) {
            onMoreIconClickedUserCreated();
            return;
        }

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ListActivity.this, R.style.MyBottomSheetDialogTheme);
        final ActivityListMoreInfoBottomSheetBinding _binding = ActivityListMoreInfoBottomSheetBinding.inflate(getLayoutInflater());

        _binding.albumTitle.setText(binding.albumTitle.getText().toString());
        _binding.albumSubTitle.setText(binding.albumSubTitle.getText().toString());
        Picasso.get().load(Uri.parse(albumItem.albumCover())).into(_binding.coverImage);

        final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(ListActivity.this);
        final SavedLibraries savedLibraries = sharedPreferenceManager.getSavedLibrariesData();
        if (savedLibraries == null || savedLibraries.lists() == null) {
            _binding.addToLibrary.getTitleTextView().setText("Add to library");
            _binding.addToLibrary.getIconImageView().setImageResource(R.drawable.round_add_24);
        } else {
            if (isAlbumInLibrary(albumItem, savedLibraries)) {
                _binding.addToLibrary.getTitleTextView().setText("Remove from library");
                _binding.addToLibrary.getIconImageView().setImageResource(R.drawable.round_close_24);
            } else {
                _binding.addToLibrary.getTitleTextView().setText("Add to library");
                _binding.addToLibrary.getIconImageView().setImageResource(R.drawable.round_add_24);
            }
        }
        _binding.addToLibrary.setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
            binding.addToLibrary.performClick();
        });

        for (ArtistData artist : artistData) {
            try {
                final String imgUrl = artist.image().isEmpty() ? "" : artist.image();
                BottomSheetItemView bottomSheetItemView = new BottomSheetItemView(ListActivity.this, artist.name(), imgUrl, artist.id());
                bottomSheetItemView.setFocusable(true);
                bottomSheetItemView.setClickable(true);
                bottomSheetItemView.setOnClickListener(view1 -> {
                    Log.i("ListActivity", "BottomSheetItemView: onCLicked!");
                    startActivity(new Intent(ListActivity.this, ArtistProfileActivity.class)
                            .putExtra("data", new Gson().toJson(
                                    new BasicDataRecord(artist.id(), artist.name(), "", imgUrl)))
                    );
                });
                _binding.main.addView(bottomSheetItemView);
            } catch (Exception e) {
                Log.e("ListActivity", "BottomSheetDialog: ", e);
            }
        }

        bottomSheetDialog.setContentView(_binding.getRoot());
        bottomSheetDialog.create();
        bottomSheetDialog.show();

    }

    private void onMoreIconClickedUserCreated() {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(ListActivity.this, R.style.MyBottomSheetDialogTheme);
        final UserCreatedListActivityMoreBottomSheetBinding _binding = UserCreatedListActivityMoreBottomSheetBinding.inflate(getLayoutInflater());

        _binding.albumTitle.setText(binding.albumTitle.getText().toString());
        _binding.albumSubTitle.setText(binding.albumSubTitle.getText().toString());
        Picasso.get().load(Uri.parse(albumItem.albumCover())).into(_binding.coverImage);

        _binding.removeLibrary.setOnClickListener(view -> {
            bottomSheetDialog.dismiss();
            binding.addToLibrary.performClick();
        });

        bottomSheetDialog.setContentView(_binding.getRoot());
        bottomSheetDialog.create();
        bottomSheetDialog.show();
    }

    private void updateAlbumInLibraryStatus() {
        SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(ListActivity.this);
        if (sharedPreferenceManager.getSavedLibrariesData() == null)
            binding.addToLibrary.setImageResource(R.drawable.round_add_24);
        else {
            final SavedLibraries savedLibraries = sharedPreferenceManager.getSavedLibrariesData();
            binding.addToLibrary.setImageResource(isAlbumInLibrary(albumItem, savedLibraries) ? R.drawable.round_done_24 : R.drawable.round_add_24);
        }
    }

    @SuppressLint("NewApi")
    private boolean isAlbumInLibrary(AlbumItem albumItem, SavedLibraries savedLibraries) {
        if (savedLibraries == null || savedLibraries.lists() == null) {
            return false;
        }
        Log.i("ListActivity", "isAlbumInLibrary: " + savedLibraries);
        if (savedLibraries.lists().isEmpty()) return false;
        return savedLibraries.lists().stream().anyMatch(library -> library.id().equals(albumItem.id()));
    }

    @SuppressLint("NewApi")
    private int getAlbumIndexInLibrary(AlbumItem albumItem, SavedLibraries savedLibraries) {
        if (savedLibraries == null || savedLibraries.lists() == null) {
            return -1;
        }
        Log.i("ListActivity", "getAlbumIndexInLibrary: " + savedLibraries);
        if (savedLibraries.lists().isEmpty()) return -1;
        int index = -1;
        for (SavedLibraries.Library library : savedLibraries.lists()) {
            if (library.id().equals(albumItem.id())) {
                index = savedLibraries.lists().indexOf(library);
                break;
            }
        }
        return index;
    }

    private void showShimmerData() {
        List<SongResponse.Song> data = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            data.add(new SongResponse.Song(
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
                    null,
                    "",
                    "",
                    null,
                    null, null, null
            ));
        }
        binding.recyclerView.setAdapter(new ActivityListSongsItemAdapter(data));
    }

    private AlbumItem albumItem;
    private boolean isAlbum = false;

    private void showData() {
        if (getIntent().getExtras() == null) return;
        albumItem = new Gson().fromJson(getIntent().getExtras().getString("data"), AlbumItem.class);
        updateAlbumInLibraryStatus();
        if (albumItem != null) {
            binding.albumTitle.setText(albumItem.albumTitle());
            binding.albumSubTitle.setText(albumItem.albumSubTitle());
            if (!albumItem.albumCover().isBlank())
                Picasso.get().load(Uri.parse(albumItem.albumCover())).into(binding.albumCover);
        }

        final ApiManager apiManager = new ApiManager(this);
        final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        final String intentId = getIntent().getExtras().getString("id", "");

        if (getIntent().getExtras().getBoolean("createdByUser", false)) {
            onUserCreatedFetch();
            return;
        }

        boolean checkIfUrlData = (intentId.startsWith("http") || intentId.startsWith("www")) && intentId.contains("jiosaavn.com");
        if (getIntent().getExtras().getString("type", "").equals("album")) {
            isAlbum = true;
            if (albumItem != null) {
                if (sharedPreferenceManager.getAlbumResponseById(albumItem.id()) != null) {
                    onAlbumFetched(sharedPreferenceManager.getAlbumResponseById(albumItem.id()));
                    return;
                }
            }
            final var requestListener = new RequestNetwork.RequestListener() {
                @Override
                public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                    AlbumSearch albumSearch = new Gson().fromJson(response, AlbumSearch.class);
                    Log.i("ListActivity", "onResponse: " + albumSearch);
                    if (albumSearch.success()) {
                        sharedPreferenceManager.setAlbumResponseById(albumSearch.data().id(), albumSearch);
                        onAlbumFetched(albumSearch);
                    }
                }

                @Override
                public void onErrorResponse(String tag, String message) {
                    Log.e("ListActivity", "onErrorResponse: " + message);
                    Toast.makeText(ListActivity.this, "Failed to fetch Album", Toast.LENGTH_SHORT).show();
                }
            };

            if (checkIfUrlData) {
                apiManager.retrieveAlbumByLink(intentId, requestListener);
            } else {
                apiManager.retrieveAlbumById(albumItem.id(), requestListener);
            }
            return;
        }

        final RequestNetwork.RequestListener responseListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                Log.i("API_RESPONSE", "onResponse: " + response);
                PlaylistSearch playlistSearch = new Gson().fromJson(response, PlaylistSearch.class);
                if (playlistSearch.success()) {
                    sharedPreferenceManager.setPlaylistResponseById(playlistSearch.data().id(), playlistSearch);
                    onPlaylistFetched(playlistSearch);
                }
            }

            @Override
            public void onErrorResponse(String tag, String message) {

            }
        };
        if (checkIfUrlData) {
            apiManager.retrievePlaylistByLink(intentId, null, null, responseListener);
        } else {
            if (sharedPreferenceManager.getPlaylistResponseById(albumItem.id()) != null) {
                onPlaylistFetched(sharedPreferenceManager.getPlaylistResponseById(albumItem.id()));
            }
            apiManager.retrievePlaylistById(albumItem.id(), null, null, responseListener);
        }
    }

    private boolean isUserCreated = false;

    private void onUserCreatedFetch() {

        isUserCreated = true;

        binding.shareIcon.setVisibility(View.INVISIBLE);
//        binding.moreIcon.setVisibility(View.INVISIBLE);
        binding.addToLibrary.setVisibility(View.INVISIBLE);
        binding.addMoreSongs.setVisibility(View.VISIBLE);

        final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
        SavedLibraries savedLibraries = sharedPreferenceManager.getSavedLibrariesData();
        if (savedLibraries == null || savedLibraries.lists().isEmpty()) finish();
        SavedLibraries.Library library = null;
        if (savedLibraries != null)
            for (SavedLibraries.Library l : savedLibraries.lists()) {
                if (l.id().equals(albumItem.id())) {
                    library = l;
                    break;
                }
            }
        if (library == null) finish();
        if (library != null) {
            binding.albumTitle.setText(library.name());
            binding.albumSubTitle.setText(library.description());
            Picasso.get().load(Uri.parse(library.image())).into(binding.albumCover);
            binding.recyclerView.setAdapter(new UserCreatedSongsListAdapter(library.songs()));
            for (SavedLibraries.Library.Songs song : library.songs())
                trackQueue.add(song.id());
        }

    }

    private void onAlbumFetched(AlbumSearch albumSearch) {
        binding.albumTitle.setText(albumSearch.data().name());
        binding.albumSubTitle.setText(albumSearch.data().description());
        Picasso.get().load(Uri.parse(albumSearch.data().image().get(albumSearch.data().image().size() - 1).url())).into(binding.albumCover);
        binding.recyclerView.setAdapter(new ActivityListSongsItemAdapter(albumSearch.data().songs()));
        for (SongResponse.Song song : albumSearch.data().songs())
            trackQueue.add(song.id());

        //((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding.shareIcon.setOnClickListener(view -> {
            if (albumSearch.data().url().isBlank()) return;
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, albumSearch.data().url());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        });

        for (SongResponse.Artist artist : albumSearch.data().artist().all()) {
            artistData.add(new ArtistData(artist.name(), artist.id(),
                    (!artist.image().isEmpty()) ? artist.image().get(artist.image().size() - 1).url()
                            : ""
            ));
        }
    }

    private void onPlaylistFetched(PlaylistSearch playlistSearch) {
        binding.albumTitle.setText(playlistSearch.data().name());
        binding.albumSubTitle.setText(playlistSearch.data().description());
        Picasso.get().load(Uri.parse(playlistSearch.data().image().get(playlistSearch.data().image().size() - 1).url())).into(binding.albumCover);
        binding.recyclerView.setAdapter(new ActivityListSongsItemAdapter(playlistSearch.data().songs()));
        for (SongResponse.Song song : playlistSearch.data().songs())
            trackQueue.add(song.id());

        //((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding.shareIcon.setOnClickListener(view -> {
            if (playlistSearch.data().url().isBlank()) return;
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, playlistSearch.data().url());
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        });

        for (PlaylistSearch.Data.Artist artist : playlistSearch.data().artists()) {
            artistData.add(new ArtistData(artist.name(), artist.id(),
                    (!artist.image().isEmpty()) ?
                            artist.image().get(artist.image().size() - 1).url()
                            : "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
            ));
        }

    }

    private final List<ArtistData> artistData = new ArrayList<>();

    public void backPress(View view) {
        finish();
    }

    private record ArtistData(
            String name,
            String id,
            String image
    ) {
    }

}