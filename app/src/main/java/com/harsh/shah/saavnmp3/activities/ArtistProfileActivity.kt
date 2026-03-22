package com.harsh.shah.saavnmp3.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.adapters.ActivityArtistProfileTopAlbumsAdapter
import com.harsh.shah.saavnmp3.adapters.ActivityArtistProfileTopSongsAdapter
import com.harsh.shah.saavnmp3.adapters.ActivitySeeMoreListAdapter
import com.harsh.shah.saavnmp3.databinding.ActivityArtistProfileBinding
import com.harsh.shah.saavnmp3.model.BasicDataRecord
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.AlbumsSearch
import com.harsh.shah.saavnmp3.records.ArtistSearch
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.SongResponse.Album
import com.harsh.shah.saavnmp3.records.SongResponse.DownloadUrl
import com.harsh.shah.saavnmp3.records.SongResponse.Lyrics
import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.squareup.picasso.Picasso

/**
 * The `ArtistProfileActivity` class displays the profile information of an artist,
 * including their name, image, top songs, top albums, and singles.
 * It fetches the artist's data from a remote API and handles network connectivity changes.
 * 
 * 
 * 
 * This activity uses a collapsing toolbar layout to provide a visually appealing
 * header that expands and collapses as the user scrolls.
 * It also utilizes Shimmer effect as placeholder while data is loading.
 * 
 */
