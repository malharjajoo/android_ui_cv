package com.beraldo.hpe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Afrazinator on 05/03/2018.
 */

public class HeartBeatBroadcastReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(HeartBeatBroadcastReceiver.class.getSimpleName(), "Service Stops");
        context.startService(new Intent(context, HeartBeatBroadcastReceiver.class));;
    }

}