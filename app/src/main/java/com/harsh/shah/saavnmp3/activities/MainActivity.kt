package com.harsh.shah.saavnmp3.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.adapters.ActivityMainAlbumItemAdapter
import com.harsh.shah.saavnmp3.adapters.ActivityMainArtistsItemAdapter
import com.harsh.shah.saavnmp3.adapters.ActivityMainPlaylistAdapter
import com.harsh.shah.saavnmp3.adapters.ActivityMainPopularSongs
import com.harsh.shah.saavnmp3.adapters.SavedLibrariesAdapter
import com.harsh.shah.saavnmp3.databinding.ActivityMainBinding
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.NetworkChangeReceiver
import com.harsh.shah.saavnmp3.network.NetworkChangeReceiver.NetworkStatusListener
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.AlbumsSearch
import com.harsh.shah.saavnmp3.records.ArtistsSearch
import com.harsh.shah.saavnmp3.records.PlaylistsSearch
import com.harsh.shah.saavnmp3.records.SongSearch
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.squareup.picasso.Picasso
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import androidx.core.net.toUri
import com.harsh.shah.saavnmp3.utils.MiniPlayerHelper
import com.harsh.shah.saavnmp3.utils.UpdateUtil
import com.harsh.shah.saavnmp3.utils.attachSnapHelper

class MainActivity : AppCompatActivity() {
    private var requestStoragePermission: ActivityResultLauncher<Array<String>>? = null
    private val TAG = "MainActivity"
    private var binding: ActivityMainBinding? = null
    private var baseApplicationClass: BaseApplicationClass? = null
    val songs: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val artists: MutableList<ArtistsSearch.Data.Results?> = ArrayList<ArtistsSearch.Data.Results?>()
    val albums: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
    val playlists: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()

    var networkChangeReceiver: NetworkChangeReceiver =
        NetworkChangeReceiver(object : NetworkStatusListener {
            override fun onNetworkConnected() {
                if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showData()
            }

            override fun onNetworkDisconnected() {
                if (songs.isEmpty() || artists.isEmpty() || albums.isEmpty() || playlists.isEmpty()) showOfflineData()
                Snackbar.make(binding!!.getRoot(), "No Internet Connection", Snackbar.LENGTH_LONG)
                    .show()
            }
        })

    private var slidingRootNavBuilder: SlidingRootNav? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        baseApplicationClass = applicationContext as BaseApplicationClass?
        BaseApplicationClass.currentActivity = this
        BaseApplicationClass.updateTheme()

        slidingRootNavBuilder = SlidingRootNavBuilder(this)
            .withMenuLayout(R.layout.main_drawer_layout)
            .withContentClickableWhenMenuOpened(false)
            .withDragDistance(250)
            .inject()

        // Set version text in the drawer layout
        updateVersionTextInDrawer()

        onDrawerItemsClicked()

        binding!!.profileIcon.setOnClickListener {
            slidingRootNavBuilder!!.openMenu(
                true
            )
        }

        val span: Int = calculateNoOfColumns(this, 200f)
        binding!!.playlistRecyclerView.setLayoutManager(GridLayoutManager(this, span))

