package com.kumar.screensaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Invoke Boot Receiver");
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Boot completed - starting service");

            // Start your service after boot
            Intent serviceIntent = new Intent(context, GlobalTouchService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
//    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "Boot completed broadcast received");
//
//        // Check if the service is already running
//        if (!isServiceRunning(context, GlobalTouchService.class)) {
//            Intent serviceIntent = new Intent(context, GlobalTouchService.class);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                Log.d(TAG, "Starting GlobalTouchService as a foreground service");
//                context.startForegroundService(serviceIntent);
//            } else {
//                Log.d(TAG, "Starting GlobalTouchService normally");
//                context.startService(serviceIntent);
//            }
//        } else {
//            Log.d(TAG, "GlobalTouchService is already running");
//        }
//
//    }
//    private boolean isServiceRunning(Context context, Class<GlobalTouchService> serviceClass) {
//        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (serviceClass.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }
}

