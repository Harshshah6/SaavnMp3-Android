package com.harsh.shah.saavnmp3.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.ChipGroup
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.adapters.ActivitySearchListItemAdapter
import com.harsh.shah.saavnmp3.databinding.ActivitySearchBinding
import com.harsh.shah.saavnmp3.model.SearchListItem
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.GlobalSearch
import com.harsh.shah.saavnmp3.records.GlobalSearch.Data.TopQuery
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import java.util.Locale
import java.util.function.Consumer

class SearchActivity : AppCompatActivity() {
    var binding: ActivitySearchBinding? = null
    private val TAG = "SearchActivity"
    var globalSearch: GlobalSearch? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        OverScrollDecoratorHelper.setUpOverScroll(binding!!.hscrollview)
        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))

        binding!!.edittext.requestFocus()

        binding!!.chipGroup.setOnCheckedStateChangeListener(ChipGroup.OnCheckedStateChangeListener { group: ChipGroup?, checkedIds: MutableList<Int?>? ->
            Log.i("SearchActivity", "checkedIds: " + checkedIds)
            if (globalSearch != null) {
                if (globalSearch!!.success) {
                    refreshData()
                }
            }
        })

        binding!!.edittext.setOnEditorActionListener(OnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            showData(textView!!.getText().toString())
            Log.i(TAG, "onCreate: " + textView.getText().toString())
            binding!!.edittext.clearFocus()
            hideKeyboard(binding!!.edittext)
            true
        })

        // Show/hide clear icon based on text input
        binding!!.edittext.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (s.toString().isEmpty()) {
                    binding!!.clearIcon.setVisibility(View.GONE)
                } else {
                    binding!!.clearIcon.setVisibility(View.VISIBLE)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })

        // Clear input when clear icon is clicked
        binding!!.clearIcon.setOnClickListener(View.OnClickListener { v: View? ->
            binding!!.edittext.setText("")
        })

        //showData("");
    }

    private fun hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0)
        }
    }

    private fun showData(query: String) {
        showShimmerData()

        if ((query.startsWith("http") || query.startsWith("www")) && query.contains("jiosaavn.com")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(query))
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.setData(Uri.parse(query))
            startActivity(intent)
            return
        }

        val apiManager = ApiManager(this)
        apiManager.globalSearch(query, object : RequestNetwork.RequestListener {
            val sharedPreferenceManager: SharedPreferenceManager =
                SharedPreferenceManager.Companion.getInstance(this@SearchActivity)

            override fun onResponse(
                tag: String?,
                response: String?,
                responseHeaders: HashMap<String?, Any?>?
            ) {
                globalSearch = Gson().fromJson<GlobalSearch?>(response, GlobalSearch::class.java)
                if (globalSearch!!.success) {
                    sharedPreferenceManager.setSearchResultCache(query, globalSearch)
                    refreshData()
                } else {
                    Toast.makeText(
                        this@SearchActivity,
                        "Opps, There was an error while searching",
                        Toast.LENGTH_SHORT
                    ).show()
                    onFailed()
                }
                Log.i(TAG, "onResponse: " + response)
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                Log.e(TAG, "onErrorResponse: " + message)
                Toast.makeText(
                    this@SearchActivity,
                    "Opps, There was an error while searching",
                    Toast.LENGTH_SHORT
                ).show()
                onFailed()
            }

            fun onFailed() {
                val resultOffline = sharedPreferenceManager.getSearchResult(query)
                if (resultOffline != null) {
                    globalSearch = resultOffline
                    refreshData()
                }
            }
        })
    }

    private fun refreshData() {
        val data: MutableList<SearchListItem?> = ArrayList<SearchListItem?>()
        val checkedChipId = binding!!.chipGroup.getCheckedChipId()
        if (checkedChipId == R.id.chip_all) {
            globalSearch!!.data?.topQuery?.results?.forEach { item: TopQuery.Results? ->
                if (item == null) return@forEach
                if (!(item.type == "song" || item.type == "album" || item.type == "playlist" || item.type == "artist")) return@forEach
                
                val images = item.image
                val imageUrl = if (!images.isNullOrEmpty()) images[images.size - 1]?.url ?: "" else ""
                
                data.add(
                    SearchListItem(
                        item.id,
                        item.title(),
                        item.description(),
                        imageUrl,
                        SearchListItem.Type.valueOf(item.type.uppercase(Locale.getDefault()))
                    )
                )
            }
            addSongsData(data)
            addAlbumsData(data)
            addPlaylistsData(data)
            addArtistsData(data)
        } else if (checkedChipId == R.id.chip_song) {
            addSongsData(data)
        } else if (checkedChipId == R.id.chip_albums) {
            addAlbumsData(data)
        } else if (checkedChipId == R.id.chip_playlists) {
            addPlaylistsData(data)
        } else if (checkedChipId == R.id.chip_artists) {
            addArtistsData(data)
        } else {
            throw IllegalStateException("Unexpected value: " + binding!!.chipGroup.getCheckedChipId())
        }
        if (!data.isEmpty()) binding!!.recyclerView.setAdapter(ActivitySearchListItemAdapter(data.filterNotNull().toMutableList()))
    }

    private fun addSongsData(data: MutableList<SearchListItem?>) {
        globalSearch!!.data?.songs?.results?.forEach { item: GlobalSearch.Data.Songs.Results? ->
                if (item == null) return@forEach
                val images = item.image
                val imageUrl = if (!images.isNullOrEmpty()) images[images.size - 1]?.url ?: "" else ""
            data.add(
                SearchListItem(
                    item.id,
                    item.title(),
                    item.description(),
                    imageUrl,
                    SearchListItem.Type.SONG
                )
            )
        }
    }

    private fun addAlbumsData(data: MutableList<SearchListItem?>) {
        globalSearch!!.data?.albums?.results?.forEach { item: GlobalSearch.Data.Albums.Results? ->
            if (item == null) return@forEach
            val images = item.image
            val imageUrl = if (!images.isNullOrEmpty()) images[images.size - 1]?.url ?: "" else ""
            data.add(
                SearchListItem(
                    item.id,
                    item.title(),
                    item.description(),
                    imageUrl,
                    SearchListItem.Type.ALBUM
                )
            )
        }
    }

    private fun addPlaylistsData(data: MutableList<SearchListItem?>) {
        globalSearch!!.data?.playlists?.results?.forEach { item: GlobalSearch.Data.Playlists.Results? ->
            if (item == null) return@forEach
            val images = item.image
            val imageUrl = if (!images.isNullOrEmpty()) images[images.size - 1]?.url ?: "" else ""
            data.add(
                SearchListItem(
                    item.id,
                    item.title(),
                    item.description(),
                    imageUrl,
                    SearchListItem.Type.PLAYLIST
                )
            )
        }
    }

    private fun addArtistsData(data: MutableList<SearchListItem?>) {
        globalSearch!!.data?.artists?.results?.forEach { item: GlobalSearch.Data.Artists.Results? ->
            if (item == null) return@forEach
            val images = item.image
            val imageUrl = if (!images.isNullOrEmpty()) images[images.size - 1]?.url ?: "" else ""
            data.add(
                SearchListItem(
                    item.id,
                    item.title(),
                    item.description(),
                    imageUrl,
                    SearchListItem.Type.ARTIST
                )
            )
        }
    }

    private fun showShimmerData() {
        val data: MutableList<SearchListItem?> = ArrayList<SearchListItem?>()
        for (i in 0..10) {
            data.add(
                SearchListItem(
                    "<shimmer>",
                    "",
                    "",
                    "",
                    SearchListItem.Type.SONG
                )
            )
        }
        binding!!.recyclerView.setAdapter(ActivitySearchListItemAdapter(data.filterNotNull().toMutableList()))
    }

    fun backPress(view: View?) {
        finish()
    }
}
