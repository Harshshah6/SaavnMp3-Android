package com.harsh.shah.saavnmp3.model;

import com.harsh.shah.saavnmp3.utils.TextParserUtil;

public record ArtistItem(
        String name,
        String image,
        String id
) {
    public String name(){
        return TextParserUtil.parseHtmlText(name);
    }
}
