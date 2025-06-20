package com.example.medremind.data.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.medremind.data.model.Jadwal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JadwalHelper {
    private static final String TAG = "JadwalHelper";

    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private ObatHelper obatHelper;
    private Context context;
    private boolean isOpen = false;

    public JadwalHelper(@NonNull Context context) {
        this.context = context.getApplicationContext(); // Prevent memory leaks
        this.dbHelper = new DbHelper(this.context);
        this.obatHelper = new ObatHelper(this.context);
    }

    /**
     * Membuka koneksi database
     * @throws SQLiteException jika gagal membuka database
     */
    public synchronized void open() throws SQLiteException {
        if (!isOpen) {
            try {
                database = dbHelper.getWritableDatabase();
                obatHelper.open();
                isOpen = true;
                Log.d(TAG, "Database opened successfully");
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to open database: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    /**
     * Menutup koneksi database
     */
    public synchronized void close() {
        if (isOpen) {
            try {
                if (obatHelper != null) {
                    obatHelper.close();
                }
                if (database != null && database.isOpen()) {
                    database.close();
                }
                isOpen = false;
                Log.d(TAG, "Database closed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error closing database: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Memastikan database terbuka sebelum operasi
     */
    private void ensureDatabaseOpen() {
        if (!isOpen || database == null || !database.isOpen()) {
            open();
        }
    }

    // 🔑 =============== DAILY RESET FUNCTIONALITY ===============

    /**
     * Get current date in YYYY-MM-DD format
     */
    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    /**
     * Get current time in HH:mm format
     */
    private String getCurrentTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return timeFormat.format(new Date());
    }

    /**
     * Check dan perform daily reset jika perlu
     * Call method ini setiap kali app dibuka
     */
    public void checkAndPerformDailyReset() {
        ensureDatabaseOpen();

        try {
            String today = getCurrentDate();

            // Check if reset sudah dilakukan hari ini
            if (isResetAlreadyDoneToday(today)) {
                Log.d(TAG, "Daily reset already performed today: " + today);
                return;
            }

            // Perform reset
            performDailyReset(today);

            Log.d(TAG, "Daily reset completed for: " + today);

        } catch (Exception e) {
            Log.e(TAG, "Error in daily reset: " + e.getMessage(), e);
        }
    }

    /**
     * Check apakah reset sudah dilakukan hari ini
     */
    private boolean isResetAlreadyDoneToday(String today) {
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(*) FROM " + DbHelper.TABLE_JADWAL +
                    " WHERE " + DbHelper.KEY_LAST_RESET_DATE + " = ?";
            cursor = database.rawQuery(query, new String[]{today});

            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                return count > 0; // Jika ada yang sudah direset hari ini
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Error checking reset status: " + e.getMessage(), e);
            return false;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Perform daily reset - clear semua status dan update last_reset_date
     */
    private void performDailyReset(String today) {
        try {
            // Reset semua status ke BELUM_DIMINUM dan update last_reset_date
            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_STATUS, Jadwal.STATUS_BELUM_DIMINUM); // Reset ke 0
            values.put(DbHelper.KEY_LAST_RESET_DATE, today);
            values.putNull(DbHelper.KEY_JADWAL_TANGGAL_DIMINUM); // Clear tanggal diminum
            values.putNull(DbHelper.KEY_JADWAL_CATATAN); // Clear catatan (optional)

            int updatedRows = database.update(DbHelper.TABLE_JADWAL, values, null, null);

            Log.d(TAG, "Daily reset: cleared status for " + updatedRows + " jadwal records");

        } catch (Exception e) {
            Log.e(TAG, "Error performing daily reset: " + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Auto-mark jadwal sebagai TERLEWAT jika sudah lewat 2 jam
     */
    public void autoMarkTerlewatJadwal() {
        ensureDatabaseOpen();

        try {
            String currentTime = getCurrentTime(); // Format: HH:mm
            String currentDate = getCurrentDate();

            // Query jadwal yang belum diminum dan waktunya sudah lewat 2+ jam
            String query = "SELECT " + DbHelper.KEY_JADWAL_ID + ", " + DbHelper.KEY_WAKTU +
                    " FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK +
                    " = o." + DbHelper.KEY_OBAT_ID +
                    " WHERE j." + DbHelper.KEY_STATUS + " = ? " +
                    "AND o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1 " +
                    "AND (j." + DbHelper.KEY_LAST_RESET_DATE + " = ? OR j." + DbHelper.KEY_LAST_RESET_DATE + " IS NULL)";

            Cursor cursor = database.rawQuery(query, new String[]{
                    String.valueOf(Jadwal.STATUS_BELUM_DIMINUM), currentDate
            });

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int jadwalId = cursor.getInt(0);
                    String waktuJadwal = cursor.getString(1);

                    if (shouldAutoMarkTerlewat(waktuJadwal, currentTime)) {
                        updateJadwalStatus(jadwalId, Jadwal.STATUS_TERLEWAT, "Auto terlewat - lewat 2 jam");
                        Log.d(TAG, "Auto marked jadwal " + jadwalId + " as TERLEWAT");
                    }

                } while (cursor.moveToNext());
            }

            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error auto marking terlewat: " + e.getMessage(), e);
        }
    }

    /**
     * Check if jadwal should be auto-marked as terlewat
     */
    private boolean shouldAutoMarkTerlewat(String waktuJadwal, String currentTime) {
        try {
            // Parse times
            String[] jadwalParts = waktuJadwal.split(":");
            String[] currentParts = currentTime.split(":");

            int jadwalHour = Integer.parseInt(jadwalParts[0]);
            int jadwalMinute = Integer.parseInt(jadwalParts[1]);
            int currentHour = Integer.parseInt(currentParts[0]);
            int currentMinute = Integer.parseInt(currentParts[1]);

            // Convert to minutes since midnight
            int jadwalMinutes = jadwalHour * 60 + jadwalMinute;
            int currentMinutes = currentHour * 60 + currentMinute;

            // Check if current time is 2+ hours (120 minutes) after jadwal time
            int diffMinutes = currentMinutes - jadwalMinutes;

            // Handle day transition (currentTime < jadwalTime bisa berarti besok)
            if (diffMinutes < 0) {
                diffMinutes += 24 * 60; // Add 24 hours in minutes
            }

            return diffMinutes >= 120; // 2 hours = 120 minutes

        } catch (Exception e) {
            Log.e(TAG, "Error checking auto terlewat: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get status untuk jadwal hari ini dengan daily reset logic
     */
    @Nullable
    public String getJadwalStatusForToday(int jadwalId, String waktuJadwal) {
        ensureDatabaseOpen();

        // Perform daily reset check
        checkAndPerformDailyReset();

        // Auto-mark terlewat jika perlu
        autoMarkTerlewatJadwal();

        // Get updated jadwal
        Jadwal jadwal = getJadwalById(jadwalId);
        if (jadwal == null) {
            return null;
        }

        // Return status based on database
        switch (jadwal.getStatus()) {
            case Jadwal.STATUS_SUDAH_DIMINUM:
                return "DONE";
            case Jadwal.STATUS_TERLEWAT:
                return "TERLEWAT";
            case Jadwal.STATUS_BELUM_DIMINUM:
            default:
                // Calculate real-time status
                return calculateRealTimeStatus(waktuJadwal);
        }
    }

    /**
     * Calculate real-time status for belum diminum jadwal
     */
    private String calculateRealTimeStatus(String waktuJadwal) {
        try {
            Calendar now = Calendar.getInstance();
            Calendar jadwalTime = Calendar.getInstance();

            // Parse waktu (format HH:mm)
            String[] timeParts = waktuJadwal.split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            jadwalTime.set(Calendar.HOUR_OF_DAY, hour);
            jadwalTime.set(Calendar.MINUTE, minute);
            jadwalTime.set(Calendar.SECOND, 0);

            long diffMillis = jadwalTime.getTimeInMillis() - now.getTimeInMillis();
            long diffMinutes = diffMillis / (1000 * 60);

            // Real-time status calculation
            if (diffMinutes < -15) {
                return "TERLAMBAT";
            } else if (diffMinutes <= 15) {
                return "WAKTUNYA";
            } else if (diffMinutes < 60) {
                return diffMinutes + " Menit Lagi";
            } else if (diffMinutes < 1440) {
                long diffHours = diffMinutes / 60;
                return diffHours + " Jam Lagi";
            } else {
                return "BESOK";
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating real-time status: " + e.getMessage(), e);
            return "ERROR";
        }
    }

    // 🔑 =============== EXISTING METHODS (UNCHANGED) ===============

    /**
     * Menambah jadwal baru
     * @param jadwal Objek jadwal yang akan disimpan
     * @return ID jadwal yang baru disimpan, atau -1 jika gagal
     */
    public long tambahJadwal(@NonNull Jadwal jadwal) {
        if (jadwal == null) {
            Log.e(TAG, "Cannot insert null jadwal");
            return -1;
        }

        if (!jadwal.isValid()) {
            Log.e(TAG, "Cannot insert invalid jadwal: " + jadwal.toString());
            return -1;
        }

        ensureDatabaseOpen();

        try {
            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_OBAT_ID_FK, jadwal.getObatId());
            values.put(DbHelper.KEY_HARI, jadwal.getHari().trim());
            values.put(DbHelper.KEY_WAKTU, jadwal.getWaktu().trim());
            values.put(DbHelper.KEY_STATUS, jadwal.getStatus());

            if (jadwal.getCatatan() != null) {
                values.put(DbHelper.KEY_JADWAL_CATATAN, jadwal.getCatatan().trim());
            }

            long result = database.insert(DbHelper.TABLE_JADWAL, null, values);

            if (result != -1) {
                Log.d(TAG, "Jadwal inserted successfully with ID: " + result);
                jadwal.setId((int) result); // Set ID ke objek jadwal
            } else {
                Log.e(TAG, "Failed to insert jadwal");
            }

            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error inserting jadwal: " + e.getMessage(), e);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error inserting jadwal: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Mendapatkan semua jadwal dengan informasi obat
     * @return List jadwal atau empty list jika tidak ada
     */
    @NonNull
    public List<Jadwal> getAllJadwal() {
        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum, o.jenis_obat " +
                    "FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK + " = o." + DbHelper.KEY_OBAT_ID + " " +
                    "WHERE o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1 " +
                    "ORDER BY j." + DbHelper.KEY_WAKTU + " ASC";

            Cursor cursor = database.rawQuery(selectQuery, null);
            List<Jadwal> result = CursorHelper.cursorToJadwalList(cursor);

            Log.d(TAG, "Retrieved " + result.size() + " jadwal records");
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting all jadwal: " + e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting all jadwal: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Mendapatkan jadwal berdasarkan obat ID
     * @param obatId ID obat yang dicari jadwalnya
     * @return List jadwal untuk obat tersebut
     */
    @NonNull
    public List<Jadwal> getJadwalByObatId(long obatId) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID: " + obatId);
            return new ArrayList<>();
        }

        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum, o.jenis_obat " +
                    "FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK + " = o." + DbHelper.KEY_OBAT_ID + " " +
                    "WHERE j." + DbHelper.KEY_OBAT_ID_FK + " = ? AND o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1 " +
                    "ORDER BY j." + DbHelper.KEY_WAKTU + " ASC";

            Cursor cursor = database.rawQuery(selectQuery, new String[]{String.valueOf(obatId)});
            List<Jadwal> result = CursorHelper.cursorToJadwalList(cursor);

            Log.d(TAG, "Retrieved " + result.size() + " jadwal for obat ID: " + obatId);
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting jadwal by obat ID: " + e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting jadwal by obat ID: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Mendapatkan jadwal untuk hari ini
     * @return List jadwal hari ini
     */
    @NonNull
    public List<Jadwal> getJadwalHariIni() {
        // Mendapatkan nama hari dalam bahasa Indonesia
        String[] hariNames = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String hariIni = hariNames[dayOfWeek - 1]; // Calendar.SUNDAY = 1, dll

        return getJadwalByHari(hariIni, true); // Include daily schedules
    }

    /**
     * Mendapatkan jadwal berdasarkan hari tertentu
     * @param hari Nama hari yang dicari
     * @param includeDaily Apakah termasuk jadwal harian (daily)
     * @return List jadwal untuk hari tersebut
     */
    @NonNull
    public List<Jadwal> getJadwalByHari(@NonNull String hari, boolean includeDaily) {
        if (hari == null || hari.trim().isEmpty()) {
            Log.e(TAG, "Invalid hari parameter");
            return new ArrayList<>();
        }

        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum, o.jenis_obat " +
                    "FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK + " = o." + DbHelper.KEY_OBAT_ID + " " +
                    "WHERE o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1 AND (";

            List<String> params = new ArrayList<>();

            if (includeDaily) {
                selectQuery += "j." + DbHelper.KEY_HARI + " = 'daily' OR ";
            }

            selectQuery += "j." + DbHelper.KEY_HARI + " = ?) " +
                    "ORDER BY j." + DbHelper.KEY_WAKTU + " ASC";

            params.add(hari.trim());

            Cursor cursor = database.rawQuery(selectQuery, params.toArray(new String[0]));
            List<Jadwal> result = CursorHelper.cursorToJadwalList(cursor);

            Log.d(TAG, "Retrieved " + result.size() + " jadwal for hari: " + hari);
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting jadwal by hari: " + e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting jadwal by hari: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Mendapatkan jadwal harian saja
     * @return List jadwal harian
     */
    @NonNull
    public List<Jadwal> getJadwalHarian() {
        return getJadwalByHari(Jadwal.HARI_DAILY, false);
    }

    /**
     * Mendapatkan jadwal mingguan untuk hari tertentu
     * @param hari Nama hari (Senin, Selasa, dll)
     * @return List jadwal mingguan untuk hari tersebut
     */
    @NonNull
    public List<Jadwal> getJadwalMingguan(@NonNull String hari) {
        return getJadwalByHari(hari, false);
    }

    /**
     * Mendapatkan jadwal berdasarkan ID
     * @param jadwalId ID jadwal yang dicari
     * @return Objek jadwal atau null jika tidak ditemukan
     */
    @Nullable
    public Jadwal getJadwalById(long jadwalId) {
        if (jadwalId <= 0) {
            Log.e(TAG, "Invalid jadwal ID: " + jadwalId);
            return null;
        }

        ensureDatabaseOpen();

        Cursor cursor = null;
        try {
            String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum, o.jenis_obat " +
                    "FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK + " = o." + DbHelper.KEY_OBAT_ID + " " +
                    "WHERE j." + DbHelper.KEY_JADWAL_ID + " = ? AND o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1";

            cursor = database.rawQuery(selectQuery, new String[]{String.valueOf(jadwalId)});

            Jadwal jadwal = null;
            if (cursor != null && cursor.moveToFirst()) {
                jadwal = CursorHelper.cursorToJadwal(cursor);
                Log.d(TAG, "Found jadwal with ID: " + jadwalId);
            } else {
                Log.w(TAG, "Jadwal not found with ID: " + jadwalId);
            }

            return jadwal;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting jadwal by ID: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting jadwal by ID: " + e.getMessage(), e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Update status jadwal dan kurangi stok obat jika sudah diminum
     * @param jadwalId ID jadwal yang akan diupdate
     * @param status Status baru (0: belum diminum, 1: sudah diminum, 2: terlewat)
     * @return Jumlah row yang teraffect
     */
    public int updateJadwalStatus(long jadwalId, int status) {
        return updateJadwalStatus(jadwalId, status, null);
    }

    /**
     * Update status jadwal dengan catatan
     * @param jadwalId ID jadwal yang akan diupdate
     * @param status Status baru
     * @param catatan Catatan tambahan
     * @return Jumlah row yang teraffect
     */
    public int updateJadwalStatus(long jadwalId, int status, @Nullable String catatan) {
        if (jadwalId <= 0) {
            Log.e(TAG, "Invalid jadwal ID for status update: " + jadwalId);
            return 0;
        }

        if (status < Jadwal.STATUS_BELUM_DIMINUM || status > Jadwal.STATUS_TERLEWAT) {
            Log.e(TAG, "Invalid status: " + status);
            return 0;
        }

        ensureDatabaseOpen();

        try {
            // Get current jadwal to check previous status
            Jadwal currentJadwal = getJadwalById(jadwalId);
            if (currentJadwal == null) {
                Log.e(TAG, "Jadwal not found for status update. ID: " + jadwalId);
                return 0;
            }

            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_STATUS, status);

            if (catatan != null) {
                values.put(DbHelper.KEY_JADWAL_CATATAN, catatan.trim());
            }

            // Set tanggal diminum jika status berubah ke sudah diminum
            if (status == Jadwal.STATUS_SUDAH_DIMINUM) {
                long currentTimestamp = System.currentTimeMillis() / 1000; // Unix timestamp
                values.put(DbHelper.KEY_JADWAL_TANGGAL_DIMINUM, currentTimestamp);
            }

            int rowsAffected = database.update(
                    DbHelper.TABLE_JADWAL,
                    values,
                    DbHelper.KEY_JADWAL_ID + " = ?",
                    new String[]{String.valueOf(jadwalId)});

            if (rowsAffected > 0) {
                Log.d(TAG, "Jadwal status updated successfully. ID: " + jadwalId + ", Status: " + status);

                // Kurangi stok obat jika status berubah menjadi sudah diminum
                // dan sebelumnya belum diminum
                if (status == Jadwal.STATUS_SUDAH_DIMINUM &&
                        currentJadwal.getStatus() != Jadwal.STATUS_SUDAH_DIMINUM) {

                    boolean stockReduced = obatHelper.kurangiJumlahObat(currentJadwal.getObatId());
                    if (!stockReduced) {
                        Log.w(TAG, "Failed to reduce stock for obat ID: " + currentJadwal.getObatId());
                    }
                }
            } else {
                Log.w(TAG, "No rows updated for jadwal ID: " + jadwalId);
            }

            return rowsAffected;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error updating jadwal status: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error updating jadwal status: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Update seluruh data jadwal
     * @param jadwal Objek jadwal dengan data yang sudah diupdate
     * @return Jumlah row yang teraffect
     */
    public int updateJadwal(@NonNull Jadwal jadwal) {
        if (jadwal == null) {
            Log.e(TAG, "Cannot update null jadwal");
            return 0;
        }

        if (jadwal.getId() <= 0) {
            Log.e(TAG, "Cannot update jadwal with invalid ID: " + jadwal.getId());
            return 0;
        }

        if (!jadwal.isValid()) {
            Log.e(TAG, "Cannot update invalid jadwal: " + jadwal.toString());
            return 0;
        }

        ensureDatabaseOpen();

        try {
            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_OBAT_ID_FK, jadwal.getObatId());
            values.put(DbHelper.KEY_HARI, jadwal.getHari().trim());
            values.put(DbHelper.KEY_WAKTU, jadwal.getWaktu().trim());
            values.put(DbHelper.KEY_STATUS, jadwal.getStatus());

            if (jadwal.getCatatan() != null) {
                values.put(DbHelper.KEY_JADWAL_CATATAN, jadwal.getCatatan().trim());
            }

            int rowsAffected = database.update(
                    DbHelper.TABLE_JADWAL,
                    values,
                    DbHelper.KEY_JADWAL_ID + " = ?",
                    new String[]{String.valueOf(jadwal.getId())});

            if (rowsAffected > 0) {
                Log.d(TAG, "Jadwal updated successfully. ID: " + jadwal.getId());
            } else {
                Log.w(TAG, "No rows updated for jadwal ID: " + jadwal.getId());
            }

            return rowsAffected;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error updating jadwal: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error updating jadwal: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Hapus jadwal berdasarkan ID
     * @param jadwalId ID jadwal yang akan dihapus
     * @return true jika berhasil, false jika gagal
     */
    public boolean deleteJadwal(long jadwalId) {
        if (jadwalId <= 0) {
            Log.e(TAG, "Invalid jadwal ID for delete: " + jadwalId);
            return false;
        }

        ensureDatabaseOpen();

        try {
            int rowsAffected = database.delete(
                    DbHelper.TABLE_JADWAL,
                    DbHelper.KEY_JADWAL_ID + " = ?",
                    new String[]{String.valueOf(jadwalId)});

            if (rowsAffected > 0) {
                Log.d(TAG, "Jadwal deleted successfully. ID: " + jadwalId);
                return true;
            } else {
                Log.w(TAG, "No rows deleted for jadwal ID: " + jadwalId);
                return false;
            }

        } catch (SQLException e) {
            Log.e(TAG, "SQL error deleting jadwal: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error deleting jadwal: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Hapus semua jadwal untuk obat tertentu
     * @param obatId ID obat yang jadwalnya akan dihapus
     * @return Jumlah jadwal yang terhapus
     */
    public int deleteJadwalByObatId(long obatId) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID for delete jadwal: " + obatId);
            return 0;
        }

        ensureDatabaseOpen();

        try {
            int rowsAffected = database.delete(
                    DbHelper.TABLE_JADWAL,
                    DbHelper.KEY_OBAT_ID_FK + " = ?",
                    new String[]{String.valueOf(obatId)});

            Log.d(TAG, "Deleted " + rowsAffected + " jadwal for obat ID: " + obatId);
            return rowsAffected;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error deleting jadwal by obat ID: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error deleting jadwal by obat ID: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Mendapatkan jadwal yang belum diminum dan sudah melewati waktu
     * @return List jadwal yang terlewat
     */
    @NonNull
    public List<Jadwal> getJadwalTerlewat() {
        List<Jadwal> jadwalHariIni = getJadwalHariIni();
        List<Jadwal> jadwalTerlewat = new ArrayList<>();

        Calendar now = Calendar.getInstance();
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(now.getTime());

        for (Jadwal jadwal : jadwalHariIni) {
            if (jadwal.getStatus() == Jadwal.STATUS_BELUM_DIMINUM) {
                try {
                    Date waktuJadwal = timeFormat.parse(jadwal.getWaktu());
                    Date waktuSekarang = timeFormat.parse(currentTime);

                    if (waktuSekarang.after(waktuJadwal)) {
                        jadwalTerlewat.add(jadwal);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing time for jadwal: " + jadwal.getWaktu(), e);
                }
            }
        }

        Log.d(TAG, "Found " + jadwalTerlewat.size() + " overdue jadwal");
        return jadwalTerlewat;
    }

    /**
     * Mendapatkan statistik jadwal
     * @return Array int [total, sudah_diminum, belum_diminum, terlewat]
     */
    @NonNull
    public int[] getJadwalStatistics() {
        ensureDatabaseOpen();

        try {
            String query = "SELECT " + DbHelper.KEY_STATUS + ", COUNT(*) as count " +
                    "FROM " + DbHelper.TABLE_JADWAL + " j " +
                    "INNER JOIN " + DbHelper.TABLE_OBAT + " o ON j." + DbHelper.KEY_OBAT_ID_FK + " = o." + DbHelper.KEY_OBAT_ID + " " +
                    "WHERE o." + DbHelper.KEY_OBAT_IS_AKTIF + " = 1 " +
                    "GROUP BY " + DbHelper.KEY_STATUS;

            Cursor cursor = database.rawQuery(query, null);

            int[] stats = new int[4]; // [total, sudah_diminum, belum_diminum, terlewat]

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    int status = cursor.getInt(0);
                    int count = cursor.getInt(1);

                    stats[0] += count; // total

                    if (status == Jadwal.STATUS_SUDAH_DIMINUM) {
                        stats[1] = count;
                    } else if (status == Jadwal.STATUS_BELUM_DIMINUM) {
                        stats[2] = count;
                    } else if (status == Jadwal.STATUS_TERLEWAT) {
                        stats[3] = count;
                    }
                } while (cursor.moveToNext());

                cursor.close();
            }

            Log.d(TAG, "Jadwal statistics - Total: " + stats[0] + ", Sudah: " + stats[1] +
                    ", Belum: " + stats[2] + ", Terlewat: " + stats[3]);
            return stats;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting jadwal statistics: " + e.getMessage(), e);
            return new int[4];
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting jadwal statistics: " + e.getMessage(), e);
            return new int[4];
        }
    }

    /**
     * Mengecek apakah database terbuka
     * @return true jika database terbuka
     */
    public boolean isOpen() {
        return isOpen && database != null && database.isOpen();
    }

    /**
     * Batch insert untuk multiple jadwal
     * @param jadwalList List jadwal yang akan disimpan
     * @return Jumlah jadwal yang berhasil disimpan
     */
    public int insertMultipleJadwal(@NonNull List<Jadwal> jadwalList) {
        if (jadwalList == null || jadwalList.isEmpty()) {
            Log.w(TAG, "Empty jadwal list for batch insert");
            return 0;
        }

        ensureDatabaseOpen();

        database.beginTransaction();
        int successCount = 0;

        try {
            for (Jadwal jadwal : jadwalList) {
                if (jadwal != null && jadwal.isValid()) {
                    long result = tambahJadwal(jadwal);
                    if (result != -1) {
                        successCount++;
                    }
                }
            }

            database.setTransactionSuccessful();
            Log.d(TAG, "Batch insert completed. Success: " + successCount + "/" + jadwalList.size());

        } catch (Exception e) {
            Log.e(TAG, "Error in batch insert: " + e.getMessage(), e);
        } finally {
            database.endTransaction();
        }

        return successCount;
    }
}