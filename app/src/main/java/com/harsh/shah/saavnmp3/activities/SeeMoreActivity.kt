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
    private var binding: ActivitySeeMoreBinding? = null
    private var totalItems = 0
    private var currentPage = 0
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySeeMoreBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        
        binding!!.recyclerView.layoutManager = LinearLayoutManager(this)

        // Set adapter before Paginate initialization to avoid "Adapter needs to be set!" exception
        showData()

        val callbacks: Paginate.Callbacks = object : Paginate.Callbacks {
            override fun onLoadMore() {
                requestDataNext()
            }

            override fun isLoading(): Boolean {
                // Qualify with this@SeeMoreActivity to avoid infinite recursion
                return this@SeeMoreActivity.isLoading
            }

            override fun hasLoadedAllItems(): Boolean {
                if (totalItems == 0) return false
                // Check if we have loaded all items (assuming 10 items per page)
                return (currentPage + 1) * 10 >= totalItems
            }
        }

        Paginate.with(binding!!.recyclerView, callbacks)
            .setLoadingTriggerThreshold(2)
            .addLoadingListItem(true)
            .build()
    }

    private fun showData() {
        val extras = intent.extras ?: run {
            finish()
            return
        }
        
        binding!!.toolbarText.text = extras.getString("artist_name")
        artistId = extras.getString("id")
        val type = extras.getString("type", ActivitySeeMoreListAdapter.Mode.TOP_SONGS.name)
        mode = ActivitySeeMoreListAdapter.Mode.valueOf(type!!)
        
        binding!!.recyclerView.adapter = if (mode == ActivitySeeMoreListAdapter.Mode.TOP_SONGS) {
            activitySeeMoreListAdapter
        } else {
            activitySeeMoreAlbumListAdapter
        }
        
        requestDataFirst()
    }

    private var artistId: String? = ""
    private var mode = ActivitySeeMoreListAdapter.Mode.TOP_SONGS
    private val activitySeeMoreListAdapter = ActivitySeeMoreListAdapter()
    private val activitySeeMoreAlbumListAdapter = ActivitySeeMoreAlbumListAdapter()

    private fun requestDataFirst() {
        if (isLoading) return
        isLoading = true
        
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
                        isLoading = false
                        val artistAllSongs = Gson().fromJson(response, ArtistAllSongs::class.java)
                        if (!artistAllSongs.success) {
                            finish()
                            return
                        }
                        val data = artistAllSongs.data ?: return
                        currentPage = 0
                        totalItems = data.total
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
                    isLoading = false
                    val artistAllAlbum = Gson().fromJson(response, ArtistAllAlbum::class.java)
                    if (!artistAllAlbum.success) {
                        finish()
                        return
                    }
                    val data = artistAllAlbum.data ?: return
                    currentPage = 0
                    totalItems = data.total
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
        if (isLoading) return
        isLoading = true
        
        val nextPage = currentPage + 1
        val apiManager = ApiManager(this)
        if (mode == ActivitySeeMoreListAdapter.Mode.TOP_SONGS) {
            apiManager.retrieveArtistSongs(
                artistId ?: "",
                nextPage,
                null,
                null,
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        isLoading = false
                        val artistAllSongs = Gson().fromJson(response, ArtistAllSongs::class.java)
                        if (!artistAllSongs.success) return
                        val data = artistAllSongs.data ?: return
                        currentPage = nextPage
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
                nextPage,
                object : RequestNetwork.RequestListener {
                    override fun onResponse(
                        tag: String?,
                        response: String?,
                        responseHeaders: HashMap<String?, Any?>?
                    ) {
                        isLoading = false
                        val artistAllAlbum = Gson().fromJson(response, ArtistAllAlbum::class.java)
                        if (!artistAllAlbum.success) return
                        val data = artistAllAlbum.data ?: return
                        currentPage = nextPage
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
