package com.harsh.shah.saavnmp3.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.adapters.ActivityListSongsItemAdapter
import com.harsh.shah.saavnmp3.adapters.UserCreatedSongsListAdapter
import com.harsh.shah.saavnmp3.databinding.ActivityListBinding
import com.harsh.shah.saavnmp3.databinding.ActivityListMoreInfoBottomSheetBinding
import com.harsh.shah.saavnmp3.databinding.UserCreatedListActivityMoreBottomSheetBinding
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.AlbumSearch
import com.harsh.shah.saavnmp3.records.PlaylistSearch
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.harsh.shah.saavnmp3.utils.MiniPlayerHelper
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.harsh.shah.saavnmp3.utils.ThemeManager
import com.harsh.shah.saavnmp3.utils.TrackDownloader
import com.harsh.shah.saavnmp3.utils.attachSnapHelper
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView
import com.bumptech.glide.Glide

class ListActivity : AppCompatActivity() {
    var binding: ActivityListBinding? = null

    private val trackQueue: MutableList<String?> = ArrayList()
    private val loadedSongs: MutableList<Song> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        MiniPlayerHelper.initMiniPlayer(this)

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.addMoreSongs.visibility = View.GONE

        Log.i("ListActivity", "onCreate: reached ListActivity")

        showShimmerData()

        binding!!.playAllBtn.setOnClickListener {
            if (!trackQueue.isEmpty()) {
                MusicPlayerManager.trackQueue = ArrayList(trackQueue)
                MusicPlayerManager.track_position = 0
                Log.i(
                    TAG,
                    "trackQueueSet:  ${MusicPlayerManager.trackQueue}"
                )
                startActivity(
                    Intent(this@ListActivity, MusicOverviewActivity::class.java).putExtra(
                        "id",
                        trackQueue[0]
                    )
                )
            }
        }

