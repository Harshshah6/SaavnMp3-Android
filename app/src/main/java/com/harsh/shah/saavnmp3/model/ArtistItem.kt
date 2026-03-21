package com.harsh.shah.saavnmp3.model

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class ArtistItem(
    val name: String?,
    val image: String?,
    val id: String?
) {
    fun name(): String {
        return TextParserUtil.parseHtmlText(name)
    }
}
