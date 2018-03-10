package com.beraldo.hpe;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.Service;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import com.google.android.gms.wearable.DataEvent;

import java.util.List;


public class HeartActivity extends WearableActivity implements HeartBeatService.OnChangeListener {

    private static final String TAG = "HeartActivity";
    private static final String DB_TAG = "Start Condition";
    private TextView mTextViewHeart;

    Intent mServiceIntent;
    private HeartBeatService mSensorService;
    Context ctx;
    public Context getCtx(){
        return ctx;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        setContentView(R.layout.activity_heart);
        mSensorService = new HeartBeatService(getCtx());
        mServiceIntent = new Intent(getCtx(),mSensorService.getClass());
        if(!isMyServiceRunning(mSensorService.getClass())){
            startService(mServiceIntent);
        }
        mTextViewHeart = (TextView) findViewById(R.id.text);
        Log.i(TAG, "LISTENER REGISTERED.");
        mTextViewHeart.setText("Please Wait...");
        bindService(new Intent(HeartActivity.this, HeartBeatService.class), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.d(TAG, "connected to service.");
                // set our change listener to get change events
                ((HeartBeatService.HeartBeatServiceBinder)service).setChangeListener(HeartActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Service.BIND_AUTO_CREATE);

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    @Override
    protected void onDestroy() {
        stopService(mServiceIntent);
        Log.i("MAINACT", "onDestroy!");
        super.onDestroy();
    }

    /**
     * Continuous sensor monitoring with onResume()
     */
    protected void onResume() {
        super.onResume();
    }

    /**
     * Updates the TextView on the Watch with a numeric value representing HR
     */
    public void onValueChanged(final int newValue) {
        // will be called by the service whenever the heartbeat value changes

        mTextViewHeart.setText(String.valueOf(newValue));
        Log.d("mhml","Value = " + newValue);

    }

}