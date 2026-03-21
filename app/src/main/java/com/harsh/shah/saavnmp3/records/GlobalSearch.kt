package com.harsh.shah.saavnmp3.records

import com.harsh.shah.saavnmp3.utils.TextParserUtil


data class GlobalSearch(
    val success: Boolean,
    val data: Data?
) {
    
    data class Data(
        val topQuery: TopQuery?,
        val songs: Songs?,
        val albums: Albums?,
        val artists: Artists?,
        val playlists: Playlists?
    ) {
        
        data class TopQuery(
            val results: MutableList<Results?>?,
            val position: Int
        ) {
            
            data class Results(
                val id: String?,
                val title: String?,
                val image: MutableList<Image?>?,
                val url: String?,
                val type: String?,
                val description: String?
            ) {
                fun title(): String {
                    return TextParserUtil.parseHtmlText(title)
                }

                fun description(): String {
                    return TextParserUtil.parseHtmlText(description)
                }
            }
        }

        
        data class Songs(
            val results: MutableList<Results?>?,
            val position: Int
        ) {
            
            data class Results(
                val id: String?,
                val title: String?,
                val image: MutableList<Image?>?,
                val album: String?,
                val url: String?,
                val type: String?,
                val description: String?,
                val primaryArtists: String?,
                val singers: String?,
                val language: String?

            ) {
                fun title(): String {
                    return TextParserUtil.parseHtmlText(title)
                }

                fun description(): String {
                    return TextParserUtil.parseHtmlText(description)
                }

                fun album(): String {
                    return TextParserUtil.parseHtmlText(album)
                }

                fun primaryArtists(): String {
                    return TextParserUtil.parseHtmlText(primaryArtists)
                }

                fun singers(): String {
                    return TextParserUtil.parseHtmlText(singers)
                }

                fun language(): String {
                    return TextParserUtil.parseHtmlText(language)
                }
            }
        }

        
        data class Albums(
            val results: MutableList<Results?>?,
            val position: Int
        ) {
            
            data class Results(
                val id: String?,
                val title: String?,
                val image: MutableList<Image?>?,
                val artist: String?,
                val url: String?,
                val type: String?,
                val description: String?,
                val year: String?,
                val songIds: String?,
                val language: String?
            ) {
                fun title(): String {
                    return TextParserUtil.parseHtmlText(title)
                }

                fun description(): String {
                    return TextParserUtil.parseHtmlText(description)
                }

                fun artist(): String {
                    return TextParserUtil.parseHtmlText(artist)
                }

                fun year(): String {
                    return TextParserUtil.parseHtmlText(year)
                }
            }
        }

        
        data class Artists(
            val results: MutableList<Results?>?,
            val position: Int
        ) {
            
            data class Results(
                val id: String?,
                val title: String?,
                val image: MutableList<Image?>?,
                val type: String?,
                val description: String?,
                val position: Int

            ) {
                fun title(): String {
                    return TextParserUtil.parseHtmlText(title)
                }

                fun description(): String {
                    return TextParserUtil.parseHtmlText(description)
                }
            }
        }

        
        data class Playlists(
            val results: MutableList<Results?>?,
            val position: Int
        ) {
            
            data class Results(
                val id: String?,
                val title: String?,
                val image: MutableList<Image?>?,
                val url: String?,
                val type: String?,
                val language: String?,
                val description: String?
            ) {
                fun title(): String {
                    return TextParserUtil.parseHtmlText(title)
                }

                fun description(): String {
                    return TextParserUtil.parseHtmlText(description)
                }
            }
        }
    }

    
    data class Image(
        val quality: String?,
        val url: String?
    )
}
