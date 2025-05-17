package com.harsh.shah.saavnmp3.model.aboutus;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record Contributors(
        @SerializedName("contributors") List<Contributor> contributors
) {
    public record Contributor(
            @SerializedName("login") String login,
            @SerializedName("avatar_url") String avatar_url,
            @SerializedName("html_url") String html_url
    ){}
}