        binding!!.popularSongsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding!!.popularArtistsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding!!.popularAlbumsRecyclerView.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )
        binding!!.savedRecyclerView.setLayoutManager(
            LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        )

        // Snap helpers — smooth magnetic scroll on all horizontal lists
        binding!!.popularSongsRecyclerView.attachSnapHelper()
        binding!!.popularArtistsRecyclerView.attachSnapHelper()
        binding!!.popularAlbumsRecyclerView.attachSnapHelper()
        binding!!.savedRecyclerView.attachSnapHelper()

        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.popularSongsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.popularArtistsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.popularAlbumsRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )
        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.savedRecyclerView,
            OverScrollDecoratorHelper.ORIENTATION_HORIZONTAL
        )

        binding!!.refreshLayout.setOnRefreshListener {
            showShimmerData()
            showData()
            binding!!.refreshLayout.isRefreshing = false
        }

        MiniPlayerHelper.initMiniPlayer(this)

        showShimmerData()
        //showOfflineData()
        showData()

        showSavedLibrariesData()

        askNotificationPermission()

        requestStoragePermission =
            registerForActivityResult(RequestMultiplePermissions()) { result ->
                if (result.containsValue(false)) {
                    Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
                }
            }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            requestStoragePermission()
        }

        UpdateUtil.checkForUpdates(this)
    }

    private fun requestStoragePermission() {
        if (!checkIfStorageAccessAvailable()) {
            requestStoragePermission!!.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun checkIfStorageAccessAvailable(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            return true
        } else {
            return (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
                    && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    private fun showSavedLibrariesData() {
        val savedLibraries =
            SharedPreferenceManager.getInstance(this).savedLibrariesData
        binding!!.savedLibrariesSection.visibility =
            if (savedLibraries != null && !(savedLibraries.lists?.isEmpty()
                    ?: true)
            ) View.VISIBLE else View.GONE
        if (savedLibraries != null) binding!!.savedRecyclerView.setAdapter(
            SavedLibrariesAdapter(
                savedLibraries.lists ?: mutableListOf()
            )
        )
    }

    private fun onDrawerItemsClicked() {
        slidingRootNavBuilder!!.layout.findViewById<View>(R.id.settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            slidingRootNavBuilder!!.closeMenu()
        }

        slidingRootNavBuilder!!.layout.findViewById<View>(R.id.logo)
            .setOnClickListener { slidingRootNavBuilder!!.closeMenu() }

        slidingRootNavBuilder!!.layout.findViewById<View>(R.id.library).setOnClickListener {
            startActivity(Intent(this@MainActivity, SavedLibrariesActivity::class.java))
            slidingRootNavBuilder!!.closeMenu()
        }

        slidingRootNavBuilder!!.layout.findViewById<View>(R.id.about)
            .setOnClickListener {
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                slidingRootNavBuilder!!.closeMenu()
            }

        slidingRootNavBuilder!!.layout.findViewById<View>(R.id.download_manager)
            .setOnClickListener {
                startActivity(Intent(this@MainActivity, DownloadManagerActivity::class.java))
                slidingRootNavBuilder!!.closeMenu()
            }
    }

    /**
     * Updates the version text in the navigation drawer with the app's current
     * version
     */
    private fun updateVersionTextInDrawer() {
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            val drawerLayout: View? = slidingRootNavBuilder!!.layout
            if (drawerLayout != null) {
                val versionTextView = drawerLayout.findViewById<View?>(R.id.versionTxt)
                if (versionTextView is TextView) {
                    versionTextView.text = "version $versionName"
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting app version: " + e.message)
        }
    }

    override fun onResume() {
        super.onResume()
        NetworkChangeReceiver.registerReceiver(this, networkChangeReceiver)
        showSavedLibrariesData()
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        NetworkChangeReceiver.unregisterReceiver(this, networkChangeReceiver)
        MiniPlayerHelper.onActivityPause(this)
    }

    override fun onBackPressed() {
        if (slidingRootNavBuilder!!.isMenuOpened) slidingRootNavBuilder!!.closeMenu()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        MusicPlayerManager.cancelNotification()
        super.onDestroy()
    }

    private fun showData() {
        songs.clear()
        artists.clear()
        albums.clear()
        playlists.clear()

        val songSeeds = listOf("2023","2024","2025","2026","Hits", "Latest", "Romantic", "Chill", "Lo-Fi", "Dance", "Hindi", "Sad", "Love", "Workout", " ")
        val artistSeeds = listOf("2023","2024","2025","2026", " ")
        val albumSeeds = listOf("2023","2024","2025","2026","Hits", "Latest", "New", "Sad", "Lo-Fi", "Romantic", "Rock", "Pop", "Classic", "Party", " ")
        val playlistSeeds = listOf("2023","2024","2025","2026","Hits", "Latest", "Trending", "Party", "Devotional", "Chill", "Love", "Sad", "Top", "Classic", " ")
        
        val songQuery = songSeeds.random()
        val artistQuery = artistSeeds.random()
        val albumQuery = albumSeeds.random()
        val playlistQuery = playlistSeeds.random()

        val apiManager = ApiManager(this)

        apiManager.searchSongs(songQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val songSearch = Gson().fromJson(response, SongSearch::class.java)
                Log.i(TAG, "onResponse: $response")
                if (songSearch.success) {
                    val resultsList = songSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        if (results == null) continue
                        val imageList = results.image
                        val imageUrl =
                            if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url
                                ?: "" else ""
                        songs.add(
                            AlbumItem(
                                results.name(), results.language + " " + results.year,
                                imageUrl, results.id
                            )
                        )
                    }
                    val adapter = ActivityMainPopularSongs(songs)
                    binding!!.popularSongsRecyclerView.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    BaseApplicationClass.sharedPreferenceManager?.homeSongsRecommended = songSearch
                } else {
                    try {
                        showOfflineData()
                        Toast.makeText(
                            this@MainActivity, JSONObject(response.toString()).getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: JSONException) {
                        Log.e(TAG, "onResponse: ", e)
                    }
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchArtists(artistQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val artistSearch =
                    Gson().fromJson(response, ArtistsSearch::class.java)
                Log.i(TAG, "onResponse: $response")
                if (artistSearch.success) {
                    val resultsList = artistSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        artists.add(results)
                    }
                    val adapter = ActivityMainArtistsItemAdapter(artists)
                    binding!!.popularArtistsRecyclerView.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    BaseApplicationClass.sharedPreferenceManager?.homeArtistsRecommended =
                        artistSearch
                } else {
                    try {
                        showOfflineData()
                        Toast.makeText(
                            this@MainActivity, JSONObject(response.toString()).getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: JSONException) {
                        Log.e(TAG, "onResponse: ", e)
                    }
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchAlbums(albumQuery, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val albumsSearch = Gson().fromJson(response, AlbumsSearch::class.java)
                Log.i(TAG, "onResponse: $response")
                if (albumsSearch.success) {
                    val resultsList = albumsSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        if (results == null) continue
                        val imageList = results.image
                        val imageUrl =
                            if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url
                                ?: "" else ""
                        albums.add(
                            AlbumItem(
                                results.name(), results.language + " " + results.year,
                                imageUrl, results.id
                            )
                        )
                    }
                    val adapter = ActivityMainAlbumItemAdapter(albums)
                    binding!!.popularAlbumsRecyclerView.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    BaseApplicationClass.sharedPreferenceManager?.homeAlbumsRecommended =
                        albumsSearch
                } else {
                    try {
                        Toast.makeText(
                            this@MainActivity, JSONObject(response.toString()).getString("message"),
                            Toast.LENGTH_SHORT
                        ).show()
                        showOfflineData()
                    } catch (e: JSONException) {
                        Log.e(TAG, "onResponse: ", e)
                    }
                }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                showOfflineData()
            }
        })

        apiManager.searchPlaylists(
            playlistQuery, null, null,
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val playlistsSearch =
                        Gson().fromJson(response, PlaylistsSearch::class.java)
                    Log.i(TAG, "onResponse: $response")
                    if (playlistsSearch.success) {
                        val resultsList = playlistsSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            val imageList = results.image
                            val imageUrl =
                                if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url
                                    ?: "" else ""
                            playlists.add(
                                AlbumItem(
                                    results.name(), "",
                                    imageUrl, results.id
                                )
                            )
                        }
                        val adapter = ActivityMainPlaylistAdapter(playlists)
                        binding!!.playlistRecyclerView.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                        BaseApplicationClass.sharedPreferenceManager?.homePlaylistRecommended =
                            playlistsSearch
                    } else {
                        try {
                            Toast.makeText(
                                this@MainActivity,
                                JSONObject(response.toString()).getString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                            showOfflineData()
                        } catch (e: JSONException) {
                            Log.e(TAG, "onResponse: ", e)
                        }
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    showOfflineData()
                }
            })
    }

    private fun showShimmerData() {
        val dataShimmer: MutableList<AlbumItem?> = ArrayList()
        val artistsShimmer: MutableList<ArtistsSearch.Data.Results?> =
            ArrayList()
        for (i in 0..10) {
            dataShimmer.add(AlbumItem("<shimmer>", "<shimmer>", "<shimmer>", "<shimmer>"))
            artistsShimmer.add(
                ArtistsSearch.Data.Results(
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    "<shimmer>",
                    null
                )
            )
        }
        binding!!.popularSongsRecyclerView.setAdapter(ActivityMainAlbumItemAdapter(dataShimmer))
        binding!!.popularAlbumsRecyclerView.setAdapter(ActivityMainAlbumItemAdapter(dataShimmer))
        binding!!.popularArtistsRecyclerView.setAdapter(
            ActivityMainArtistsItemAdapter(
                artistsShimmer
            )
        )
        binding!!.playlistRecyclerView.setAdapter(ActivityMainPlaylistAdapter(dataShimmer))
    }

    private fun showOfflineData() {
        val prefManager = BaseApplicationClass.sharedPreferenceManager ?: return
        val songSearch: SongSearch? = prefManager.homeSongsRecommended
        if (songSearch != null) {
            val resultsList = songSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                songs.add(
                    AlbumItem(
                        results.name(), results.language + " " + results.year,
                        imageUrl, results.id
                    )
                )
            }
            val adapter = ActivityMainPopularSongs(songs)
            binding!!.popularSongsRecyclerView.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }

        val artistsSearch: ArtistsSearch? = prefManager.homeArtistsRecommended
        if (artistsSearch != null) {
            val resultsList = artistsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                artists.add(results)
            }
            val adapter = ActivityMainArtistsItemAdapter(artists)
            binding!!.popularArtistsRecyclerView.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }

        val albumsSearch: AlbumsSearch? = prefManager.homeAlbumsRecommended
        if (albumsSearch != null) {
            val resultsList = albumsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                albums.add(
                    AlbumItem(
                        results.name(), results.language + " " + results.year,
                        imageUrl, results.id
                    )
                )
            }
            val adapter = ActivityMainAlbumItemAdapter(albums)
            binding!!.popularAlbumsRecyclerView.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }

        val playlistsSearch: PlaylistsSearch? = prefManager.homePlaylistRecommended
        if (playlistsSearch != null) {
            val resultsList = playlistsSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl =
                    if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
                playlists.add(
                    AlbumItem(
                        results.name(), "", imageUrl,
                        results.id
                    )
                )
            }
            val adapter = ActivityMainPlaylistAdapter(playlists)
            binding!!.playlistRecyclerView.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }

        // showData(); //TODO: showData if new data is available
    }

    fun openSearch(view: View?) {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { _: Boolean? -> }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    companion object {
        fun calculateNoOfColumns(
            context: Context,
            columnWidthDp: Float
        ): Int { // For example columnWidthDp=180
            val displayMetrics = context.resources.displayMetrics
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return (screenWidthDp / columnWidthDp + 0.5).toInt() // +0.5 for correct rounding to int.
        }
    }
}
