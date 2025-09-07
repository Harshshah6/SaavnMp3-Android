package com.harsh.shah.saavnmp3.utils;

import android.content.Context;import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask; // Import AsyncTask
import android.os.Environment;
import android.util.Log;

import com.harsh.shah.saavnmp3.records.SongResponse;
import com.squareup.picasso.Picasso;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TrackCacheHelper {
    private static final String TAG = "TrackCacheHelper";
    private final Context context;

    public TrackCacheHelper(Context context) {
        this.context = context;
    }

    private SharedPreferences getTrackPreference() {
        return context.getSharedPreferences("TrackCacheHelper", Context.MODE_PRIVATE);
    }

    public void setTrackToCache(String id, String uri) {
        if (getTrackPreference().contains(id)) return;
        getTrackPreference().edit().putString(id, uri).apply();
    }

    public boolean isTrackInCache(String id) {
        return getTrackPreference().contains(id);
    }

    public String getTrackFromCache(String id) {
        return getTrackPreference().getString(id, "");
    }

    // --- Start of changes for AsyncTask ---
    private static class CopyFileTask extends AsyncTask<Void, Void, String> {
        private final String sourcePathOrUrl; // Can be local path or a URL
        private final String newFileName;
        private final SongResponse.Song song;
        private final Context appContext; // Use application context to avoid leaks
        private final File musicDir;

        CopyFileTask(Context context, String sourcePathOrUrl, String newFileName, SongResponse.Song song) {
            this.appContext = context.getApplicationContext();
            this.sourcePathOrUrl = sourcePathOrUrl;
            this.newFileName = newFileName;
            this.song = song;

            // Prepare music directory
            this.musicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melotune");
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }
        }

        @Override
        protected String doInBackground(Void... voids) {
            File destFile = new File(musicDir, newFileName + ".mp3");
            File tempImageFileForArtwork = null;

            try {
                // If sourcePathOrUrl is a URL, download it first to a temporary cache location
                // For simplicity, this example assumes sourcePath is already a local file path
                // If it's a URL, you'd need to add download logic here or in another AsyncTask
                File sourceFile = new File(sourcePathOrUrl);

                // Copy file (if source is local)
                if (sourceFile.exists()) {
                    try (FileInputStream inputStream = new FileInputStream(sourceFile);
                         FileOutputStream outputStream = new FileOutputStream(destFile)) {

                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                } else {
                    Log.e(TAG, "Source file does not exist: " + sourcePathOrUrl);
                    return "Error: Source file not found.";
                }


                // --- Metadata and Artwork ---
                AudioFile audioFile = AudioFileIO.read(destFile);
                Tag tag = audioFile.getTag();

                if (tag == null) {
                    tag = audioFile.createDefaultTag();
                    audioFile.setTag(tag);
                }

                // Set metadata
                tag.setField(FieldKey.ARTIST, song.artists().primary().get(0).name());
                tag.setField(FieldKey.ALBUM, song.album().name());
                tag.setField(FieldKey.TITLE, song.name());
                tag.setField(FieldKey.YEAR, song.year());

                // Download image for artwork (THIS IS THE NETWORK OPERATION)
                String imageUrl = song.image().get(song.image().size() - 1).url();
                tempImageFileForArtwork = new File(appContext.getCacheDir(), "temp_cover_for_" + newFileName + ".jpg"); // Save to cache
                downloadImage(imageUrl, tempImageFileForArtwork); // This performs network I/O

                // Create artwork from the downloaded image file
                Artwork artwork = ArtworkFactory.createArtworkFromFile(tempImageFileForArtwork);

                tag.deleteArtworkField();
                tag.setField(artwork);

                audioFile.commit();

                return "File copied successfully to: " + destFile.getAbsolutePath();

            } catch (Exception e) {
                Log.e(TAG, "Error in CopyFileTask: ", e);
                return "Error: " + e.getMessage();
            } finally {
                if (tempImageFileForArtwork != null && tempImageFileForArtwork.exists()) {
                    tempImageFileForArtwork.delete(); // Clean up temporary image
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // This runs on the UI thread. You can show a Toast or update UI here.
            Log.d(TAG, result);
            // Example: Toast.makeText(appContext, result, Toast.LENGTH_LONG).show();
        }
    }

    public void copyFileToMusicDir(String sourcePath, String newFileName, SongResponse.Song song) {
        // Execute the AsyncTask
        new CopyFileTask(context, sourcePath, newFileName, song).execute();
    }
    // --- End of changes for AsyncTask ---


    // downloadImage should ideally also be off the main thread if called directly,
    // but in this AsyncTask setup, it's called from doInBackground.
    public static File downloadImage(String urlStr, File outputFile) throws IOException { // Changed Exception to IOException for more specificity
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000);    // 15 seconds
            connection.setRequestMethod("GET");   // Optional: specify GET
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + responseCode + " for URL: " + urlStr);
            }

            input = connection.getInputStream();
            output = new FileOutputStream(outputFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            return outputFile;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing output stream", e);
                }
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] bitmapToBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }
}
