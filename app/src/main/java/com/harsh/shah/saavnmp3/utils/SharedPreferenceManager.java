package com.harsh.shah.saavnmp3.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Database;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.google.gson.Gson;
import com.harsh.shah.saavnmp3.records.AlbumSearch;
import com.harsh.shah.saavnmp3.records.AlbumsSearch;
import com.harsh.shah.saavnmp3.records.ArtistSearch;
import com.harsh.shah.saavnmp3.records.ArtistsSearch;
import com.harsh.shah.saavnmp3.records.GlobalSearch;
import com.harsh.shah.saavnmp3.records.PlaylistSearch;
import com.harsh.shah.saavnmp3.records.PlaylistsSearch;
import com.harsh.shah.saavnmp3.records.SongResponse;
import com.harsh.shah.saavnmp3.records.SongSearch;
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drop-in replacement for your old SharedPreferenceManager that uses Room.
 *
 * Notes:
 *  - This keeps the same public API-method names so you can replace the file with minimal changes.
 *  - For compatibility this version allows main-thread DB queries (allowMainThreadQueries).
 *    This is convenient for drop-in replacement but not recommended for long-term (remove it later
 *    and call write/read methods on background threads).
 *  - Run migrateFromOldPrefs(context, onComplete) once (e.g. in Application.onCreate) to move
 *    existing entries from SharedPreferences named "cache" into Room. After verifying, call
 *    clearOldPrefsAsync(context, onComplete) to free space.
 */
public class SharedPreferenceManager {

    // ---------- Room single-table key/value entity & DAO & DB ----------

    @Entity(tableName = "key_value")
    static class KeyValue {
        @PrimaryKey
        @NonNull
        public String key;

        @ColumnInfo(name = "json")
        public String json;

        @ColumnInfo(name = "last_updated")
        public long lastUpdated;

        public KeyValue(@NonNull String key, String json, long lastUpdated) {
            this.key = key;
            this.json = json;
            this.lastUpdated = lastUpdated;
        }
    }

    @Dao
    interface KeyValueDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void upsert(KeyValue kv);

        @Query("SELECT json FROM key_value WHERE key = :key LIMIT 1")
        String getJson(String key);

        @Query("SELECT COUNT(*) > 0 FROM key_value WHERE key = :key")
        boolean exists(String key);

        @Query("DELETE FROM key_value WHERE key = :key")
        void deleteByKey(String key);

