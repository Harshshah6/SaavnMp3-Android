package com.harsh.shah.saavnmp3.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

class NetworkChangeReceiver(private val listener: NetworkStatusListener) : BroadcastReceiver() {
    // Callback interface to notify when the internet is connected or disconnected
    interface NetworkStatusListener {
        fun onNetworkConnected()

        fun onNetworkDisconnected()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo

        if (activeNetwork != null && activeNetwork.isConnected) {
            listener.onNetworkConnected() // Internet is connected
        } else {
            listener.onNetworkDisconnected() // No internet connection
        }
    }

    companion object {
        // Register the receiver
        fun registerReceiver(context: Context, receiver: NetworkChangeReceiver?) {
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(receiver, filter)
        }

        // Unregister the receiver
        fun unregisterReceiver(context: Context, receiver: NetworkChangeReceiver?) {
            context.unregisterReceiver(receiver)
        }
    }
}
