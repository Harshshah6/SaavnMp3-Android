package com.harsh.shah.saavnmp3.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.model.AlbumItem
import com.harsh.shah.saavnmp3.network.ApiManager
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.records.ArtistAllSongs
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.SongSearch

object RecommendationManager {
    private const val TAG = "RecommendationManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Retrieves intelligent song recommendations for a given song.
     * Strategy:
     * 1. Try API song suggestions (/songs/{id}/suggestions).
     * 2. Fallback to Primary Artist's top songs (/artists/{artistId}/songs).
     * 3. Fallback to Search Songs by artist name + language/genre.
     */
    fun getRecommendationsForSong(
        context: Context,
        song: SongResponse.Song,
        limit: Int = 15,
        callback: (List<SongResponse.Song>) -> Unit
    ) {
        val songId = song.id ?: run {
            mainHandler.post { callback(emptyList()) }
            return
        }

        val apiManager = ApiManager(context)

        // Step 1: Try song suggestions API
        apiManager.retrieveSongSuggestions(songId, limit, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                try {
                    val songResponse = Gson().fromJson(response, SongResponse::class.java)
                    val suggestions = songResponse?.data?.filterNotNull()?.filter { it.id != songId }
                    if (songResponse?.success == true && !suggestions.isNullOrEmpty()) {
                        mainHandler.post { callback(suggestions.take(limit)) }
                        return
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Song suggestions parsing failed: ${e.message}")
                }
                fallbackToArtistTopSongs(apiManager, song, limit, callback)
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                fallbackToArtistTopSongs(apiManager, song, limit, callback)
            }
        })
    }

    private fun fallbackToArtistTopSongs(
        apiManager: ApiManager,
        song: SongResponse.Song,
        limit: Int,
        callback: (List<SongResponse.Song>) -> Unit
    ) {
        val primaryArtist = song.artists?.primary?.firstOrNull()
        val artistId = primaryArtist?.id

        if (!artistId.isNullOrEmpty()) {
            apiManager.retrieveArtistSongs(artistId, 0, ApiManager.SortBy.popularity, ApiManager.SortOrder.desc, object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val artistSongs = Gson().fromJson(response, ArtistAllSongs::class.java)
                        val songs = artistSongs?.data?.songs?.filterNotNull()?.filter { it.id != song.id }
                        if (artistSongs?.success == true && !songs.isNullOrEmpty()) {
                            mainHandler.post { callback(songs.take(limit)) }
                            return
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Artist top songs parsing failed: ${e.message}")
                    }
                    fallbackToSearch(apiManager, song, limit, callback)
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    fallbackToSearch(apiManager, song, limit, callback)
                }
            })
        } else {
            fallbackToSearch(apiManager, song, limit, callback)
        }
    }

    private fun fallbackToSearch(
        apiManager: ApiManager,
        song: SongResponse.Song,
        limit: Int,
        callback: (List<SongResponse.Song>) -> Unit
    ) {
        val query = song.artists?.primary?.firstOrNull()?.name()
            ?: song.language
            ?: song.name()

        apiManager.searchSongs(query, 0, limit + 5, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                try {
                    val searchResult = Gson().fromJson(response, SongSearch::class.java)
                    val songs = searchResult?.data?.results?.filterNotNull()?.filter { it.id != song.id }
                    if (searchResult?.success == true && !songs.isNullOrEmpty()) {
                        mainHandler.post { callback(songs.take(limit)) }
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Search fallback failed: ${e.message}")
                }
                mainHandler.post { callback(emptyList()) }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                mainHandler.post { callback(emptyList()) }
            }
        })
    }

    /**
     * Generates personalized home recommendations based on recent activity or trending seeds.
     */
    fun getHomePersonalizedRecommendations(
        context: Context,
        callback: (title: String, subtitle: String, items: List<AlbumItem>) -> Unit
    ) {
        val prefs = SharedPreferenceManager.getInstance(context)
        val recentSongs = prefs.getRecentlyPlayed()
        val lastArtistName = prefs.lastPlayedArtistName
        val lastArtistId = prefs.lastPlayedArtistId
        val apiManager = ApiManager(context)

        if (!lastArtistId.isNullOrEmpty() && !lastArtistName.isNullOrEmpty()) {
            val sectionTitle = "Recommended For You"
            val sectionSubtitle = "Because you listened to $lastArtistName"

            apiManager.retrieveArtistSongs(lastArtistId, 0, ApiManager.SortBy.popularity, ApiManager.SortOrder.desc, object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val artistSongs = Gson().fromJson(response, ArtistAllSongs::class.java)
                        val songs = artistSongs?.data?.songs?.filterNotNull()
                        if (artistSongs?.success == true && !songs.isNullOrEmpty()) {
                            val items = songs.take(15).map { s ->
                                val img = s.image?.lastOrNull()?.url ?: ""
                                AlbumItem(s.name(), "${s.language ?: ""} ${s.year ?: ""}".trim(), img, s.id)
                            }
                            mainHandler.post { callback(sectionTitle, sectionSubtitle, items) }
                            return
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}")
                    }
                    fallbackHomeTrending(apiManager, callback)
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    fallbackHomeTrending(apiManager, callback)
                }
            })
        } else if (recentSongs.isNotEmpty()) {
            val first = recentSongs.first()
            val sectionTitle = "Recommended For You"
            val sectionSubtitle = "Based on ${first.title ?: "your taste"}"
            val query = first.title ?: "Hits"

            apiManager.searchSongs(query, 0, 15, object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val songSearch = Gson().fromJson(response, SongSearch::class.java)
                        val results = songSearch?.data?.results?.filterNotNull()
                        if (songSearch?.success == true && !results.isNullOrEmpty()) {
                            val items = results.map { s ->
                                val img = s.image?.lastOrNull()?.url ?: ""
                                AlbumItem(s.name(), "${s.language ?: ""} ${s.year ?: ""}".trim(), img, s.id)
                            }
                            mainHandler.post { callback(sectionTitle, sectionSubtitle, items) }
                            return
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error: ${e.message}")
                    }
                    fallbackHomeTrending(apiManager, callback)
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    fallbackHomeTrending(apiManager, callback)
                }
            })
        } else {
            fallbackHomeTrending(apiManager, callback)
        }
    }

    private fun fallbackHomeTrending(
        apiManager: ApiManager,
        callback: (title: String, subtitle: String, items: List<AlbumItem>) -> Unit
    ) {
        val trendingQueries = listOf("Trending", "Hits", "Top Songs", "Featured", "Romance")
        val query = trendingQueries.random()
        apiManager.searchSongs(query, 0, 15, object : RequestNetwork.RequestListener {
            override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                try {
                    val songSearch = Gson().fromJson(response, SongSearch::class.java)
                    val results = songSearch?.data?.results?.filterNotNull()
                    if (songSearch?.success == true && !results.isNullOrEmpty()) {
                        val items = results.map { s ->
                            val img = s.image?.lastOrNull()?.url ?: ""
                            AlbumItem(s.name(), "${s.language ?: ""} ${s.year ?: ""}".trim(), img, s.id)
                        }
                        mainHandler.post { callback("Made For You", "Trending Hits", items) }
                        return
                    }
                } catch (_: Exception) {}
                mainHandler.post { callback("Made For You", "Curated Selection", emptyList()) }
            }

            override fun onErrorResponse(tag: String?, message: String?) {
                mainHandler.post { callback("Made For You", "Curated Selection", emptyList()) }
            }
        })
    }
}
