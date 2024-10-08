package com.kumar.screensaver;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class GlobalTouchService extends Service implements View.OnTouchListener {
    private static final String CHANNEL_ID = "GlobalTouchServiceChannel";

    private Handler mHandler;
    private Runnable mRunnable;
    private final int mTimerDelay = 20000;//inactivity delay in milliseconds
    private LinearLayout mTouchLayout;//the transparent view

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("GlobalTouchService", "Service created");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                // Consider notifying the user to grant the permission
                return;
            }
        }
        createNotificationChannel();
        mTouchLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mTouchLayout.setLayoutParams(lp);
        mTouchLayout.setOnTouchListener(this);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        WindowManager.LayoutParams mParams;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android O (API 26) and above
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, // Use this for Android 13+
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
        } else {
            mParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT
            );
        }

        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        windowManager.addView(mTouchLayout, mParams);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("GlobalTouchService", "Service started");

        // Create a foreground notification for the service (for Android O and above)
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Global Touch Service")
                .setContentText("Service is running...")
                .setSmallIcon(R.drawable.screensaver_logo)  // Change this with your own icon
                .build();

        startForeground(1, notification);

        // Start the service in the foreground
        initTimer();
        return START_STICKY;
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("IdleDetectorService", "Touch detected. Resetting timer");
        initTimer();
        return false;
    }

    private boolean isAudioPlaying(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            return audioManager.isMusicActive();
        }
        return false;
    }


    /**
     * (Re)sets the timer to send the inactivity broadcast
     */
    private void initTimer() {
        // Start timer and timer task
        if (mRunnable == null) {

            mRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d("IdleDetectorService", "Inactivity detected. Sending broadcast to start the app");

                    try {
                        if (!isAudioPlaying(getApplicationContext())) {
                            boolean isInForeground = new ForegroundCheckTask().execute(getApplicationContext()).get();

                            if (!isInForeground) {
                                Intent launchIntent = getApplication()
                                        .getPackageManager()
                                        .getLaunchIntentForPackage("com.kumar.screensaver");
                                if (launchIntent != null) {
                                    Log.d("IdleDetectorService", "App started");
                                    getApplication().startActivity(launchIntent);
                                }
                            }
                        }

                        stopSelf();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        if (mHandler == null) {
            mHandler = new Handler();
        }

        mHandler.removeCallbacks(mRunnable);
        mHandler.postDelayed(mRunnable, mTimerDelay);
    }


    private class ForegroundCheckTask extends AsyncTask<Context, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Context... params) {
            final Context context = params[0];
            return isAppOnForeground(context);
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                // For Android KitKat and below
                List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
                if (appProcesses != null) {
                    final String packageName = context.getPackageName();
                    for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                                appProcess.processName.equals(packageName)) {
                            return true;
                        }
                    }
                }
            } else {
                // For Android Lollipop (API 21) and above
                UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
                long currentTime = System.currentTimeMillis();

                // Query app usage stats for the last minute
                List<UsageStats> usageStatsList = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        currentTime - 1000 * 60, currentTime);

                if (usageStatsList != null && !usageStatsList.isEmpty()) {
                    SortedMap<Long, UsageStats> sortedMap = new TreeMap<>();
                    for (UsageStats usageStats : usageStatsList) {
                        sortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    }

                    if (!sortedMap.isEmpty()) {
                        UsageStats recentStats = sortedMap.get(sortedMap.lastKey());
                        if (recentStats != null && recentStats.getPackageName().equals(context.getPackageName())) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Global Touch Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

}
