package com.harsh.shah.saavnmp3.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.harsh.shah.saavnmp3.R
import com.harsh.shah.saavnmp3.adapters.SavedLibrariesAdapter
import com.harsh.shah.saavnmp3.databinding.ActivitySavedLibrariesBinding
import com.harsh.shah.saavnmp3.databinding.AddNewLibraryBottomSheetBinding
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries.Library
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import com.harsh.shah.saavnmp3.utils.MiniPlayerHelper
import com.harsh.shah.saavnmp3.utils.attachSnapHelper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date

class SavedLibrariesActivity : AppCompatActivity() {
    var binding: ActivitySavedLibrariesBinding? = null
    var savedLibraries: SavedLibraries? = null

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val sharedPreferenceManager = SharedPreferenceManager.getInstance(this)
                val savedLibrariesObj = sharedPreferenceManager.savedLibrariesData
                val exportMap = HashMap<String, Any?>()
                exportMap["saved_libraries"] = savedLibrariesObj

                val detailsMap = HashMap<String, String>()
                savedLibrariesObj?.lists?.forEach { lib ->
                    if (lib != null && lib.id != null) {
                        val detailJson = sharedPreferenceManager.getJson(lib.id)
                        if (!detailJson.isNullOrEmpty()) {
                            detailsMap[lib.id] = detailJson
                        }
                    }
                }
                exportMap["libraries_details"] = detailsMap

                val exportJson = com.google.gson.Gson().toJson(exportMap)
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(exportJson.toByteArray())
                }
                Snackbar.make(binding!!.getRoot(), "Library exported successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("SavedLibrariesActivity", "Export failed", e)
                Snackbar.make(binding!!.getRoot(), "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    val importJson = stringBuilder.toString()
                    val gson = com.google.gson.Gson()
                    val type = object : com.google.gson.reflect.TypeToken<HashMap<String, Any?>>() {}.type
                    val importMap = gson.fromJson<HashMap<String, Any?>>(importJson, type)

                    val savedLibrariesJson = gson.toJson(importMap["saved_libraries"])
                    val importedSavedLibraries = gson.fromJson(savedLibrariesJson, SavedLibraries::class.java)

                    val detailsJson = gson.toJson(importMap["libraries_details"])
                    val importedDetails = gson.fromJson<HashMap<String, String>>(detailsJson, object : com.google.gson.reflect.TypeToken<HashMap<String, String>>() {}.type)

                    val sharedPreferenceManager = SharedPreferenceManager.getInstance(this)
                    val currentSavedLibraries = sharedPreferenceManager.savedLibrariesData ?: SavedLibraries(ArrayList())
                    val currentLists = currentSavedLibraries.lists ?: ArrayList()

                    importedSavedLibraries?.lists?.forEach { importedLib ->
                        if (importedLib != null && importedLib.id != null) {
                            if (currentLists.none { it?.id == importedLib.id }) {
                                currentLists.add(importedLib)
                            }
                            val detailJson = importedDetails?.get(importedLib.id)
                            if (!detailJson.isNullOrEmpty()) {
                                sharedPreferenceManager.putJson(importedLib.id, detailJson)
                            }
                        }
                    }

                    sharedPreferenceManager.savedLibrariesData = SavedLibraries(currentLists)
                    showData(sharedPreferenceManager)
                    Snackbar.make(binding!!.getRoot(), "Library imported successfully", Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SavedLibrariesActivity", "Import failed", e)
                Snackbar.make(binding!!.getRoot(), "Import failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedLibrariesBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())
        MiniPlayerHelper.initMiniPlayer(this)

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        binding!!.recyclerView.attachSnapHelper()
        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.recyclerView,
            OverScrollDecoratorHelper.ORIENTATION_VERTICAL
        )

        binding!!.addNewLibrary.setOnClickListener(View.OnClickListener {
            val addNewLibraryBottomSheetBinding =
                AddNewLibraryBottomSheetBinding.inflate(layoutInflater)
            val bottomSheetDialog = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
            bottomSheetDialog.setContentView(addNewLibraryBottomSheetBinding.getRoot())
            addNewLibraryBottomSheetBinding.cancel.setOnClickListener {
                bottomSheetDialog.dismiss()
            }
            addNewLibraryBottomSheetBinding.create.setOnClickListener(View.OnClickListener {
                val name = addNewLibraryBottomSheetBinding.edittext.getText().toString()
                if (name.isEmpty()) {
                    addNewLibraryBottomSheetBinding.edittext.error = "Name cannot be empty"
                    return@OnClickListener
                }
                addNewLibraryBottomSheetBinding.edittext.error = null
                Log.i("SavedLibrariesActivity", "BottomSheetDialog_create: $name")

                val currentTime = System.currentTimeMillis().toString()

                val library = Library(
                    "#" + currentTime,
                    true,
                    false,
                    name,
                    "",
                    "Created on :- " + formatMillis(currentTime.toLong()),
                    ArrayList<Library.Songs?>()
                )

                val sharedPreferenceManager: SharedPreferenceManager =
                    SharedPreferenceManager.getInstance(this)
                sharedPreferenceManager.addLibraryToSavedLibraries(library)
                Snackbar.make(
                    binding!!.getRoot(),
                    "Library added successfully",
                    Snackbar.LENGTH_SHORT
                ).show()


                bottomSheetDialog.dismiss()
                showData(sharedPreferenceManager)
            })
            bottomSheetDialog.show()
        })

        binding?.importLibrary?.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        binding?.exportLibrary?.setOnClickListener {
            exportLauncher.launch("saavn_library.json")
        }

        showData()
    }

    private fun formatMillis(millis: Long): String {
        val date = Date(millis)
        @SuppressLint("SimpleDateFormat") val simpleDateFormat =
            SimpleDateFormat("MM-dd-yyyy HH:mm a")
        return simpleDateFormat.format(date)
    }


    private fun showData(
        sharedPreferenceManager: SharedPreferenceManager = SharedPreferenceManager.getInstance(
            this
        )
    ) {
        val libs = sharedPreferenceManager.savedLibrariesData
        savedLibraries = libs
        binding!!.emptyListTv.visibility = if (libs == null) View.VISIBLE else View.GONE
        if (libs != null) binding!!.recyclerView.setAdapter(
            SavedLibrariesAdapter(
                libs.lists ?: mutableListOf()
            )
        )
    }

    override fun onResume() {
        super.onResume()
        showData()
        MiniPlayerHelper.onActivityResume(this)
    }

    override fun onPause() {
        super.onPause()
        MiniPlayerHelper.onActivityPause(this)
    }

    fun backPress(view: View?) {
        finish()
    }
}
