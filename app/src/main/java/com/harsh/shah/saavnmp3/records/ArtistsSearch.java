package com.harsh.shah.saavnmp3.records;

import com.google.gson.annotations.SerializedName;
import com.harsh.shah.saavnmp3.utils.TextParserUtil;

import java.util.List;

public record ArtistsSearch(
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
                @SerializedName("role") String role,
                @SerializedName("type") String type,
                @SerializedName("url") String url,
                @SerializedName("image") List<GlobalSearch.Image> image
        ) {
            public String name() {
                return TextParserUtil.parseHtmlText(name);
            }

        }
    }
}
