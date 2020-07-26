package com.mahafuz.fatak2.Fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import com.mahafuz.fatak2.MainActivity;
import com.mahafuz.fatak2.R;
import com.nbsp.materialfilepicker.MaterialFilePicker;

import java.io.File;
import java.util.regex.Pattern;


public class Home extends Fragment {
    private static final int FILE_PICKER_REQUEST_CODE = 1;
    private static final int RESULT_OK = 1;
    MainActivity activity;


    private String File_EXTERNAL_STORAGE_FOLDER = "File";

    public Home(MainActivity activity) {
        this.activity = activity;
    }

    LinearLayout customSend,customReceive,customApps;
    public Home() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    //
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        customApps = view.findViewById(R.id.customApps);
        customReceive = view.findViewById(R.id.customSave);
        customSend = view.findViewById(R.id.customSend);


        customSend.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View view) {
                activity.findPeer();

                File externalStorage= Environment.getExternalStorageDirectory();

                File folderPath= new File(externalStorage,File_EXTERNAL_STORAGE_FOLDER);
                new MaterialFilePicker()
                        .withSupportFragment(Home.this)
                        .withRequestCode(FILE_PICKER_REQUEST_CODE)
                        .withHiddenFiles(true)
                        .start();
            }
        });



        customReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                assert getFragmentManager() != null;
                getFragmentManager().beginTransaction().replace(R.id.mainHome,new SacnnerFragment(activity))
                       .addToBackStack("Home")
                       .commit();
            }
        });

    }
    @Override
     void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra();
            // Do anything with file
        }
    }
}