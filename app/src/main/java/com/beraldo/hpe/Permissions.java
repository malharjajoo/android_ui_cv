package com.beraldo.hpe;


import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;


import java.io.File;

import com.beraldo.hpe.dlib.Constants;

import com.beraldo.hpe.utils.FileUtils;
import hugo.weaving.DebugLog;

@EActivity(R.layout.activity_permissions)
public class Permissions extends AppCompatActivity {

    // Noise permission
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;



    private static final int REQUEST_CODE_PERMISSION = 3;
    private static final String TAG = "Permissions";

    public static int mode = Constants.MODE_ITERATIVE;
    public static boolean saveFile = false;

    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    @ViewById(R.id.toolbar)
    protected Toolbar mToolbar;

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);
        int audio_permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);

        if (write_permission != PackageManager.PERMISSION_GRANTED ||
                read_permission != PackageManager.PERMISSION_GRANTED ||
                camera_permission != PackageManager.PERMISSION_GRANTED ||
                audio_permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_REQ,
                    REQUEST_CODE_PERMISSION
            );
            return false;
        } else {
            return true;
        }
    }


/*
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        //if (!permissionToRecordAccepted ) finish();

    }
*/


    /**
     * Called whenever he launch button gets clicked.
     * @param view
     */
    public void onLauchButtonClicked(View view) {
        startActivity(new Intent(this, CameraActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSupportActionBar(mToolbar);
        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }

        // Noise permission
        //ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);


        // Create main directory
        File sdcard = Environment.getExternalStorageDirectory();
        File main = new File(sdcard, "Selfear2");
        if(!main.exists()){
            Log.d(TAG, "Creating main directory in " + main.getAbsolutePath());
            if(!main.mkdir()) {
                Log.e(TAG, "Failed to create main directory in " + main.getAbsolutePath());
            }
        }
        FileUtils.savePreference(this, FileUtils.MAIN_DIR_PREFS_NAME, main.getAbsolutePath());

        // Create data directory
        File data = new File(main, "data");
        if(!data.exists()){
            Log.d(TAG, "Creating data directory in " + data.getAbsolutePath());
            if(!data.mkdir()) {
                Log.e(TAG, "Failed to create data directory in " + data.getAbsolutePath());
            }
        }
        FileUtils.savePreference(this, FileUtils.DATA_DIR_PREFS_NAME, data.getAbsolutePath());

        // Create detections directory
        File detections = new File(main, "detections");
        if(!detections.exists()){
            Log.d(TAG, "Creating detections directory in " + detections.getAbsolutePath());
            if(!detections.mkdir()) {
                Log.e(TAG, "Failed to create data directory in " + detections.getAbsolutePath());
            }
        }
        FileUtils.savePreference(this, FileUtils.DETECTIONS_DIR_PREFS_NAME, detections.getAbsolutePath());

        // Create params directory
        File params = new File(main, "params");
        if(!params.exists()){
            Log.d(TAG, "Creating detections directory in " + params.getAbsolutePath());
            if(!params.mkdir()) {
                Log.e(TAG, "Failed to create params directory in " + params.getAbsolutePath());
            }
        }
        FileUtils.savePreference(this, FileUtils.PARAMS_DIR_PREFS_NAME, params.getAbsolutePath());

    }

    @AfterViews
    protected void setupUI() {
        mToolbar.setTitle(getString(R.string.app_name));
    }

    /* Checks if external storage is available for read and write */
    @DebugLog
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    @DebugLog
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}