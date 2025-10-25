package com.harsh.shah.saavnmp3.network;

import android.app.Activity;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.harsh.shah.saavnmp3.network.utility.RequestNetwork;
import com.harsh.shah.saavnmp3.network.utility.RequestNetworkController;

import java.util.HashMap;

public class ApiManager {
    private static final String BASE_URL = "https://meloapi.vercel.app/api/";
    private static final String SEARCH_URL = BASE_URL + "search";
    private static final String SONGS = "/songs";
    private static final String ALBUMS = "/albums";
    private static final String ARTISTS = "/artists";
    private static final String PLAYLISTS = "/playlists";
    private static final String SONGS_URL = BASE_URL + "songs";
    private static final String ALBUMS_URL = BASE_URL + "albums";
    private static final String ARTISTS_URL = BASE_URL + "artists";
    private static final String PLAYLISTS_URL = BASE_URL + "playlists";

    private final RequestNetwork requestNetwork;

    public ApiManager(Activity activity) {
        requestNetwork = new RequestNetwork(activity);
    }

    public void globalSearch(String text, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Uri.encode(text));
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SEARCH_URL, "", listener);
    }

    public void searchSongs(@NonNull String query, Integer page, Integer limit, @NonNull RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Uri.encode(query));
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SEARCH_URL + SONGS, "", listener);
    }

    public void searchAlbums(@NonNull String query, Integer page, Integer limit, @NonNull RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Uri.encode(query));
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SEARCH_URL + ALBUMS, "", listener);
    }

    public void searchArtists(@NonNull String query, Integer page, Integer limit, @NonNull RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Uri.encode(query));
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SEARCH_URL + ARTISTS, "", listener);
    }

    public void searchPlaylists(@NonNull String query, Integer page, Integer limit, @NonNull RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("query", Uri.encode(query));
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SEARCH_URL + PLAYLISTS, "", listener);
    }

    public void retrieveSongsByIds(@NonNull String ids, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("ids", ids);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SONGS_URL, "", listener);
    }

    public void retrieveSongByLink(@NonNull String link, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("link", link);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SONGS_URL, "", listener);
    }

    public void retrieveSongById(@NonNull String id, Boolean lyrics, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("lyrics", lyrics);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SONGS_URL + "/" + id, "", listener);
    }

    public void retrieveLyricsById(@NonNull String id, RequestNetwork.RequestListener listener) {
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SONGS_URL + "/" + id + "/lyrics", "", listener);
    }

    public void retrieveSongSuggestions(@NonNull String id, Integer limit, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("limit", limit);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, SONGS_URL + "/" + id + "/suggestions", "", listener);
    }

    public void retrieveAlbumById(@NonNull String id, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("id", id);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ALBUMS_URL, "", listener);
    }

    public void retrieveAlbumByLink(@NonNull String link, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("link", link);
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ALBUMS_URL, "", listener);
    }

    public void retrieveArtistsById(@NonNull String id, Integer page, Integer songCount, Integer albumCount, String sortBy, String sortOrder, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("id", id);

        if (page != null) queryMap.put("page", page);
        if (songCount != null) queryMap.put("songCount", songCount);
        if (albumCount != null) queryMap.put("albumCount", albumCount);
        if (sortBy != null) queryMap.put("sortBy", sortBy);
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder);

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL, "", listener);
    }

    public void retrieveArtistsByLink(@NonNull String link, Integer page, Integer songCount, Integer albumCount, String sortBy, String sortOrder, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("link", link);

        if (page != null) queryMap.put("page", page);
        if (songCount != null) queryMap.put("songCount", songCount);
        if (albumCount != null) queryMap.put("albumCount", albumCount);
        if (sortBy != null) queryMap.put("sortBy", sortBy);

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL, "", listener);

    }


    /**
     * @param id
     * @param page
     * @param songCount
     * @param albumCount
     * @param sortBy
     * @param sortOrder
     * @param listener   <p>sortBy(popularity | latest | alphabetical)</p>
     *                   <p>sortOrder(asc | desc)</p>
     */
    public void retrieveArtistById(@NonNull String id, Integer page, Integer songCount, Integer albumCount, SortBy sortBy, SortOrder sortOrder, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();

        if (page != null) queryMap.put("page", page);
        if (songCount != null) queryMap.put("songCount", songCount);
        if (albumCount != null) queryMap.put("albumCount", albumCount);
        if (sortBy != null) queryMap.put("sortBy", sortBy.name());
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name());

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL + "/" + id, "", listener);
    }

    public void retrieveArtistSongs(@NonNull String id, Integer page, SortBy sortBy, SortOrder sortOrder, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        if (page != null) queryMap.put("page", page);
        if (sortBy != null) queryMap.put("sortBy", sortBy.name());
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name());
        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL + "/" + id + "/songs", "", listener);
    }

    public void retrieveArtistAlbums(@NonNull String id, Integer page, SortBy sortBy, SortOrder sortOrder, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        if (page != null) queryMap.put("page", page);
        if (sortBy != null) queryMap.put("sortBy", sortBy.name());
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name());

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL + "/" + id + "/albums", "", listener);
    }

    public void retrievePlaylistById(@NonNull String id, Integer page, Integer limit, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("id", id);
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, PLAYLISTS_URL, "", listener);
    }

    public void retrievePlaylistByLink(@NonNull String link, Integer page, Integer limit, RequestNetwork.RequestListener listener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("link", link);
        if (page != null) queryMap.put("page", page);
        if (limit != null) queryMap.put("limit", limit);

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, PLAYLISTS_URL, "", listener);
    }

    public void retrieveArtistById(String artistId, RequestNetwork.RequestListener requestListener) {
        retrieveArtistById(artistId, null, null, null, null, null, requestListener);
    }

    public void retrieveArtistSongs(String artistId, int page, SortBy sortBy, SortOrder sortOrder, RequestNetwork.RequestListener requestListener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("page", page == -1 ? 0 : page);
        if (sortBy != null) queryMap.put("sortBy", sortBy.name());
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name());

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL + Integer.valueOf(artistId) + "/songs", "", requestListener);
    }

    public void retrieveArtistSongs(String artistId, RequestNetwork.RequestListener requestListener) {
        retrieveArtistSongs(artistId, 0, null, null, requestListener);
    }

    public void retrieveArtistAlbums(String artistId, int page, SortBy sortBy, SortOrder sortOrder, RequestNetwork.RequestListener requestListener) {
        HashMap<String, Object> queryMap = new HashMap<>();
        queryMap.put("page", page == -1 ? 0 : page);
        if (sortBy != null) queryMap.put("sortBy", sortBy.name());
        if (sortOrder != null) queryMap.put("sortOrder", sortOrder.name());

        requestNetwork.setParams(queryMap, RequestNetworkController.REQUEST_PARAM);
        requestNetwork.startRequestNetwork(RequestNetworkController.GET, ARTISTS_URL + Integer.valueOf(artistId) + "/albums", "", requestListener);
    }

    public void retrieveArtistAlbums(String artistId, int page, RequestNetwork.RequestListener requestListener) {
        retrieveArtistAlbums(artistId, page, null, null, requestListener);
    }


    public enum SortBy {
        popularity,
        latest,
        alphabetical
    }

    public enum SortOrder {
        asc,
        desc
    }
}