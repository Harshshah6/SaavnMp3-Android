package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.records.SongResponse.Song


data class SongSearch(
    val success: Boolean,
    val data: Data?

) {
    
    data class Data(
        val total: Int,
        val start: Int,
        val results: MutableList<Song?>?
    )
}