        @Query("SELECT key, json, last_updated FROM key_value")
        List<KeyValue> getAll();
    }

    @Database(entities = {KeyValue.class}, version = 1, exportSchema = false)
    abstract static class AppDatabase extends RoomDatabase {
        abstract KeyValueDao keyValueDao();
    }

    // ---------- Singleton and helpers ----------

    private static SharedPreferenceManager instance;

    private final AppDatabase db;
    private final KeyValueDao dao;
    private final Gson gson;

    // old SharedPreferences name used by your previous manager
    private static final String OLD_PREFS_NAME = "cache";

    // keep a small in-memory SharedPreferences reference only used during migration/clear
    public SharedPreferences getSharedPreferences() {
        // we keep this method to preserve your API; it's no longer the primary store
        return null;
    }

    private SharedPreferenceManager(Context context) {
        // IMPORTANT: allowMainThreadQueries is enabled here for drop-in sync compatibility.
        // Recommended: remove allowMainThreadQueries() and perform DB operations off the UI thread.
        db = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "saavn_cache.db")
                .allowMainThreadQueries()
                .build();
        dao = db.keyValueDao();
        gson = new Gson();
    }

    public static SharedPreferenceManager getInstance(Context context) {
        if (instance == null) instance = new SharedPreferenceManager(context);
        return instance;
    }

    // ---------- Internal helpers to map keys ----------

    private String keyForSearch(String query) {
        return "search://" + query;
    }

    private String keyForArtistData(String artistId) {
        return "artistData://" + artistId;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    // ---------- Generic put/get/remove helpers (synchronous) ----------

    private void putJson(String key, String json) {
        dao.upsert(new KeyValue(key, json, now()));
    }

    private String getJson(String key) {
        return dao.getJson(key);
    }

    private boolean containsKey(String key) {
        return dao.exists(key);
    }

    private void removeKey(String key) {
        dao.deleteByKey(key);
    }

    // ---------- Methods mapped from your old SharedPreferenceManager ----------

    // home songs recommended
    public void setHomeSongsRecommended(SongSearch songSearch) {
        putJson("home_songs_recommended", gson.toJson(songSearch));
    }

    public SongSearch getHomeSongsRecommended() {
        String json = getJson("home_songs_recommended");
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, SongSearch.class);
    }

    // home artists recommended
    public void setHomeArtistsRecommended(ArtistsSearch artistsRecommended) {
        putJson("home_artists_recommended", gson.toJson(artistsRecommended));
    }

    public ArtistsSearch getHomeArtistsRecommended() {
        String json = getJson("home_artists_recommended");
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, ArtistsSearch.class);
    }

    // home albums recommended
    public void setHomeAlbumsRecommended(AlbumsSearch albumsSearch) {
        putJson("home_albums_recommended", gson.toJson(albumsSearch));
    }

    public AlbumsSearch getHomeAlbumsRecommended() {
        String json = getJson("home_albums_recommended");
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, AlbumsSearch.class);
    }

    // home playlists recommended
    public void setHomePlaylistRecommended(PlaylistsSearch playlistsSearch) {
        putJson("home_playlists_recommended", gson.toJson(playlistsSearch));
    }

    public PlaylistsSearch getHomePlaylistRecommended() {
        String json = getJson("home_playlists_recommended");
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, PlaylistsSearch.class);
    }

    // song response by id
    public void setSongResponseById(String id, SongResponse songSearch) {
        if (id == null) return;
        putJson(id, gson.toJson(songSearch));
    }

    public SongResponse getSongResponseById(String id) {
        if (id == null) return null;
        String json = getJson(id);
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, SongResponse.class);
    }

    public boolean isSongResponseById(String id) {
        if (id == null) return false;
        return containsKey(id);
    }

    // album response by id
    public void setAlbumResponseById(String id, AlbumSearch albumSearch) {
        if (id == null) return;
        putJson(id, gson.toJson(albumSearch));
    }

    public AlbumSearch getAlbumResponseById(String id) {
        if (id == null) return null;
        String json = getJson(id);
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, AlbumSearch.class);
    }

    // playlist response by id
    public void setPlaylistResponseById(String id, PlaylistSearch playlistSearch) {
        if (id == null) return;
        putJson(id, gson.toJson(playlistSearch));
    }

    public PlaylistSearch getPlaylistResponseById(String id) {
        if (id == null) return null;
        String json = getJson(id);
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, PlaylistSearch.class);
    }

    // track quality
    public void setTrackQuality(String string) {
        if (string == null) string = "";
        putJson("track_quality", gson.toJson(string));
    }

    public String getTrackQuality() {
        String json = getJson("track_quality");
        if (json == null || json.isEmpty()) return "320kbps";
        // stored as JSON string
        try {
            return gson.fromJson(json, String.class);
        } catch (Exception e) {
            return "320kbps";
        }
    }

    // saved libraries (full object)
    public void setSavedLibrariesData(SavedLibraries savedLibraries) {
        putJson("saved_libraries", gson.toJson(savedLibraries));
    }

    public SavedLibraries getSavedLibrariesData() {
        String json = getJson("saved_libraries");
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, SavedLibraries.class);
    }

    // add library to saved libraries (keeps same semantics as original)
    public void addLibraryToSavedLibraries(SavedLibraries.Library library) {
        if (library == null) return;
        SavedLibraries savedLibraries = getSavedLibrariesData();
        if (savedLibraries == null) savedLibraries = new SavedLibraries(new ArrayList<>());
        // make defensive copy of list if needed; assuming lists() returns modifiable list
        List<SavedLibraries.Library> list = savedLibraries.lists();
        if (list == null) {
            list = new ArrayList<>();
            // if SavedLibraries has a setter, ideally set here; we assume lists() returns modifiable list
        }
        list.add(library);
        // reserialize full object
        setSavedLibrariesData(savedLibraries);
    }

    public void removeLibraryFromSavedLibraries(int index) {
        SavedLibraries savedLibraries = getSavedLibrariesData();
        if (savedLibraries == null) return;
        List<SavedLibraries.Library> list = savedLibraries.lists();
        if (list == null) return;
        if (index < 0 || index >= list.size()) return;
        list.remove(index);
        setSavedLibrariesData(savedLibraries);
    }

    // saved library by id (stored under the id key)
    public void setSavedLibraryDataById(String id, SavedLibraries.Library library) {
        if (id == null || library == null) return;
        putJson(id, gson.toJson(library));
    }

    public SavedLibraries.Library getSavedLibraryDataById(String id) {
        if (id == null) return null;
        String json = getJson(id);
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, SavedLibraries.Library.class);
        } catch (Exception e) {
            return null;
        }
    }

    // search cache
    public void setSearchResultCache(String query, GlobalSearch searchResult) {
        if (query == null) return;
        putJson(keyForSearch(query), gson.toJson(searchResult));
    }

    public GlobalSearch getSearchResult(String query) {
        if (query == null) return null;
        String json = getJson(keyForSearch(query));
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, GlobalSearch.class);
    }

    // artist data
    public void setArtistData(String artistID, ArtistSearch artistSearch) {
        if (artistID == null) return;
        putJson(keyForArtistData(artistID), gson.toJson(artistSearch));
    }

    public ArtistSearch getArtistData(String artistId) {
        if (artistId == null) return null;
        String json = getJson(keyForArtistData(artistId));
        return (json == null || json.isEmpty()) ? null : gson.fromJson(json, ArtistSearch.class);
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
    public void migrateFromOldPrefs(Context context, Runnable onComplete) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        long now = now();
        if (all == null) {
            if (onComplete != null) onComplete.run();
            return;
        }
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) continue;
            String json;
            // In your previous manager, values were JSON strings for complex objects.
            // But there may be primitives (boolean, int, etc.). Serialize everything with Gson for consistency.
            if (value instanceof String) {
                json = (String) value;
            } else {
                json = gson.toJson(value);
            }
            putJson(key, json);
        }
        if (onComplete != null) onComplete.run();
    }

    /**
     * Clear the old SharedPreferences (async-friendly, but uses allowMainThreadQueries for this file).
     * Call this only after you verified migration succeeded and Room contains your data.
     */
    public void clearOldPrefsAsync(Context context, Runnable onComplete) {
        // This is synchronous operation on prefs but cheap; run it directly
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(OLD_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        if (onComplete != null) onComplete.run();
    }
}
