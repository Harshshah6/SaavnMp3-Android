package com.harsh.shah.saavnmp3.activities

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.databinding.ActivitySettingsBinding
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.harsh.shah.saavnmp3.utils.customview.MaterialCustomSwitch.OnCheckChangeListener

class SettingsActivity : AppCompatActivity() {
    var binding: ActivitySettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        val settingsSharedPrefManager = SettingsSharedPrefManager(this)
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)

        binding!!.downloadOverCellular.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                settingsSharedPrefManager.downloadOverCellular = isChecked
            }
        })
        binding!!.highQualityTrack.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                settingsSharedPrefManager.highQualityTrack = isChecked
            }
        })
        binding!!.storeInCache.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                settingsSharedPrefManager.storeInCache = isChecked
            }
        })
        binding!!.explicit.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                settingsSharedPrefManager.explicit = isChecked
            }
        })

        binding!!.downloadOverCellular.setChecked(settingsSharedPrefManager.downloadOverCellular)
        binding!!.highQualityTrack.setChecked(settingsSharedPrefManager.highQualityTrack)
        binding!!.storeInCache.setChecked(settingsSharedPrefManager.storeInCache)
        binding!!.explicit.setChecked(settingsSharedPrefManager.explicit)

        binding!!.themeChipGroup.setOnCheckedChangeListener(RadioGroup.OnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            settingsSharedPrefManager.theme =
                if (checkedId == R.id.dark) "dark" else if (checkedId == R.id.light) "light" else "system"
            BaseApplicationClass.updateTheme()
        })

        binding!!.clearCache.setOnClickListener(View.OnClickListener { v: View? ->
            AlertDialog.Builder(this)
                .setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear the cache?")
                .setPositiveButton(
                    "Yes",
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        sharedPreferenceManager.clearOldPrefsAsync(
                            this@SettingsActivity,
                            null
                        )
                    })
                .setNegativeButton("No", null)
                .show()
        })

        binding!!.themeChipGroup.check(if (settingsSharedPrefManager.theme == "dark") R.id.dark else if (settingsSharedPrefManager.theme == "light") R.id.light else R.id.system)
    }

    fun backPress(view: View?) {
        finish()
    }

    class SettingsSharedPrefManager(context: Context) {
        var sharedPreferences: SharedPreferences

        init {
            sharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)
        }

        var downloadOverCellular: Boolean
            get() = sharedPreferences.getBoolean("download_over_cellular", true)
            set(value) {
                sharedPreferences.edit().putBoolean("download_over_cellular", value).apply()
            }

        var highQualityTrack: Boolean
            get() = sharedPreferences.getBoolean("high_quality_track", true)
            set(value) {
                sharedPreferences.edit().putBoolean("high_quality_track", value).apply()
            }

        var storeInCache: Boolean
            get() = sharedPreferences.getBoolean("store_in_cache", true)
            set(value) {
                sharedPreferences.edit().putBoolean("store_in_cache", value).apply()
            }

        var explicit: Boolean
            get() = sharedPreferences.getBoolean("explicit", true)
            set(value) {
                sharedPreferences.edit().putBoolean("explicit", value).apply()
            }

        var theme: String?
            get() = sharedPreferences.getString("theme", "system")
            set(theme) {
                sharedPreferences.edit().putString("theme", theme).apply()
            }
    }
}
