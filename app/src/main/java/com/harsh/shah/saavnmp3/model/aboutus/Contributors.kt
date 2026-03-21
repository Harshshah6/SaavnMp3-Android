package com.harsh.shah.saavnmp3.model.aboutus


data class Contributors(
    val contributors: MutableList<Contributor?>?
) {
    
    data class Contributor(
        val login: String?,
        val avatar_url: String?,
        val html_url: String?
    )
}
