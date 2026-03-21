package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class PlaylistSearch(
    val success: Boolean,
    val data: Data?

) {
    
    data class Data(
        val id: String?,
        val name: String?,
        val url: String?,
        val description: String?,
        val type: String?,
        val year: Int,
        val playCount: Int,
        val songCount: Int,
        val language: String?,
        val explicitContent: Boolean,
        val artists: MutableList<Artist?>?,
        val image: MutableList<GlobalSearch.Image?>?,
        val songs: MutableList<Song?>?
    ) {
        fun name(): String {
            return TextParserUtil.parseHtmlText(name)
        }

        fun description(): String {
            return TextParserUtil.parseHtmlText(description)
        }

        
        data class Artist(
            val id: String?,
            val name: String?,
            val url: String?,
            val role: String?,
            val type: String?,
            val image: MutableList<GlobalSearch.Image?>?

        ) {
            fun name(): String {
                return TextParserUtil.parseHtmlText(name)
            }
        }
    }
}
