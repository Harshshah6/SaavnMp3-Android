package com.harsh.shah.saavnmp3.utils;

import android.text.Html;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

public class TextParserUtil {

    /**
     * @param htmlText HTML text to be parsed
     * @return Parsed text
     */
    @NonNull
    public static String parseHtmlText(String htmlText){
        return HtmlCompat.fromHtml(htmlText, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
    }

}
