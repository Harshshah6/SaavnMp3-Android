package com.harsh.shah.saavnmp3.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.harsh.shah.saavnmp3.R;
import com.harsh.shah.saavnmp3.adapters.SavedLibrariesAdapter;
import com.harsh.shah.saavnmp3.databinding.ActivitySavedLibrariesBinding;
import com.harsh.shah.saavnmp3.databinding.AddNewLibraryBottomSheetBinding;
import com.harsh.shah.saavnmp3.records.sharedpref.SavedLibraries;
import com.harsh.shah.saavnmp3.utils.SharedPreferenceManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SavedLibrariesActivity extends AppCompatActivity {

    ActivitySavedLibrariesBinding binding;
    SavedLibraries savedLibraries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedLibrariesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));

        binding.addNewLibrary.setOnClickListener(view -> {
            AddNewLibraryBottomSheetBinding addNewLibraryBottomSheetBinding = AddNewLibraryBottomSheetBinding.inflate(getLayoutInflater());
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.MyBottomSheetDialogTheme);
            bottomSheetDialog.setContentView(addNewLibraryBottomSheetBinding.getRoot());
            addNewLibraryBottomSheetBinding.cancel.setOnClickListener(view1 -> {
                bottomSheetDialog.dismiss();
            });
            addNewLibraryBottomSheetBinding.create.setOnClickListener(view1 -> {
                final String name = addNewLibraryBottomSheetBinding.edittext.getText().toString();
                if(name.isEmpty()) {
                    addNewLibraryBottomSheetBinding.edittext.setError("Name cannot be empty");
                    return;
                }
                addNewLibraryBottomSheetBinding.edittext.setError(null);
                Log.i("SavedLibrariesActivity", "BottomSheetDialog_create: " + name);

                final String currentTime = String.valueOf(System.currentTimeMillis());

                SavedLibraries.Library library = new SavedLibraries.Library(
                        "#"+currentTime,
                        true,
                        false,
                        name,
                        "",
                        "Created on :- " + formatMillis(Long.parseLong(currentTime)),
                        new ArrayList<>()
                );

                final SharedPreferenceManager sharedPreferenceManager = SharedPreferenceManager.getInstance(this);
                sharedPreferenceManager.addLibraryToSavedLibraries(library);
                Snackbar.make(binding.getRoot(), "Library added successfully", Snackbar.LENGTH_SHORT).show();

                bottomSheetDialog.dismiss();
            });
            bottomSheetDialog.show();
        });

        showData();
    }

    private String formatMillis(long millis) {
        Date date = new Date(millis);
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm a");
        return simpleDateFormat.format(date);
    }

    private void showData() {
        savedLibraries = SharedPreferenceManager.getInstance(this).getSavedLibrariesData();
        binding.emptyListTv.setVisibility(savedLibraries == null ? View.VISIBLE : View.GONE);
        if(savedLibraries != null) binding.recyclerView.setAdapter(new SavedLibrariesAdapter(savedLibraries.lists()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        showData();
    }

    public void backPress(View view){
        finish();
    }
}