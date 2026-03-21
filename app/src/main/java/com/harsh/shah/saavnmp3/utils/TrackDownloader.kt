package com.harsh.shah.saavnmp3.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object TrackDownloader {
    private const val TAG = "TrackDownloader"

    fun isAlreadyDownloaded(title: String?): Boolean {
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )

        if (!musicDir.exists()) {
            return false
        }
        val songFile = File(musicDir, title + ".m4a")
        val songFile1 = File(musicDir, title + ".mp4")
        val songFile2 = File(musicDir, title + ".mp3")
        return songFile.exists() || songFile1.exists() || songFile2.exists()
    }

    fun getDownloadedTracks(context: Context): MutableList<DownloadedTrack?> {
        val data: MutableList<DownloadedTrack?> = ArrayList<DownloadedTrack?>()
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) {
            return data
        }
        val files = musicDir.listFiles()
        if (files == null) {
            return data
        }
        for (file in files) {
            try {
                val f = AudioFileIO.read(file)
                val tag = f.getTag()
                val audioHeader = f.getAudioHeader()
                val title: String?
                val artist: String?
                val album: String?
                val year: String?
                val bitrate: String?
                val trackLength: String?
                var trackUID: String?
                val uidFieldIdPrefix = "----:" + context.getPackageName() + ":" // prefix for safety
                val uidFieldId = uidFieldIdPrefix + "TrackUID"
                title = if (tag != null) tag.getFirst(FieldKey.TITLE) else file.getName()
                artist = if (tag != null) tag.getFirst(FieldKey.ARTIST) else ""
                album = if (tag != null) tag.getFirst(FieldKey.ALBUM) else ""
                year = if (tag != null) tag.getFirst(FieldKey.YEAR) else ""
                bitrate = if (audioHeader != null) audioHeader.getBitRate().toString() else "344"
                trackLength =
                    if (audioHeader != null) audioHeader.getTrackLength().toString() else "0"

                trackUID = null
                if (tag != null) {
                    val field = tag.getFirstField(uidFieldId)
                    if (field != null) {
                        try {
                            trackUID = field.toString()
                            try {
                                val m = field.javaClass.getMethod("getContent")
                                val `val` = m.invoke(field)
                                if (`val` != null) trackUID = `val`.toString()
                            } catch (ignored: NoSuchMethodException) {
                            }
                        } catch (ignored: Exception) {
                        }
                    } else {
                        val it = tag.getFields()
                        while (it.hasNext()) {
                            val fField = it.next()
                            val id = fField.getId()
                            if (id != null && id.equals(uidFieldId, ignoreCase = true)) {
                                try {
                                    trackUID = fField.toString()
                                    break
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                    }
                }

                val coverImage = if (tag != null) tag.getFirstArtwork().getBinaryData() else null
                val bitmap = if (coverImage != null) BitmapFactory.decodeByteArray(
                    coverImage,
                    0,
                    coverImage.size
                ) else null
                val downloadedTrack = DownloadedTrack(
                    file,
                    title,
                    artist,
                    album,
                    year,
                    bitrate,
                    trackLength,
                    bitmap,
                    trackUID
                )
                data.add(downloadedTrack)
                println(downloadedTrack)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file: " + e.message)
            }
        }
        return data
    }

    fun downloadAndEmbedMetadata(context: Context, song: Song, listener: TrackDownloadListener) {
        val audioUrl = song.downloadUrl?.lastOrNull()?.url ?: ""
        val imageUrl = song.image?.lastOrNull()?.url ?: ""
        val title = song.name()
        val artist = song.artists?.primary?.firstOrNull()?.name() ?: ""
        val album = song.album?.name() ?: ""

        Thread(Runnable {
            Handler(Looper.getMainLooper()).post(Runnable { listener.onStarted() })
            Log.d(TAG, "⬇️ Downloading and embedding metadata...")
            try {
                val tempFile = File(context.getCacheDir(), title + ".mp4")
                URL(audioUrl).openStream().use { `in` ->
                    FileOutputStream(tempFile).use { out ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while ((`in`.read(buffer).also { bytesRead = it }) != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }
                val audioFile = AudioFileIO.read(tempFile)
                val tag = audioFile.getTagOrCreateAndSetDefault()

                tag.setField(FieldKey.TITLE, title)
                tag.setField(FieldKey.ARTIST, artist)
                tag.setField(FieldKey.ALBUM, album)
                tag.setField(FieldKey.YEAR, song.year)
                tag.setField(FieldKey.ARTIST, artist)

                val uidField = Mp4TagReverseDnsField(
                    "----",
                    context.getPackageName(),
                    "TrackUID",
                    song.id
                )

                tag.setField(uidField)

                val artworkFile = File(context.getCacheDir(), "artwork.jpg")
                URL(imageUrl).openStream().use { `in` ->
                    FileOutputStream(artworkFile).use { out ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while ((`in`.read(buffer).also { bytesRead = it }) != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }
                val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
                tag.deleteArtworkField()
                tag.setField(artwork)

                audioFile.setTag(tag)
                audioFile.commit()

                val values = getContentValues(title, artist, album, song.id)

                val resolver = context.getContentResolver()
                val audioCollection =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    ) else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                val newUri = resolver.insert(audioCollection, values)
                if (newUri == null) {
                    Log.e(TAG, "Failed to insert into MediaStore")
                    Handler(Looper.getMainLooper()).post(Runnable { listener.onError("Failed to insert into MediaStore") })
                    return@Runnable
                }

                URL("file://" + tempFile.getAbsolutePath()).openStream().use { `in` ->
                    resolver.openOutputStream(newUri).use { out ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while ((`in`.read(buffer).also { bytesRead = it }) != -1) {
                            checkNotNull(out)
                            out.write(buffer, 0, bytesRead)
                        }
                        checkNotNull(out)
                        out.flush()
                    }
                }
                if (!tempFile.delete()) Log.e(TAG, "Failed to delete temp file")

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri))
                }

                Log.d(TAG, "✅ Downloaded and tagged: " + title)
            } catch (e: Exception) {
                Log.e(TAG, "Error: " + e.message)
                Handler(Looper.getMainLooper()).post(Runnable { listener.onError(e.message) })
            } finally {
                Handler(Looper.getMainLooper()).post(Runnable { listener.onFinished() })
            }
        }).start()
    }

    private fun getContentValues(
        title: String?,
        artist: String?,
        album: String?,
        id: String?
    ): ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, title)
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        values.put(MediaStore.Audio.Media.IS_MUSIC, true)
        values.put(MediaStore.Audio.Media.TITLE, title)
        values.put(MediaStore.Audio.Media.ARTIST, artist)
        values.put(MediaStore.Audio.Media.ALBUM, album)
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Melotune")
        values.put(MediaStore.Audio.Media._ID, id)
        return values
    }

    interface TrackDownloadListener {
        fun onStarted()

        fun onFinished()

        fun onError(errorMessage: String?)
    }

    
    data class DownloadedTrack(
        val file: File?,
        val title: String?,
        val artist: String?,
        val album: String?,
        val year: String?,
        val bitrate: String?,
        val trackLength: String?,
        val coverImage: Bitmap?,
        val trackUID: String?
    )
}

