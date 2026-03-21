package com.harsh.shah.saavnmp3.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build

object NetworkUtil {
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For Android 10 (API 29) and above, use the Network API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val network = connectivityManager.getActiveNetwork()
            return network != null && connectivityManager.getNetworkCapabilities(network) != null
        } else {
            // For below Android 10 (API 29), use the older method
            val networkInfo = connectivityManager.getActiveNetworkInfo()
            return networkInfo != null && networkInfo.isConnected()
        }
    }
}