        binding!!.downloadAlbumBtn?.setOnClickListener {
            startDownloadCurrentCollection()
        }
        binding!!.downloadAlbumIcon?.setOnClickListener {
            startDownloadCurrentCollection()
        }
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)

        binding!!.addToLibrary.setOnClickListener(View.OnClickListener {
            if (albumItem == null) return@OnClickListener
            if (isAlbumInLibrary(albumItem!!, sharedPreferenceManager.savedLibrariesData)) {
                MaterialAlertDialogBuilder(this@ListActivity)
                    .setTitle("Are you sure?")
                    .setMessage("Do you want to remove this album from your library?")
                    .setPositiveButton(
                        "Yes",
                        DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                            val index = getAlbumIndexInLibrary(
                                albumItem!!,
                                sharedPreferenceManager.savedLibrariesData
                            )
                            if (index == -1) return@OnClickListener
                            sharedPreferenceManager.removeLibraryFromSavedLibraries(index)
                            Snackbar.make(
                                binding!!.getRoot(),
                                "Removed from Library",
                                Snackbar.LENGTH_SHORT
                            ).show()
                            updateAlbumInLibraryStatus()
                            finish()
                        })
                    .setNegativeButton(
                        "No"
                    ) { _: DialogInterface?, _: Int -> }
                    .show()
            } else {
                val library = Library(
                    albumItem!!.id,
                    false,
                    isAlbum,
                    binding!!.albumTitle.text.toString(),
                    albumItem!!.albumCover,
                    binding!!.albumSubTitle.text.toString(),
                    ArrayList()
                )
                sharedPreferenceManager.addLibraryToSavedLibraries(library)
                Snackbar.make(binding!!.getRoot(), "Added to Library", Snackbar.LENGTH_SHORT).show()
            }
            updateAlbumInLibraryStatus()
        })

        binding!!.addMoreSongs.setOnClickListener {
            startActivity(Intent(this@ListActivity, SearchActivity::class.java))
        }

        binding!!.moreIcon.setOnClickListener { onMoreIconClicked() }

        showData()
    }

    override fun onResume() {
        super.onResume()
        if (intent.extras != null && intent.extras!!
                .getBoolean("createdByUser", false)
        ) {
            onUserCreatedFetch()
        }
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    private fun onMoreIconClicked() {
        if (albumItem == null) return

        if (isUserCreated) {
            onMoreIconClickedUserCreated()
            return
        }

        val bottomSheetDialog =
            BottomSheetDialog(this@ListActivity, R.style.MyBottomSheetDialogTheme)
        val _binding = ActivityListMoreInfoBottomSheetBinding.inflate(layoutInflater)

        _binding.albumTitle.text = binding!!.albumTitle.text.toString()
        _binding.albumSubTitle.text = binding!!.albumSubTitle.text.toString()
        Glide.with(_binding.coverImage).load(albumItem!!.albumCover).into(_binding.coverImage)

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)
        val savedLibraries: SavedLibraries? = sharedPreferenceManager.savedLibrariesData
        if (savedLibraries == null || savedLibraries.lists == null) {
            _binding.addToLibrary.titleTextView?.text = "Add to library"
            _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_add_24)
        } else {
            if (isAlbumInLibrary(albumItem!!, savedLibraries)) {
                _binding.addToLibrary.titleTextView?.text = "Remove from library"
                _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_close_24)
            } else {
                _binding.addToLibrary.titleTextView?.text = "Add to library"
                _binding.addToLibrary.iconImageView?.setImageResource(R.drawable.round_add_24)
            }
        }
        _binding.addToLibrary.setOnClickListener {
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        }

        val isDownloaded = isAlbumDownloaded()
        val primaryColor = ThemeManager.getPrimaryColor(this)
        val textMainColor = resources.getColor(R.color.textMain, null)

        if (isDownloaded) {
            _binding.downloadAlbum?.titleTextView?.text = "Downloaded"
            _binding.downloadAlbum?.iconImageView?.setImageResource(R.drawable.round_done_24)
            _binding.downloadAlbum?.iconImageView?.imageTintList = ColorStateList.valueOf(primaryColor)
        } else {
            val label = if (isAlbum) "Download Album" else "Download Playlist"
            _binding.downloadAlbum?.titleTextView?.text = label
            _binding.downloadAlbum?.iconImageView?.setImageResource(R.drawable.baseline_download_for_offline_24)
            _binding.downloadAlbum?.iconImageView?.imageTintList = ColorStateList.valueOf(textMainColor)
        }

        _binding.downloadAlbum?.setOnClickListener {
            bottomSheetDialog.dismiss()
            startDownloadCurrentCollection()
        }

        for (artist in artistData) {
            try {
                val imgUrl = artist.image ?: ""
                val bottomSheetItemView =
                    BottomSheetItemView(this@ListActivity, artist.name, imgUrl, artist.id)
                bottomSheetItemView.setFocusable(true)
                bottomSheetItemView.isClickable = true
                bottomSheetItemView.setOnClickListener {
                    Log.i("ListActivity", "BottomSheetItemView: onCLicked!")
                    startActivity(
                        Intent(this@ListActivity, ArtistProfileActivity::class.java)
                            .putExtra(
                                "data", Gson().toJson(
                                    BasicDataRecord(artist.id, artist.name, "", imgUrl)
                                )
                            )
                    )
                }
                _binding.main.addView(bottomSheetItemView)
            } catch (e: Exception) {
                Log.e("ListActivity", "BottomSheetDialog: ", e)
            }
        }

        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.create()
        bottomSheetDialog.show()
    }

    private fun onMoreIconClickedUserCreated() {
        val bottomSheetDialog =
            BottomSheetDialog(this@ListActivity, R.style.MyBottomSheetDialogTheme)
        val _binding = UserCreatedListActivityMoreBottomSheetBinding.inflate(layoutInflater)

        _binding.albumTitle.text = binding!!.albumTitle.text.toString()
        _binding.albumSubTitle.text = binding!!.albumSubTitle.text.toString()
        Glide.with(_binding.coverImage).load(albumItem!!.albumCover).into(_binding.coverImage)

        _binding.removeLibrary.setOnClickListener {
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        }

        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.create()
        bottomSheetDialog.show()
    }

    private fun updateAlbumInLibraryStatus() {
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this@ListActivity)
        if (sharedPreferenceManager.savedLibrariesData == null) binding!!.addToLibrary.setImageResource(
            R.drawable.round_add_24
        )
        else {
            val savedLibraries = sharedPreferenceManager.savedLibrariesData
            binding!!.addToLibrary.setImageResource(
                if (savedLibraries != null && isAlbumInLibrary(
                        albumItem!!,
                        savedLibraries
                    )
                ) R.drawable.round_done_24 else R.drawable.round_add_24
            )
        }
    }

    @SuppressLint("NewApi")
    private fun isAlbumInLibrary(albumItem: AlbumItem, savedLibraries: SavedLibraries?): Boolean {
        if (savedLibraries == null || savedLibraries.lists == null) {
            return false
        }
        Log.i("ListActivity", "isAlbumInLibrary: $savedLibraries")
        if (savedLibraries.lists.isEmpty()) return false
        val lists = savedLibraries.lists
        return lists.stream()
            .anyMatch { library: Library? -> library?.id == albumItem.id }
    }

    @SuppressLint("NewApi")
    private fun getAlbumIndexInLibrary(albumItem: AlbumItem, savedLibraries: SavedLibraries?): Int {
        if (savedLibraries == null || savedLibraries.lists == null) {
            return -1
        }
        Log.i("ListActivity", "getAlbumIndexInLibrary: $savedLibraries")
        if (savedLibraries.lists.isEmpty()) return -1
        var index = -1
        val lists = savedLibraries.lists
        for (library in lists) {
            if (library?.id == albumItem.id) {
                index = lists.indexOf(library)
                break
            }
        }
        return index
    }

    private fun showShimmerData() {
        val data: MutableList<Song?> = ArrayList()
        for (i in 0..10) {
            data.add(
                Song(
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
                )
            )
        }
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                data.filterNotNull().toMutableList()
            )
        )
    }

    private var albumItem: AlbumItem? = null
    private var isAlbum = false

    private fun showData() {
        if (intent.extras == null) return
        albumItem = Gson().fromJson(
            intent.extras!!.getString("data"),
            AlbumItem::class.java
        )
        updateAlbumInLibraryStatus()
        if (albumItem != null) {
            binding!!.albumTitle.text = albumItem!!.albumTitle()
            binding!!.albumSubTitle.text = albumItem!!.albumSubTitle()
            if (albumItem!!.albumCover?.isNotBlank() == true) {
                Glide.with(binding!!.albumCover)
                    .load(albumItem!!.albumCover!!)
                    .placeholder(R.drawable.baseline_album_24)
                    .error(R.drawable.baseline_album_24)
                    .into(binding!!.albumCover)
            }
        }

        val apiManager = ApiManager(this)
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)
        var intentId = intent.extras!!.getString("id", "").trim()
        if (intentId.startsWith("jiosaavn.com")) {
            intentId = "https://$intentId"
        } else if (intentId.startsWith("www.jiosaavn.com")) {
            intentId = "https://$intentId"
        }

        if (intent.extras!!.getBoolean("createdByUser", false)) {
            onUserCreatedFetch()
            return
        }

        val checkIfUrlData =
            (intentId.startsWith("http") || intentId.startsWith("www")) && intentId.contains("jiosaavn.com")
        if (intent.extras!!.getString("type", "") == "album") {
            isAlbum = true
            val effectiveId = albumItem?.id ?: intentId
            if (effectiveId.isNotBlank()) {
                val cached = sharedPreferenceManager.getAlbumResponseById(effectiveId)
                if (cached != null) {
                    onAlbumFetched(cached)
                    return
                }
            }
            val requestListener: RequestNetwork.RequestListener =
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val albumSearch =
                            Gson().fromJson(response, AlbumSearch::class.java)
                        Log.i("ListActivity", "onResponse: $albumSearch")
                        if (albumSearch.success) {
                            val data = albumSearch.data ?: return
                            sharedPreferenceManager.setAlbumResponseById(
                                data.id ?: effectiveId,
                                albumSearch
                            )
                            onAlbumFetched(albumSearch)
                        }
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        Log.e("ListActivity", "onErrorResponse: $message")
                        Toast.makeText(
                            this@ListActivity,
                            "Failed to fetch Album",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

            if (checkIfUrlData) {
                apiManager.retrieveAlbumByLink(intentId, requestListener)
            } else {
                apiManager.retrieveAlbumById(effectiveId, requestListener)
            }
            return
        }

        val responseListener: RequestNetwork.RequestListener =
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    Log.i("API_RESPONSE", "onResponse: $response")
                    val playlistSearch =
                        Gson().fromJson(response, PlaylistSearch::class.java)
                    if (playlistSearch.success) {
                        val data = playlistSearch.data ?: return
                        sharedPreferenceManager.setPlaylistResponseById(
                            data.id ?: "",
                            playlistSearch
                        )
                        onPlaylistFetched(playlistSearch)
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                }
            }
        if (checkIfUrlData) {
            apiManager.retrievePlaylistByLink(intentId, null, null, responseListener)
        } else {
            val cached = sharedPreferenceManager.getPlaylistResponseById(albumItem!!.id ?: "")
            if (cached != null) {
                onPlaylistFetched(cached)
            }
            apiManager.retrievePlaylistById(albumItem!!.id ?: "", null, null, responseListener)
        }
    }

    private var isUserCreated = false

    private fun onUserCreatedFetch() {
        isUserCreated = true

        binding!!.shareIcon.visibility = View.INVISIBLE
        // binding.moreIcon.setVisibility(View.INVISIBLE);
        binding!!.addToLibrary.visibility = View.INVISIBLE
        binding!!.addMoreSongs.visibility = View.VISIBLE

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)
        val savedLibraries = sharedPreferenceManager.savedLibrariesData
        if (savedLibraries == null || savedLibraries.lists.isNullOrEmpty()) finish()
        var library: Library? = null
        if (savedLibraries != null) for (l in savedLibraries.lists ?: emptyList()) {
            if (l?.id == albumItem!!.id) {
                library = l
                break
            }
        }
        if (library == null) finish()
        if (library != null) {
            binding!!.albumTitle.text = library.name
            binding!!.albumSubTitle.text = library.description
            Glide.with(binding!!.albumCover).load(library.image).into(binding!!.albumCover)

            val songs = library.songs ?: mutableListOf()
            loadedSongs.clear()
            for (s in songs) {
                if (s != null && !s.id.isNullOrBlank()) {
                    val convertedSong = Song(
                        id = s.id,
                        name = s.title,
                        type = null,
                        year = null,
                        releaseDate = null,
                        duration = null,
                        label = null,
                        explicitContent = false,
                        playCount = null,
                        language = null,
                        hasLyrics = false,
                        lyricsId = null,
                        lyrics = null,
                        url = null,
                        copyright = null,
                        album = null,
                        artists = null,
                        image = null,
                        downloadUrl = null
                    )
                    loadedSongs.add(convertedSong)
                }
            }
            updateAlbumDownloadUI()

            binding!!.recyclerView.setAdapter(
                UserCreatedSongsListAdapter(
                    songs.filterNotNull().toMutableList()
                )
            )
            for (song in songs) {
                if (song != null) trackQueue.add(song.id)
            }
        }
    }

    private fun onAlbumFetched(albumSearch: AlbumSearch) {
        val data = albumSearch.data ?: return

        val imageList = data.image
        val coverUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: ""
                       else albumItem?.albumCover ?: ""
        if (albumItem == null) {
            albumItem = AlbumItem(data.name(), data.description(), coverUrl, data.id)
            updateAlbumInLibraryStatus()
        }

        binding!!.albumTitle.text = data.name()
        binding!!.albumSubTitle.text = data.description()
        if (coverUrl.isNotEmpty()) {
            Glide.with(binding!!.albumCover).load(coverUrl)
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(binding!!.albumCover)
        }
        val songs = data.songs ?: mutableListOf()
        loadedSongs.clear()
        loadedSongs.addAll(songs.filterNotNull())
        updateAlbumDownloadUI()
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                songs.filterNotNull().toMutableList(),
                fallbackCoverUrl = coverUrl
            )
        )
        for (song in songs) {
            if (song != null) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener {
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        for (artist in data.artist?.all ?: emptyList()) {
            if (artist == null) continue
            val aImgList = artist.image
            artistData.add(
                ArtistData(
                    artist.name(), artist.id,
                    if (!aImgList.isNullOrEmpty())
                        aImgList[aImgList.size - 1]?.url ?: ""
                    else
                        ""
                )
            )
        }
    }

    private fun onPlaylistFetched(playlistSearch: PlaylistSearch) {
        val data = playlistSearch.data ?: return

        val imageList = data.image
        val coverUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: ""
                       else albumItem?.albumCover ?: ""
        if (albumItem == null) {
            albumItem = AlbumItem(data.name(), data.description(), coverUrl, data.id)
            updateAlbumInLibraryStatus()
        }

        binding!!.albumTitle.text = data.name()
        binding!!.albumSubTitle.text = data.description()
        if (coverUrl.isNotEmpty()) {
            Glide.with(binding!!.albumCover).load(coverUrl)
                .placeholder(R.drawable.baseline_album_24)
                .error(R.drawable.baseline_album_24)
                .into(binding!!.albumCover)
        }
        val songs = data.songs ?: mutableListOf()
        loadedSongs.clear()
        loadedSongs.addAll(songs.filterNotNull())
        updateAlbumDownloadUI()
        binding!!.recyclerView.setAdapter(
            ActivityListSongsItemAdapter(
                songs.filterNotNull().toMutableList(),
                fallbackCoverUrl = coverUrl
            )
        )
        for (song in songs) {
            if (song != null) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener {
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

        for (artist in data.artists ?: emptyList()) {
            if (artist == null) continue
            val aImgList = artist.image
            artistData.add(
                ArtistData(
                    artist.name(), artist.id,
                    if (!aImgList.isNullOrEmpty())
                        aImgList[aImgList.size - 1]?.url
                            ?: "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
                    else
                        "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
                )
            )
        }
    }

    private val artistData: MutableList<ArtistData> = ArrayList<ArtistData>()

    fun backPress(view: View?) {
        finish()
    }


    private data class ArtistData(
        val name: String?,
        val id: String?,
        val image: String?
    )

    private fun isAlbumDownloaded(): Boolean {
        val title = binding?.albumTitle?.text?.toString()
        return loadedSongs.isNotEmpty() && loadedSongs.all { song ->
            TrackDownloader.isAlreadyDownloaded(song.name(), title)
        }
    }

    private fun updateAlbumDownloadUI() {
        val downloaded = isAlbumDownloaded()
        val primaryColor = ThemeManager.getPrimaryColor(this)
        val textMainColor = resources.getColor(R.color.textMain, null)
        val collectionType = if (isAlbum) "Album" else "Playlist"
        if (downloaded) {
            binding?.downloadAlbumIcon?.setImageResource(R.drawable.round_done_24)
            binding?.downloadAlbumIcon?.imageTintList = ColorStateList.valueOf(primaryColor)
            binding?.downloadAlbumIcon?.contentDescription = "$collectionType Downloaded"
        } else {
            binding?.downloadAlbumIcon?.setImageResource(R.drawable.baseline_download_for_offline_24)
            binding?.downloadAlbumIcon?.imageTintList = ColorStateList.valueOf(textMainColor)
            binding?.downloadAlbumIcon?.contentDescription = "Download $collectionType"
        }
    }

    private fun startDownloadCurrentCollection() {
        if (loadedSongs.isEmpty()) {
            Toast.makeText(this, "No songs available to download", Toast.LENGTH_SHORT).show()
            return
        }
        val title = binding?.albumTitle?.text?.toString() ?: if (isAlbum) "Album" else "Playlist"
        val collectionType = if (isAlbum) "Album" else "Playlist"
        if (isAlbumDownloaded()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("$collectionType Downloaded")
                .setMessage("All songs from \"$title\" are downloaded on this device.")
                .setPositiveButton("Open Downloads") { _, _ ->
                    startActivity(Intent(this, DownloadManagerActivity::class.java))
                }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        val settingsMgr = SettingsActivity.SettingsSharedPrefManager(this)
        val format = TrackDownloader.DownloadFormat.fromExtension(settingsMgr.downloadFormat)

        MaterialAlertDialogBuilder(this)
            .setTitle("Download $collectionType")
            .setMessage("Download all ${loadedSongs.size} tracks from \"$title\" (${format.label}) into \"Melotune/$title\"?")
            .setPositiveButton("Download") { _, _ ->
                val progressDialog = ProgressDialog(this@ListActivity)
                progressDialog.setTitle("Downloading $collectionType")
                progressDialog.setMessage("Downloading ${loadedSongs.size} songs from \"$title\"...")
                progressDialog.setCancelable(true)
                progressDialog.setCanceledOnTouchOutside(true)
                progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel") { _, _ ->
                    TrackDownloader.cancelDownload(this@ListActivity)
                    Toast.makeText(this@ListActivity, "$collectionType download cancelled", Toast.LENGTH_SHORT).show()
                }
                progressDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Download in Background") { dialog, _ ->
                    dialog.dismiss()
                    Snackbar.make(binding!!.root, "Downloading $collectionType in background. Check notifications.", Snackbar.LENGTH_LONG)
                        .setAction("Cancel") {
                            TrackDownloader.cancelDownload(this@ListActivity)
                        }.show()
                }
                progressDialog.setOnCancelListener {
                    Snackbar.make(binding!!.root, "Downloading $collectionType in background. Check notifications.", Snackbar.LENGTH_LONG)
                        .setAction("Cancel") {
                            TrackDownloader.cancelDownload(this@ListActivity)
                        }.show()
                }

                TrackDownloader.downloadAlbum(
                    this@ListActivity,
                    title,
                    loadedSongs,
                    format,
                    object : TrackDownloader.AlbumDownloadListener {
                        override fun onAlbumStarted(totalTracks: Int) {
                            if (!isFinishing && !isDestroyed) {
                                progressDialog.show()
                            }
                        }

                        override fun onTrackProgress(current: Int, total: Int, trackTitle: String) {
                            if (progressDialog.isShowing) {
                                progressDialog.setMessage("Downloading ($current/$total):\n$trackTitle")
                            }
                        }

                        override fun onAlbumCompleted(downloadedCount: Int, failedCount: Int) {
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            Toast.makeText(
                                this@ListActivity,
                                "Successfully downloaded $downloadedCount tracks into \"Melotune/$title\".",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateAlbumDownloadUI()
                        }

                        override fun onAlbumCancelled() {
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            Toast.makeText(this@ListActivity, "$collectionType download cancelled", Toast.LENGTH_SHORT).show()
                            updateAlbumDownloadUI()
                        }

                        override fun onError(errorMessage: String?) {
                            if (progressDialog.isShowing) {
                                progressDialog.dismiss()
                            }
                            if (errorMessage != "Cancelled") {
                                Toast.makeText(
                                    this@ListActivity,
                                    "Download error: $errorMessage",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            updateAlbumDownloadUI()
                        }
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        private const val TAG = "ListActivity"
    }
}
