package com.harsh.shah.saavnmp3.network

import android.content.Context
import android.net.Uri
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.network.utility.RequestNetworkController

class ApiManager(context: Context?) {
    private val requestNetwork: RequestNetwork

    init {
        requestNetwork = RequestNetwork(context)
    }

    fun globalSearch(text: String?, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("query", Uri.encode(text))
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL,
            "",
            listener
        )
    }

    fun searchSongs(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("query", Uri.encode(query))
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + SONGS,
            "",
            listener
        )
    }

    fun searchAlbums(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("query", Uri.encode(query))
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + ALBUMS,
            "",
            listener
        )
    }

    fun searchArtists(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("query", Uri.encode(query))
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + ARTISTS,
            "",
            listener
        )
    }

    fun searchPlaylists(
        query: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("query", Uri.encode(query))
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SEARCH_URL + PLAYLISTS,
            "",
            listener
        )
    }

    fun retrieveSongsByIds(ids: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("ids", ids)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SONGS_URL,
            "",
            listener
        )
    }

    fun retrieveSongByLink(link: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("link", link)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SONGS_URL,
            "",
            listener
        )
    }

    fun retrieveSongById(id: String, lyrics: Boolean?, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("lyrics", lyrics)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            SONGS_URL + "/" + id,
            "",
            listener
        )
    }

    fun retrieveLyricsById(id: String, listener: RequestNetwork.RequestListener?) {
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, SONGS_URL + "/" + id + "/lyrics", "",
            listener
        )
    }

    fun retrieveSongSuggestions(
        id: String,
        limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("limit", limit)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, SONGS_URL + "/" + id + "/suggestions", "",
            listener
        )
    }

    fun retrieveAlbumById(id: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("id", id)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ALBUMS_URL,
            "",
            listener
        )
    }

    fun retrieveAlbumByLink(link: String, listener: RequestNetwork.RequestListener?) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("link", link)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ALBUMS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistsById(
        id: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: String?, sortOrder: String?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("id", id)

        if (page != null) queryMap.put("page", page)
        if (songCount != null) queryMap.put("songCount", songCount)
        if (albumCount != null) queryMap.put("albumCount", albumCount)
        if (sortBy != null) queryMap.put("sortBy", sortBy)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistsByLink(
        link: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: String?, sortOrder: String?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("link", link)

        if (page != null) queryMap.put("page", page)
        if (songCount != null) queryMap.put("songCount", songCount)
        if (albumCount != null) queryMap.put("albumCount", albumCount)
        if (sortBy != null) queryMap.put("sortBy", sortBy)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistById(
        id: String, page: Int?, songCount: Int?, albumCount: Int?,
        sortBy: SortBy?, sortOrder: SortOrder?, listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()

        if (page != null) queryMap.put("page", page)
        if (songCount != null) queryMap.put("songCount", songCount)
        if (albumCount != null) queryMap.put("albumCount", albumCount)
        if (sortBy != null) queryMap.put("sortBy", sortBy.name)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL + "/" + id,
            "",
            listener
        )
    }

    fun retrieveArtistSongs(
        id: String, page: Int?, sortBy: SortBy?, sortOrder: SortOrder?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        if (page != null) queryMap.put("page", page)
        if (sortBy != null) queryMap.put("sortBy", sortBy.name)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name)
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, ARTISTS_URL + "/" + id + "/songs", "",
            listener
        )
    }

    fun retrieveArtistAlbums(
        id: String, page: Int?, sortBy: SortBy?, sortOrder: SortOrder?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        if (page != null) queryMap.put("page", page)
        if (sortBy != null) queryMap.put("sortBy", sortBy.name)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET, ARTISTS_URL + "/" + id + "/albums", "",
            listener
        )
    }

    fun retrievePlaylistById(
        id: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("id", id)
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            PLAYLISTS_URL,
            "",
            listener
        )
    }

    fun retrievePlaylistByLink(
        link: String, page: Int?, limit: Int?,
        listener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("link", link)
        if (page != null) queryMap.put("page", page)
        if (limit != null) queryMap.put("limit", limit)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            PLAYLISTS_URL,
            "",
            listener
        )
    }

    fun retrieveArtistById(artistId: String, requestListener: RequestNetwork.RequestListener?) {
        retrieveArtistById(artistId, null, null, null, null, null, requestListener)
    }

    fun retrieveArtistSongs(
        artistId: String, page: Int, sortBy: SortBy?, sortOrder: SortOrder?,
        requestListener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("page", if (page == -1) 0 else page)
        if (sortBy != null) queryMap.put("sortBy", sortBy.name)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL + "/" + artistId.toInt() + "/songs", "", requestListener
        )
    }

    fun retrieveArtistSongs(artistId: String, requestListener: RequestNetwork.RequestListener?) {
        retrieveArtistSongs(artistId, 0, null, null, requestListener)
    }

    fun retrieveArtistAlbums(
        artistId: String, page: Int, sortBy: SortBy?, sortOrder: SortOrder?,
        requestListener: RequestNetwork.RequestListener?
    ) {
        val queryMap = HashMap<String?, Any?>()
        queryMap.put("page", if (page == -1) 0 else page)
        if (sortBy != null) queryMap.put("sortBy", sortBy.name)
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name)

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            ARTISTS_URL + "/" + artistId.toInt() + "/albums", "", requestListener
        )
    }

    fun retrieveArtistAlbums(
        artistId: String,
        page: Int,
        requestListener: RequestNetwork.RequestListener?
    ) {
        retrieveArtistAlbums(artistId, page, null, null, requestListener)
    }

    enum class SortBy {
        popularity,
        latest,
        alphabetical
    }

    enum class SortOrder {
        asc,
        desc
    }

    companion object {
        private const val BASE_URL = "https://meloapi.vercel.app/api/"
        private val SEARCH_URL: String = BASE_URL + "search"
        private const val SONGS = "/songs"
        private const val ALBUMS = "/albums"
        private const val ARTISTS = "/artists"
        private const val PLAYLISTS = "/playlists"
        private val SONGS_URL: String = BASE_URL + "songs"
        private val ALBUMS_URL: String = BASE_URL + "albums"
        private val ARTISTS_URL: String = BASE_URL + "artists"
        private val PLAYLISTS_URL: String = BASE_URL + "playlists"
    }
}
