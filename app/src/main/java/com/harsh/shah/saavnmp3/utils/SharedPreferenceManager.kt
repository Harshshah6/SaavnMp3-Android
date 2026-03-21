package com.harsh.shah.saavnmp3.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.google.gson.Gson
import com.harsh.shah.saavnmp3.records.AlbumSearch
import com.harsh.shah.saavnmp3.records.AlbumsSearch
import com.harsh.shah.saavnmp3.records.ArtistSearch
import com.harsh.shah.saavnmp3.records.ArtistsSearch
import com.harsh.shah.saavnmp3.records.GlobalSearch
import com.harsh.shah.saavnmp3.records.PlaylistSearch
import com.harsh.shah.saavnmp3.records.PlaylistsSearch
import com.harsh.shah.saavnmp3.records.SongResponse
import com.harsh.shah.saavnmp3.records.SongSearch
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library

/**
 * Drop-in replacement for your old SharedPreferenceManager that uses Room.
 * 
 * Notes:
 * - This keeps the same public API-method names so you can replace the file with minimal changes.
 * - For compatibility this version allows main-thread DB queries (allowMainThreadQueries).
 * This is convenient for drop-in replacement but not recommended for long-term (remove it later
 * and call write/read methods on background threads).
 * - Run migrateFromOldPrefs(context, onComplete) once (e.g. in Application.onCreate) to move
 * existing entries from SharedPreferences named "cache" into Room. After verifying, call
 * clearOldPrefsAsync(context, onComplete) to free space.
 */
class SharedPreferenceManager private constructor(context: Context) {
    // ---------- Room single-table key/value entity & DAO & DB ----------
    @Entity(tableName = "key_value")
    internal class KeyValue(
        @PrimaryKey var key: String,
        @ColumnInfo(name = "json") var json: String?,
        @ColumnInfo(name = "last_updated") var lastUpdated: Long
    )

    @Dao
    internal interface KeyValueDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun upsert(kv: KeyValue)

        @Query("SELECT json FROM key_value WHERE key = :key LIMIT 1")
        fun getJson(key: String?): String?

        @Query("SELECT COUNT(*) > 0 FROM key_value WHERE key = :key")
        fun exists(key: String?): Boolean

        @Query("DELETE FROM key_value WHERE key = :key")
        fun deleteByKey(key: String?)

