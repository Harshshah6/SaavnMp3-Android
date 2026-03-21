package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.records.SongResponse.Song
import com.harsh.shah.saavnmp3.utils.TextParserUtil

/**
 * @param success
 * @param data
 * 
 * To be used when called retrieveArtistsByIdOrLink
 */

data class ArtistSearch(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val id: String?,
        val name: String?,
        val url: String?,
        val type: String?,
        val followerCount: Int,
        val fanCount: Int,
        val isVerified: Boolean,
        val dominantLanguage: String?,
        val dominantType: String?,
        val bio: MutableList<Bio?>?,
        val dob: String?,
        val fb: String?,
        val twitter: String?,
        val wiki: String?,
        val availableLanguages: MutableList<String?>?,
        val isRadioPresent: Boolean,
        val image: MutableList<GlobalSearch.Image?>?,
        val topSongs: MutableList<Song?>?,
        val topAlbums: MutableList<AlbumsSearch.Data.Results?>?,
        val singles: MutableList<AlbumsSearch.Data.Results?>?,
        val similarArtists: MutableList<SimilarArtist?>?

    ) {
        fun name(): String {
            return TextParserUtil.parseHtmlText(name)
        }

        
        data class Bio(
            val text: String?,
            val title: String?,
            val sequence: Int
        ) {
            fun text(): String {
                return TextParserUtil.parseHtmlText(text)
            }

            fun title(): String {
                return TextParserUtil.parseHtmlText(title)
            }
        }

        
        data class SimilarArtist(
            val id: String?,
            val name: String?
        ) {
            fun name(): String {
                return TextParserUtil.parseHtmlText(name)
            }
        }
    }
}
