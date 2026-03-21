package com.harsh.shah.saavnmp3.records


data class ArtistAllAlbum(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val total: Int,
        val albums: MutableList<AlbumsSearch.Data.Results?>?
    )
}
