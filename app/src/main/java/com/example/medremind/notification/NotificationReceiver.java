package com.example.medremind.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    public static final String ACTION_MEDICATION_REMINDER = "com.example.medremind.MEDICATION_REMINDER";
    public static final String EXTRA_OBAT_ID = "obat_id";
    public static final String EXTRA_OBAT_NAMA = "obat_nama";
    public static final String EXTRA_WAKTU = "waktu";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received broadcast action: " + action);

        if (ACTION_MEDICATION_REMINDER.equals(action)) {
            handleMedicationReminder(context, intent);
        }
    }

    private void handleMedicationReminder(Context context, Intent intent) {
        try {
            int obatId = intent.getIntExtra(EXTRA_OBAT_ID, -1);
            String obatNama = intent.getStringExtra(EXTRA_OBAT_NAMA);
            String waktu = intent.getStringExtra(EXTRA_WAKTU);

            if (obatId == -1 || obatNama == null || waktu == null) {
                Log.e(TAG, "Invalid medication reminder data - ObatId: " + obatId +
                        ", Nama: " + obatNama + ", Waktu: " + waktu);
                return;
            }

            Log.d(TAG, "Processing medication reminder - Obat: " + obatNama +
                    ", Waktu: " + waktu + ", ObatId: " + obatId);

            // Validate if reminder is still relevant
            if (isValidMedicationReminder(context, obatId, waktu)) {
                // Get obat details
                ObatHelper obatHelper = new ObatHelper(context);
                try {
                    obatHelper.open();
                    Obat obat = obatHelper.getObatById(obatId);

                    if (obat != null && obat.isAktif()) {
                        // Check stock
                        if (obat.getJumlahObat() <= 0) {
                            Log.w(TAG, "Obat out of stock, skipping reminder: " + obatNama);
                            return;
                        }

                        // Show notification
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.showMedicationReminder(
                                obatId,
                                obatNama,
                                waktu,
                                obat.getDosisObat()
                        );

                        Log.d(TAG, "Medication reminder notification shown for: " + obatNama);

                        // Check low stock warning (<=3 tablet atau <=500mg)
                        String jenis = obat.getJenisObat().toLowerCase();
                        int jumlah = obat.getJumlahObat();
                        boolean lowStock = false;
                        if (jenis.contains("tablet") && jumlah <= 3) {
                            lowStock = true;
                        } else if (jenis.contains("mg") && jumlah <= 500) {
                            lowStock = true;
                        }
                        if (lowStock) {
                            notificationHelper.showLowStockWarning(obatNama, jumlah);
                        }

                    } else {
                        Log.w(TAG, "Obat not found or inactive: " + obatNama + " (ID: " + obatId + ")");
                    }
                } finally {
                    obatHelper.close();
                }
            } else {
                Log.d(TAG, "Medication reminder not valid anymore: " + obatNama + " at " + waktu);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling medication reminder: " + e.getMessage(), e);
        }
    }

    private boolean isValidMedicationReminder(Context context, int obatId, String waktu) {
        JadwalHelper jadwalHelper = new JadwalHelper(context);
        try {
            jadwalHelper.open();

            // Perform daily reset to ensure data consistency
            jadwalHelper.checkAndPerformDailyReset();
            jadwalHelper.autoMarkTerlewatJadwal();

            // Get today's jadwal
            List<Jadwal> todayJadwal = filterJadwalForToday(jadwalHelper, obatId);

            // Check if this specific jadwal exists and is still pending
            for (Jadwal jadwal : todayJadwal) {
                if (jadwal.getObatId() == obatId &&
                        jadwal.getWaktu().equals(waktu) &&
                        jadwal.getStatus() == Jadwal.STATUS_BELUM_DIMINUM) {

                    Log.d(TAG, "Found valid pending jadwal for obat " + obatId + " at " + waktu);
                    return true;
                }
            }

            Log.d(TAG, "No valid pending jadwal found for obat " + obatId + " at " + waktu);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error validating medication reminder: " + e.getMessage(), e);
            return false;
        } finally {
            jadwalHelper.close();
        }
    }

    private List<Jadwal> filterJadwalForToday(JadwalHelper jadwalHelper, int obatId) {
        try {
            // Get all jadwal for this obat
            List<Jadwal> allJadwal = jadwalHelper.getJadwalByObatId(obatId);

            // Filter for today only
            Calendar now = Calendar.getInstance();
            String currentDay = getCurrentDayName(now);

            return allJadwal.stream()
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

        } catch (Exception e) {
            Log.e(TAG, "Error filtering jadwal for today: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    private String getCurrentDayName(Calendar calendar) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        return dayFormat.format(calendar.getTime());
    }
}