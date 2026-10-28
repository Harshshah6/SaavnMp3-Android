package com.harsh.shah.saavnmp3.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.services.DownloadCancelReceiver
import com.samyak.lrclib.LrcLib
import kotlinx.coroutines.runBlocking
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.mp4.field.Mp4TagReverseDnsField
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.Collections
import java.util.concurrent.CancellationException

object TrackDownloader {
    private const val TAG = "TrackDownloader"

    const val DOWNLOAD_CHANNEL_ID = "melotune_download_channel"
    const val DOWNLOAD_NOTIFICATION_ID = 2001
    const val ACTION_CANCEL_DOWNLOAD = "com.harsh.shah.saavnmp3.CANCEL_DOWNLOAD"

    @Volatile
    var isCancelled: Boolean = false
    @Volatile
    private var currentDownloadThread: Thread? = null
    private val activeTempFiles = Collections.synchronizedList(mutableListOf<File>())

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

    fun sanitizeFilename(name: String): String {
        val sanitized = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .trim { it == '.' || it.isWhitespace() }
        return if (sanitized.isBlank()) "Collection" else sanitized
    }

    fun isAlreadyDownloaded(title: String?, subfolder: String? = null): Boolean {
        if (title.isNullOrBlank()) return false
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) return false
        val safeTitle = sanitizeFilename(title)
        val validFormats = DownloadFormat.values()

        // 1. If a specific subfolder (e.g. album/playlist) is passed, check inside that subfolder first
        if (!subfolder.isNullOrBlank()) {
            val safeSubfolder = sanitizeFilename(subfolder)
            val subDir = File(musicDir, safeSubfolder)
            if (subDir.exists() && subDir.isDirectory) {
                val foundInSubDir = validFormats.any { fmt ->
                    File(subDir, "$safeTitle.${fmt.extension}").exists() ||
                    File(subDir, "$title.${fmt.extension}").exists()
                }
                if (foundInSubDir) return true
            }
        }

        // 2. Check directly in the root Melotune directory
        val foundInRoot = validFormats.any { fmt ->
            File(musicDir, "$safeTitle.${fmt.extension}").exists() ||
            File(musicDir, "$title.${fmt.extension}").exists()
        }
        if (foundInRoot) return true

        // 3. Check in any immediate subfolder of Melotune
        val subdirs = musicDir.listFiles { f -> f.isDirectory }
        if (subdirs != null) {
            for (dir in subdirs) {
                val foundInAnySub = validFormats.any { fmt ->
                    File(dir, "$safeTitle.${fmt.extension}").exists() ||
                    File(dir, "$title.${fmt.extension}").exists()
                }
                if (foundInAnySub) return true
            }
        }

