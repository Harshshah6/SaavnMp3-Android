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
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.samyak.lrclib.LrcLib
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object TrackDownloader {
    private const val TAG = "TrackDownloader"

    enum class DownloadFormat(
        val label: String,
        val extension: String,
        val mimeType: String,
        val description: String
    ) {
        M4A(
            label = "M4A (AAC)",
            extension = "m4a",
            mimeType = "audio/mp4",
            description = "Native format — best quality, no conversion needed"
        ),
        MP3(
            label = "MP3",
            extension = "mp3",
            mimeType = "audio/mpeg",
            description = "Most compatible — works on all car players & old devices"
        ),
        OGG(
            label = "OGG (Vorbis)",
            extension = "ogg",
            mimeType = "audio/ogg",
            description = "Open format — good quality & smaller file size"
        );

        companion object {
            fun fromExtension(ext: String?): DownloadFormat =
                values().firstOrNull { it.extension.equals(ext, ignoreCase = true) } ?: M4A
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    fun isAlreadyDownloaded(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) return false
        val safeTitle = sanitizeFilename(title)
        return DownloadFormat.values().any { fmt ->
            File(musicDir, "$safeTitle.${fmt.extension}").exists() ||
            File(musicDir, "$title.${fmt.extension}").exists()
        }
    }

    fun getDownloadedTracks(context: Context): MutableList<DownloadedTrack?> {
        val data: MutableList<DownloadedTrack?> = ArrayList()
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) return data

        val files = musicDir.listFiles() ?: return data
        for (file in files) {
            try {
                val f = AudioFileIO.read(file)
                val tag = f.getTag()
                val audioHeader = f.getAudioHeader()
                val uidFieldIdPrefix = "----:" + context.packageName + ":"
                val uidFieldId = uidFieldIdPrefix + "TrackUID"
                val title: String? = if (tag != null) tag.getFirst(FieldKey.TITLE) else file.name
                val artist: String? = if (tag != null) tag.getFirst(FieldKey.ARTIST) else ""
                val album: String? = if (tag != null) tag.getFirst(FieldKey.ALBUM) else ""
                val year: String? = if (tag != null) tag.getFirst(FieldKey.YEAR) else ""
                val bitrate = if (audioHeader != null) audioHeader.bitRate.toString() else "344"
                val trackLength = audioHeader?.trackLength?.toString() ?: "0"

                var trackUID: String? = null
                if (tag != null) {
                    val field = tag.getFirstField(uidFieldId)
                    if (field != null) {
                        try {
                            trackUID = field.toString()
                            try {
                                val m = field.javaClass.getMethod("getContent")
                                val `val` = m.invoke(field)
                                if (`val` != null) trackUID = `val`.toString()
                            } catch (_: NoSuchMethodException) {
                            }
                        } catch (_: Exception) {
                        }
                    } else {
                        val it = tag.fields
                        while (it.hasNext()) {
                            val fField = it.next()
                            val id = fField.id
                            if (id != null && id.equals(uidFieldId, ignoreCase = true)) {
                                try {
                                    trackUID = fField.toString()
                                    break
                                } catch (_: Exception) {
                                }
                            }
                        }
                    }

                    // For MP3 and OGG, also check COMMENT
                    if (trackUID.isNullOrEmpty()) {
                        val comment = tag.getFirst(FieldKey.COMMENT)
                        if (!comment.isNullOrEmpty()) {
                            trackUID = if (comment.startsWith("TrackUID:")) {
                                comment.removePrefix("TrackUID:")
                            } else {
                                comment
                            }
                        }
                    }
                }

                val coverImage = if (tag != null) tag.firstArtwork?.binaryData else null
                val bitmap = if (coverImage != null) BitmapFactory.decodeByteArray(
                    coverImage, 0, coverImage.size
                ) else null

                val downloadedTrack = DownloadedTrack(
                    file, title, artist, album, year, bitrate, trackLength, bitmap, trackUID
                )
                data.add(downloadedTrack)
                println(downloadedTrack)
            } catch (e: Exception) {
                Log.e(TAG, "Error reading file: " + e.message)
            }
        }
        return data
    }

    fun deleteDownloadedTrack(context: Context, track: DownloadedTrack): Boolean {
        var deleted = false
        try {
            val file = track.file
            if (file != null && file.exists()) {
                val path = file.absolutePath
                deleted = file.delete()
                runCatching {
                    val resolver = context.contentResolver
                    val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    resolver.delete(
                        audioUri,
                        "${MediaStore.Audio.Media.DATA} = ?",
                        arrayOf(path)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting track: ${e.message}", e)
        }
        return deleted
    }

    fun deleteDownloadedSong(context: Context, songTitle: String?): Boolean {
        if (songTitle.isNullOrBlank()) return false
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) return false
        val safeTitle = sanitizeFilename(songTitle)
        var anyDeleted = false
        val filesToCheck = mutableListOf<File>()
        for (fmt in DownloadFormat.values()) {
            filesToCheck.add(File(musicDir, "$safeTitle.${fmt.extension}"))
            filesToCheck.add(File(musicDir, "$songTitle.${fmt.extension}"))
        }
        for (f in filesToCheck) {
            if (f.exists()) {
                val path = f.absolutePath
                if (f.delete()) {
                    anyDeleted = true
                    runCatching {
                        val resolver = context.contentResolver
                        val audioUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        resolver.delete(
                            audioUri,
                            "${MediaStore.Audio.Media.DATA} = ?",
                            arrayOf(path)
                        )
                    }
                }
            }
        }
        return anyDeleted
    }

    fun downloadAndEmbedMetadata(
        context: Context,
        song: Song,
        format: DownloadFormat = DownloadFormat.M4A,
        listener: TrackDownloadListener
    ) {
        val audioUrl = song.downloadUrl?.lastOrNull()?.url ?: ""
        val imageUrl = song.image?.lastOrNull()?.url ?: ""
        val title = song.name()
        val safeTitle = sanitizeFilename(title)
        val artist = song.artists?.primary?.firstOrNull()?.name() ?: ""
        val albumArtist = song.artists?.all?.mapNotNull { it?.name() }
            ?.joinToString(", ") ?: artist
        val album = song.album?.name() ?: ""
        val duration = song.duration?.toInt() ?: -1

        Thread {
            Handler(Looper.getMainLooper()).post { listener.onStarted() }
            Log.d(TAG, "Downloading [${format.label}] and embedding metadata...")

            try {
                // Step 1: Download raw M4A to cache
                val tempM4aFile = File(context.cacheDir, "${safeTitle}_raw.mp4")
                URL(audioUrl).openStream().use { inp ->
                    FileOutputStream(tempM4aFile).use { out ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inp.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                        }
                    }
                }

                // Step 2: Fetch lyrics (LrcLib synced preferred, then plain)
                val plainLyrics: String? = song.lyrics?.lyrics()?.takeIf { it.isNotBlank() }
                val lrcLyrics: String? = runCatching {
                    runBlocking {
                        LrcLib.getLyrics(title = title, artist = artist, duration = duration).getOrNull()
                            ?: LrcLib.getLyrics(title = title, artist = artist, duration = -1).getOrNull()
                    }
                }.getOrNull()
                val lyricsToEmbed: String? = lrcLyrics ?: plainLyrics
                Log.d(
                    TAG,
                    if (lyricsToEmbed != null)
                        "Lyrics found (${if (lrcLyrics != null) "synced LRC" else "plain"})"
                    else "No lyrics available for: $title"
                )

                // Step 3: Download artwork
                val artworkFile = File(context.cacheDir, "${safeTitle}_artwork.jpg")
                if (imageUrl.isNotBlank()) {
                    runCatching {
                        URL(imageUrl).openStream().use { inp ->
                            FileOutputStream(artworkFile).use { out ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (inp.read(buffer).also { bytesRead = it } != -1) {
                                    out.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }
                }

                // Step 4: Convert/transcode if needed, then embed tags
                val taggedFile: File = when (format) {
                    DownloadFormat.M4A -> {
                        embedTagsIntoM4a(
                            context = context,
                            file = tempM4aFile,
                            title = title,
                            artist = artist,
                            albumArtist = albumArtist,
                            album = album,
                            song = song,
                            artworkFile = artworkFile,
                            lyrics = lyricsToEmbed
                        )
                        tempM4aFile
                    }
                    DownloadFormat.MP3 -> {
                        val mp3File = File(context.cacheDir, "${safeTitle}.mp3")
                        val session = FFmpegKit.executeWithArguments(
                            arrayOf(
                                "-i", tempM4aFile.absolutePath,
                                "-codec:a", "libmp3lame",
                                "-qscale:a", "0",
                                "-id3v2_version", "3",
                                "-y", mp3File.absolutePath
                            )
                        )
                        if (!ReturnCode.isSuccess(session.returnCode)) {
                            throw RuntimeException("FFmpeg MP3 transcoding failed: ${session.failStackTrace ?: session.logsAsString}")
                        }
                        tempM4aFile.delete()
                        embedTagsIntoMp3OrOgg(
                            file = mp3File,
                            title = title,
                            artist = artist,
                            albumArtist = albumArtist,
                            album = album,
                            song = song,
                            artworkFile = artworkFile,
                            lyrics = lyricsToEmbed
                        )
                        mp3File
                    }
                    DownloadFormat.OGG -> {
                        val oggFile = File(context.cacheDir, "${safeTitle}.ogg")
                        val session = FFmpegKit.executeWithArguments(
                            arrayOf(
                                "-i", tempM4aFile.absolutePath,
                                "-codec:a", "libvorbis",
                                "-qscale:a", "6",
                                "-y", oggFile.absolutePath
                            )
                        )
                        if (!ReturnCode.isSuccess(session.returnCode)) {
                            throw RuntimeException("FFmpeg OGG transcoding failed: ${session.failStackTrace ?: session.logsAsString}")
                        }
                        tempM4aFile.delete()
                        embedTagsIntoMp3OrOgg(
                            file = oggFile,
                            title = title,
                            artist = artist,
                            albumArtist = albumArtist,
                            album = album,
                            song = song,
                            artworkFile = artworkFile,
                            lyrics = lyricsToEmbed
                        )
                        oggFile
                    }
                }

                // Step 5: Insert into MediaStore
                val values = getContentValues(safeTitle, title, artist, album, song.id, format)
                val resolver = context.contentResolver
                val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                val newUri = resolver.insert(audioCollection, values)
                if (newUri == null) {
                    Log.e(TAG, "Failed to insert into MediaStore")
                    Handler(Looper.getMainLooper()).post {
                        listener.onError("Failed to insert into MediaStore")
                    }
                    return@Thread
                }

                URL("file://${taggedFile.absolutePath}").openStream().use { inp ->
                    resolver.openOutputStream(newUri).use { out ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inp.read(buffer).also { bytesRead = it } != -1) {
                            checkNotNull(out)
                            out.write(buffer, 0, bytesRead)
                        }
                        checkNotNull(out)
                        out.flush()
                    }
                }

                taggedFile.delete()
                artworkFile.delete()

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri))
                }

                Log.d(
                    TAG,
                    "Downloaded and tagged [${format.label}]: $title" +
                            if (lyricsToEmbed != null) " (with lyrics)" else ""
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error: " + e.message, e)
                Handler(Looper.getMainLooper()).post { listener.onError(e.message) }
            } finally {
                Handler(Looper.getMainLooper()).post { listener.onFinished() }
            }
        }.start()
    }

    private fun embedTagsIntoM4a(
        context: Context,
        file: File,
        title: String,
        artist: String,
        albumArtist: String,
        album: String,
        song: Song,
        artworkFile: File,
        lyrics: String?
    ) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.getTagOrCreateAndSetDefault()
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        if (albumArtist.isNotBlank()) tag.setField(FieldKey.ALBUM_ARTIST, albumArtist)
        tag.setField(FieldKey.ALBUM, album)
        if (!song.year.isNullOrBlank()) tag.setField(FieldKey.YEAR, song.year)
        if (!song.language.isNullOrBlank()) tag.setField(FieldKey.LANGUAGE, song.language)
        if (!song.label.isNullOrBlank()) tag.setField(FieldKey.RECORD_LABEL, song.label)
        if (!song.copyright.isNullOrBlank()) tag.setField(FieldKey.COPYRIGHT, song.copyright)
        if (!lyrics.isNullOrBlank()) tag.setField(FieldKey.LYRICS, lyrics)
        if (!song.id.isNullOrBlank()) {
            tag.setField(Mp4TagReverseDnsField("----", context.packageName, "TrackUID", song.id))
        }
        if (artworkFile.exists()) {
            val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
            tag.deleteArtworkField()
            tag.setField(artwork)
        }
        audioFile.setTag(tag)
        audioFile.commit()
    }

    private fun embedTagsIntoMp3OrOgg(
        file: File,
        title: String,
        artist: String,
        albumArtist: String,
        album: String,
        song: Song,
        artworkFile: File,
        lyrics: String?
    ) {
        val audioFile = AudioFileIO.read(file)
        val tag = audioFile.getTagOrCreateAndSetDefault()
        tag.setField(FieldKey.TITLE, title)
        tag.setField(FieldKey.ARTIST, artist)
        if (albumArtist.isNotBlank()) tag.setField(FieldKey.ALBUM_ARTIST, albumArtist)
        tag.setField(FieldKey.ALBUM, album)
        if (!song.year.isNullOrBlank()) tag.setField(FieldKey.YEAR, song.year)
        if (!song.language.isNullOrBlank()) tag.setField(FieldKey.LANGUAGE, song.language)
        if (!song.label.isNullOrBlank()) tag.setField(FieldKey.RECORD_LABEL, song.label)
        if (!song.copyright.isNullOrBlank()) tag.setField(FieldKey.COPYRIGHT, song.copyright)
        if (!lyrics.isNullOrBlank()) tag.setField(FieldKey.LYRICS, lyrics)
        if (!song.id.isNullOrBlank()) {
            tag.setField(FieldKey.COMMENT, "TrackUID:${song.id}")
        }
        if (artworkFile.exists()) {
            val artwork = ArtworkFactory.createArtworkFromFile(artworkFile)
            tag.deleteArtworkField()
            tag.setField(artwork)
        }
        audioFile.setTag(tag)
        audioFile.commit()
    }

    private fun getContentValues(
        displayName: String,
        title: String?,
        artist: String?,
        album: String?,
        id: String?,
        format: DownloadFormat = DownloadFormat.M4A
    ): ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "$displayName.${format.extension}")
        values.put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
        values.put(MediaStore.Audio.Media.IS_MUSIC, true)
        values.put(MediaStore.Audio.Media.TITLE, title)
        values.put(MediaStore.Audio.Media.ARTIST, artist)
        values.put(MediaStore.Audio.Media.ALBUM, album)
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Melotune")
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