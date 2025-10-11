package com.harsh.shah.saavnmp3.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

    public static boolean isAlreadyDownloaded(Context context, String title, String artist) {
        ContentResolver resolver = context.getContentResolver();

        Uri collection = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ? MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                : MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.DISPLAY_NAME
        };

        String selection = MediaStore.Audio.Media.TITLE + "=? AND " + MediaStore.Audio.Media.ARTIST + "=?";
        String[] selectionArgs = new String[]{ title, artist };

        try (Cursor cursor = resolver.query(collection, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String filePath = null;

                    // 1️⃣ Try DATA column first (works on Android 9 and below)
                    int dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    if (dataIndex != -1) {
                        filePath = cursor.getString(dataIndex);
                        if (filePath != null) {
                            File file = new File(filePath);
                            if (file.exists()) {
                                return true;
                            }
                        }
                    }

                    // 2️⃣ Try RELATIVE_PATH + DISPLAY_NAME for Android 10+
                    int relIndex = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH);
                    int nameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                    if (relIndex != -1 && nameIndex != -1) {
                        String relativePath = cursor.getString(relIndex);
                        String displayName = cursor.getString(nameIndex);
                        if (relativePath != null && displayName != null) {
                            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                            filePath = new File(musicDir, relativePath + "/" + displayName).getAbsolutePath();
                            File file = new File(filePath);
                            if (file.exists()) {
                                return true;
                            }
                        }
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e("TrackDownloader", "Error checking track existence: " + e.getMessage());
        }

        return false;
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

