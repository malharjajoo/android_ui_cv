package com.beraldo.hpe;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.beraldo.hpe.dlib.Constants;
import com.beraldo.hpe.utils.FileUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.ViewById;

import java.io.File;

import hugo.weaving.DebugLog;

public class SessionCountDown extends AppCompatActivity {

    private String debugTag ;

    private Timer timer;
    private Button resetButton;

    // Create countdown timer (time is in millisec)
    private long countdown_ms = 5000;
    private static int OVERLAY_PERMISSION_REQ_CODE = 1;


    // Noise permission
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};


    private static final int REQUEST_CODE_PERMISSION = 3;
    private static final String TAG = "Permissions";



    // Storage Permissions
    private static String[] PERMISSIONS_REQ = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };



    public class Timer
    {
        private long countDownInterval = 1000;
        public TextView countdownTextview;
        private CountDownTimer countDownTimer;
        private long timeLeft;
        private long countdown;
        private boolean isRunning;

        public Timer(long countdown)
        {
            this.isRunning = false;
            this.countdown = countdown;
            this.timeLeft = countdown;
            this.countdownTextview = (TextView) findViewById(R.id.text_view_countdown);

            //textview is usually set to 00:00, hence update with countdown value.
            this.updateTextView();
        }

        public void startTimer()
        {
            this.countDownTimer = new CountDownTimer(timeLeft,countDownInterval) {
                @Override
                public void onTick(long millisUntilFinished) {

                    timeLeft = millisUntilFinished ;
                    updateTextView();
                }

                @Override
                public void onFinish()
                {
                    isRunning = false;
                    Log.d(debugTag,"Finished countdown timer !!...");
                    openCameraActivity();
                }

            }.start();

            this.isRunning = true;

        }

        public void updateTextView()
        {
            int minutes = (int) (timeLeft / 1000) / 60;
            int seconds = (int) (timeLeft / 1000) % 60;

            String timeLeftFormatted = String.format( "%02d:%02d", minutes, seconds);

            this.countdownTextview.setText(timeLeftFormatted);
        }

        public void cancelTimer()
        {
            this.countDownTimer.cancel();
        }
        // on reset button click
        public void resetTimer()
        {
            this.countDownTimer.cancel();
            this.timeLeft = countdown;
            updateTextView();
            this.countDownTimer.start();
        }


        /*
        public void saveInstanceState(Bundle outstate)
        {
            //outstate.putLong("timeLeft",)
        }
        */
    }


    //===============================

    public void initializeButtons()
    {
        this.resetButton.setOnClickListener(new View.OnClickListener(){

            public void onClick(View view){
                timer.resetTimer();
            }

        });
    }


    // Handle Restoring state when orientation is changed.
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_count_down);
        this.debugTag = getResources().getString(R.string.debugTag);


        // Just use hugo to print log
        isExternalStorageWritable();
        isExternalStorageReadable();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // For API 23+ you need to request the read/write permissions even if they are already in your manifest.
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;

        if (currentapiVersion >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        }


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




        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
            }
        }



        this.timer = new Timer(countdown_ms);

        // cache XML layout elements..
        this.resetButton = (Button)findViewById(R.id.button_reset);

        initializeButtons();
        this.timer.startTimer();
        /*
        // Used when mobile changes orientation (from vertical to landscape)
        if (savedInstanceState != null) {
            this.timer.restoreInstanceState(savedInstanceState);
        }
        */

    }


    // make sure to do this otherwise next activity will be triggered.
    @Override
    public void onPause()
    {
        super.onPause();
        this.timer.cancelTimer();
    }




    /*
    // handle Saving state when orientation is changed.
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        this.timer.saveInstanceState(outState);
    }
    */

    public void openCameraActivity()
    {
        Log.d(debugTag, "Opening Camera activity ...");
        startActivity(new Intent(this, CameraActivity.class));

    }



    //===================== Permissions ==================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this.getApplicationContext())) {
                    Toast.makeText(this, "PermissionsActivity\", \"SYSTEM_ALERT_WINDOW, permission not granted...", Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);

                }
            }
        }
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

    /**
     * Checks if the app has permission to write to device storage or open camera
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    @DebugLog
    private static boolean verifyPermissions(Activity activity) {
        // Check if we have write permission
        int write_permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int read_permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        int camera_permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA);
        int audio_permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.RECORD_AUDIO);

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



}