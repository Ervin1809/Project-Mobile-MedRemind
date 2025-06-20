package com.example.medremind.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.medremind.R;
import com.example.medremind.ui.activity.DetailJadwalActivity;
import com.example.medremind.ui.activity.MainActivity;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";

    // Notification Channel IDs
    public static final String CHANNEL_REMINDER = "medication_reminder";
    public static final String CHANNEL_GENERAL = "general_notifications";

    // Notification IDs
    public static final int NOTIFICATION_REMINDER_BASE_ID = 1000;
    public static final int NOTIFICATION_GENERAL_ID = 2000;

    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Medication Reminder Channel
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDER,
                    "Pengingat Obat",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Notifikasi untuk mengingatkan jadwal minum obat");
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(Color.BLUE);
            reminderChannel.enableVibration(true);
            reminderChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            // General Notifications Channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "Notifikasi Umum",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            generalChannel.setDescription("Notifikasi umum aplikasi MedRemind");

            // Register channels
            notificationManager.createNotificationChannel(reminderChannel);
            notificationManager.createNotificationChannel(generalChannel);

            Log.d(TAG, "Notification channels created successfully");
        }
    }

    /**
     * Show medication reminder notification
     */
    public void showMedicationReminder(int obatId, @NonNull String obatNama,
                                       @NonNull String waktu, @NonNull String dosis) {
        try {
            // Create intent untuk buka DetailJadwalActivity
            Intent intent = new Intent(context, DetailJadwalActivity.class);
            intent.putExtra(DetailJadwalActivity.EXTRA_OBAT_ID, obatId);
            intent.putExtra(DetailJadwalActivity.EXTRA_OBAT_NAMA, obatNama);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    obatId, // Use obatId as request code untuk uniqueness
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create "Sudah Minum" action
            Intent sudahMinumIntent = new Intent(context, NotificationActionReceiver.class);
            sudahMinumIntent.setAction(NotificationActionReceiver.ACTION_SUDAH_MINUM);
            sudahMinumIntent.putExtra("obat_id", obatId);
            sudahMinumIntent.putExtra("obat_nama", obatNama);
            sudahMinumIntent.putExtra("waktu", waktu);

            PendingIntent sudahMinumPendingIntent = PendingIntent.getBroadcast(
                    context,
                    obatId * 10 + 1, // Unique request code
                    sudahMinumIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create "Lewati" action
            Intent lewatiIntent = new Intent(context, NotificationActionReceiver.class);
            lewatiIntent.setAction(NotificationActionReceiver.ACTION_LEWATI);
            lewatiIntent.putExtra("obat_id", obatId);
            lewatiIntent.putExtra("obat_nama", obatNama);
            lewatiIntent.putExtra("waktu", waktu);

            PendingIntent lewatiPendingIntent = PendingIntent.getBroadcast(
                    context,
                    obatId * 10 + 2, // Unique request code
                    lewatiIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Build notification dengan custom style
            String bigText = String.format(
                    "🕐 Waktu: %s\n💊 Dosis: %s\n\nJangan lupa minum obat secara teratur untuk kesembuhan yang optimal.\n\nTap untuk buka detail jadwal.",
                    waktu, dosis
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDER)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert) // Default icon
                    .setContentTitle("⏰ Waktunya Minum Obat!")
                    .setContentText(obatNama + " - " + dosis)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(bigText)
                            .setBigContentTitle("💊 " + obatNama)
                            .setSummaryText("MedRemind - Pengingat Obat"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setAutoCancel(true)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .addAction(android.R.drawable.ic_input_add, "✅ Sudah Minum", sudahMinumPendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "⏭️ Lewati", lewatiPendingIntent)
                    .setWhen(System.currentTimeMillis())
                    .setShowWhen(true);

            // Show notification
            int notificationId = NOTIFICATION_REMINDER_BASE_ID + obatId;
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            // Check if notifications are enabled
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "Medication reminder shown - Obat: " + obatNama + ", Waktu: " + waktu + ", ID: " + notificationId);
            } else {
                Log.w(TAG, "Notifications are disabled for this app");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error showing medication reminder: " + e.getMessage(), e);
        }
    }

    /**
     * Show low stock warning notification
     */
    public void showLowStockWarning(@NonNull String obatNama, int sisaStok) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String title = "⚠️ Stok Obat Menipis";
            String message = String.format("%s tersisa %d tablet. Segera beli obat baru.", obatNama, sisaStok);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText(message + "\n\nTap untuk membuka aplikasi MedRemind."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat.from(context).notify(NOTIFICATION_GENERAL_ID + 1, builder.build());
            Log.d(TAG, "Low stock warning shown: " + obatNama + " (" + sisaStok + " left)");

        } catch (Exception e) {
            Log.e(TAG, "Error showing low stock warning: " + e.getMessage(), e);
        }
    }

    /**
     * Show general notification
     */
    public void showGeneralNotification(@NonNull String title, @NonNull String message) {
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            NotificationManagerCompat.from(context).notify(NOTIFICATION_GENERAL_ID, builder.build());
            Log.d(TAG, "General notification shown: " + title);

        } catch (Exception e) {
            Log.e(TAG, "Error showing general notification: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel specific notification
     */
    public void cancelNotification(int notificationId) {
        try {
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Notification cancelled: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling notification: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel medication reminder for specific obat
     */
    public void cancelMedicationReminder(int obatId) {
        int notificationId = NOTIFICATION_REMINDER_BASE_ID + obatId;
        cancelNotification(notificationId);
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        try {
            notificationManager.cancelAll();
            Log.d(TAG, "All notifications cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling all notifications: " + e.getMessage(), e);
        }
    }

    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled();
        }
        return true;
    }

    /**
     * Check if specific channel is enabled
     */
    public boolean isChannelEnabled(@NonNull String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
        return true;
    }

    /**
     * Get notification count for debugging
     */
    public void logNotificationStatus() {
        try {
            boolean enabled = areNotificationsEnabled();
            boolean reminderEnabled = isChannelEnabled(CHANNEL_REMINDER);
            boolean generalEnabled = isChannelEnabled(CHANNEL_GENERAL);

            Log.d(TAG, "Notification Status - Enabled: " + enabled +
                    ", Reminder Channel: " + reminderEnabled +
                    ", General Channel: " + generalEnabled);
        } catch (Exception e) {
            Log.e(TAG, "Error logging notification status: " + e.getMessage(), e);
        }
    }
}