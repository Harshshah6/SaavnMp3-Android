package com.harsh.shah.saavnmp3.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class TrackDownloader {

    private static final String TAG = "TrackDownloader";

    public interface TrackDownloadListener {
        void onStarted();
        void onFinished();
        void onError(String errorMessage);
    }

    public static boolean isAlreadyDownloaded(String title) {
        File musicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Melotune");

        if (!musicDir.exists()) {
            return false;
        }
        File songFile = new File(musicDir, title + ".m4a");
        File songFile1 = new File(musicDir, title + ".mp4");
        File songFile2 = new File(musicDir, title + ".mp3");
        return songFile.exists() || songFile1.exists() || songFile2.exists();
    }

    public static void downloadAndEmbedMetadata(Context context, String audioUrl, String imageUrl, String title, String artist, String album, TrackDownloadListener listener) {

        Log.i(TAG, audioUrl);
        Log.i(TAG, imageUrl);
        Log.i(TAG, title);
        Log.i(TAG, artist);
        Log.i(TAG, album);

        new Thread(() -> {
            new android.os.Handler(Looper.getMainLooper()).post(listener::onStarted);
            Log.d(TAG, "⬇️ Downloading and embedding metadata...");
            try {
                File tempFile = new File(context.getCacheDir(), title + ".mp4");
                try (InputStream in = new URL(audioUrl).openStream();
                     FileOutputStream out = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                AudioFile audioFile = AudioFileIO.read(tempFile);
                Tag tag = audioFile.getTagOrCreateAndSetDefault();

                tag.setField(FieldKey.TITLE, title);
                tag.setField(FieldKey.ARTIST, artist);
                tag.setField(FieldKey.ALBUM, album);

                File artworkFile = new File(context.getCacheDir(), "artwork.jpg");
                try (InputStream in = new URL(imageUrl).openStream();
                     FileOutputStream out = new FileOutputStream(artworkFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                Artwork artwork = ArtworkFactory.createArtworkFromFile(artworkFile);
                tag.deleteArtworkField();
                tag.setField(artwork);

                audioFile.setTag(tag);
                audioFile.commit();

                ContentValues values = getContentValues(title, artist, album);

                ContentResolver resolver = context.getContentResolver();
                Uri audioCollection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

                Uri newUri = resolver.insert(audioCollection, values);
                if (newUri == null) {
                    Log.e(TAG, "Failed to insert into MediaStore");
                    new android.os.Handler(Looper.getMainLooper()).post(()-> listener.onError("Failed to insert into MediaStore"));
                    return;
                }

                try (InputStream in = new URL("file://" + tempFile.getAbsolutePath()).openStream();
                     OutputStream out = resolver.openOutputStream(newUri)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        assert out != null;
                        out.write(buffer, 0, bytesRead);
                    }
                    assert out != null;
                    out.flush();
                }

                if (!tempFile.delete()) Log.e(TAG, "Failed to delete temp file");

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
                }

                Log.d(TAG, "✅ Downloaded and tagged: " + title);

            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
                new android.os.Handler(Looper.getMainLooper()).post(()-> listener.onError(e.getMessage()));
            }finally {
                new android.os.Handler(Looper.getMainLooper()).post(listener::onFinished);
            }
        }).start();
    }

    @NonNull
    private static ContentValues getContentValues(String title, String artist, String album) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, title);
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
        values.put(MediaStore.Audio.Media.IS_MUSIC, true);
        values.put(MediaStore.Audio.Media.TITLE, title);
        values.put(MediaStore.Audio.Media.ARTIST, artist);
        values.put(MediaStore.Audio.Media.ALBUM, album);
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Melotune");
        return values;
    }

}

