package com.harsh.shah.saavnmp3.activities;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.BaseApplicationClass;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.databinding.ActivityMusicOverviewBinding;
import com.harsh.shah.saavnmp3.databinding.MusicOverviewMoreInfoBottomSheetBinding;
import com.harsh.shah.saavnmp3.model.AlbumItem;
import com.harsh.shah.saavnmp3.model.BasicDataRecord;
import com.harsh.shah.saavnmp3.network.ApiManager;
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries;
import com.harsh.shah.saavnmp3.services.ActionPlaying;
import com.harsh.shah.saavnmp3.services.MusicService;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;
import com.harsh.shah.saavnmp3.utils.TrackDownloader;
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MusicOverviewActivity extends AppCompatActivity implements ActionPlaying, ServiceConnection {

    private final String TAG = "MusicOverviewActivity";
    //private final MediaPlayer mediaPlayer = new MediaPlayer();
    private final Handler handler = new Handler();
    ActivityMusicOverviewBinding binding;
    private String SONG_URL = "";
    private String ID_FROM_EXTRA = "";
    private String IMAGE_URL = "";
    MusicService musicService;
    private List<SongResponse.Artist> artsitsList = new ArrayList<>();
    private final boolean isDebugMode = false;

    //@SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMusicOverviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.title.setSelected(true);
        binding.description.setSelected(true);

        if (!(((BaseApplicationClass) getApplicationContext()).getTrackQueue().size() > 1))
            binding.shuffleIcon.setVisibility(View.INVISIBLE);

        binding.playPauseImage.setOnClickListener(view -> {
            try {
                if (BaseApplicationClass.player == null) {
                    Log.e(TAG, "Player is null, cannot toggle playback");
                    Toast.makeText(this, "Media player not ready. Try again.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Get application instance to control playback
                BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();

                if (BaseApplicationClass.player.isPlaying()) {
                    // Handle pause action
                    Log.i(TAG, "Pausing playback");
                    handler.removeCallbacks(runnable);
                    BaseApplicationClass.player.pause();
                    binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
                } else {
                    // Handle play action
                    Log.i(TAG, "Starting playback");
                    BaseApplicationClass.player.play();
                    binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
                    updateSeekbar();
                }

                // Update notification with correct playback state
                boolean isPlaying = BaseApplicationClass.player.isPlaying();
                Log.i(TAG, "Player state after click: isPlaying=" + isPlaying);
                showNotification(isPlaying ? R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
            } catch (Exception e) {
                Log.e(TAG, "Error toggling playback", e);
                Toast.makeText(this, "Error controlling playback. Try again.", Toast.LENGTH_SHORT).show();
            }
        });

        binding.seekbar.setMax(100);

        binding.seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int playPosition = (int) ((BaseApplicationClass.player.getDuration() / 100) * binding.seekbar.getProgress());
                BaseApplicationClass.player.seekTo(playPosition);
                binding.elapsedDuration.setText(convertDuration(BaseApplicationClass.player.getCurrentPosition()));
            }
        });

        final BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();

        binding.nextIcon.setOnClickListener(view -> {
            try {
                Log.i(TAG, "Next button clicked");
                if (BaseApplicationClass.player == null) {
                    Log.e(TAG, "Player is null, cannot skip to next track");
                    Toast.makeText(MusicOverviewActivity.this, "Media player not ready", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add visual feedback
                binding.nextIcon.setAlpha(0.5f);
                binding.nextIcon.animate().alpha(1.0f).setDuration(200).start();

                // Call next track method
                baseApplicationClass.nextTrack();

                // Update UI state
                updateSeekbar();
                updateTrackInfo();

                // Make sure play icon reflects current state
                if (BaseApplicationClass.player.isPlaying()) {
                    binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
                } else {
                    binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
                }

                if (isDebugMode)
                    Toast.makeText(MusicOverviewActivity.this, "Playing next track", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error skipping to next track", e);
                Toast.makeText(MusicOverviewActivity.this, "Error skipping to next track", Toast.LENGTH_SHORT).show();
            }
        });

        binding.prevIcon.setOnClickListener(view -> {
            try {
                Log.i(TAG, "Previous button clicked");
                if (BaseApplicationClass.player == null) {
                    Log.e(TAG, "Player is null, cannot skip to previous track");
                    Toast.makeText(MusicOverviewActivity.this, "Media player not ready", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add visual feedback
                binding.prevIcon.setAlpha(0.5f);
                binding.prevIcon.animate().alpha(1.0f).setDuration(200).start();

                // If we're already at the beginning of the track, go to previous track
                // Otherwise just restart the current track
                if (BaseApplicationClass.player.getCurrentPosition() > 3000) {
                    BaseApplicationClass.player.seekTo(0);
                    if (isDebugMode)
                        Toast.makeText(MusicOverviewActivity.this, "Restarting current track", Toast.LENGTH_SHORT).show();
                } else {
                    // Call previous track method
                    baseApplicationClass.prevTrack();
                    if (isDebugMode)
                        Toast.makeText(MusicOverviewActivity.this, "Playing previous track", Toast.LENGTH_SHORT).show();
                }

                // Update UI state
                updateSeekbar();
                updateTrackInfo();

                // Make sure play icon reflects current state
                if (BaseApplicationClass.player.isPlaying()) {
                    binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
                } else {
                    binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error going to previous track", e);
                Toast.makeText(MusicOverviewActivity.this, "Error going to previous track", Toast.LENGTH_SHORT).show();
            }
        });

        binding.repeatIcon.setOnClickListener(view -> {
            try {
                // Cycle through all three repeat modes
                int currentMode = BaseApplicationClass.player.getRepeatMode();
                int newMode;
                String modeMessage;
                switch (currentMode) {
                    case Player.REPEAT_MODE_OFF:
                        newMode = Player.REPEAT_MODE_ONE;
                        modeMessage = "Repeat One";
                        break;
                    case Player.REPEAT_MODE_ONE:
                        newMode = Player.REPEAT_MODE_ALL;
                        modeMessage = "Repeat All";
                        break;
                    case Player.REPEAT_MODE_ALL:
                    default:
                        newMode = Player.REPEAT_MODE_OFF;
                        modeMessage = "Repeat Off";
                        break;
                }

                BaseApplicationClass.player.setRepeatMode(newMode);

                // Update UI to reflect the current mode
                updateRepeatButtonUI();

                if (isDebugMode)
                    Toast.makeText(MusicOverviewActivity.this, modeMessage, Toast.LENGTH_SHORT).show();

                Log.i(TAG, "Repeat mode changed to: " + newMode);
            } catch (Exception e) {
                Log.e(TAG, "Error changing repeat mode", e);
                Toast.makeText(MusicOverviewActivity.this, "Error changing repeat mode", Toast.LENGTH_SHORT).show();
            }
        });

        binding.shuffleIcon.setOnClickListener(view -> {
            BaseApplicationClass.player.setShuffleModeEnabled(!BaseApplicationClass.player.getShuffleModeEnabled());

            if (BaseApplicationClass.player.getShuffleModeEnabled())
                binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));
            else
                binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));

            if (isDebugMode)
                Toast.makeText(MusicOverviewActivity.this, "Shuffle Mode Changed.", Toast.LENGTH_SHORT).show();
        });

        binding.shareIcon.setOnClickListener(view -> {
            if (SHARE_URL.isBlank()) return;
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, SHARE_URL);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        });

        binding.moreIcon.setOnClickListener(view -> {
            final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(MusicOverviewActivity.this, R.style.MyBottomSheetDialogTheme);
            final MusicOverviewMoreInfoBottomSheetBinding _binding = MusicOverviewMoreInfoBottomSheetBinding.inflate(getLayoutInflater());
            _binding.albumTitle.setText(binding.title.getText().toString());
            _binding.albumSubTitle.setText(binding.description.getText().toString());
            Picasso.get().load(Uri.parse(IMAGE_URL)).into(_binding.coverImage);
            final LinearLayout linearLayout = _binding.main;

            _binding.goToAlbum.setOnClickListener(go_to_album -> {
                if (mSongResponse == null) return;
                if (mSongResponse.data().get(0).album() == null) return;
                final SongResponse.Album album = mSongResponse.data().get(0).album();
                startActivity(new Intent(MusicOverviewActivity.this, ListActivity.class)
                        .putExtra("type", "album")
                        .putExtra("id", album.id())
                        .putExtra("data", new Gson().toJson(new AlbumItem(album.name(), "", "", album.id())))
                );
            });

            _binding.addToLibrary.setOnClickListener(v -> {
                int index = -1;
                final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(MusicOverviewActivity.this);
                SavedLibraries savedLibraries = sharedPreferenceManager.getSavedLibrariesData();
                if (savedLibraries == null) savedLibraries = new SavedLibraries(new ArrayList<>());
                if (savedLibraries.lists().isEmpty()) {
                    Snackbar.make(_binding.getRoot(), "No Libraries Found", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                final List<String> userCreatedLibraries = new ArrayList<>();
                for (SavedLibraries.Library library : savedLibraries.lists()) {
                    if (library.isCreatedByUser())
                        userCreatedLibraries.add(library.name());
                }

                MaterialAlertDialogBuilder materialAlertDialogBuilder = getMaterialAlertDialogBuilder(userCreatedLibraries, savedLibraries, sharedPreferenceManager);
                materialAlertDialogBuilder.show();

            });

            final SongResponse.Song song = mSongResponse.data().get(0);

            if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                _binding.download.getTitleTextView().setText("Download Manager");
            }

            _binding.download.setOnClickListener(v -> {

                if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                    startActivity(new Intent(MusicOverviewActivity.this, DownloadManagerActivity.class));
                    return;
                }

                ProgressDialog progressDialog = new ProgressDialog(MusicOverviewActivity.this);
                progressDialog.setMessage("Downloading...");
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                TrackDownloader.downloadAndEmbedMetadata(
                        MusicOverviewActivity.this,
                        song,
                        new TrackDownloader.TrackDownloadListener() {
                            @Override
                            public void onStarted() {
                                progressDialog.show();
                            }

                            @Override
                            public void onFinished() {
                                progressDialog.dismiss();
                                if (TrackDownloader.isAlreadyDownloaded(song.name())) {
                                    Toast.makeText(MusicOverviewActivity.this, "Successfully Downloaded.", Toast.LENGTH_SHORT).show();
                                    _binding.download.getTitleTextView().setText("Download Manager");
                                }
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(MusicOverviewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                MaterialAlertDialogBuilder alertDialogBuilder = new MaterialAlertDialogBuilder(MusicOverviewActivity.this);
                                alertDialogBuilder.setTitle("Error");
                                alertDialogBuilder.setMessage(errorMessage);
                                alertDialogBuilder.setPositiveButton("OK", (dialogInterface, i) -> dialogInterface.dismiss());
                                alertDialogBuilder.show();
                            }
                        }
                );

            });

            for (SongResponse.Artist artist : artsitsList) {
                try {
                    final String imgUrl = artist.image().isEmpty() ? "" : artist.image().get(artist.image().size() - 1).url();
                    BottomSheetItemView bottomSheetItemView = new BottomSheetItemView(MusicOverviewActivity.this, artist.name(), imgUrl, artist.id());
                    bottomSheetItemView.setFocusable(true);
                    bottomSheetItemView.setClickable(true);
                    bottomSheetItemView.setOnClickListener(view1 -> {
                        Log.i(TAG, "BottomSheetItemView: onCLicked!");
                        startActivity(new Intent(MusicOverviewActivity.this, ArtistProfileActivity.class)
                                .putExtra("data", new Gson().toJson(
                                        new BasicDataRecord(artist.id(), artist.name(), "", imgUrl)))
                        );
                    });
                    linearLayout.addView(bottomSheetItemView);
                } catch (Exception e) {
                    Log.e(TAG, "BottomSheetDialog: ", e);
                }
            }
            bottomSheetDialog.setContentView(_binding.getRoot());
            bottomSheetDialog.create();
            bottomSheetDialog.show();
        });

        binding.trackQuality.setOnClickListener(view -> {
            PopupMenu popupMenu = new PopupMenu(MusicOverviewActivity.this, view);
            popupMenu.getMenuInflater().inflate(R.menu.track_quality_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                Toast.makeText(MusicOverviewActivity.this, menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                //Objects.requireNonNull(menuItem.getTitle());
                BaseApplicationClass.setTrackQuality(menuItem.getTitle().toString());
                onSongFetched(mSongResponse, true);
                prepareMediaPLayer();
                binding.trackQuality.setText(BaseApplicationClass.TRACK_QUALITY);
                return true;
            });
            popupMenu.show();
        });

        binding.trackQuality.setText(BaseApplicationClass.TRACK_QUALITY);

        showData();

        updateTrackInfo();
    }

    @NonNull
    private MaterialAlertDialogBuilder getMaterialAlertDialogBuilder(List<String> userCreatedLibraries, SavedLibraries savedLibraries, SharedPreferenceManager sharedPreferenceManager) {
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MusicOverviewActivity.this);
        ListAdapter listAdapter = new ArrayAdapter<>(MusicOverviewActivity.this, android.R.layout.simple_list_item_1, userCreatedLibraries);
        final SavedLibraries finalSavedLibraries = savedLibraries;
        materialAlertDialogBuilder.setAdapter(listAdapter, (dialogInterface, i) -> {
            //index = i;
            Log.i(TAG, "pickedLibrary: " + i);

            final SongResponse.Song song = mSongResponse.data().get(0);

            SavedLibraries.Library.Songs songs = new SavedLibraries.Library.Songs(
                    song.id(),
                    song.name(),
                    binding.description.getText().toString(),
                    IMAGE_URL
            );

            finalSavedLibraries.lists().get(i).songs().add(songs);
            sharedPreferenceManager.setSavedLibrariesData(finalSavedLibraries);
            Toast.makeText(MusicOverviewActivity.this, "Added to " + finalSavedLibraries.lists().get(i).name(), Toast.LENGTH_SHORT).show();
        });


        materialAlertDialogBuilder.setTitle("Select Library");
        return materialAlertDialogBuilder;
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Set as current activity in ApplicationClass
        BaseApplicationClass.setCurrentActivity(this);

        // Bind to the service
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);

        // Update UI with current playback state
        if (BaseApplicationClass.player != null) {
            updateTrackInfo();
            if (BaseApplicationClass.player.isPlaying()) {
                updateSeekbar();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove callbacks to prevent leaks
        handler.removeCallbacks(runnable);
        mHandler.removeCallbacks(mUpdateTimeTask);

        // Unbind from service
        try {
            unbindService(this);
        } catch (Exception e) {
            Log.e(TAG, "Error unbinding service", e);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Ensure we're not updating UI when activity is in background
        handler.removeCallbacks(runnable);
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Final cleanup
        handler.removeCallbacks(runnable);
        mHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder binder = (MusicService.MyBinder) service;
        musicService = binder.getService();
        musicService.setCallback(MusicOverviewActivity.this);
        Log.i(TAG, "onServiceConnected: ");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        Log.e(TAG, "onServiceDisconnected: ");
        musicService = null;
    }

    private String SHARE_URL = "";

    void showData() {
        if (getIntent().getExtras() == null) return;
        final ApiManager apiManager = new ApiManager(this);
        final String ID = getIntent().getExtras().getString("id", "");
        ID_FROM_EXTRA = ID;
        //((ApplicationClass)getApplicationContext()).setMusicDetails(null,null,null,ID);
        if (BaseApplicationClass.MUSIC_ID.equals(ID)) {
            updateSeekbar();
            if (BaseApplicationClass.player.isPlaying())
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
            else
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
        }

        final RequestNetwork.RequestListener requestListener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
                SongResponse songResponse = new Gson().fromJson(response, SongResponse.class);
                if (songResponse.success()) {
                    onSongFetched(songResponse);
                    SharedPreferenceManager.getInstance(MusicOverviewActivity.this).setSongResponseById(ID, songResponse);
                } else if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID))
                    onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
                else
                    finish();
            }

            @Override
            public void onErrorResponse(String tag, String message) {
                if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID))
                    onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
                else
                    Toast.makeText(MusicOverviewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        };

        if (getIntent().getExtras().getString("type", "").equals("clear")) {
            BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();
            baseApplicationClass.setTrackQueue(new ArrayList<>(Collections.singletonList(ID)));
        }
        if((ID.startsWith("http") || ID.startsWith("www")) && ID.contains("jiosaavn.com")){
            apiManager.retrieveSongByLink(ID, requestListener);
        }else{
            if (SharedPreferenceManager.getInstance(MusicOverviewActivity.this).isSongResponseById(ID)) {
                onSongFetched(SharedPreferenceManager.getInstance(MusicOverviewActivity.this).getSongResponseById(ID));
            }else {
                apiManager.retrieveSongById(ID, null, requestListener);
            }
        }
    }

    private SongResponse mSongResponse;

    private void onSongFetched(SongResponse songResponse) {
        onSongFetched(songResponse, false);
    }

    private void onSongFetched(SongResponse songResponse, boolean forced) {
        mSongResponse = songResponse;
        BaseApplicationClass.CURRENT_TRACK = mSongResponse;
        binding.title.setText(songResponse.data().get(0).name());
        binding.description.setText(
                String.format("%s plays | %s | %s",
                        convertPlayCount(songResponse.data().get(0).playCount()),
                        songResponse.data().get(0).year(),
                        songResponse.data().get(0).copyright())
        );
        List<SongResponse.Image> image = songResponse.data().get(0).image();
        IMAGE_URL = image.get(image.size() - 1).url();
        SHARE_URL = songResponse.data().get(0).url();
        Picasso.get().load(Uri.parse(image.get(image.size() - 1).url())).into(binding.coverImage);
        List<SongResponse.DownloadUrl> downloadUrls = songResponse.data().get(0).downloadUrl();

        artsitsList = songResponse.data().get(0).artists().primary();

        SONG_URL = BaseApplicationClass.getDownloadUrl(downloadUrls);

        if ((!BaseApplicationClass.MUSIC_ID.equals(ID_FROM_EXTRA) || forced)) {
            BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();
            baseApplicationClass.setMusicDetails(IMAGE_URL, binding.title.getText().toString(), binding.description.getText().toString(), ID_FROM_EXTRA);
            baseApplicationClass.setSongUrl(SONG_URL);
            prepareMediaPLayer();
        }

    }

    public void backPress(View view) {
        finish();
    }

    public static String convertPlayCount(int playCount) {
        if (playCount < 1000) return playCount + "";
        if (playCount < 1000000) return playCount / 1000 + "K";
        return playCount / 1000000 + "M";
    }

    public static String convertDuration(long duration) {
        String timeString = "";
        String secondString;

        int hours = (int) (duration / (1000 * 60 * 60));
        int minutes = (int) (duration % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((duration % (1000 * 60 * 60)) % (1000 * 60) / 1000);
        if (hours > 0) {
            timeString = hours + ":";
        }
        if (seconds < 10) {
            secondString = "0" + seconds;
        } else {
            secondString = "" + seconds;
        }
        timeString = timeString + minutes + ":" + secondString;
        return timeString;
    }

    @OptIn(markerClass = UnstableApi.class)
    void prepareMediaPLayer() {
        try {
            BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();
            baseApplicationClass.prepareMediaPlayer();

            // Wait until player is actually ready
            if (BaseApplicationClass.player != null && BaseApplicationClass.player.getDuration() > 0) {
                binding.totalDuration.setText(convertDuration(BaseApplicationClass.player.getDuration()));
            } else {
                // If duration is not yet available, set a default or retry
                binding.totalDuration.setText("00:00");
                // Schedule a retry to get the duration
                new Handler().postDelayed(() -> {
                    if (BaseApplicationClass.player != null && BaseApplicationClass.player.getDuration() > 0) {
                        binding.totalDuration.setText(convertDuration(BaseApplicationClass.player.getDuration()));
                    }
                }, 500);
            }

            // Set play state
            if (BaseApplicationClass.player.isPlaying()) {
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
                updateSeekbar();
            } else {
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
            }

            // Update notification
            showNotification(BaseApplicationClass.player.isPlaying() ?
                    R.drawable.baseline_pause_24 : R.drawable.play_arrow_24px);
        } catch (Exception e) {
            Log.e(TAG, "Error preparing media player", e);
            // Try to recover
            Toast.makeText(this, "Error playing track. Retrying...", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::prepareMediaPLayer, 1000);
        }
    }

    private final Runnable runnable = this::updateSeekbar;

    void updateSeekbar() {
        try {
            if (BaseApplicationClass.player == null) {
                Log.e(TAG, "Player is null in updateSeekbar");
                return;
            }

            if (BaseApplicationClass.player.isPlaying()) {
                try {
                    long duration = BaseApplicationClass.player.getDuration();
                    long currentPosition = BaseApplicationClass.player.getCurrentPosition();

                    // Check for valid duration to avoid division by zero
                    if (duration > 0) {
                        int progress = (int) (((float) currentPosition / duration) * 100);
                        binding.seekbar.setProgress(progress);
                        binding.elapsedDuration.setText(convertDuration(currentPosition));
                    }

                    // Schedule the next update
                    handler.postDelayed(runnable, 1000);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating seekbar", e);
                }
            } else {
                // If player is paused, still show correct position but don't schedule updates
                try {
                    long duration = BaseApplicationClass.player.getDuration();
                    long currentPosition = BaseApplicationClass.player.getCurrentPosition();

                    if (duration > 0) {
                        int progress = (int) (((float) currentPosition / duration) * 100);
                        binding.seekbar.setProgress(progress);
                        binding.elapsedDuration.setText(convertDuration(currentPosition));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating seekbar while paused", e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception in updateSeekbar", e);
        }
    }

    private final Handler mHandler = new Handler();
    private final Runnable mUpdateTimeTask = this::updateTrackInfo;

    private void updateTrackInfo() {
        if (!binding.title.getText().toString().equals(BaseApplicationClass.MUSIC_TITLE))
            binding.title.setText(BaseApplicationClass.MUSIC_TITLE);
        if (!binding.title.getText().toString().equals(BaseApplicationClass.MUSIC_TITLE))
            binding.description.setText(BaseApplicationClass.MUSIC_DESCRIPTION);
        Picasso.get().load(Uri.parse(BaseApplicationClass.IMAGE_URL)).into(binding.coverImage);
        binding.seekbar.setProgress((int) (((float) BaseApplicationClass.player.getCurrentPosition() / BaseApplicationClass.player.getDuration()) * 100));

        binding.seekbar.setSecondaryProgress((int) (((float) BaseApplicationClass.player.getBufferedPosition() / BaseApplicationClass.player.getDuration()) * 100));

        long currentDuration = BaseApplicationClass.player.getCurrentPosition();
        binding.elapsedDuration.setText(convertDuration(currentDuration));

        if (!binding.totalDuration.getText().toString().equals(convertDuration(BaseApplicationClass.player.getDuration())))
            binding.totalDuration.setText(convertDuration(BaseApplicationClass.player.getDuration()));

        if (BaseApplicationClass.player.isPlaying())
            binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
        else
            binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);

        //((ApplicationClass)getApplicationContext()).showNotification();

        // Update repeat and shuffle button UI
        updateRepeatButtonUI();

        if (BaseApplicationClass.player.getShuffleModeEnabled())
            binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.spotify_green)));
        else
            binding.shuffleIcon.setImageTintList(ColorStateList.valueOf(getResources().getColor(R.color.textSec)));

        mHandler.postDelayed(mUpdateTimeTask, 1000);
    }

    private void updateRepeatButtonUI() {
        int tintColor;
        int repeatMode = BaseApplicationClass.player.getRepeatMode();

        switch (repeatMode) {
            case Player.REPEAT_MODE_ONE:
                tintColor = getResources().getColor(R.color.spotify_green);
                try {
                    binding.repeatIcon.setImageResource(R.drawable.repeat_one_24px);
                } catch (Exception e) {
                    // Fallback to regular repeat icon if repeat_one_24px isn't available
                    Log.e(TAG, "Error setting repeat_one icon: " + e.getMessage());
                    binding.repeatIcon.setImageResource(R.drawable.repeat_24px);
                }
                break;
            case Player.REPEAT_MODE_ALL:
                tintColor = getResources().getColor(R.color.spotify_green);
                binding.repeatIcon.setImageResource(R.drawable.repeat_24px);
                break;
            case Player.REPEAT_MODE_OFF:
            default:
                tintColor = getResources().getColor(R.color.textSec);
                binding.repeatIcon.setImageResource(R.drawable.repeat_24px);
                break;
        }

        binding.repeatIcon.setImageTintList(ColorStateList.valueOf(tintColor));
    }

    @Override
    public void nextClicked() {
        Log.i(TAG, "nextClicked called from service");
        try {
            if (BaseApplicationClass.player == null) {
                Log.e(TAG, "Player is null in nextClicked");
                return;
            }

            // Update UI to show active button state
            binding.nextIcon.setAlpha(0.5f);
            binding.nextIcon.animate().alpha(1.0f).setDuration(200).start();

            // Get application instance
            BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();
            baseApplicationClass.nextTrack();

            // Update UI
            updateTrackInfo();
            updateSeekbar();

            // Ensure play/pause button shows correct state
            if (BaseApplicationClass.player.isPlaying()) {
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
            } else {
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in nextClicked", e);
        }
    }

    @Override
    public void prevClicked() {
        Log.i(TAG, "prevClicked called from service");
        try {
            if (BaseApplicationClass.player == null) {
                Log.e(TAG, "Player is null in prevClicked");
                return;
            }

            // Update UI to show active button state
            binding.prevIcon.setAlpha(0.5f);
            binding.prevIcon.animate().alpha(1.0f).setDuration(200).start();

            // Get application instance
            BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();

            // If we're already at the beginning of the track, go to previous track
            // Otherwise just restart the current track
            if (BaseApplicationClass.player.getCurrentPosition() > 3000) {
                BaseApplicationClass.player.seekTo(0);
            } else {
                baseApplicationClass.prevTrack();
            }

            // Update UI
            updateTrackInfo();
            updateSeekbar();

            // Ensure play/pause button shows correct state
            if (BaseApplicationClass.player.isPlaying()) {
                binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
            } else {
                binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in prevClicked", e);
        }
    }

    @Override
    public void playClicked() {
        //binding.playPauseImage.performClick();
        if (!BaseApplicationClass.player.isPlaying()) {
            binding.playPauseImage.setImageResource(R.drawable.play_arrow_24px);
        } else {
            binding.playPauseImage.setImageResource(R.drawable.baseline_pause_24);
        }
    }

    @Override
    public void onProgressChanged(int progress) {

    }

    public void showNotification(int playPauseButton) {
        BaseApplicationClass baseApplicationClass = (BaseApplicationClass) getApplicationContext();
        String songId = getIntent().getExtras().getString("id", "");
        baseApplicationClass.setMusicDetails(IMAGE_URL, binding.title.getText().toString(), binding.description.getText().toString(), songId);
        Log.i(TAG, "MusicOverviewActivity showNotification for song ID: " + songId);
        baseApplicationClass.showNotification();
    }


}