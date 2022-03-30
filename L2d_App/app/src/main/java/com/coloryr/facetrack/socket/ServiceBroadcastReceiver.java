package com.coloryr.facetrack.socket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    public static final String START_ACTION = "Live2dStart";
//    public static final String STOP_ACTION = "Live2dStop";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(ConnectService.TAG, Thread.currentThread().getName() + "---->"
                + "ServiceBroadcastReceiver onReceive");

        String action = intent.getAction();
        if (START_ACTION.equalsIgnoreCase(action)) {
            if (ConnectService.isStart)
                return;
            Intent intent1 = new Intent(context, ConnectService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent1);
            } else {
                context.startService(intent1);
            }

            Log.d(ConnectService.TAG, Thread.currentThread().getName() + "---->"
                    + "ServiceBroadcastReceiver onReceive start end");
//        } else if (STOP_ACTION.equalsIgnoreCase(action)) {
//            context.stopService(new Intent(context, ConnectService.class));
//            Log.d(ConnectService.TAG, Thread.currentThread().getName() + "---->"
//                    + "ServiceBroadcastReceiver onReceive stop end");
        }
    }
}