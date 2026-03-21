package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class ArtistsSearch(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val total: Int,
        val start: Int,
        val results: MutableList<Results?>?
    ) {
        
        data class Results(
            val id: String?,
            val name: String?,
            val role: String?,
            val type: String?,
            val url: String?,
            val image: MutableList<GlobalSearch.Image?>?
        ) {
            fun name(): String {
                return TextParserUtil.parseHtmlText(name)
            }
        }
    }
}
