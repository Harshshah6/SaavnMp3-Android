package com.harsh.shah.saavnmp3.activities

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.BaseApplicationClass
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
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.harsh.shah.saavnmp3.utils.customview.BottomSheetItemView
import com.squareup.picasso.Picasso

class ListActivity : AppCompatActivity() {
    var binding: ActivityListBinding? = null

    private val trackQueue: MutableList<String?> = ArrayList<String?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.addMoreSongs.setVisibility(View.GONE)

        Log.i("ListActivity", "onCreate: reached ListActivity")

        showShimmerData()

        binding!!.playAllBtn.setOnClickListener(View.OnClickListener { view: View? ->
            if (!trackQueue.isEmpty()) {
                (getApplicationContext() as BaseApplicationClass).trackQueue = trackQueue
                BaseApplicationClass.Companion.track_position = 0
                Log.i(
                    TAG,
                    "trackQueueSet: " + BaseApplicationClass.Companion.trackQueue + " With POS " + BaseApplicationClass.Companion.track_position
                )
                startActivity(
                    Intent(this@ListActivity, MusicOverviewActivity::class.java).putExtra(
                        "id",
                        trackQueue.get(0)
                    )
                )
            }
        })
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.Companion.getInstance(this@ListActivity)

        binding!!.addToLibrary.setOnClickListener(View.OnClickListener { view: View? ->
            if (albumItem == null) return@OnClickListener
            if (isAlbumInLibrary(albumItem!!, sharedPreferenceManager.savedLibrariesData)) {
                MaterialAlertDialogBuilder(this@ListActivity)
                    .setTitle("Are you sure?")
                    .setMessage("Do you want to remove this album from your library?")
                    .setPositiveButton(
                        "Yes",
                        DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
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
                        "No",
                        DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int -> })
                    .show()
            } else {
                val library = Library(
                    albumItem!!.id,
                    false,
                    isAlbum,
                    binding!!.albumTitle.getText().toString(),
                    albumItem!!.albumCover,
                    binding!!.albumSubTitle.getText().toString(),
                    ArrayList<Library.Songs?>()
                )
                sharedPreferenceManager.addLibraryToSavedLibraries(library)
                Snackbar.make(binding!!.getRoot(), "Added to Library", Snackbar.LENGTH_SHORT).show()
            }
            updateAlbumInLibraryStatus()
        })

        binding!!.addMoreSongs.setOnClickListener(View.OnClickListener { view: View? ->
            startActivity(Intent(this@ListActivity, SearchActivity::class.java))
        })

        binding!!.moreIcon.setOnClickListener(View.OnClickListener { view: View? -> onMoreIconClicked() })

        showData()
    }

    override fun onResume() {
        super.onResume()
        if (getIntent().getExtras() != null && getIntent().getExtras()!!
                .getBoolean("createdByUser", false)
        ) {
            onUserCreatedFetch()
        }
    }

    private fun onMoreIconClicked() {
        if (albumItem == null) return

        if (isUserCreated) {
            onMoreIconClickedUserCreated()
            return
        }

        val bottomSheetDialog =
            BottomSheetDialog(this@ListActivity, R.style.MyBottomSheetDialogTheme)
        val _binding = ActivityListMoreInfoBottomSheetBinding.inflate(getLayoutInflater())

        _binding.albumTitle.setText(binding!!.albumTitle.getText().toString())
        _binding.albumSubTitle.setText(binding!!.albumSubTitle.getText().toString())
        Picasso.get().load(Uri.parse(albumItem!!.albumCover)).into(_binding.coverImage)

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.Companion.getInstance(this@ListActivity)
        val savedLibraries: SavedLibraries? = sharedPreferenceManager.savedLibrariesData as? SavedLibraries
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
        _binding.addToLibrary.setOnClickListener(View.OnClickListener { view: View? ->
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        })

        for (artist in artistData) {
            try {
                val imgUrl = artist.image ?: ""
                val bottomSheetItemView =
                    BottomSheetItemView(this@ListActivity, artist.name, imgUrl, artist.id)
                bottomSheetItemView.setFocusable(true)
                bottomSheetItemView.setClickable(true)
                bottomSheetItemView.setOnClickListener(View.OnClickListener { view1: View? ->
                    Log.i("ListActivity", "BottomSheetItemView: onCLicked!")
                    startActivity(
                        Intent(this@ListActivity, ArtistProfileActivity::class.java)
                            .putExtra(
                                "data", Gson().toJson(
                                    BasicDataRecord(artist.id, artist.name, "", imgUrl)
                                )
                            )
                    )
                })
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
        val _binding = UserCreatedListActivityMoreBottomSheetBinding.inflate(getLayoutInflater())

        _binding.albumTitle.setText(binding!!.albumTitle.getText().toString())
        _binding.albumSubTitle.setText(binding!!.albumSubTitle.getText().toString())
        Picasso.get().load(Uri.parse(albumItem!!.albumCover)).into(_binding.coverImage)

        _binding.removeLibrary.setOnClickListener(View.OnClickListener { view: View? ->
            bottomSheetDialog.dismiss()
            binding!!.addToLibrary.performClick()
        })

        bottomSheetDialog.setContentView(_binding.getRoot())
        bottomSheetDialog.create()
        bottomSheetDialog.show()
    }

    private fun updateAlbumInLibraryStatus() {
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.Companion.getInstance(this@ListActivity)
        if (sharedPreferenceManager.savedLibrariesData == null) binding!!.addToLibrary.setImageResource(
            R.drawable.round_add_24
        )
        else {
            val savedLibraries = sharedPreferenceManager.savedLibrariesData as? SavedLibraries
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
        Log.i("ListActivity", "isAlbumInLibrary: " + savedLibraries)
        if (savedLibraries.lists.isNullOrEmpty()) return false
        val lists = savedLibraries.lists ?: emptyList<Library?>()
        return lists.stream()
            .anyMatch { library: Library? -> library?.id == albumItem.id }
    }

    @SuppressLint("NewApi")
    private fun getAlbumIndexInLibrary(albumItem: AlbumItem, savedLibraries: SavedLibraries?): Int {
        if (savedLibraries == null || savedLibraries.lists == null) {
            return -1
        }
        Log.i("ListActivity", "getAlbumIndexInLibrary: " + savedLibraries)
        if (savedLibraries.lists.isNullOrEmpty()) return -1
        var index = -1
        val lists = savedLibraries.lists ?: emptyList<Library?>()
        for (library in lists) {
            if (library?.id == albumItem.id) {
                index = lists.indexOf(library)
                break
            }
        }
        return index
    }

    private fun showShimmerData() {
        val data: MutableList<Song?> = ArrayList<Song?>()
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
        binding!!.recyclerView.setAdapter(ActivityListSongsItemAdapter(data.filterNotNull().toMutableList()))
    }

    private var albumItem: AlbumItem? = null
    private var isAlbum = false

    private fun showData() {
        if (getIntent().getExtras() == null) return
        albumItem = Gson().fromJson<AlbumItem?>(
            getIntent().getExtras()!!.getString("data"),
            AlbumItem::class.java
        )
        updateAlbumInLibraryStatus()
        if (albumItem != null) {
            binding!!.albumTitle.setText(albumItem!!.albumTitle())
            binding!!.albumSubTitle.setText(albumItem!!.albumSubTitle())
            if (albumItem!!.albumCover?.isNotBlank() == true) Picasso.get()
                .load(Uri.parse(albumItem!!.albumCover)).into(binding!!.albumCover)
        }

        val apiManager = ApiManager(this)
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.Companion.getInstance(this)
        val intentId = getIntent().getExtras()!!.getString("id", "")

        if (getIntent().getExtras()!!.getBoolean("createdByUser", false)) {
            onUserCreatedFetch()
            return
        }

        val checkIfUrlData =
            (intentId.startsWith("http") || intentId.startsWith("www")) && intentId.contains("jiosaavn.com")
        if (getIntent().getExtras()!!.getString("type", "") == "album") {
            isAlbum = true
            if (albumItem != null) {
                val cached = sharedPreferenceManager.getAlbumResponseById(albumItem!!.id)
                if (cached != null) {
                    onAlbumFetched(cached)
                    return
                }
            }
            val requestListener: RequestNetwork.RequestListener? =
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val albumSearch =
                            Gson().fromJson<AlbumSearch>(response, AlbumSearch::class.java)
                        Log.i("ListActivity", "onResponse: " + albumSearch)
                        if (albumSearch.success) {
                            val data = albumSearch.data ?: return
                            sharedPreferenceManager.setAlbumResponseById(
                                data.id ?: "",
                                albumSearch
                            )
                            onAlbumFetched(albumSearch)
                        }
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        Log.e("ListActivity", "onErrorResponse: " + message)
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
                apiManager.retrieveAlbumById(albumItem!!.id ?: "", requestListener)
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
                    Log.i("API_RESPONSE", "onResponse: " + response)
                    val playlistSearch =
                        Gson().fromJson<PlaylistSearch>(response, PlaylistSearch::class.java)
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

        binding!!.shareIcon.setVisibility(View.INVISIBLE)
        // binding.moreIcon.setVisibility(View.INVISIBLE);
        binding!!.addToLibrary.setVisibility(View.INVISIBLE)
        binding!!.addMoreSongs.setVisibility(View.VISIBLE)

        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.Companion.getInstance(this)
        val savedLibraries = sharedPreferenceManager.savedLibrariesData as? SavedLibraries
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
            binding!!.albumTitle.setText(library.name)
            binding!!.albumSubTitle.setText(library.description)
            Picasso.get().load(Uri.parse(library.image)).into(binding!!.albumCover)
            
            val songs = library.songs ?: mutableListOf()
            binding!!.recyclerView.setAdapter(UserCreatedSongsListAdapter(songs.filterNotNull().toMutableList()))
            for (song in songs) {
                if (song != null) trackQueue.add(song.id)
            }
        }
    }

    private fun onAlbumFetched(albumSearch: AlbumSearch) {
        val data = albumSearch.data ?: return
        
        binding!!.albumTitle.setText(data.name())
        binding!!.albumSubTitle.setText(data.description())
        val imageList = data.image
        if (!imageList.isNullOrEmpty()) {
            Picasso.get()
                .load(Uri.parse(imageList[imageList.size - 1]?.url ?: ""))
                .into(binding!!.albumCover)
        }
        val songs = data.songs ?: mutableListOf()
        binding!!.recyclerView.setAdapter(ActivityListSongsItemAdapter(songs.filterNotNull().toMutableList()))
        for (song in songs) {
            if (song != null) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener { view: View? ->
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.setAction(Intent.ACTION_SEND)
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.setType("text/plain")
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
        
        binding!!.albumTitle.setText(data.name())
        binding!!.albumSubTitle.setText(data.description())
        val imageList = data.image
        if (!imageList.isNullOrEmpty()) {
            Picasso.get()
                .load(Uri.parse(imageList[imageList.size - 1]?.url ?: ""))
                .into(binding!!.albumCover)
        }
        val songs = data.songs ?: mutableListOf()
        binding!!.recyclerView.setAdapter(ActivityListSongsItemAdapter(songs.filterNotNull().toMutableList()))
        for (song in songs) {
            if (song != null) trackQueue.add(song.id)
        }

        // ((ApplicationClass)getApplicationContext()).setTrackQueue(trackQueue);
        binding!!.shareIcon.setOnClickListener(View.OnClickListener { view: View? ->
            if (data.url.isNullOrBlank()) return@OnClickListener
            val sendIntent = Intent()
            sendIntent.setAction(Intent.ACTION_SEND)
            sendIntent.putExtra(Intent.EXTRA_TEXT, data.url)
            sendIntent.setType("text/plain")
            startActivity(sendIntent)
        })

        for (artist in data.artists ?: emptyList()) {
            if (artist == null) continue
            val aImgList = artist.image
            artistData.add(
                ArtistData(
                    artist.name(), artist.id,
                    if (!aImgList.isNullOrEmpty())
                        aImgList[aImgList.size - 1]?.url ?: "https://i.pinimg.com/564x/1d/04/a8/1d04a87b8e6cf2c3829c7af2eccf6813.jpg"
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

    companion object {
        private const val TAG = "ListActivity"
    }
}
