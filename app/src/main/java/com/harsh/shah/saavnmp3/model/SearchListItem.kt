package com.harsh.shah.saavnmp3.model

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class SearchListItem(
    val id: String?,
    val title: String?,
    val subtitle: String?,
    val coverImage: String?,
    val type: Type?
) {
    fun title(): String {
        return TextParserUtil.parseHtmlText(title)
    }

    fun subtitle(): String {
        return TextParserUtil.parseHtmlText(subtitle)
    }

    enum class Type {
        SONG,
        ALBUM,
        PLAYLIST,
        ARTIST
    }
}