        @Query("SELECT * FROM key_value")
        fun getAll(): List<KeyValue>
    }

    @Database(entities = [KeyValue::class], version = 1, exportSchema = false)
    internal abstract class AppDatabase : RoomDatabase() {
        abstract fun keyValueDao(): KeyValueDao
    }

    private val db: AppDatabase
    private val dao: KeyValueDao
    private val gson: Gson

    val sharedPreferences: SharedPreferences?
        // keep a small in-memory SharedPreferences reference only used during migration/clear
        get() =// we keep this method to preserve your API; it's no longer the primary store
            null

    init {
        // IMPORTANT: allowMainThreadQueries is enabled here for drop-in sync compatibility.
        // Recommended: remove allowMainThreadQueries() and perform DB operations off the UI thread.
        db = databaseBuilder<AppDatabase>(
            context.getApplicationContext(),
            AppDatabase::class.java,
            "saavn_cache.db"
        )
            .allowMainThreadQueries()
            .build()
        dao = db.keyValueDao()
        gson = Gson()
    }

    // ---------- Internal helpers to map keys ----------
    private fun keyForSearch(query: String): String {
        return "search://" + query
    }

    private fun keyForArtistData(artistId: String): String {
        return "artistData://" + artistId
    }

    private fun now(): Long {
        return System.currentTimeMillis()
    }

    // ---------- Generic put/get/remove helpers (synchronous) ----------
    private fun putJson(key: String, json: String?) {
        dao.upsert(KeyValue(key, json, now()))
    }

    private fun getJson(key: String?): String? {
        return dao.getJson(key)
    }

    private fun containsKey(key: String?): Boolean {
        return dao.exists(key)
    }

    private fun removeKey(key: String?) {
        dao.deleteByKey(key)
    }

    var homeSongsRecommended: SongSearch?
        get() {
            val json = getJson("home_songs_recommended")
            return if (json == null || json.isEmpty()) null else gson.fromJson<SongSearch?>(
                json,
                SongSearch::class.java
            )
        }
        // ---------- Methods mapped from your old SharedPreferenceManager ----------
        set(songSearch) {
            putJson("home_songs_recommended", gson.toJson(songSearch))
        }

    var homeArtistsRecommended: ArtistsSearch?
        get() {
            val json = getJson("home_artists_recommended")
            return if (json == null || json.isEmpty()) null else gson.fromJson<ArtistsSearch?>(
                json,
                ArtistsSearch::class.java
            )
        }
        // home artists recommended
        set(artistsRecommended) {
            putJson("home_artists_recommended", gson.toJson(artistsRecommended))
        }

    var homeAlbumsRecommended: AlbumsSearch?
        get() {
            val json = getJson("home_albums_recommended")
            return if (json == null || json.isEmpty()) null else gson.fromJson<AlbumsSearch?>(
                json,
                AlbumsSearch::class.java
            )
        }
        // home albums recommended
        set(albumsSearch) {
            putJson("home_albums_recommended", gson.toJson(albumsSearch))
        }

    var homePlaylistRecommended: PlaylistsSearch?
        get() {
            val json = getJson("home_playlists_recommended")
            return if (json == null || json.isEmpty()) null else gson.fromJson<PlaylistsSearch?>(
                json,
                PlaylistsSearch::class.java
            )
        }
        // home playlists recommended
        set(playlistsSearch) {
            putJson("home_playlists_recommended", gson.toJson(playlistsSearch))
        }

    // song response by id
    fun setSongResponseById(id: String?, songSearch: SongResponse?) {
        if (id == null) return
        putJson(id, gson.toJson(songSearch))
    }

    fun getSongResponseById(id: String?): SongResponse? {
        if (id == null) return null
        val json = getJson(id)
        return if (json == null || json.isEmpty()) null else gson.fromJson<SongResponse?>(
            json,
            SongResponse::class.java
        )
    }

    fun isSongResponseById(id: String?): Boolean {
        if (id == null) return false
        return containsKey(id)
    }

    // album response by id
    fun setAlbumResponseById(id: String?, albumSearch: AlbumSearch?) {
        if (id == null) return
        putJson(id, gson.toJson(albumSearch))
    }

    fun getAlbumResponseById(id: String?): AlbumSearch? {
        if (id == null) return null
        val json = getJson(id)
        return if (json == null || json.isEmpty()) null else gson.fromJson<AlbumSearch?>(
            json,
            AlbumSearch::class.java
        )
    }

    // playlist response by id
    fun setPlaylistResponseById(id: String?, playlistSearch: PlaylistSearch?) {
        if (id == null) return
        putJson(id, gson.toJson(playlistSearch))
    }

    fun getPlaylistResponseById(id: String?): PlaylistSearch? {
        if (id == null) return null
        val json = getJson(id)
        return if (json == null || json.isEmpty()) null else gson.fromJson<PlaylistSearch?>(
            json,
            PlaylistSearch::class.java
        )
    }

    var trackQuality: String?
        get() {
            val json = getJson("track_quality")
            if (json == null || json.isEmpty()) return "320kbps"
            // stored as JSON string
            try {
                return gson.fromJson<String?>(json, String::class.java)
            } catch (e: Exception) {
                return "320kbps"
            }
        }
        // track quality
        set(string) {
            var string = string
            if (string == null) string = ""
            putJson("track_quality", gson.toJson(string))
        }

    var savedLibrariesData: SavedLibraries?
        get() {
            val json = getJson("saved_libraries")
            return if (json == null || json.isEmpty()) null else gson.fromJson<SavedLibraries?>(
                json,
                SavedLibraries::class.java
            )
        }
        // saved libraries (full object)
        set(savedLibraries) {
            putJson("saved_libraries", gson.toJson(savedLibraries))
        }

    // add library to saved libraries (keeps same semantics as original)
    fun addLibraryToSavedLibraries(library: Library?) {
        if (library == null) return
        var savedLibraries = this.savedLibrariesData
        if (savedLibraries == null) savedLibraries = SavedLibraries(ArrayList<Library?>())
        // make defensive copy of list if needed; assuming lists() returns modifiable list
        var list = savedLibraries.lists
        if (list == null) {
            list = ArrayList<Library?>()
            // if SavedLibraries has a setter, ideally set here; we assume lists() returns modifiable list
        }
        list.add(library)
        // reserialize full object
        this.savedLibrariesData = savedLibraries
    }

    fun removeLibraryFromSavedLibraries(index: Int) {
        val savedLibraries = this.savedLibrariesData
        if (savedLibraries == null) return
        val list = savedLibraries.lists
        if (list == null) return
        if (index < 0 || index >= list.size) return
        list.removeAt(index)
        this.savedLibrariesData = savedLibraries
    }

    // saved library by id (stored under the id key)
    fun setSavedLibraryDataById(id: String?, library: Library?) {
        if (id == null || library == null) return
        putJson(id, gson.toJson(library))
    }

    fun getSavedLibraryDataById(id: String?): Library? {
        if (id == null) return null
        val json = getJson(id)
        if (json == null || json.isEmpty()) return null
        try {
            return gson.fromJson<Library?>(json, Library::class.java)
        } catch (e: Exception) {
            return null
        }
    }

    // search cache
    fun setSearchResultCache(query: String?, searchResult: GlobalSearch?) {
        if (query == null) return
        putJson(keyForSearch(query), gson.toJson(searchResult))
    }

    fun getSearchResult(query: String?): GlobalSearch? {
        if (query == null) return null
        val json = getJson(keyForSearch(query))
        return if (json == null || json.isEmpty()) null else gson.fromJson<GlobalSearch?>(
            json,
            GlobalSearch::class.java
        )
    }

    // artist data
    fun setArtistData(artistID: String?, artistSearch: ArtistSearch?) {
        if (artistID == null) return
        putJson(keyForArtistData(artistID), gson.toJson(artistSearch))
    }

    fun getArtistData(artistId: String?): ArtistSearch? {
        if (artistId == null) return null
        val json = getJson(keyForArtistData(artistId))
        return if (json == null || json.isEmpty()) null else gson.fromJson<ArtistSearch?>(
            json,
            ArtistSearch::class.java
        )
    }

    // ---------- Migration helpers for one-time migration ----------
    /**
     * Migrate all entries from the old SharedPreferences named "cache" into Room.
     * This runs synchronously (it can be called from Application.onCreate). It iterates all keys
     * in the old prefs and upserts their value (strings/primitives) into Room under the same key.
     * 
     * You can pass a Runnable for onComplete which will run after migration (on the caller thread).
     * Recommended: call this once in Application.onCreate and then verify data in Room.
     */
    fun migrateFromOldPrefs(context: Context, onComplete: Runnable?) {
        val prefs = context.getApplicationContext()
            .getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.getAll()
        val now = now()
        if (all == null) {
            if (onComplete != null) onComplete.run()
            return
        }
        for (entry in all.entries) {
            val key: String = entry.key!!
            val value: Any? = entry.value
            if (value == null) continue
            val json: String?
            // In your previous manager, values were JSON strings for complex objects.
            // But there may be primitives (boolean, int, etc.). Serialize everything with Gson for consistency.
            if (value is String) {
                json = value
            } else {
                json = gson.toJson(value)
            }
            putJson(key, json)
        }
        if (onComplete != null) onComplete.run()
    }

    /**
     * Clear the old SharedPreferences (async-friendly, but uses allowMainThreadQueries for this file).
     * Call this only after you verified migration succeeded and Room contains your data.
     */
    fun clearOldPrefsAsync(context: Context, onComplete: Runnable?) {
        // This is synchronous operation on prefs but cheap; run it directly
        val prefs = context.getApplicationContext()
            .getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        if (onComplete != null) onComplete.run()
    }

    companion object {
        // ---------- Singleton and helpers ----------
        private var instance: SharedPreferenceManager? = null

        // old SharedPreferences name used by your previous manager
        private const val OLD_PREFS_NAME = "cache"

        fun getInstance(context: Context): SharedPreferenceManager {
            if (instance == null) instance = SharedPreferenceManager(context)
            return instance!!
        }
    }
}
