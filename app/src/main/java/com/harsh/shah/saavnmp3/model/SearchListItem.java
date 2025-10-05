package com.harsh.shah.saavnmp3.model;

import com.harsh.shah.saavnmp3.utils.TextParserUtil;

public record SearchListItem(
        String id,
        String title,
        String subtitle,
        String coverImage,
        Type type
) {

    public String title(){
        return TextParserUtil.parseHtmlText(title);
    }

    public String subtitle(){
        return TextParserUtil.parseHtmlText(subtitle);
    }

    public static enum Type {
        SONG,
        ALBUM,
        PLAYLIST,
        ARTIST
    }
}
