package com.example.medremind.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    private Context context;
    private AlarmManager alarmManager;

    public AlarmScheduler(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Schedule all medication reminders for today
     */
    public void scheduleAllMedicationReminders() {
        try {
            JadwalHelper jadwalHelper = new JadwalHelper(context);
            ObatHelper obatHelper = new ObatHelper(context);

            jadwalHelper.open();
            obatHelper.open();

            // Perform daily reset
            jadwalHelper.checkAndPerformDailyReset();
            jadwalHelper.autoMarkTerlewatJadwal();

            // Get all active obat
            List<Obat> activeObatList = obatHelper.getAllObat(true);

            int totalScheduled = 0;
            for (Obat obat : activeObatList) {
                if (obat.getJumlahObat() > 0) { // Only schedule if stock available
                    int scheduled = scheduleRemindersForObat(jadwalHelper, obat);
                    totalScheduled += scheduled;
                }
            }

            jadwalHelper.close();
            obatHelper.close();

            Log.d(TAG, "Scheduled " + totalScheduled + " medication reminders for " +
                    activeObatList.size() + " active medications");

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling all medication reminders: " + e.getMessage(), e);
        }
    }

    /**
     * Schedule reminders untuk obat tertentu
     */
    private int scheduleRemindersForObat(@NonNull JadwalHelper jadwalHelper, @NonNull Obat obat) {
        try {
            // Get jadwal untuk obat ini
            List<Jadwal> allJadwal = jadwalHelper.getJadwalByObatId(obat.getId());

            if (allJadwal.isEmpty()) {
                Log.d(TAG, "No jadwal found for obat: " + obat.getNamaObat());
                return 0;
            }

            // Filter untuk jadwal hari ini
            List<Jadwal> todayJadwal = filterJadwalForToday(allJadwal);

            int scheduledCount = 0;
            for (Jadwal jadwal : todayJadwal) {
                // Only schedule for pending jadwal
                if (jadwal.getStatus() == Jadwal.STATUS_BELUM_DIMINUM) {
                    if (scheduleReminder(jadwal, obat.getNamaObat())) {
                        scheduledCount++;
                    }
                }
            }

            Log.d(TAG, "Scheduled " + scheduledCount + " reminders for obat: " + obat.getNamaObat());
            return scheduledCount;

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminders for obat " + obat.getNamaObat() + ": " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Schedule reminder untuk jadwal tertentu
     */
    public boolean scheduleReminder(@NonNull Jadwal jadwal, @NonNull String obatNama) {
        try {
            Calendar now = Calendar.getInstance();
            Calendar reminderTime = Calendar.getInstance();

            // Parse waktu jadwal (format HH:mm)
            String[] timeParts = jadwal.getWaktu().split(":");
            if (timeParts.length != 2) {
                Log.e(TAG, "Invalid time format: " + jadwal.getWaktu());
                return false;
            }

            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            reminderTime.set(Calendar.HOUR_OF_DAY, hour);
            reminderTime.set(Calendar.MINUTE, minute);
            reminderTime.set(Calendar.SECOND, 0);
            reminderTime.set(Calendar.MILLISECOND, 0);

            // Jika waktu sudah lewat hari ini, skip
            if (reminderTime.before(now) || reminderTime.equals(now)) {
                Log.d(TAG, "Jadwal " + jadwal.getWaktu() + " sudah lewat, skip scheduling");
                return false;
            }

            // Check if exact alarms are allowed (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarms not allowed, using inexact alarm");
            }

            // Create intent
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(NotificationReceiver.ACTION_MEDICATION_REMINDER);
            intent.putExtra(NotificationReceiver.EXTRA_OBAT_ID, jadwal.getObatId());
            intent.putExtra(NotificationReceiver.EXTRA_OBAT_NAMA, obatNama);
            intent.putExtra(NotificationReceiver.EXTRA_WAKTU, jadwal.getWaktu());

            // Create PendingIntent dengan unique request code
            int requestCode = generateRequestCode(jadwal.getObatId(), jadwal.getWaktu());
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule alarm based on Android version
            long triggerTime = reminderTime.getTimeInMillis();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ - Use setExactAndAllowWhileIdle for doze mode compatibility
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4+ - Use setExact
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                // Older versions - Use set (less precise)
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Log.d(TAG, "Scheduled reminder for " + obatNama + " at " +
                    dateFormat.format(reminderTime.getTime()) + " (RequestCode: " + requestCode + ")");

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder for " + obatNama + ": " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cancel reminder untuk jadwal tertentu
     */
    public void cancelReminder(int obatId, @NonNull String waktu) {
        try {
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(NotificationReceiver.ACTION_MEDICATION_REMINDER);

            int requestCode = generateRequestCode(obatId, waktu);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled reminder for obat " + obatId + " at " + waktu + " (RequestCode: " + requestCode + ")");

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling reminder: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel all scheduled reminders
     */
    public void cancelAllReminders() {
        try {
            JadwalHelper jadwalHelper = new JadwalHelper(context);
            jadwalHelper.open();

            List<Jadwal> allJadwal = jadwalHelper.getAllJadwal();

            int cancelledCount = 0;
            for (Jadwal jadwal : allJadwal) {
                cancelReminder(jadwal.getObatId(), jadwal.getWaktu());
                cancelledCount++;
            }

            jadwalHelper.close();
            Log.d(TAG, "Cancelled " + cancelledCount + " scheduled reminders");

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling all reminders: " + e.getMessage(), e);
        }
    }

    /**
     * Filter jadwal untuk hari ini
     */
    private List<Jadwal> filterJadwalForToday(@NonNull List<Jadwal> jadwalList) {
        Calendar now = Calendar.getInstance();
        String currentDay = getCurrentDayName(now);

        return jadwalList.stream()
                .filter(jadwal -> {
                    String jadwalHari = jadwal.getHari();

                    // Daily schedule
                    if (jadwalHari.equalsIgnoreCase("daily") ||
                            jadwalHari.equalsIgnoreCase("setiap hari") ||
                            jadwalHari.equalsIgnoreCase("harian")) {
                        return true;
                    }

                    // Weekly schedule - check if today matches
                    return jadwalHari.equalsIgnoreCase(currentDay);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get current day name in Indonesian
     */
    private String getCurrentDayName(Calendar calendar) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        return dayFormat.format(calendar.getTime());
    }

    /**
     * Generate unique request code untuk alarm
     */
    private int generateRequestCode(int obatId, @NonNull String waktu) {
        try {
            // Combine obatId dan waktu untuk unique code
            String[] timeParts = waktu.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            // Format: obatId (max 999) + hour (00-23) + minute (00-59)
            // Example: obat_id=1, waktu=08:30 → 1 * 10000 + 8 * 100 + 30 = 10830
            return (obatId * 10000) + (hour * 100) + minute;

        } catch (Exception e) {
            Log.e(TAG, "Error generating request code for obat " + obatId + " at " + waktu + ": " + e.getMessage(), e);
            // Fallback to simple hash
            return (obatId + waktu.hashCode()) % Integer.MAX_VALUE;
        }
    }

    /**
     * Check if exact alarms are allowed (Android 12+)
     */
    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /**
     * Get scheduled alarms count for debugging
     */
    public void logSchedulerStatus() {
        boolean canScheduleExact = canScheduleExactAlarms();
        Log.d(TAG, "AlarmScheduler Status - Can schedule exact alarms: " + canScheduleExact);
    }
}