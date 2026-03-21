package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class PlaylistsSearch(
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
            val type: String?,
            val url: String?,
            val image: MutableList<GlobalSearch.Image?>?,
            val songCount: Int,
            val language: String?,
            val explicitContent: Boolean
        ) {
            fun name(): String {
                return TextParserUtil.parseHtmlText(name)
            }
        }
    }
}
