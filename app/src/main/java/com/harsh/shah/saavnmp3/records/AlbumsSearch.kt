package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class AlbumsSearch(
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
            val description: String?,
            val url: String?,
            val year: Int,
            val type: String?,
            val playCount: Int,
            val language: String?,
            val explicitContent: Boolean,
            val artist: Artists?,
            val image: MutableList<GlobalSearch.Image?>?

        ) {
            fun name(): String {
                return TextParserUtil.parseHtmlText(name)
            }

            fun description(): String {
                return TextParserUtil.parseHtmlText(description)
            }

            
            data class Artists(
                val primary: MutableList<Artist?>?,
                val featured: MutableList<Artist?>?,
                val all: MutableList<Artist?>?
            ) {
                
                data class Artist(
                    val id: String?,
                    val name: String?,
                    val url: String?,
                    val role: String?,
                    val image: MutableList<GlobalSearch.Image?>?,
                    val type: String?
                ) {
                    fun name(): String {
                        return TextParserUtil.parseHtmlText(name)
                    }
                }
            }
        }
    }
}
