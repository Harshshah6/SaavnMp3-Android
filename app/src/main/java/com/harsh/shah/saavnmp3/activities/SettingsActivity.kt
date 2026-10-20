package com.harsh.shah.saavnmp3.activities

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.harsh.shah.saavnmp3.BaseApplicationClass
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.databinding.ActivitySettingsBinding
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import com.harsh.shah.saavnmp3.utils.customview.MaterialCustomSwitch.OnCheckChangeListener
import com.harsh.shah.saavnmp3.utils.MiniPlayerHelper
import com.harsh.shah.saavnmp3.utils.ThemeManager
import com.harsh.shah.saavnmp3.utils.TrackDownloader
import androidx.core.content.edit
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.harsh.shah.saavnmp3.databinding.SleepTimerBottomSheetBinding
import com.harsh.shah.saavnmp3.utils.SleepTimerManager
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    var binding: ActivitySettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(null)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        MiniPlayerHelper.initMiniPlayer(this)
        val settingsSharedPrefManager = SettingsSharedPrefManager(this)
        val sharedPreferenceManager: SharedPreferenceManager =
            SharedPreferenceManager.getInstance(this)

        // Initialize switch states before listeners
        binding!!.downloadOverCellular.setChecked(settingsSharedPrefManager.downloadOverCellular)
        binding!!.highQualityTrack.setChecked(settingsSharedPrefManager.highQualityTrack)
        binding!!.storeInCache.setChecked(settingsSharedPrefManager.storeInCache)
        binding!!.explicit.setChecked(settingsSharedPrefManager.explicit)
        binding!!.playInBackground?.setChecked(settingsSharedPrefManager.playInBackground)

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
        binding!!.playInBackground?.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                settingsSharedPrefManager.playInBackground = isChecked
            }
        })

        // Autoplay Recommendations Switch
        binding!!.autoplaySwitch?.setChecked(sharedPreferenceManager.isAutoplayEnabled)
        binding!!.autoplaySwitch?.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                sharedPreferenceManager.isAutoplayEnabled = isChecked
            }
        })

        // Sleep Timer Launcher
        updateSleepTimerText()
        binding!!.sleepTimerSetting?.setOnClickListener {
            showSleepTimerBottomSheet()
        }

        // Theme Mode (System, Dark, Light)
        val themeCheckedId = if (settingsSharedPrefManager.theme == "dark") R.id.dark
            else if (settingsSharedPrefManager.theme == "light") R.id.light
            else R.id.system
        binding!!.themeChipGroup.check(themeCheckedId)
        binding!!.themeChipGroup.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            val newTheme = if (checkedId == R.id.dark) "dark" else if (checkedId == R.id.light) "light" else "system"
            if (settingsSharedPrefManager.theme != newTheme) {
                settingsSharedPrefManager.theme = newTheme
                ThemeManager.setThemeMode(this, newTheme)
                BaseApplicationClass.updateTheme()
            }
        }

        // Dynamic Color (Material You)
        val isDynamic = ThemeManager.isDynamicColorEnabled(this)
        binding?.dynamicColorSwitch?.setChecked(isDynamic)
        binding?.paletteSection?.visibility = if (isDynamic) View.GONE else View.VISIBLE
        binding?.dynamicColorSwitch?.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                if (ThemeManager.isDynamicColorEnabled(this@SettingsActivity) != isChecked) {
                    ThemeManager.setDynamicColorEnabled(this@SettingsActivity, isChecked)
                    binding?.paletteSection?.visibility = if (isChecked) View.GONE else View.VISIBLE
                    recreate()
                }
            }
        })

        // Palette Selector
        val currentPalette = ThemeManager.getThemePalette(this)
        when (currentPalette) {
            "ocean" -> binding?.paletteChipGroup?.check(R.id.palette_ocean)
            "purple" -> binding?.paletteChipGroup?.check(R.id.palette_purple)
            "crimson" -> binding?.paletteChipGroup?.check(R.id.palette_crimson)
            "amber" -> binding?.paletteChipGroup?.check(R.id.palette_amber)
            else -> binding?.paletteChipGroup?.check(R.id.palette_emerald)
        }
        binding?.paletteChipGroup?.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val newPalette = when (checkedIds[0]) {
                    R.id.palette_ocean -> "ocean"
                    R.id.palette_purple -> "purple"
                    R.id.palette_crimson -> "crimson"
                    R.id.palette_amber -> "amber"
                    else -> "emerald"
                }
                if (ThemeManager.getThemePalette(this) != newPalette) {
                    ThemeManager.setThemePalette(this, newPalette)
                    recreate()
                }
            }
        }

        // AMOLED Pure Black
        binding?.amoledSwitch?.setChecked(ThemeManager.isAmoledEnabled(this))
        binding?.amoledSwitch?.setOnCheckChangeListener(object : OnCheckChangeListener {
            override fun onCheckChanged(isChecked: Boolean) {
                if (ThemeManager.isAmoledEnabled(this@SettingsActivity) != isChecked) {
                    ThemeManager.setAmoledEnabled(this@SettingsActivity, isChecked)
                    recreate()
                }
            }
        })

        // Clear Cache with MaterialAlertDialogBuilder
        binding!!.clearCache.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Clear Cache")
                .setMessage("Are you sure you want to clear all cached data?")
                .setPositiveButton("Yes") { _: DialogInterface?, _: Int ->
                    sharedPreferenceManager.clearOldPrefsAsync(this@SettingsActivity, null)
                }
                .setNegativeButton("No", null)
                .show()
        }

        // Download Format selector (MP3 / M4A / OGG)
        val currentFormat = settingsSharedPrefManager.downloadFormat
        val formatCheckedId = when (currentFormat) {
            TrackDownloader.DownloadFormat.MP3.extension -> R.id.format_mp3
            TrackDownloader.DownloadFormat.OGG.extension -> R.id.format_ogg
            else -> R.id.format_m4a
        }
        binding!!.downloadFormatChipGroup?.check(formatCheckedId)
        binding!!.downloadFormatChipGroup?.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val newExt = when (checkedIds[0]) {
                    R.id.format_mp3 -> TrackDownloader.DownloadFormat.MP3.extension
                    R.id.format_ogg -> TrackDownloader.DownloadFormat.OGG.extension
                    else -> TrackDownloader.DownloadFormat.M4A.extension
                }
                settingsSharedPrefManager.downloadFormat = newExt
            }
        }
    }

    private fun updateSleepTimerText() {
        if (SleepTimerManager.isTimerActive) {
            binding!!.sleepTimerStatusText?.text = "Timer active: ${SleepTimerManager.getFormattedRemainingTime()} remaining"
        } else {
            binding!!.sleepTimerStatusText?.text = "Automatically stop music when you fall asleep."
        }
    }

    private fun showSleepTimerBottomSheet() {
        val bottomSheet = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
        val sheetBinding = SleepTimerBottomSheetBinding.inflate(layoutInflater)
        bottomSheet.setContentView(sheetBinding.root)

        fun refreshTimerUI() {
            if (SleepTimerManager.isTimerActive) {
                sheetBinding.activeTimerCard.visibility = View.VISIBLE
                sheetBinding.remainingTimeText.text = SleepTimerManager.getFormattedRemainingTime()
            } else {
                sheetBinding.activeTimerCard.visibility = View.GONE
            }
            updateSleepTimerText()
        }

        refreshTimerUI()

        val timerListener = object : SleepTimerManager.SleepTimerListener {
            override fun onTimerTick(remainingSeconds: Long) {
                sheetBinding.remainingTimeText.text = SleepTimerManager.getFormattedRemainingTime()
                updateSleepTimerText()
            }

            override fun onTimerFinished() {
                refreshTimerUI()
            }

            override fun onTimerCancelled() {
                refreshTimerUI()
            }
        }

        SleepTimerManager.addListener(timerListener)
        bottomSheet.setOnDismissListener {
            SleepTimerManager.removeListener(timerListener)
        }

        sheetBinding.timerOption15.setOnClickListener {
            SleepTimerManager.startTimer(15)
            refreshTimerUI()
        }
        sheetBinding.timerOption30.setOnClickListener {
            SleepTimerManager.startTimer(30)
            refreshTimerUI()
        }
        sheetBinding.timerOption45.setOnClickListener {
            SleepTimerManager.startTimer(45)
            refreshTimerUI()
        }
        sheetBinding.timerOption60.setOnClickListener {
            SleepTimerManager.startTimer(60)
            refreshTimerUI()
        }
        sheetBinding.timerOptionEndOfTrack.setOnClickListener {
            SleepTimerManager.startEndOfTrackTimer()
            refreshTimerUI()
        }

        sheetBinding.btnAdd5.setOnClickListener {
            SleepTimerManager.addMinutes(5)
            refreshTimerUI()
        }

        sheetBinding.btnCancelTimer.setOnClickListener {
            SleepTimerManager.cancelTimer()
            refreshTimerUI()
            Toast.makeText(this, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
        }

        bottomSheet.show()
    }

    override fun onResume() {
        super.onResume()
        updateSleepTimerText()
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    fun backPress(view: View?) {
        finish()
    }

    class SettingsSharedPrefManager(context: Context) {
        var sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", MODE_PRIVATE)

        var downloadOverCellular: Boolean
            get() = sharedPreferences.getBoolean("download_over_cellular", true)
            set(value) {
                sharedPreferences.edit { putBoolean("download_over_cellular", value) }
            }

        var highQualityTrack: Boolean
            get() = sharedPreferences.getBoolean("high_quality_track", true)
            set(value) {
                sharedPreferences.edit { putBoolean("high_quality_track", value) }
            }

        var storeInCache: Boolean
            get() = sharedPreferences.getBoolean("store_in_cache", true)
            set(value) {
                sharedPreferences.edit { putBoolean("store_in_cache", value) }
            }

        var explicit: Boolean
            get() = sharedPreferences.getBoolean("explicit", true)
            set(value) {
                sharedPreferences.edit { putBoolean("explicit", value) }
            }

        var playInBackground: Boolean
            get() = sharedPreferences.getBoolean("play_in_background", true)
            set(value) {
                sharedPreferences.edit { putBoolean("play_in_background", value) }
            }

        var downloadFormat: String
            get() = sharedPreferences.getString("download_format",
                TrackDownloader.DownloadFormat.M4A.extension) ?:
                TrackDownloader.DownloadFormat.M4A.extension
            set(value) {
                sharedPreferences.edit { putString("download_format", value) }
            }

        var theme: String?
            get() = sharedPreferences.getString("theme", "system")
            set(theme) {
                sharedPreferences.edit { putString("theme", theme) }
            }
    }
}
