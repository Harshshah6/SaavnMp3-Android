package com.harsh.shah.saavnmp3.model

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class AlbumItem(
    val albumTitle: String?,
    val albumSubTitle: String?,
    val albumCover: String?,
    val id: String?
) {
    fun albumTitle(): String {
        return TextParserUtil.parseHtmlText(albumTitle)
    }

    fun albumSubTitle(): String {
        return TextParserUtil.parseHtmlText(albumSubTitle)
    }
}
