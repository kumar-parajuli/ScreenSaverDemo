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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_OVERLAY_PERMISSION = 123;
    private int REQUEST_CODE_BOOT_RECEIVER_PERMISSION = 321;
    private static final long SLIDE_DELAY_MS = 3000; // 3 seconds
    private int currentPage = 0;
    private TextView tvClock,tvDate;

    private List<Integer> images = Arrays.asList(R.drawable.image1, R.drawable.image4, R.drawable.image5);
    private Handler handler = new Handler();
    private Runnable slideRunnable;
    private ViewPager2 viewPager;
    private RecyclerView imageSliderRecyclerView;
    private LinearLayout dotIndicatorLayout;
    private ImageView[] dots;
    private int totalSlides = 3;
//    private final String externalAppPackage = "com.kumar.screensaver";
    private final String externalAppPackage = "com.ingenico.template";

    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



//        test screen swipe roght
        gestureDetector = new GestureDetector(this, new GestureListener());

        // Enable full-screen immersive mode
        enableFullScreenMode();

        // Find the TextViews by their IDs
        tvClock = findViewById(R.id.tv_clock);
        tvDate = findViewById(R.id.tv_date);

        viewPager = findViewById(R.id.viewPager);
        dotIndicatorLayout = findViewById(R.id.dotIndicatorLayout);
        imageSliderRecyclerView = findViewById(R.id.imageSliderRecyclerView);


        // Initialize the ViewPager2 and its adapter
        ImageSliderAdapter adapter = new ImageSliderAdapter(this, images);
        viewPager.setAdapter(adapter);


        // Start updating the time and date
        updateTimeAndDate();

        // Start image slider
        startImageSlider();


        // Call the method to add dot indicators
        addDotsIndicator(0);

        // Set a page change listener to update dots on page change
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                addDotsIndicator(position);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            } else {
                startService();
            }
        }

        batterySaverModeDisablePermissionRequest();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            return true; // If a gesture is detected, return true to indicate the event is handled
        }
        return super.dispatchTouchEvent(event); // Otherwise, pass the event to other views/components
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;
        private static final int EDGE_THRESHOLD = 50; // Threshold for detecting edge swipe


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            Log.d("Gesture", "onFling detected");

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        // Right swipe detected
                        Log.d("Gesture", "Right swipe detected");
//                        openFewapayApp();
                    } else {
                        // Left swipe detected
                        Log.d("Gesture", "Left swipe detected");
                        returnToScreensaverApp();
                    }
                    return true;
                }
            }
            return false;
        }

        private void returnToScreensaverApp() {
            // You can finish the current activity and return to the screensaver.
            finish();

            // Or start the screensaver app again if needed.
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

        private void openFewapayApp() {
            PackageManager packageManager = getPackageManager();
            Intent intent = packageManager.getLaunchIntentForPackage(externalAppPackage);
            if (intent != null) {
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Fewapay app not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to handle the dot indicator logic
    private void addDotsIndicator(int position) {
        dotIndicatorLayout.removeAllViews(); // Clear previous dots
        dots = new ImageView[totalSlides]; // Array to hold the dots

        for (int i = 0; i < totalSlides; i++) {
            dots[i] = new ImageView(this);
            if (i == position) {
                // Set the active dot drawable
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.active_dot));
            } else {
                // Set the inactive dot drawable
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.inactive_dot));
            }

            // Define the layout parameters for the dots
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0); // Margins between dots

            // Add the dot to the layout
            dotIndicatorLayout.addView(dots[i], params);
        }
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

    //To Update the fetching the real Date and time
    private void updateTimeAndDate() {
        // Format time as hh:mm AM/PM (12-hour format)
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());

        // Format date as EEEE, MMMM dd, yyyy (e.g., Monday, August 22, 2023)
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        // Set the current time and date in the respective TextViews
        tvClock.setText(currentTime);
        tvDate.setText(currentDate);

        // Repeat this task every 1000 milliseconds (1 second)
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateTimeAndDate(); // Recursive call every second
            }
        }, 1000);
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


