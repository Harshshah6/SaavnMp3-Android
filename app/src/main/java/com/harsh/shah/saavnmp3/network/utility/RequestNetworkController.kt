package com.harsh.shah.saavnmp3.network.utility

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class RequestNetworkController {
    protected var client: OkHttpClient? = null

    private fun getOkHttpClient(): OkHttpClient {
        if (client == null) {
            val builder = OkHttpClient.Builder()

            try {
                val trustAllCerts: Array<TrustManager> =
                    arrayOf<TrustManager>(object : X509TrustManager {
                        @Throws(CertificateException::class)
                        override fun checkClientTrusted(
                            chain: Array<X509Certificate?>?,
                            authType: String?
                        ) {
                        }

                        @Throws(CertificateException::class)
                        override fun checkServerTrusted(
                            chain: Array<X509Certificate?>?,
                            authType: String?
                        ) {
                        }

                        override fun getAcceptedIssuers(): Array<X509Certificate?> {
                            return arrayOf<X509Certificate?>()
                        }
                    }
                    )

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, SecureRandom())
                val sslSocketFactory = sslContext.getSocketFactory()
                builder.sslSocketFactory(
                    sslSocketFactory,
                    (trustAllCerts[0] as javax.net.ssl.X509TrustManager?)!!
                )
                builder.connectTimeout(SOCKET_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                builder.readTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                builder.writeTimeout(READ_TIMEOUT.toLong(), TimeUnit.MILLISECONDS)
                builder.hostnameVerifier(object : HostnameVerifier {
                    override fun verify(hostname: String?, session: SSLSession?): Boolean {
                        return true
                    }
                })
            } catch (e: Exception) {
            }

            client = builder.build()
        }

        return client!!
    }

    fun execute(
        requestNetwork: RequestNetwork, method: String, url: String, tag: String?,
        requestListener: RequestNetwork.RequestListener
    ) {
        val reqBuilder = Request.Builder()
        val headerBuilder = Headers.Builder()

        if (requestNetwork.headers?.isNotEmpty() == true) {
            val headers = requestNetwork.headers!!

            for (header in headers.entries) {
                headerBuilder.add(header.key!!, header.value.toString())
            }
        }

        try {
            if (requestNetwork.requestType == REQUEST_PARAM) {
                if (method == GET) {
                    val httpBuilder: HttpUrl.Builder?

                    try {
                        httpBuilder = url.toHttpUrl().newBuilder()
                    } catch (ne: NullPointerException) {
                        throw NullPointerException("unexpected url: " + url)
                    }

                    if (requestNetwork.params?.isNotEmpty() == true) {
                        val params = requestNetwork.params!!

                        for (param in params.entries) {
                            httpBuilder.addQueryParameter(param.key!!, param.value.toString())
                        }
                    }

                    reqBuilder.url(httpBuilder.build()).headers(headerBuilder.build()).get()
                } else {
                    val formBuilder = FormBody.Builder()
                    if (requestNetwork.params?.isNotEmpty() == true) {
                        val params = requestNetwork.params!!

                        for (param in params.entries) {
                            formBuilder.add(param.key!!, param.value.toString())
                        }
                    }

                    val reqBody: RequestBody = formBuilder.build()

                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody)
                }
            } else {
                val reqBody = RequestBody.create(
                    "application/json".toMediaTypeOrNull(),
                    Gson().toJson(requestNetwork.params)
                )

                if (method == GET) {
                    reqBuilder.url(url).headers(headerBuilder.build()).get()
                } else {
                    reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody)
                }
            }

            val req = reqBuilder.build()

            getOkHttpClient().newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Handler(Looper.getMainLooper()).post(Runnable {
                        requestListener.onErrorResponse(tag, e.message)
                    })
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()?.trim { it <= ' ' }
                    Handler(Looper.getMainLooper()).post(Runnable {
                        try {
                            val b = response.headers
                            val map = HashMap<String?, Any?>()
                            for (s in b.names()) {
                                map.put(s, if (b.get(s) != null) b.get(s) else "null")
                            }
                            requestListener.onResponse(tag, responseBody, map)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    })
                }
            })
        } catch (e: Exception) {
            requestListener.onErrorResponse(tag, e.message)
        }
    }

    companion object {
        const val GET: String = "GET"
        const val POST: String = "POST"
        const val PUT: String = "PUT"
        const val DELETE: String = "DELETE"

        const val REQUEST_PARAM: Int = 0
        const val REQUEST_BODY: Int = 1

        private const val SOCKET_TIMEOUT = 15000
        private const val READ_TIMEOUT = 25000

        private var mInstance: RequestNetworkController? = null

        @get:Synchronized
        val instance: RequestNetworkController
            get() {
                if (mInstance == null) {
                    mInstance = RequestNetworkController()
                }
                return mInstance!!
            }
    }
}
