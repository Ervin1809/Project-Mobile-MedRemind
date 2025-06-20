package com.example.medremind.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
                Intent.ACTION_PACKAGE_REPLACED.equals(action)) {

            rescheduleNotifications(context);
        }
    }

    private void rescheduleNotifications(Context context) {
        try {
            Log.d(TAG, "Rescheduling medication reminders after boot/update");

            // Initialize notification system
            NotificationHelper notificationHelper = new NotificationHelper(context);
            AlarmScheduler alarmScheduler = new AlarmScheduler(context);

            // Clear any existing notifications
            notificationHelper.cancelAllNotifications();

            // Reschedule all reminders
            alarmScheduler.scheduleAllMedicationReminders();

            Log.d(TAG, "Medication reminders rescheduled successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error rescheduling notifications: " + e.getMessage(), e);
        }
    }
}