package com.harsh.shah.saavnmp3.network.utility

import android.content.Context
import android.util.Log

class RequestNetwork(val activity: Context?) {
    var params: HashMap<String?, Any?>? = HashMap<String?, Any?>()
        private set
    var headers: HashMap<String?, Any?>? = HashMap<String?, Any?>()

    var requestType: Int = 0
        private set

    fun setParams(params: HashMap<String?, Any?>?, requestType: Int) {
        this.params = params
        this.requestType = requestType
    }

    fun startRequestNetwork(
        method: String?,
        url: String?,
        tag: String?,
        requestListener: RequestListener?
    ) {
        Log.i(
            "RequestNetwork.java", "startRequestNetwork: " +
                    "\tmethod: " + method +
                    "\turl: " + url +
                    "\tHeaders: " + this.headers +
                    "\tParams: " + this.params
        )
        RequestNetworkController.instance
            .execute(this, method ?: "", url ?: "", tag, requestListener!!)
    }

    interface RequestListener {
        fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?)

        fun onErrorResponse(tag: String?, message: String?)
    }
}
