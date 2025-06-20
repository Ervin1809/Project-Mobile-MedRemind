package com.example.medremind.notification;

import static android.app.ProgressDialog.show;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;

import java.util.List;

public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationActionReceiver";

    public static final String ACTION_SUDAH_MINUM = "com.example.medremind.ACTION_SUDAH_MINUM";
    public static final String ACTION_LEWATI = "com.example.medremind.ACTION_LEWATI";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int obatId = intent.getIntExtra("obat_id", -1);
        String obatNama = intent.getStringExtra("obat_nama");

        Log.d(TAG, "Received action: " + action + " for obat: " + obatNama);

        if (obatId == -1 || obatNama == null) {
            Log.e(TAG, "Invalid action data");
            return;
        }

        if (ACTION_SUDAH_MINUM.equals(action)) {
            handleSudahMinum(context, obatId, obatNama);
        } else if (ACTION_LEWATI.equals(action)) {
            handleLewati(context, obatId, obatNama);
        }

        // Cancel notification after action
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.cancelMedicationReminder(obatId);
    }

    private void handleSudahMinum(Context context, int obatId, String obatNama) {
        JadwalHelper jadwalHelper = new JadwalHelper(context);
        ObatHelper obatHelper = new ObatHelper(context);

        try {
            jadwalHelper.open();
            obatHelper.open();

            // Find current pending jadwal for this obat
            List<Jadwal> todayJadwal = jadwalHelper.getJadwalHariIni();
            Jadwal targetJadwal = null;

            for (Jadwal jadwal : todayJadwal) {
                if (jadwal.getObatId() == obatId &&
                        jadwal.getStatus() == Jadwal.STATUS_BELUM_DIMINUM) {
                    targetJadwal = jadwal;
                    break;
                }
            }

            if (targetJadwal != null) {
                // 🔑 Update status menggunakan existing method (otomatis kurangi stok)
                int rowsAffected = jadwalHelper.updateJadwalStatus(
                        targetJadwal.getId(),
                        Jadwal.STATUS_SUDAH_DIMINUM,
                        "Via notification action"
                );

                if (rowsAffected > 0) {
                    Toast.makeText(context,
                            "✅ " + obatNama + " - Sudah diminum",
                            Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Successfully marked as taken: " + obatNama);
                } else {
                    Toast.makeText(context, "Gagal mengupdate status", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Jadwal tidak ditemukan", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling sudah minum: " + e.getMessage(), e);
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            jadwalHelper.close();
            obatHelper.close();
        }
    }

    private void handleLewati(Context context, int obatId, String obatNama) {
        JadwalHelper jadwalHelper = new JadwalHelper(context);

        try {
            jadwalHelper.open();

            // Find current pending jadwal for this obat
            List<Jadwal> todayJadwal = jadwalHelper.getJadwalHariIni();
            Jadwal targetJadwal = null;

            for (Jadwal jadwal : todayJadwal) {
                if (jadwal.getObatId() == obatId &&
                        jadwal.getStatus() == Jadwal.STATUS_BELUM_DIMINUM) {
                    targetJadwal = jadwal;
                    break;
                }
            }

            if (targetJadwal != null) {
                int rowsAffected = jadwalHelper.updateJadwalStatus(
                        targetJadwal.getId(),
                        Jadwal.STATUS_TERLEWAT,
                        "Dilewati via notification"
                );

                if (rowsAffected > 0) {
                    Toast.makeText(context,
                            "⏭️ " + obatNama + " - Dilewati",
                            Toast.LENGTH_SHORT).show();

                    Log.d(TAG, "Successfully marked as skipped: " + obatNama);
                } else {
                    Toast.makeText(context, "Gagal mengupdate status", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "Jadwal tidak ditemukan", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling lewati: " + e.getMessage(), e);
            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            jadwalHelper.close();
        }
    }
}