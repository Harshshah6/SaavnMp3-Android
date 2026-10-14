package com.harsh.shah.saavnmp3

import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.harsh.shah.saavnmp3.activities.SettingsActivity.SettingsSharedPrefManager
import com.harsh.shah.saavnmp3.utils.MusicPlayerManager
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager

open class BaseApplicationClass : Application() {
    private val TAG = "ApplicationClass"

    override fun onCreate() {
        super.onCreate()

        sharedPreferenceManager = SharedPreferenceManager.getInstance(this)
        com.harsh.shah.saavnmp3.utils.ThemeManager.init(this)
        MusicPlayerManager.init(this)

        sharedPreferenceManager!!.migrateFromOldPrefs(this) { sharedPreferenceManager!!.clearOldPrefsAsync(this, null) }
    }

    companion object {
        var sharedPreferenceManager: SharedPreferenceManager? = null
        var currentActivity: Activity? = null

        fun updateTheme() {
            val act = currentActivity ?: return
            com.harsh.shah.saavnmp3.utils.ThemeManager.applyNightMode(
                com.harsh.shah.saavnmp3.utils.ThemeManager.getThemeMode(act)
            )
        }
    }
}
