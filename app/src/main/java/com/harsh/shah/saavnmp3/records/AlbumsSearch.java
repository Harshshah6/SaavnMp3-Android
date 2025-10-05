package com.harsh.shah.saavnmp3.records;

import com.google.gson.annotations.SerializedName;
import com.harsh.shah.saavnmp3.utils.TextParserUtil;

import java.util.List;

public record AlbumsSearch(
        @SerializedName("success") boolean success,
        @SerializedName("data") Data data
) {
    public record Data(
            @SerializedName("total") int total,
            @SerializedName("start") int start,
            @SerializedName("results") List<Results> results
    ) {
        public record Results(
                @SerializedName("id") String id,
                @SerializedName("name") String name,
                @SerializedName("description") String description,
                @SerializedName("url") String url,
                @SerializedName("year") int year,
                @SerializedName("type") String type,
                @SerializedName("playCount") int playCount,
                @SerializedName("language") String language,
                @SerializedName("explicitContent") boolean explicitContent,
                @SerializedName("artists") Artists artist,
                @SerializedName("image") List<GlobalSearch.Image> image

        ){
            public String name(){
                return TextParserUtil.parseHtmlText(name);
            }
            public String description(){
                return TextParserUtil.parseHtmlText(description);
            }
            public record Artists(
                    @SerializedName("primary") List<Artist> primary,
                    @SerializedName("featured") List<Artist> featured,
                    @SerializedName("all") List<Artist> all
            ){
                public record Artist(
                        @SerializedName("id") String id,
                        @SerializedName("name") String name,
                        @SerializedName("url") String url,
                        @SerializedName("role") String role,
                        @SerializedName("image") List<GlobalSearch.Image> image,
                        @SerializedName("type") String type
                ){
                    public String name(){
                        return TextParserUtil.parseHtmlText(name);
                    }
                }
            }
        }
    }
}
