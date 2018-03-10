package com.beraldo.hpe;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.Math;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static java.lang.Math.abs;


public class NoiseRecorder extends Service{

    private String debugTag ;
    boolean isRecording = false;
    double DecibelNumber;
    public double total = 0;
    public double sum = 0;
    public int maint = 1;

    final class RecorderThread implements Runnable{
        int serviceId;
        RecorderThread(int serviceId){
            this.serviceId = serviceId;
        }

        @Override
        public void run() {
            Record();

        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        this.debugTag = getResources().getString(R.string.debugTag);


    }
    int i = 0;
    double Rn = 1.6;
    String thenumber;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Toast.makeText(NoiseRecorder.this, "Service started", Toast.LENGTH_LONG).show();
        Log.d(debugTag,"Noise service started ...");
        Thread myThread = new Thread(new RecorderThread(startId));
        myThread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        mRecorder.stop();
        mRecorder.reset();
        mRecorder = null;
        //Toast.makeText(NoiseRecorder.this, "Service destroyed and " + thenumber, Toast.LENGTH_LONG).show();
        Log.d(debugTag,"Noise service stopped ...");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final String LOG_TAG = "AudioRecordTest";

    MediaRecorder mRecorder = new MediaRecorder();

    double amp;
    double dBvalue;

    Handler postDecibels = new Handler();

    public void Record() {
        isRecording = true;

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if(Build.VERSION.SDK_INT >= 24){
            if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) != null) {
                mRecorder.setAudioSource(MediaRecorder.AudioSource.UNPROCESSED);
                //doprocessing(true);
            }
            ///Check if it returns null, if it does then just use VOICE_RECOGNITION
            else {
                mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
                //doprocessing(false);
            }
        }
        else{
            mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION);
            //doprocessing(false);
        }

        //learn what the differences between these output formats are.
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        //I assume this line just sets the output file as null considering we don't actually need it
        mRecorder.setOutputFile("/dev/null");

        try{
            mRecorder.prepare();
        }catch (IOException exception){
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();

        Runnable decibelRunnable = new Runnable() {
            @Override
            public void run() {
                getDecibels();
            }
        };
        new Thread(decibelRunnable).start();

    }

    double prevdB = 0;
    public String[] abnormalarray = null;
    int a = 0;

    private void getDecibels() {
        while(isRecording) {
            //amp = RanNum.nextDouble();
            amp = mRecorder.getMaxAmplitude();
            thenumber = String.valueOf(amp);
            //Log.d(debugTag,"maxAmp =" + thenumber);

            // Broadcast message using implicit intent.
            sendMessageToActivity(amp);


            prevdB = dBvalue;
            dBvalue = 20*Math.log10(amp/32768);
            //String abevent = abonormalevents(prevdB,dBvalue);
            //if(abevent != null){
            //    abnormalarray[a] = abevent;
            //    a++;
            //}
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //postDecibels.post(new Runnable() {
            //    @Override
            //    public void run() {
            //DecibelNumber = (TextView) findViewById(R.id.DecibelNumber);
            //DecibelNumber.setText(String.valueOf(amp));
            //    }
            //});
        }
    }

    /*
    public void doprocessing(boolean type){
        if(type){
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setAudioSamplingRate(11025);
            mRecorder.setAudioEncodingBitRate(AudioFormat.ENCODING_PCM_16BIT);
            mRecorder.setAudioChannels(AudioFormat.CHANNEL_IN_MONO);
        }
        else{
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setAudioSamplingRate(44100);
            mRecorder.setAudioEncodingBitRate(AudioFormat.ENCODING_PCM_16BIT);
            mRecorder.setAudioChannels(AudioFormat.CHANNEL_IN_MONO);
        }
    }


    private double movingaverage(double value){
        sum = sum + value;
        total = sum/maint;
        maint++;
        return total;
    }

    private String abonormalevents(double dBnow, double dBprev){
        double difference;
        difference = abs(dBnow-dBprev);
        if(difference>10){
            Calendar c = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(c.getTime());
        }
        return null;
    }
    */


    private void sendMessageToActivity(double Decibels) {
        Intent intent = new Intent("com.app.StudyBuddy.DECIBELS");
        // You can also include some extra data.
        intent.putExtra("CurrentDecibels", Decibels);
        //intent.putExtra("Abnormal Events", abnormalarray);
        //intent.putExtra("Session average decibels", );
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


}
