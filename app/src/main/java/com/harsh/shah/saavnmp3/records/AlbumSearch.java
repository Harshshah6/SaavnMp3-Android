package com.harsh.shah.saavnmp3.records;

import com.google.gson.annotations.SerializedName;
import com.harsh.shah.saavnmp3.utils.TextParserUtil;

import java.util.List;

public record AlbumSearch(
        @SerializedName("success") boolean success,
        @SerializedName("data") Data data

) {
    public record Data(
            @SerializedName("id") String id,
            @SerializedName("name") String name,
            @SerializedName("url") String url,
            @SerializedName("description") String description,
            @SerializedName("year") int year,
            @SerializedName("playCount") int playCount,
            @SerializedName("language") String language,
            @SerializedName("explicitContent") boolean explicitContent,
            @SerializedName("artists") SongResponse.Artists artist,
            @SerializedName("image") List<GlobalSearch.Image> image,
            @SerializedName("songs") List<SongResponse.Song> songs
    ) {
        public String name(){
            return TextParserUtil.parseHtmlText(name);
        }
        public String description(){
            return TextParserUtil.parseHtmlText(description);
        }
    }
}
