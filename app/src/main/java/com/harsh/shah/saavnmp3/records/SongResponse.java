package com.harsh.shah.saavnmp3.records;

import com.google.gson.annotations.SerializedName;
import com.harsh.shah.saavnmp3.utils.TextParserUtil;

import java.util.List;

public record SongResponse(
        @SerializedName("success") boolean success,
        @SerializedName("data") List<Song> data
) {
    public record Song(
            @SerializedName("id") String id,
            @SerializedName("name") String name,
            @SerializedName("type") String type,
            @SerializedName("year") String year,
            @SerializedName("releaseDate") String releaseDate,
            @SerializedName("duration") Double duration,
            @SerializedName("label") String label,
            @SerializedName("explicitContent") boolean explicitContent,
            @SerializedName("playCount") Integer playCount,
            @SerializedName("language") String language,
            @SerializedName("hasLyrics") boolean hasLyrics,
            @SerializedName("lyricsId") String lyricsId,
            @SerializedName("lyrics") Lyrics lyrics,
            @SerializedName("url") String url,
            @SerializedName("copyright") String copyright,
            @SerializedName("album") Album album,
            @SerializedName("artists") Artists artists,
            @SerializedName("image") List<Image> image,
            @SerializedName("downloadUrl") List<DownloadUrl> downloadUrl

    ) {
        public String name() {
            return TextParserUtil.parseHtmlText(name);
        }
    }

    public record Lyrics(
            @SerializedName("lyrics") String lyrics,
            @SerializedName("copyright") String copyright,
            @SerializedName("snippet") String snippet
    ) {
        public String lyrics() {
            return TextParserUtil.parseHtmlText(lyrics);
        }
    }

    public record Album(
            @SerializedName("id") String id,
            @SerializedName("name") String name,
            @SerializedName("url") String url
    ) {
        public String name() {
            return TextParserUtil.parseHtmlText(name);
        }
    }

    public record Artists(
            @SerializedName("primary") List<Artist> primary,
            @SerializedName("featured") List<Artist> featured,
            @SerializedName("all") List<Artist> all
    ) {
    }

    public record Artist(
            @SerializedName("id") String id,
            @SerializedName("name") String name,
            @SerializedName("role") String role,
            @SerializedName("type") String type,
            @SerializedName("image") List<Image> image,
            @SerializedName("url") String url
    ) {
        public String name() {
            return TextParserUtil.parseHtmlText(name);
        }
        public String role() {
            return TextParserUtil.parseHtmlText(role);
        }
    }

    public record Image(
            @SerializedName("quality") String quality,
            @SerializedName("url") String url
    ) {
    }

    public record DownloadUrl(
            @SerializedName("quality") String quality,
            @SerializedName("url") String url
    ) {
    }

}