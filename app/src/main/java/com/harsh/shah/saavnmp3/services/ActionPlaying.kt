package com.harsh.shah.saavnmp3.services

interface ActionPlaying {
    fun nextClicked()
    fun prevClicked()
    fun playClicked()
    fun onProgressChanged(progress: Int)
}
