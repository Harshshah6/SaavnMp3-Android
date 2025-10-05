package com.harsh.shah.saavnmp3.model;

import com.harsh.shah.saavnmp3.utils.TextParserUtil;

public record BasicDataRecord(
        String id,
        String title,
        String subtitle,
        String image
) {
    public String title(){
        return TextParserUtil.parseHtmlText(title);
    }
    public String subtitle(){
        return TextParserUtil.parseHtmlText(subtitle);
    }
}
