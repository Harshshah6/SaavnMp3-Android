package com.harsh.shah.saavnmp3.records;

import com.google.gson.annotations.SerializedName;
import com.harsh.shah.saavnmp3.utils.TextParserUtil;

public record LyricsSearch (
    @SerializedName("success") boolean success,
    @SerializedName("data") Data data
){
    public record Data(
            @SerializedName("lyrics") String lyrics,
            @SerializedName("copyright") String copyright,
            @SerializedName("snippet") String snippet
    ){
        public String lyrics(){
            return TextParserUtil.parseHtmlText(lyrics);
        }
    }
}
