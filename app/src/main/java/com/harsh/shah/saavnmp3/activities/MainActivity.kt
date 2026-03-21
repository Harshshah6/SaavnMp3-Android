package com.harsh.shah.saavnmp3.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
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
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.records.SongSearch
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries
import com.harsh.shah.saavnmp3.utils.NetworkUtil
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.squareup.picasso.Picasso
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.json.JSONException
import org.json.JSONObject
import java.util.Calendar
import java.util.function.Consumer

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
        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        baseApplicationClass = getApplicationContext() as BaseApplicationClass?
        BaseApplicationClass.Companion.currentActivity = this
        BaseApplicationClass.Companion.updateTheme()

        slidingRootNavBuilder = SlidingRootNavBuilder(this)
            .withMenuLayout(R.layout.main_drawer_layout)
            .withContentClickableWhenMenuOpened(false)
            .withDragDistance(250)
            .inject()

        // Set version text in the drawer layout
        updateVersionTextInDrawer()

        onDrawerItemsClicked()

        binding!!.profileIcon.setOnClickListener(View.OnClickListener { view: View? ->
            slidingRootNavBuilder!!.openMenu(
                true
            )
        })

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

        binding!!.refreshLayout.setOnRefreshListener(OnRefreshListener {
            showShimmerData()
            showData()
            binding!!.refreshLayout.setRefreshing(false)
        })

        binding!!.playBarPlayPauseIcon.setOnClickListener(View.OnClickListener { view: View? ->
            val baseApplicationClass = getApplicationContext() as BaseApplicationClass
            baseApplicationClass.togglePlayPause()
            // Allow state to settle before updating UI
            Handler().postDelayed(Runnable {
                val p = BaseApplicationClass.player
                if (p != null) {
                    binding!!.playBarPlayPauseIcon
                        .setImageResource(
                            if (p.isPlaying)
                                R.drawable.baseline_pause_24
                            else
                                R.drawable.play_arrow_24px
                        )
                }
            }, 100)
        })

        binding!!.playBarBackground.setOnClickListener(View.OnClickListener { view: View? ->
            if (!BaseApplicationClass.MUSIC_ID.isNullOrBlank()) startActivity(
                Intent(
                    this,
                    MusicOverviewActivity::class.java
                ).putExtra("id", BaseApplicationClass.Companion.MUSIC_ID)
            )
        })

        binding!!.playBarPrevIcon.setOnClickListener(View.OnClickListener { view: View? ->
            try {
                Log.i(TAG, "Play bar previous button clicked")
                if (baseApplicationClass == null) {
                    Log.e(TAG, "Application class is null")
                    return@OnClickListener
                }

                // Add visual feedback
                binding!!.playBarPrevIcon.setAlpha(0.5f)
                binding!!.playBarPrevIcon.animate().alpha(1.0f).setDuration(200).start()

                // Call the previous track method
                baseApplicationClass!!.prevTrack()

                // UI update should happen via onResume/timer or observer,
                // but for now we rely on the loop in showPlayBarData/onResume
                updatePlaybarUi()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling previous button click", e)
            }
        })

        binding!!.playBarNextIcon.setOnClickListener(View.OnClickListener { view: View? ->
            try {
                Log.i(TAG, "Play bar next button clicked")
                if (baseApplicationClass == null) {
                    Log.e(TAG, "Application class is null")
                    return@OnClickListener
                }

                // Add visual feedback
                binding!!.playBarNextIcon.setAlpha(0.5f)
                binding!!.playBarNextIcon.animate().alpha(1.0f).setDuration(200).start()

                // Call the next track method
                baseApplicationClass!!.nextTrack()

                updatePlaybarUi()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling next button click", e)
            }
        })

        showShimmerData()
        showOfflineData()

        // showData();
        showPlayBarData()

        showSavedLibrariesData()

        askNotificationPermission()

        requestStoragePermission = registerForActivityResult(RequestMultiplePermissions()) { result ->
            if (result.containsValue(false)) {
                Toast.makeText(this, "Storage Permission Denied", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show()
            }
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (!checkIfStorageAccessAvailable()) {
            requestStoragePermission!!.launch(
                arrayOf<String>(
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
            SharedPreferenceManager.Companion.getInstance(this).savedLibrariesData as? SavedLibraries
        binding!!.savedLibrariesSection.setVisibility(
            if (savedLibraries != null && !(savedLibraries.lists?.isEmpty() ?: true)) View.VISIBLE else View.GONE
        )
        if (savedLibraries != null) binding!!.savedRecyclerView.setAdapter(
            SavedLibrariesAdapter(
                savedLibraries.lists ?: mutableListOf()
            )
        )
    }

    private fun onDrawerItemsClicked() {
        slidingRootNavBuilder!!.getLayout().findViewById<View?>(R.id.settings).setOnClickListener(
            View.OnClickListener { v: View? ->
                startActivity(Intent(this, SettingsActivity::class.java))
                slidingRootNavBuilder!!.closeMenu()
            })

        slidingRootNavBuilder!!.getLayout().findViewById<View?>(R.id.logo)
            .setOnClickListener(View.OnClickListener { view: View? -> slidingRootNavBuilder!!.closeMenu() })

        slidingRootNavBuilder!!.getLayout().findViewById<View?>(R.id.library).setOnClickListener(
            View.OnClickListener { view: View? ->
                startActivity(Intent(this@MainActivity, SavedLibrariesActivity::class.java))
                slidingRootNavBuilder!!.closeMenu()
            })

        slidingRootNavBuilder!!.getLayout().findViewById<View?>(R.id.about)
            .setOnClickListener(View.OnClickListener { view: View? ->
                startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                slidingRootNavBuilder!!.closeMenu()
            })

        slidingRootNavBuilder!!.getLayout().findViewById<View?>(R.id.download_manager)
            .setOnClickListener(
                View.OnClickListener { v: View? ->
                    startActivity(Intent(this@MainActivity, DownloadManagerActivity::class.java))
                    slidingRootNavBuilder!!.closeMenu()
                })
    }

    /**
     * Updates the version text in the navigation drawer with the app's current
     * version
     */
    private fun updateVersionTextInDrawer() {
        try {
            val versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName
            val drawerLayout: View? = slidingRootNavBuilder!!.getLayout()
            if (drawerLayout != null) {
                val versionTextView = drawerLayout.findViewById<View?>(R.id.versionTxt)
                if (versionTextView is TextView) {
                    versionTextView.setText("version " + versionName)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Error getting app version: " + e.message)
        }
    }

    var handler: Handler = Handler()
    var runnable: Runnable = Runnable { this.showPlayBarData() }

    fun showPlayBarData() {
        if (BaseApplicationClass.MUSIC_ID.isNullOrBlank()) binding!!.playBarBackground.setVisibility(
            View.GONE
        )
        else binding!!.playBarBackground.setVisibility(View.VISIBLE)

        binding!!.playBarMusicTitle.setText(BaseApplicationClass.Companion.MUSIC_TITLE)
        binding!!.playBarMusicDesc.setText(BaseApplicationClass.Companion.MUSIC_DESCRIPTION)
        Picasso.get().load(Uri.parse(BaseApplicationClass.Companion.IMAGE_URL))
            .into(binding!!.playBarCoverImage)
        updatePlaybarUi()

        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(BaseApplicationClass.Companion.IMAGE_BG_COLOR)
        gradientDrawable.setCornerRadius(18f)

        binding!!.playBarBackground.setBackground(gradientDrawable)

        binding!!.playBarMusicTitle.setTextColor(BaseApplicationClass.Companion.TEXT_ON_IMAGE_COLOR1)
        binding!!.playBarMusicDesc.setTextColor(BaseApplicationClass.Companion.TEXT_ON_IMAGE_COLOR1)

        binding!!.playBarPlayPauseIcon.setImageTintList(ColorStateList.valueOf(BaseApplicationClass.Companion.TEXT_ON_IMAGE_COLOR))
        binding!!.playBarPrevIcon.setImageTintList(ColorStateList.valueOf(BaseApplicationClass.Companion.TEXT_ON_IMAGE_COLOR))
        binding!!.playBarNextIcon.setImageTintList(ColorStateList.valueOf(BaseApplicationClass.Companion.TEXT_ON_IMAGE_COLOR))

        OverScrollDecoratorHelper.setUpStaticOverScroll(
            binding!!.getRoot(),
            OverScrollDecoratorHelper.ORIENTATION_VERTICAL
        )

        handler.postDelayed(runnable, 1000)
    }

    private fun updatePlaybarUi() {
        val p = BaseApplicationClass.player
        if (p != null) {
            binding!!.playBarPlayPauseIcon.setImageResource(
                if (p.isPlaying)
                    R.drawable.baseline_pause_24
                else
                    R.drawable.play_arrow_24px
            )
        }
    }

    override fun onResume() {
        super.onResume()
        NetworkChangeReceiver.Companion.registerReceiver(this, networkChangeReceiver)
        showSavedLibrariesData()
    }

    override fun onPause() {
        super.onPause()
        NetworkChangeReceiver.Companion.unregisterReceiver(this, networkChangeReceiver)
    }

    override fun onBackPressed() {
        if (slidingRootNavBuilder!!.isMenuOpened()) slidingRootNavBuilder!!.closeMenu()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        BaseApplicationClass.Companion.cancelNotification()
        super.onDestroy()
    }

    private fun showData() {
        songs.clear()
        artists.clear()
        albums.clear()
        playlists.clear()

        val apiManager = ApiManager(this)

        apiManager.searchSongs(" ", 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val songSearch = Gson().fromJson(response, SongSearch::class.java)
                Log.i(TAG, "onResponse: " + response)
                if (songSearch.success) {
                    val resultsList = songSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        if (results == null) continue
                        val imageList = results.image
                        val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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
                            this@MainActivity, JSONObject(response).getString("message"),
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

        apiManager.searchArtists(" ", 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val artistSearch =
                    Gson().fromJson(response, ArtistsSearch::class.java)
                Log.i(TAG, "onResponse: " + response)
                if (artistSearch.success) {
                    val resultsList = artistSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        artists.add(results)
                    }
                    val adapter = ActivityMainArtistsItemAdapter(artists)
                    binding!!.popularArtistsRecyclerView.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                    BaseApplicationClass.sharedPreferenceManager?.homeArtistsRecommended = artistSearch
                } else {
                    try {
                        showOfflineData()
                        Toast.makeText(
                            this@MainActivity, JSONObject(response).getString("message"),
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

        apiManager.searchAlbums(" ", 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                val albumsSearch = Gson().fromJson(response, AlbumsSearch::class.java)
                Log.i(TAG, "onResponse: " + response)
                if (albumsSearch.success) {
                    val resultsList = albumsSearch.data?.results ?: emptyList()
                    for (results in resultsList) {
                        if (results == null) continue
                        val imageList = results.image
                        val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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
                    BaseApplicationClass.sharedPreferenceManager?.homeAlbumsRecommended = albumsSearch
                } else {
                    try {
                        Toast.makeText(
                            this@MainActivity, JSONObject(response).getString("message"),
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

        // String.valueOf(Calendar.getInstance().get(Calendar.YEAR))
        apiManager.searchPlaylists(
            Calendar.getInstance().get(Calendar.YEAR).toString(), null, null,
            object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val playlistsSearch =
                        Gson().fromJson(response, PlaylistsSearch::class.java)
                    Log.i(TAG, "onResponse: " + response)
                    if (playlistsSearch.success) {
                        val resultsList = playlistsSearch.data?.results ?: emptyList()
                        for (results in resultsList) {
                            if (results == null) continue
                            val imageList = results.image
                            val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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
                        BaseApplicationClass.sharedPreferenceManager?.homePlaylistRecommended = playlistsSearch
                    } else {
                        try {
                            Toast.makeText(
                                this@MainActivity, JSONObject(response).getString("message"),
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
        val data_shimmer: MutableList<AlbumItem?> = ArrayList<AlbumItem?>()
        val artists_shimmer: MutableList<ArtistsSearch.Data.Results?> =
            ArrayList<ArtistsSearch.Data.Results?>()
        for (i in 0..10) {
            data_shimmer.add(AlbumItem("<shimmer>", "<shimmer>", "<shimmer>", "<shimmer>"))
            artists_shimmer.add(
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
        binding!!.popularSongsRecyclerView.setAdapter(ActivityMainAlbumItemAdapter(data_shimmer))
        binding!!.popularAlbumsRecyclerView.setAdapter(ActivityMainAlbumItemAdapter(data_shimmer))
        binding!!.popularArtistsRecyclerView.setAdapter(
            ActivityMainArtistsItemAdapter(
                artists_shimmer
            )
        )
        binding!!.playlistRecyclerView.setAdapter(ActivityMainPlaylistAdapter(data_shimmer))
    }

    fun tryConnect() {
        if (!NetworkUtil.isNetworkAvailable(this@MainActivity)) {
            try {
                Thread.sleep(2000)
                // showData();
            } catch (e: Exception) {
                Log.e(TAG, "onErrorResponse: ", e)
            }
        }
    }

    private fun showOfflineData() {
        val prefManager = BaseApplicationClass.sharedPreferenceManager ?: return
        val songSearch: SongSearch? = prefManager.homeSongsRecommended
        if (songSearch != null) {
            val resultsList = songSearch.data?.results ?: emptyList()
            for (results in resultsList) {
                if (results == null) continue
                val imageList = results.image
                val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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
                val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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
                val imageUrl = if (!imageList.isNullOrEmpty()) imageList[imageList.size - 1]?.url ?: "" else ""
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

    private fun playBarPopUpAnimation() {
        showPopup()
    }

    private fun showPopup() {
        // Set the popup to visible
        binding!!.playBarBackground.setVisibility(View.VISIBLE)

        // Create an animation to make the popup appear
        val slideUp = TranslateAnimation(0f, 0f, 1000f, 0f) // Slide from bottom
        slideUp.setDuration(500)
        slideUp.setFillAfter(true) // Keeps the position after animation

        // You can add fade-in effect as well
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.setDuration(500)

        // Combine the animations
        slideUp.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding!!.playBarBackground.startAnimation(fadeIn) // Start fade-in when slide-up starts
            }

            override fun onAnimationEnd(animation: Animation?) {
                // You can add any logic after the animation ends
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // Not needed here
            }
        })

        // Start the slide-up animation
        binding!!.playBarBackground.startAnimation(slideUp)
    }

    // Method to close the popup (can be triggered by a button)
    fun closePopup() {
        // Fade-out animation
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.setDuration(500)
        fadeOut.setFillAfter(true) // Ensures it stays hidden after the animation

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                // You can add any logic before the animation starts
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding!!.playBarBackground.setVisibility(View.GONE) // Hide after animation ends
            }

            override fun onAnimationRepeat(animation: Animation?) {
                // Not needed here
            }
        })

        binding!!.playBarBackground.startAnimation(fadeOut) // Start fade-out animation
    }

    fun openSearch(view: View?) {
        startActivity(Intent(this, SearchActivity::class.java))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        RequestPermission()
    ) { isGranted: Boolean? -> }

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
            val displayMetrics = context.getResources().getDisplayMetrics()
            val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
            return (screenWidthDp / columnWidthDp + 0.5).toInt() // +0.5 for correct rounding to int.
        }
    }
}
