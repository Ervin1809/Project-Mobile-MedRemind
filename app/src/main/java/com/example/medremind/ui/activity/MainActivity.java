package com.example.medremind.ui.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.medremind.R;
import com.example.medremind.notification.AlarmScheduler;
import com.example.medremind.notification.NotificationHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_NOTIFICATION = 101;

    // 🔑 Notification components
    private NotificationHelper notificationHelper;
    private AlarmScheduler alarmScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply dark mode setting before setting content view
        SharedPreferences sharedPreferences = getSharedPreferences("MedRemindPrefs", Context.MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup navigation
        setupNavigation();

        // 🔑 Setup notifications
        setupNotifications();
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            NavigationUI.setupWithNavController(bottomNavigation, navController);
        } else {
            Log.e(TAG, "NavHostFragment tidak ditemukan!");
        }
    }

    // 🔑 Setup notification system
    private void setupNotifications() {
        try {
            // Initialize notification components
            notificationHelper = new NotificationHelper(this);
            alarmScheduler = new AlarmScheduler(this);

            // Check and request notification permission for Android 13+
            checkNotificationPermission();

            Log.d(TAG, "Notification system initialized");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up notifications: " + e.getMessage(), e);
        }
    }

    // 🔑 Check notification permission
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_NOTIFICATION);
            } else {
                // Permission already granted, schedule reminders
                scheduleReminders();
            }
        } else {
            // No permission needed for older versions
            scheduleReminders();
        }
    }

    // 🔑 Schedule medication reminders
    private void scheduleReminders() {
        try {
            if (alarmScheduler != null) {
                alarmScheduler.scheduleAllMedicationReminders();
                Log.d(TAG, "Medication reminders scheduled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminders: " + e.getMessage(), e);
        }
    }

    // 🔑 Handle permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
                scheduleReminders();
            } else {
                Log.w(TAG, "Notification permission denied");
            }
        }
    }

    // 🔑 Reschedule reminders when activity resumes
    @Override
    protected void onResume() {
        super.onResume();
        scheduleReminders();
    }

    // 🔑 Public methods untuk akses dari fragment
    public NotificationHelper getNotificationHelper() {
        return notificationHelper;
    }

    public AlarmScheduler getAlarmScheduler() {
        return alarmScheduler;
    }

    // 🔑 Test notification method (untuk debugging)
    public void testNotification() {
        if (notificationHelper != null) {
            notificationHelper.showMedicationReminder(1, "Test Paracetamol", "18:30", "500mg");
            Log.d(TAG, "Test notification sent");
        }
    }
}