        return false
    }

    fun getDownloadedTracks(context: Context): MutableList<DownloadedTrack?> {
        val data: MutableList<DownloadedTrack?> = ArrayList()
        val musicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "Melotune"
        )
        if (!musicDir.exists()) return data

        val audioExtensions = DownloadFormat.values().map { it.extension.lowercase() }.toSet()
        val files = musicDir.walkTopDown().maxDepth(3).filter { file ->
            file.isFile && !file.name.startsWith(".") && file.extension.lowercase() in audioExtensions
        }.sortedByDescending { it.lastModified() }.toList()

        for (file in files) {
            try {
                val f = AudioFileIO.read(file)
                val tag = f.getTag()
                val audioHeader = f.getAudioHeader()
                val uidFieldIdPrefix = "----:" + context.packageName + ":"
                val uidFieldId = uidFieldIdPrefix + "TrackUID"
                val title: String = tag?.getFirst(FieldKey.TITLE)?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
                val artist: String = tag?.getFirst(FieldKey.ARTIST) ?: ""
                val album: String = tag?.getFirst(FieldKey.ALBUM) ?: ""
                val year: String = tag?.getFirst(FieldKey.YEAR) ?: ""
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
                val parentDir = file.parentFile
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
                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "Melotune"
                )
                if (parentDir != null && parentDir != musicDir && parentDir.parentFile == musicDir) {
                    val remaining = parentDir.listFiles()
                    if (remaining.isNullOrEmpty()) {
                        parentDir.delete()
                    }
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
        val subdirs = musicDir.listFiles { f -> f.isDirectory }
        if (subdirs != null) {
            for (dir in subdirs) {
                for (fmt in DownloadFormat.values()) {
                    filesToCheck.add(File(dir, "$safeTitle.${fmt.extension}"))
                    filesToCheck.add(File(dir, "$songTitle.${fmt.extension}"))
                }
            }
        }
        for (f in filesToCheck) {
            if (f.exists()) {
                val path = f.absolutePath
                val parentDir = f.parentFile
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
                    if (parentDir != null && parentDir != musicDir && parentDir.parentFile == musicDir) {
                        val remaining = parentDir.listFiles()
                        if (remaining.isNullOrEmpty()) {
                            parentDir.delete()
                        }
                    }
                }
            }
        }
        return anyDeleted
    }

    fun createDownloadNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(DOWNLOAD_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "Melotune Downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Download progress and completion notifications"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun getCancelPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DownloadCancelReceiver::class.java).apply {
            action = ACTION_CANCEL_DOWNLOAD
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun updateDownloadProgressNotification(
        context: Context,
        title: String,
        contentText: String,
        max: Int = 0,
        progress: Int = 0,
        indeterminate: Boolean = true
    ) {
        createDownloadNotificationChannel(context)
        val cancelPendingIntent = getCancelPendingIntent(context)
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_download_for_offline_24)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setProgress(max, progress, indeterminate)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelPendingIntent)
            .setOnlyAlertOnce(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showDownloadCompletedNotification(
        context: Context,
        title: String,
        contentText: String
    ) {
        createDownloadNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_download_for_offline_24)
            .setContentTitle(title)
            .setContentText(contentText)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun showDownloadFailedNotification(
        context: Context,
        title: String,
        errorMessage: String?
    ) {
        createDownloadNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_download_for_offline_24)
            .setContentTitle(title)
            .setContentText(errorMessage ?: "An error occurred during download")
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.notify(DOWNLOAD_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun cancelDownload(context: Context) {
        isCancelled = true
        try {
            currentDownloadThread?.interrupt()
        } catch (_: Exception) {
        }
        synchronized(activeTempFiles) {
            for (f in activeTempFiles) {
                try {
                    if (f.exists()) f.delete()
                } catch (_: Exception) {
                }
            }
            activeTempFiles.clear()
        }
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notifManager?.let { mgr ->
            createDownloadNotificationChannel(context)
            val notif = NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_download_for_offline_24)
                .setContentTitle("Download Cancelled")
                .setContentText("The download was cancelled.")
                .setAutoCancel(true)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .build()
            try {
                mgr.notify(DOWNLOAD_NOTIFICATION_ID, notif)
            } catch (_: SecurityException) {
            }
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Download cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(Exception::class)
    fun downloadSingleTrackSync(
        context: Context,
        song: Song,
        format: DownloadFormat = DownloadFormat.M4A,
        subfolder: String? = null,
        onProgress: ((String) -> Unit)? = null
    ): Boolean {
        if (isCancelled || Thread.currentThread().isInterrupted) {
            throw CancellationException("Download cancelled")
        }

        var resolvedSong = song
        var audioUrl = song.downloadUrl?.lastOrNull()?.url ?: ""
        var imageUrl = song.image?.lastOrNull()?.url ?: ""

        // If audioUrl is blank and song id is available, resolve it synchronously
        if (audioUrl.isBlank() && !song.id.isNullOrBlank()) {
            runCatching {
                val url = "${ApiManager.SONGS_URL}/${song.id}"
                val connection = URL(url).openConnection()
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val json = connection.getInputStream().bufferedReader().use { it.readText() }
                val fetchedResponse = Gson().fromJson(json, SongResponse::class.java)
                val fetchedSong = fetchedResponse.data?.firstOrNull()
                if (fetchedSong != null) {
                    resolvedSong = fetchedSong
                    audioUrl = fetchedSong.downloadUrl?.lastOrNull()?.url ?: ""
                    if (imageUrl.isBlank()) {
                        imageUrl = fetchedSong.image?.lastOrNull()?.url ?: ""
                    }
                }
            }
        }

        val title = resolvedSong.name()
        val safeTitle = sanitizeFilename(title)
        val artist = resolvedSong.artists?.primary?.firstOrNull()?.name() ?: ""
        val albumArtist = resolvedSong.artists?.all?.mapNotNull { it?.name() }
            ?.joinToString(", ") ?: artist
        val album = resolvedSong.album?.name() ?: ""
        val duration = resolvedSong.duration?.toInt() ?: -1

        if (audioUrl.isBlank()) {
            throw IllegalStateException("Audio URL not found for song: $title")
        }

        onProgress?.invoke("Downloading audio...")
        val tempM4aFile = File(context.cacheDir, "${safeTitle}_raw_${System.currentTimeMillis()}.mp4")
        activeTempFiles.add(tempM4aFile)

        val artworkFile = File(context.cacheDir, "${safeTitle}_artwork_${System.currentTimeMillis()}.jpg")
        activeTempFiles.add(artworkFile)

        var convertedFile: File? = null

        try {
            // Step 1: Download raw audio
            URL(audioUrl).openStream().use { inp ->
                FileOutputStream(tempM4aFile).use { out ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inp.read(buffer).also { bytesRead = it } != -1) {
                        if (isCancelled || Thread.currentThread().isInterrupted) {
                            throw CancellationException("Download cancelled")
                        }
                        out.write(buffer, 0, bytesRead)
                    }
                }
            }

            if (isCancelled || Thread.currentThread().isInterrupted) {
                throw CancellationException("Download cancelled")
            }

            // Step 2: Fetch lyrics
            onProgress?.invoke("Fetching lyrics...")
            val plainLyrics: String? = resolvedSong.lyrics?.lyrics()?.takeIf { it.isNotBlank() }
            val lrcLyrics: String? = runCatching {
                runBlocking {
                    LrcLib.getLyrics(title = title, artist = artist, duration = duration).getOrNull()
                        ?: LrcLib.getLyrics(title = title, artist = artist, duration = -1).getOrNull()
                }
            }.getOrNull()
            val lyricsToEmbed: String? = lrcLyrics ?: plainLyrics

            if (isCancelled || Thread.currentThread().isInterrupted) {
                throw CancellationException("Download cancelled")
            }

            // Step 3: Download artwork
            if (imageUrl.isNotBlank()) {
                runCatching {
                    URL(imageUrl).openStream().use { inp ->
                        FileOutputStream(artworkFile).use { out ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (inp.read(buffer).also { bytesRead = it } != -1) {
                                if (isCancelled || Thread.currentThread().isInterrupted) {
                                    throw CancellationException("Download cancelled")
                                }
                                out.write(buffer, 0, bytesRead)
                            }
                        }
                    }
                }
            }

            if (isCancelled || Thread.currentThread().isInterrupted) {
                throw CancellationException("Download cancelled")
            }

            // Step 4: Transcode / Tagging
            onProgress?.invoke("Embedding metadata (${format.label})...")
            val taggedFile: File = when (format) {
                DownloadFormat.M4A -> {
                    embedTagsIntoM4a(
                        context = context,
                        file = tempM4aFile,
                        title = title,
                        artist = artist,
                        albumArtist = albumArtist,
                        album = album,
                        song = resolvedSong,
                        artworkFile = artworkFile,
                        lyrics = lyricsToEmbed
                    )
                    tempM4aFile
                }
                DownloadFormat.MP3 -> {
                    val mp3File = File(context.cacheDir, "${safeTitle}_${System.currentTimeMillis()}.mp3")
                    convertedFile = mp3File
                    activeTempFiles.add(mp3File)
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
                        song = resolvedSong,
                        artworkFile = artworkFile,
                        lyrics = lyricsToEmbed
                    )
                    mp3File
                }
                DownloadFormat.OGG -> {
                    val oggFile = File(context.cacheDir, "${safeTitle}_${System.currentTimeMillis()}.ogg")
                    convertedFile = oggFile
                    activeTempFiles.add(oggFile)
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
                        song = resolvedSong,
                        artworkFile = artworkFile,
                        lyrics = lyricsToEmbed
                    )
                    oggFile
                }
            }

            if (isCancelled || Thread.currentThread().isInterrupted) {
                throw CancellationException("Download cancelled")
            }

            // Step 5: Save to MediaStore
            onProgress?.invoke("Saving to storage...")
            val values = getContentValues(safeTitle, title, artist, album, resolvedSong.id, format, subfolder)
            val resolver = context.contentResolver
            val audioCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            val newUri = resolver.insert(audioCollection, values)
                ?: throw RuntimeException("Failed to insert into MediaStore")

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

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri))
            }

            return true
        } finally {
            activeTempFiles.remove(tempM4aFile)
            activeTempFiles.remove(artworkFile)
            if (convertedFile != null) {
                activeTempFiles.remove(convertedFile)
                if (convertedFile.exists()) convertedFile.delete()
            }
            if (tempM4aFile.exists()) tempM4aFile.delete()
            if (artworkFile.exists()) artworkFile.delete()
        }
    }

    fun downloadAndEmbedMetadata(
        context: Context,
        song: Song,
        format: DownloadFormat = DownloadFormat.M4A,
        listener: TrackDownloadListener
    ) {
        downloadAndEmbedMetadata(context, song, format, null, listener)
    }

    fun downloadAndEmbedMetadata(
        context: Context,
        song: Song,
        format: DownloadFormat = DownloadFormat.M4A,
        subfolder: String? = null,
        listener: TrackDownloadListener
    ) {
        isCancelled = false
        val thread = Thread {
            Handler(Looper.getMainLooper()).post { listener.onStarted() }
            val title = song.name()
            Log.d(TAG, "Downloading [${format.label}] and embedding metadata...")
            updateDownloadProgressNotification(context, "Downloading $title", "Downloading audio...", 0, 0, true)

            try {
                val success = downloadSingleTrackSync(context, song, format, subfolder) { status ->
                    updateDownloadProgressNotification(context, "Downloading $title", status, 0, 0, true)
                }

                if (success) {
                    val destText = if (!subfolder.isNullOrBlank()) {
                        "Music/Melotune/${sanitizeFilename(subfolder)}"
                    } else {
                        "Music/Melotune"
                    }
                    showDownloadCompletedNotification(context, "Download Complete", "$title saved to $destText")
                    Handler(Looper.getMainLooper()).post { listener.onFinished() }
                } else {
                    if (isCancelled) {
                        Handler(Looper.getMainLooper()).post { listener.onError("Cancelled") }
                    } else {
                        showDownloadFailedNotification(context, "Download Failed", "Could not complete download")
                        Handler(Looper.getMainLooper()).post { listener.onError("Failed to download") }
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Download cancelled by user")
                Handler(Looper.getMainLooper()).post { listener.onError("Cancelled") }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading track: ${e.message}", e)
                if (!isCancelled) {
                    showDownloadFailedNotification(context, "Download Failed", e.message)
                }
                Handler(Looper.getMainLooper()).post { listener.onError(e.message) }
            } finally {
                if (currentDownloadThread == Thread.currentThread()) {
                    currentDownloadThread = null
                }
            }
        }
        currentDownloadThread = thread
        thread.start()
    }

    fun downloadAlbum(
        context: Context,
        albumTitle: String,
        songs: List<Song>,
        format: DownloadFormat = DownloadFormat.M4A,
        listener: AlbumDownloadListener? = null
    ) {
        if (songs.isEmpty()) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "No songs to download in collection", Toast.LENGTH_SHORT).show()
                listener?.onError("Empty songs list")
            }
            return
        }

        isCancelled = false
        val safeAlbumFolder = sanitizeFilename(albumTitle).ifBlank { "Collection" }

        val thread = Thread {
            Handler(Looper.getMainLooper()).post {
                listener?.onAlbumStarted(songs.size)
            }
            val total = songs.size
            var downloaded = 0
            var failed = 0

            updateDownloadProgressNotification(
                context,
                "Downloading: $albumTitle",
                "Starting download (0/$total)...",
                total,
                0,
                false
            )

            for ((index, song) in songs.withIndex()) {
                if (isCancelled || Thread.currentThread().isInterrupted) {
                    break
                }
                val songTitle = song.name()
                Handler(Looper.getMainLooper()).post {
                    listener?.onTrackProgress(index + 1, total, songTitle)
                }
                updateDownloadProgressNotification(
                    context,
                    "Downloading: $albumTitle (${index + 1}/$total)",
                    songTitle,
                    total,
                    index,
                    false
                )

                try {
                    if (isAlreadyDownloaded(songTitle, safeAlbumFolder)) {
                        downloaded++
                        continue
                    }
                    val success = downloadSingleTrackSync(
                        context = context,
                        song = song,
                        format = format,
                        subfolder = safeAlbumFolder
                    )
                    if (success) {
                        downloaded++
                    } else {
                        failed++
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Collection download cancelled at track: $songTitle")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Failed downloading track $songTitle in collection: ${e.message}")
                    failed++
                }
            }

            if (isCancelled) {
                cancelDownload(context)
                Handler(Looper.getMainLooper()).post {
                    listener?.onAlbumCancelled()
                }
            } else {
                showDownloadCompletedNotification(
                    context,
                    "Download Complete",
                    "$albumTitle: $downloaded downloaded to Melotune/$safeAlbumFolder" + if (failed > 0) ", $failed failed" else ""
                )
                Handler(Looper.getMainLooper()).post {
                    listener?.onAlbumCompleted(downloaded, failed)
                    Toast.makeText(
                        context,
                        "\"$albumTitle\" downloaded to Melotune/$safeAlbumFolder ($downloaded/$total songs)",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            if (currentDownloadThread == Thread.currentThread()) {
                currentDownloadThread = null
            }
        }
        currentDownloadThread = thread
        thread.start()
    }

    fun downloadCollection(
        context: Context,
        collectionTitle: String,
        songs: List<Song>,
        format: DownloadFormat = DownloadFormat.M4A,
        listener: AlbumDownloadListener? = null
    ) {
        downloadAlbum(context, collectionTitle, songs, format, listener)
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
        format: DownloadFormat = DownloadFormat.M4A,
        subfolder: String? = null
    ): ContentValues {
        val values = ContentValues()
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, "$displayName.${format.extension}")
        values.put(MediaStore.Audio.Media.MIME_TYPE, format.mimeType)
        values.put(MediaStore.Audio.Media.IS_MUSIC, true)
        values.put(MediaStore.Audio.Media.TITLE, title)
        values.put(MediaStore.Audio.Media.ARTIST, artist)
        values.put(MediaStore.Audio.Media.ALBUM, album)

        val folderName = if (!subfolder.isNullOrBlank()) {
            "Melotune/${sanitizeFilename(subfolder)}"
        } else {
            "Melotune"
        }

        val targetDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            folderName
        )
        runCatching {
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/$folderName/")
        } else {
            val destFile = File(targetDir, "$displayName.${format.extension}")
            values.put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
        }
        return values
    }

    interface TrackDownloadListener {
        fun onStarted()
        fun onFinished()
        fun onError(errorMessage: String?)
    }

    interface AlbumDownloadListener {
        fun onAlbumStarted(totalTracks: Int)
        fun onTrackProgress(current: Int, total: Int, trackTitle: String)
        fun onAlbumCompleted(downloadedCount: Int, failedCount: Int)
        fun onAlbumCancelled()
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
