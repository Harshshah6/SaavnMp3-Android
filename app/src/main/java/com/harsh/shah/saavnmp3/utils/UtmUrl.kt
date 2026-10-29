package com.harsh.shah.saavnmp3.utils

import androidx.core.net.toUri

fun String.withUtmParameters(campaign: String, content: String): String {
    val uri = toUri()
    if (uri.scheme != "http" && uri.scheme != "https") return this

    return uri.buildUpon()
        .appendQueryParameter("utm_source", "melotune_android")
        .appendQueryParameter("utm_medium", "app")
        .appendQueryParameter("utm_campaign", campaign)
        .appendQueryParameter("utm_content", content)
        .build()
        .toString()
}