class ArtistProfileActivity : AppCompatActivity() {
    private val TAG = "ArtistProfileActivity"
    var binding: ActivityArtistProfileBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtistProfileBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        setSupportActionBar(binding!!.collapsingToolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayShowTitleEnabled(false)
        binding!!.collapsingToolbarLayout.setExpandedTitleColor(getResources().getColor(android.R.color.transparent))

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        //        binding.collapsingToolbarLayout.setTitle("Artist Name");
        binding!!.collapsingToolbarAppbarlayout.addOnOffsetChangedListener(OnOffsetChangedListener { appBarLayout: AppBarLayout?, verticalOffset: Int ->
            if (verticalOffset == 0) {
            } else {
            }
        })

        binding!!.topSongsRecyclerview.setLayoutManager(LinearLayoutManager(this))
        binding!!.topAlbumsRecyclerview.setLayoutManager(LinearLayoutManager(this))
        binding!!.topSinglesRecyclerview.setLayoutManager(LinearLayoutManager(this))

        binding!!.topSongsSeeMore.setOnClickListener { v: View? ->
            startActivity(
                Intent(this@ArtistProfileActivity, SeeMoreActivity::class.java)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_SONGS.name)
                    .putExtra("artist_name", binding!!.artistName.text.toString())
            )
        }
        binding!!.topAlbumsSeeMore.setOnClickListener { v: View? ->
            startActivity(
                Intent(this@ArtistProfileActivity, SeeMoreActivity::class.java)
                    .putExtra("id", artistId)
                    .putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_ALBUMS.name)
                    .putExtra("artist_name", binding!!.artistName.text.toString())
            )
        }
        binding!!.topSinglesSeeMore.visibility = View.GONE

        //binding.topSinglesSeeMore.setOnClickListener(v -> startActivity(seeMoreIntent.putExtra("type", ActivitySeeMoreListAdapter.Mode.TOP_SINGLES.name())));
        showShimmerData()
        showData()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    private var artistId = "9999"
    private var artistSearch: ArtistSearch? = null

    fun showData() {
        if (intent.extras == null) return
        Log.i(TAG, "showData: " + intent.extras)
        val artist = intent.extras!!.getString("data", "null")
        val apiManager = ApiManager(this)
        val responseListener: RequestNetwork.RequestListener =
            object : RequestNetwork.RequestListener {
                val sharedPreferenceManager: SharedPreferenceManager =
                    SharedPreferenceManager.getInstance(this@ArtistProfileActivity)

                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    try {
                        artistSearch =
                            Gson().fromJson<ArtistSearch>(response, ArtistSearch::class.java)
                        Log.i(TAG, "onResponse: $response")
                        val id = artistSearch?.data?.id
                        if (id != null) {
                            sharedPreferenceManager.setArtistData(id, artistSearch)
                            Log.i(
                                TAG,
                                "onResponse: " + sharedPreferenceManager.getArtistData(id)
                            )
                        }
                        display()
                    } catch (e: Exception) {
                        Log.e(TAG, "onResponse: ", e)
                        finish()
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    Log.i(TAG, "onErrorResponse: $message")
                    if (artistId != "9999") {
                        val offlineData = sharedPreferenceManager.getArtistData(artistId)
                        if (offlineData != null) {
                            artistSearch = offlineData
                            display()
                        }
                    }
                }
            }

        if ((artist.startsWith("http") || artist.startsWith("www")) && artist.contains("jiosaavn.com")) {
            apiManager.retrieveArtistsByLink(artist, null, null, null, null, null, responseListener)
            return
        }

        val artistItem = Gson().fromJson(artist, BasicDataRecord::class.java) ?: return
        artistId = artistItem.id ?: ""

        Picasso.get().load(artistItem.image?.toUri()).into(binding!!.artistImg)
        binding!!.artistName.text = artistItem.title()
        binding!!.collapsingToolbarLayout.title = artistItem.title()

        apiManager.retrieveArtistById(artistId, responseListener)
    }

    private fun display() {
        Log.i(TAG, "display: $artistSearch")
        if (artistSearch?.success == true && artistSearch?.data != null) {
            val data = artistSearch!!.data!!
            if (!data.image.isNullOrEmpty()) {
                Picasso.get()
                    .load((data.image[data.image.size - 1]?.url ?: "").toUri())
                    .into(binding!!.artistImg)
            }
            binding!!.artistName.text = data.name()
            binding!!.collapsingToolbarLayout.title = data.name()
            binding!!.topSongsRecyclerview.setAdapter(
                ActivityArtistProfileTopSongsAdapter(
                    data.topSongs ?: mutableListOf()
                )
            )
            binding!!.topAlbumsRecyclerview.setAdapter(
                ActivityArtistProfileTopAlbumsAdapter(
                    data.topAlbums ?: mutableListOf()
                )
            )
            binding!!.topSinglesRecyclerview.setAdapter(
                ActivityArtistProfileTopAlbumsAdapter(
                    data.singles ?: mutableListOf()
                )
            )
        }
    }

    fun showShimmerData() {
        val shimmerData: MutableList<Song?> = shimmerData
        val shimmerDataAlbum: MutableList<AlbumsSearch.Data.Results?> = ArrayList()
        for (i in 0..10) {
            shimmerDataAlbum.add(
                AlbumsSearch.Data.Results(
                    "<shimmer>",
                    null,
                    null,
                    null,
                    0,
                    null,
                    0,
                    null,
                    false,
                    null,
                    null
                )
            )
        }

        binding!!.topSongsRecyclerview.setAdapter(ActivityArtistProfileTopSongsAdapter(shimmerData))
        binding!!.topAlbumsRecyclerview.setAdapter(
            ActivityArtistProfileTopAlbumsAdapter(
                shimmerDataAlbum
            )
        )
        binding!!.topSinglesRecyclerview.setAdapter(
            ActivityArtistProfileTopAlbumsAdapter(
                shimmerDataAlbum
            )
        )

        val offlineData: ArtistSearch? =
            SharedPreferenceManager.getInstance(this).getArtistData(artistId)
        if (offlineData != null) {
            artistSearch = offlineData
            display()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun backPress(v: View?) {
        finish()
    }

    companion object {
        private val shimmerData: MutableList<Song?>
            get() {
                val shimmerData: MutableList<Song?> =
                    ArrayList<Song?>()
                for (i in 0..10) {
                    shimmerData.add(
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
                            Lyrics("", "", ""),
                            "",
                            "",
                            Album("", "", ""),
                            SongResponse.Artists(
                                ArrayList<SongResponse.Artist?>(),
                                ArrayList<SongResponse.Artist?>(),
                                ArrayList<SongResponse.Artist?>()
                            ),
                            ArrayList<SongResponse.Image?>(),
                            ArrayList<DownloadUrl?>()
                        )
                    )
                }
                return shimmerData
            }
    }
}
