package com.harsh.shah.saavnmp3.records.sharedpref


data class SavedLibraries(
    val lists: MutableList<Library?>?
) {
    
    data class Library(
        val id: String?,
        val isCreatedByUser: Boolean,
        val isAlbum: Boolean,
        val name: String?,
        val image: String?,
        val description: String?,
        val songs: MutableList<Songs?>?
    ) {
        
        data class Songs(
            val id: String?,
            val title: String?,
            val description: String?,
            val image: String?
        )
    }
}
