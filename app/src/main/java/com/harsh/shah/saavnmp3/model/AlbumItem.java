package com.harsh.shah.saavnmp3.model;

import com.harsh.shah.saavnmp3.utils.TextParserUtil;

public record AlbumItem(
        String albumTitle,
        String albumSubTitle,
        String albumCover,
        String id
) {
    public String albumTitle(){
        return TextParserUtil.parseHtmlText(albumTitle);
    }

    public String albumSubTitle(){
        return TextParserUtil.parseHtmlText(albumSubTitle);
    }
}
