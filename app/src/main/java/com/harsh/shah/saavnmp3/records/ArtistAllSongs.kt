package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.records.SongResponse.Song


data class ArtistAllSongs(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val total: Int,
        val songs: MutableList<Song?>?
    )
}
