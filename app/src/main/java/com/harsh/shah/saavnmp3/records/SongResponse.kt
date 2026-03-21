package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class SongResponse(
    val success: Boolean,
    val data: MutableList<Song?>?
) {
    
    data class Song(
        val id: String?,
        val name: String?,
        val type: String?,
        val year: String?,
        val releaseDate: String?,
        val duration: Double?,
        val label: String?,
        val explicitContent: Boolean,
        val playCount: Int?,
        val language: String?,
        val hasLyrics: Boolean,
        val lyricsId: String?,
        val lyrics: Lyrics?,
        val url: String?,
        val copyright: String?,
        val album: Album?,
        val artists: Artists?,
        val image: MutableList<Image?>?,
        val downloadUrl: MutableList<DownloadUrl?>?

    ) {
        fun name(): String {
            return TextParserUtil.parseHtmlText(name)
        }
    }

    
    data class Lyrics(
        val lyrics: String?,
        val copyright: String?,
        val snippet: String?
    ) {
        fun lyrics(): String {
            return TextParserUtil.parseHtmlText(lyrics)
        }
    }

    
    data class Album(
        val id: String?,
        val name: String?,
        val url: String?
    ) {
        fun name(): String {
            return TextParserUtil.parseHtmlText(name)
        }
    }

    
    data class Artists(
        val primary: MutableList<Artist?>?,
        val featured: MutableList<Artist?>?,
        val all: MutableList<Artist?>?
    )

    
    data class Artist(
        val id: String?,
        val name: String?,
        val role: String?,
        val type: String?,
        val image: MutableList<Image?>?,
        val url: String?
    ) {
        fun name(): String {
            return TextParserUtil.parseHtmlText(name)
        }

        fun role(): String {
            return TextParserUtil.parseHtmlText(role)
        }
    }

    
    data class Image(
        val quality: String?,
        val url: String?
    )

    
    data class DownloadUrl(
        val quality: String?,
        val url: String?
    )
}
