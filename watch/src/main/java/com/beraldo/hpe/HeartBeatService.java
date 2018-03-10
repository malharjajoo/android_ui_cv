package com.beraldo.hpe;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

public class HeartBeatService extends Service implements SensorEventListener, DataClient.OnDataChangedListener, CapabilityClient.OnCapabilityChangedListener {

    private SensorManager mSensorManager;
    private int currentValue=0;
    private static final String LOG_TAG = "MyHeart";
    private IBinder binder = new HeartBeatServiceBinder();
    private static final String HR_PATH = "/heartrate";
    private OnChangeListener onChangeListener;
    public int counter = 0;
    public HeartBeatService(Context applicationContext){
        super();
        Log.i("HERE","here I am");
    }

    public HeartBeatService(){
    }

    // interface to pass a heartbeat value to the implementing class
    public interface OnChangeListener {
        void onValueChanged(int newValue);
    }

    /**
     * Binder for this service. The binding activity passes a listener we send the heartbeat to.
     */
    public class HeartBeatServiceBinder extends Binder {
        public void setChangeListener(OnChangeListener listener) {
            onChangeListener = listener;
            // return currently known value
            listener.onValueChanged(currentValue);
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent,flags,startId);
        startTimer();
        return START_STICKY;
    }

    private Timer timer;
    private TimerTask timerTask;

    long oldTime = 0;
    public void startTimer(){
        //set a new timer
        timer = new Timer();

        //intialize the timer tasks job
        initalizeTimerTask();

        //schedule the timer to wake up every second
        timer.schedule(timerTask,1000,1000);

    }

    public void initalizeTimerTask(){
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.i("in Timer","in timer ++++"+(counter++));
            }
        };
    }
    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // register us as a sensor listener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        boolean res = mSensorManager.registerListener(this, mHeartRateSensor,  SensorManager.SENSOR_DELAY_UI);
        Log.d(LOG_TAG, " sensor registered: " + (res ? "yes" : "no"));
    }

    /**
     *The onDestroy method kills the sensors so that battery drainage is minimised.
     */

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        Log.d(LOG_TAG," sensor unregistered");
        Intent broadcastIntent = new Intent("com.testingwatch.RestartSensor");
        sendBroadcast(broadcastIntent);
        stoptimertask();
    }

    /**
     *The onSensorChanged function is the main logic of the heart beat sensor. It Waits for new values
     * and then displays them on the log in Android Studio.
     * TODO: Create a new function that can send it to a companion app.
     */

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // is this a heartbeat event and does it have data?
        if(sensorEvent.sensor.getType()==Sensor.TYPE_HEART_RATE && sensorEvent.values.length>0 ) {
            int newValue = Math.round(sensorEvent.values[0]);
            Log.d(LOG_TAG,sensorEvent.sensor.getName() + " changed to value=: " + newValue);
            // only do something if the value differs from the value before and the value is not 0.
            if(currentValue != newValue && newValue!=0) {
                // save the new value
                currentValue = newValue;
                // send the value to the listener
                if(onChangeListener!=null) {
                    Log.d(LOG_TAG,"sending new value to listener: " + newValue);
                    onChangeListener.onValueChanged(newValue);
                    final PutDataMapRequest putRequest = PutDataMapRequest.create(HR_PATH);
                    final DataMap map = putRequest.getDataMap();
                    map.putInt("Heart Rate", currentValue);

                    Wearable.getDataClient(this).putDataItem(putRequest.asPutDataRequest()).addOnSuccessListener(new OnSuccessListener<DataItem>() {
                        @Override
                        public void onSuccess(DataItem dataItem) {
                            Log.i("com.testingwatch","Successfully sending data");
                        }
                    });
                }
            }
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.d(LOG_TAG, "onAccuracyChanged - accuracy: " + i);
    }

    /**
     * This function sends the HR data to a companion app in the same project on an android phone,
     * connected to the Android Wear Device.
     */

    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEventBuffer);
        for(DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri!=null ? uri.getPath() : null;
            if(HR_PATH.equals(path)) {
                final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
            }
        }

    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {

    }

}