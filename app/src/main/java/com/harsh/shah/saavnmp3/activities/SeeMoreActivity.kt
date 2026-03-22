package com.harsh.shah.saavnmp3.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.adapters.ActivitySeeMoreAlbumListAdapter
import com.harsh.shah.saavnmp3.adapters.ActivitySeeMoreListAdapter
import com.harsh.shah.saavnmp3.databinding.ActivitySeeMoreBinding
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.ArtistAllAlbum
import com.harsh.shah.saavnmp3.records.ArtistAllSongs
import com.paginate.Paginate

class SeeMoreActivity : AppCompatActivity() {
    var binding: ActivitySeeMoreBinding? = null
    private var totalItems = 0
    private var currentPage = 0
    private var isLoading = false
    private val isLastPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeeMoreBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.recyclerView.setAdapter(activitySeeMoreListAdapter)

        val callbacks: Paginate.Callbacks = object : Paginate.Callbacks {
            override fun onLoadMore() {
                requestDataNext()
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun hasLoadedAllItems(): Boolean {
                return currentPage == totalItems / 10
            }
        }

        Paginate.with(binding!!.recyclerView, callbacks)
            .setLoadingTriggerThreshold(2)
            .addLoadingListItem(true)
            .build()

        showData()
    }

    private fun showData() {
        if (intent.extras == null) finish()
        binding!!.toolbarText.text = intent.extras!!.getString("artist_name")
        artistId = intent.extras!!.getString("id")
        val type = intent.extras!!
            .getString("type", ActivitySeeMoreListAdapter.Mode.TOP_SONGS.name)
        mode = ActivitySeeMoreListAdapter.Mode.valueOf(type!!)
        binding!!.recyclerView.setAdapter(
            if (mode == ActivitySeeMoreListAdapter.Mode.TOP_SONGS) activitySeeMoreListAdapter else activitySeeMoreAlbumListAdapter
        )
        requestDataFirst()
    }

    private var artistId: String? = ""
    private var mode = ActivitySeeMoreListAdapter.Mode.TOP_SONGS
    private val activitySeeMoreListAdapter = ActivitySeeMoreListAdapter()
    private val activitySeeMoreAlbumListAdapter = ActivitySeeMoreAlbumListAdapter()

    //private final ActivitySeeMoreSinglesListAdapter activitySeeMoreSinglesListAdapter = new ActivitySeeMoreSinglesListAdapter();
    private fun requestDataFirst() {
        val apiManager = ApiManager(this)
        if (mode == ActivitySeeMoreListAdapter.Mode.TOP_SONGS) {
            apiManager.retrieveArtistSongs(
                artistId ?: "",
                0,
                null,
                null,
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val artistAllSongs =
                            Gson().fromJson<ArtistAllSongs>(response, ArtistAllSongs::class.java)
                        if (!artistAllSongs.success) finish()
                        val data = artistAllSongs.data ?: return
                        isLoading = false
                        currentPage = 0
                        totalItems = data.total ?: 0
                        activitySeeMoreListAdapter.addAll(data.songs ?: mutableListOf())
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        isLoading = false
                        Toast.makeText(this@SeeMoreActivity, message, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            apiManager.retrieveArtistAlbums(artistId ?: "", 0, object : RequestNetwork.RequestListener {
                override fun onResponse(
                    tag: String?,
                    response: String?,
                    responseHeaders: HashMap<String?, Any?>?
                ) {
                    val artistAllSongs =
                        Gson().fromJson<ArtistAllAlbum>(response, ArtistAllAlbum::class.java)
                    if (!artistAllSongs.success) finish()
                    val data = artistAllSongs.data ?: return
                    isLoading = false
                    currentPage = 0
                    totalItems = data.total ?: 0
                    activitySeeMoreAlbumListAdapter.addAll(data.albums ?: mutableListOf())
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    isLoading = false
                    Toast.makeText(this@SeeMoreActivity, message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun requestDataNext() {
        currentPage++
        val apiManager = ApiManager(this)
        if (mode == ActivitySeeMoreListAdapter.Mode.TOP_SONGS) {
            apiManager.retrieveArtistSongs(
                artistId ?: "",
                currentPage,
                null,
                null,
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val artistAllSongs =
                            Gson().fromJson<ArtistAllSongs>(response, ArtistAllSongs::class.java)
                        if (!artistAllSongs.success) finish()
                        val data = artistAllSongs.data ?: return
                        isLoading = false
                        activitySeeMoreListAdapter.addAll(data.songs ?: mutableListOf())
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        isLoading = false
                        Toast.makeText(this@SeeMoreActivity, message, Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            apiManager.retrieveArtistAlbums(
                artistId ?: "",
                currentPage,
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        val artistAllSongs =
                            Gson().fromJson<ArtistAllAlbum>(response, ArtistAllAlbum::class.java)
                        if (!artistAllSongs.success) finish()
                        val data = artistAllSongs.data ?: return
                        isLoading = false
                        activitySeeMoreAlbumListAdapter.addAll(data.albums ?: mutableListOf())
                    }

                    override fun onErrorResponse(tag: String?, message: String?) {
                        isLoading = false
                        Toast.makeText(this@SeeMoreActivity, message, Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    fun backPress(view: View?) {
        finish()
    }
}
