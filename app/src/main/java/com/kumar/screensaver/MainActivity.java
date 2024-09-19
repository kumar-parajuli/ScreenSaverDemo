package com.kumar.screensaver;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_OVERLAY_PERMISSION = 123;
    private int REQUEST_CODE_BOOT_RECEIVER_PERMISSION = 321;
    private static final long SLIDE_DELAY_MS = 3000; // 3 seconds
    private int currentPage = 0;

    private List<Integer> images = Arrays.asList(R.drawable.screensaver_background, R.drawable.img, R.drawable.screensaver_logo);
    private Handler handler = new Handler();
    private Runnable slideRunnable;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Enable full-screen immersive mode
        enableFullScreenMode();
        viewPager = findViewById(R.id.viewPager);
        ImageSliderAdapter adapter = new ImageSliderAdapter(this, images);
        viewPager.setAdapter(adapter);

        // Start the image sliding
        startImageSlider();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } else {
                startService();
            }
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_BOOT_COMPLETED)
//                    != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_BOOT_COMPLETED}, REQUEST_CODE_BOOT_RECEIVER_PERMISSION);
//            }
//        }
        batterySaverModeDisablePermissionRequest();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, register the BootReceiver dynamically
                BootReceiver bootReceiver = new BootReceiver();
                IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
                registerReceiver(bootReceiver, filter);
            }
        }
        if (requestCode == REQUEST_CODE_BOOT_RECEIVER_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, register the BootReceiver dynamically
                BootReceiver bootReceiver = new BootReceiver();
                IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
                registerReceiver(bootReceiver, filter);
            }
        }
    }

    private void startImageSlider() {
        slideRunnable = new Runnable() {
            @Override
            public void run() {
                // Set the current item with smooth scrolling
                currentPage = (currentPage + 1) % images.size();
                viewPager.setCurrentItem(currentPage, true);

                // Schedule the next slide
                handler.postDelayed(this, SLIDE_DELAY_MS);
            }
        };
        handler.postDelayed(slideRunnable, SLIDE_DELAY_MS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                // Permission granted, now you can start the service
                startService();
            } else {
                // Permission not granted, show a message to the user
                Toast.makeText(this, "Permission is required to detect inactivity", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startService() {
        Intent serviceIntent = new Intent(this, GlobalTouchService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(slideRunnable);
    }

    // Full-screen immersive mode method
    private void enableFullScreenMode() {
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        // Automatically re-enable immersive mode when UI visibility changes
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> {
            if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                enableFullScreenMode(); // Re-enable immersive mode
            }
        });
    }

    private void batterySaverModeDisablePermissionRequest() {

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager.isPowerSaveMode()) {
            Log.d("MainActivity", "PowerSaveMode is ON");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                // Battery saver is OFF, continue as normal
                Log.d("MainActivity", "PowerSaveMode is OFF");
            }
        }
    }
}


//        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//        intent.setData(Uri.parse("package:" + getPackageName()));
//        startActivity(intent);
//    }