package com.harsh.shah.saavnmp3.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import com.harsh.shah.saavnmp3.network.utility.RequestNetwork
import com.harsh.shah.saavnmp3.network.utility.RequestNetworkController
import org.json.JSONObject

object UpdateUtil {

    fun checkForUpdates(context: Context, showToasts: Boolean = false) {
        val requestNetwork = RequestNetwork(context)
        requestNetwork.startRequestNetwork(
            RequestNetworkController.GET,
            "https://api.github.com/repos/Harshshah6/SaavnMp3-Android/releases/latest",
            "UPDATE_CHECK",
            object : RequestNetwork.RequestListener {
                override fun onResponse(tag: String?, response: String?, responseHeaders: HashMap<String?, Any?>?) {
                    try {
                        val jsonObject = JSONObject(response.toString())
                        val tagName = jsonObject.optString("tag_name", "")
                        val appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""

                        val latestVersionStr = tagName.replace("v", "", true)
                        val currentVersionStr = appVersion.replace("v", "", true)

                        if (isVersionGreater(latestVersionStr, currentVersionStr)) {
                            AlertDialog.Builder(context)
                                .setTitle("Update Available")
                                .setMessage("A new version ($tagName) is available on GitHub. Would you like to update?")
                                .setPositiveButton("Yes") { _, _ ->
                                    val htmlUrl = jsonObject.optString("html_url", "")
                                    if (htmlUrl.isNotEmpty()) {
                                        val intent = Intent(Intent.ACTION_VIEW)
                                        intent.data = htmlUrl.toUri()
                                        context.startActivity(intent)
                                    }
                                }
                                .setNegativeButton("Later", null)
                                .show()
                        } else {
                            if (showToasts) {
                                Toast.makeText(context, "You are on the latest version!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("UpdateUtil", "Failed to parse update info", e)
                        if (showToasts) {
                            Toast.makeText(context, "Failed to check for updates.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onErrorResponse(tag: String?, message: String?) {
                    android.util.Log.e("UpdateUtil", "Update check failed: $message")
                    if (showToasts) {
                        Toast.makeText(context, "Failed to check for updates.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun isVersionGreater(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }

        val length = kotlin.math.max(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
