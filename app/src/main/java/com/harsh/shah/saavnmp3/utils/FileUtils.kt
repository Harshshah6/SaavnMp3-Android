package com.harsh.shah.saavnmp3.utils

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

object FileUtils {
    fun readFileToByteArray(file: File?): ByteArray? {
        try {
            FileInputStream(file).use { fis ->
                ByteArrayOutputStream().use { bos ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while ((fis.read(buffer).also { bytesRead = it }) != -1) {
                        bos.write(buffer, 0, bytesRead)
                    }
                    return bos.toByteArray()
                }
            }
        } catch (e: IOException) {
            Log.e("FileUtils", "Error reading file to byte array", e)
        }
        return null
    }
}
