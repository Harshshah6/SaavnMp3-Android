package com.harsh.shah.saavnmp3.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
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
import java.text.SimpleDateFormat
import java.util.Date

class SavedLibrariesActivity : AppCompatActivity() {
    var binding: ActivitySavedLibrariesBinding? = null
    var savedLibraries: SavedLibraries? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedLibrariesBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        binding!!.recyclerView.setLayoutManager(LinearLayoutManager(this))
        OverScrollDecoratorHelper.setUpOverScroll(
            binding!!.recyclerView,
            OverScrollDecoratorHelper.ORIENTATION_VERTICAL
        )

        binding!!.addNewLibrary.setOnClickListener(View.OnClickListener { view: View? ->
            val addNewLibraryBottomSheetBinding =
                AddNewLibraryBottomSheetBinding.inflate(layoutInflater)
            val bottomSheetDialog = BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme)
            bottomSheetDialog.setContentView(addNewLibraryBottomSheetBinding.getRoot())
            addNewLibraryBottomSheetBinding.cancel.setOnClickListener(View.OnClickListener { view1: View? ->
                bottomSheetDialog.dismiss()
            })
            addNewLibraryBottomSheetBinding.create.setOnClickListener(View.OnClickListener { view1: View? ->
                val name = addNewLibraryBottomSheetBinding.edittext.getText().toString()
                if (name.isEmpty()) {
                    addNewLibraryBottomSheetBinding.edittext.error = "Name cannot be empty"
                    return@OnClickListener
                }
                addNewLibraryBottomSheetBinding.edittext.error = null
                Log.i("SavedLibrariesActivity", "BottomSheetDialog_create: " + name)

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
    }

    fun backPress(view: View?) {
        finish()
    }
}
