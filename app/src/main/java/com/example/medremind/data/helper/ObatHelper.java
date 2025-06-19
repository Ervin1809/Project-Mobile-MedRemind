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

import com.example.medremind.data.model.Obat;

import java.util.List;

public class ObatHelper {
    private static final String TAG = "ObatHelper";

    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private Context context;
    private boolean isOpen = false;

    public ObatHelper(@NonNull Context context) {
        this.context = context.getApplicationContext(); // Prevent memory leaks
        this.dbHelper = new DbHelper(this.context);
    }

    /**
     * Membuka koneksi database
     * @throws SQLiteException jika gagal membuka database
     */
    public synchronized void open() throws SQLiteException {
        if (!isOpen) {
            try {
                database = dbHelper.getWritableDatabase();
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
        if (isOpen && database != null) {
            try {
                database.close();
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

    /**
     * Insert obat baru ke database
     * @param obat Objek obat yang akan disimpan
     * @return ID obat yang baru disimpan, atau -1 jika gagal
     */
    public long insertObat(@NonNull Obat obat) {
        if (obat == null) {
            Log.e(TAG, "Cannot insert null obat");
            return -1;
        }

        if (!obat.isValid()) {
            Log.e(TAG, "Cannot insert invalid obat: " + obat.toString());
            return -1;
        }

        ensureDatabaseOpen();

        try {
            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_NAMA_OBAT, obat.getNamaObat().trim());
            values.put(DbHelper.KEY_JENIS_OBAT, obat.getJenisObat().trim());
            values.put(DbHelper.KEY_DOSIS_OBAT, obat.getDosisObat().trim());
            values.put(DbHelper.KEY_ATURAN_MINUM, obat.getAturanMinum().trim());
            values.put(DbHelper.KEY_JUMLAH_OBAT, obat.getJumlahObat());
            values.put(DbHelper.KEY_TIPE_JADWAL, obat.getTipeJadwal());
            values.put(DbHelper.KEY_OBAT_IS_AKTIF, obat.isAktif() ? 1 : 0);

            long result = database.insert(DbHelper.TABLE_OBAT, null, values);

            if (result != -1) {
                Log.d(TAG, "Obat inserted successfully with ID: " + result);
                obat.setId((int) result); // Set ID ke objek obat
            } else {
                Log.e(TAG, "Failed to insert obat");
            }

            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error inserting obat: " + e.getMessage(), e);
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error inserting obat: " + e.getMessage(), e);
            return -1;
        }
    }

    /**
     * Mendapatkan semua obat aktif
     * @return List obat atau empty list jika tidak ada
     */
    @NonNull
    public List<Obat> getAllObat() {
        return getAllObat(true); // Default: hanya obat aktif
    }

    /**
     * Mendapatkan semua obat berdasarkan status aktif
     * @param activeOnly true untuk hanya obat aktif, false untuk semua
     * @return List obat atau empty list jika tidak ada
     */
    @NonNull
    public List<Obat> getAllObat(boolean activeOnly) {
        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT * FROM " + DbHelper.TABLE_OBAT;
            if (activeOnly) {
                selectQuery += " WHERE " + DbHelper.KEY_OBAT_IS_AKTIF + " = 1";
            }
            selectQuery += " ORDER BY " + DbHelper.KEY_OBAT_TANGGAL_DIBUAT + " DESC";

            Cursor cursor = database.rawQuery(selectQuery, null);
            List<Obat> result = CursorHelper.cursorToObatList(cursor);

            Log.d(TAG, "Retrieved " + result.size() + " obat records");
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting all obat: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting all obat: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Mendapatkan obat berdasarkan ID
     * @param obatId ID obat yang dicari
     * @return Objek obat atau null jika tidak ditemukan
     */
    @Nullable
    public Obat getObatById(long obatId) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID: " + obatId);
            return null;
        }

        ensureDatabaseOpen();

        Cursor cursor = null;
        try {
            cursor = database.query(
                    DbHelper.TABLE_OBAT,
                    null, // semua kolom
                    DbHelper.KEY_OBAT_ID + "=? AND " + DbHelper.KEY_OBAT_IS_AKTIF + "=?",
                    new String[]{String.valueOf(obatId), "1"},
                    null, null, null, null);

            Obat obat = null;
            if (cursor != null && cursor.moveToFirst()) {
                obat = CursorHelper.cursorToObat(cursor);
                Log.d(TAG, "Found obat with ID: " + obatId);
            } else {
                Log.w(TAG, "Obat not found with ID: " + obatId);
            }

            return obat;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting obat by ID: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting obat by ID: " + e.getMessage(), e);
            return null;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Update data obat
     * @param obat Objek obat dengan data yang sudah diupdate
     * @return Jumlah row yang teraffect (1 jika berhasil, 0 jika gagal)
     */
    public int updateObat(@NonNull Obat obat) {
        if (obat == null) {
            Log.e(TAG, "Cannot update null obat");
            return 0;
        }

        if (obat.getId() <= 0) {
            Log.e(TAG, "Cannot update obat with invalid ID: " + obat.getId());
            return 0;
        }

        if (!obat.isValid()) {
            Log.e(TAG, "Cannot update invalid obat: " + obat.toString());
            return 0;
        }

        ensureDatabaseOpen();

        try {
            ContentValues values = new ContentValues();
            values.put(DbHelper.KEY_NAMA_OBAT, obat.getNamaObat().trim());
            values.put(DbHelper.KEY_JENIS_OBAT, obat.getJenisObat().trim());
            values.put(DbHelper.KEY_DOSIS_OBAT, obat.getDosisObat().trim());
            values.put(DbHelper.KEY_ATURAN_MINUM, obat.getAturanMinum().trim());
            values.put(DbHelper.KEY_JUMLAH_OBAT, obat.getJumlahObat());
            values.put(DbHelper.KEY_TIPE_JADWAL, obat.getTipeJadwal());
            values.put(DbHelper.KEY_OBAT_IS_AKTIF, obat.isAktif() ? 1 : 0);

            int rowsAffected = database.update(
                    DbHelper.TABLE_OBAT,
                    values,
                    DbHelper.KEY_OBAT_ID + " = ?",
                    new String[]{String.valueOf(obat.getId())});

            if (rowsAffected > 0) {
                Log.d(TAG, "Obat updated successfully. ID: " + obat.getId());
            } else {
                Log.w(TAG, "No rows updated for obat ID: " + obat.getId());
            }

            return rowsAffected;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error updating obat: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error updating obat: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Soft delete obat (mengubah status menjadi tidak aktif)
     * @param obatId ID obat yang akan dihapus
     * @return true jika berhasil, false jika gagal
     */
    public boolean deleteObat(long obatId) {
        return deleteObat(obatId, false); // Default: soft delete
    }

    /**
     * Delete obat (soft delete atau hard delete)
     * @param obatId ID obat yang akan dihapus
     * @param hardDelete true untuk hard delete, false untuk soft delete
     * @return true jika berhasil, false jika gagal
     */
    public boolean deleteObat(long obatId, boolean hardDelete) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID for delete: " + obatId);
            return false;
        }

        ensureDatabaseOpen();

        try {
            int rowsAffected;

            if (hardDelete) {
                // Hard delete: hapus dari database
                // Note: Jadwal terkait akan otomatis terhapus karena CASCADE DELETE
                rowsAffected = database.delete(
                        DbHelper.TABLE_OBAT,
                        DbHelper.KEY_OBAT_ID + " = ?",
                        new String[]{String.valueOf(obatId)});

                Log.d(TAG, "Hard delete obat. Rows affected: " + rowsAffected);
            } else {
                // Soft delete: ubah status menjadi tidak aktif
                ContentValues values = new ContentValues();
                values.put(DbHelper.KEY_OBAT_IS_AKTIF, 0);

                rowsAffected = database.update(
                        DbHelper.TABLE_OBAT,
                        values,
                        DbHelper.KEY_OBAT_ID + " = ?",
                        new String[]{String.valueOf(obatId)});

                Log.d(TAG, "Soft delete obat. Rows affected: " + rowsAffected);
            }

            return rowsAffected > 0;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error deleting obat: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error deleting obat: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mengurangi jumlah obat ketika diminum
     * @param obatId ID obat yang dikurangi
     * @return true jika berhasil, false jika gagal atau stok habis
     */
    public boolean kurangiJumlahObat(long obatId) {
        return kurangiJumlahObat(obatId, 1);
    }

    /**
     * Mengurangi jumlah obat dengan jumlah tertentu
     * @param obatId ID obat yang dikurangi
     * @param jumlah Jumlah yang akan dikurangi
     * @return true jika berhasil, false jika gagal atau stok tidak cukup
     */
    public boolean kurangiJumlahObat(long obatId, int jumlah) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID for reducing stock: " + obatId);
            return false;
        }

        if (jumlah <= 0) {
            Log.e(TAG, "Invalid quantity for reducing stock: " + jumlah);
            return false;
        }

        Obat obat = getObatById(obatId);
        if (obat == null) {
            Log.e(TAG, "Obat not found for reducing stock. ID: " + obatId);
            return false;
        }

        if (obat.getJumlahObat() < jumlah) {
            Log.w(TAG, "Insufficient stock. Current: " + obat.getJumlahObat() + ", Required: " + jumlah);
            return false;
        }

        try {
            obat.setJumlahObat(obat.getJumlahObat() - jumlah);
            int result = updateObat(obat);

            if (result > 0) {
                Log.d(TAG, "Stock reduced successfully. Obat ID: " + obatId + ", Reduced by: " + jumlah);
                return true;
            } else {
                Log.e(TAG, "Failed to update stock for obat ID: " + obatId);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error reducing obat stock: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Menambah jumlah obat (untuk restocking)
     * @param obatId ID obat yang ditambah
     * @param jumlah Jumlah yang akan ditambahkan
     * @return true jika berhasil, false jika gagal
     */
    public boolean tambahJumlahObat(long obatId, int jumlah) {
        if (obatId <= 0) {
            Log.e(TAG, "Invalid obat ID for adding stock: " + obatId);
            return false;
        }

        if (jumlah <= 0) {
            Log.e(TAG, "Invalid quantity for adding stock: " + jumlah);
            return false;
        }

        Obat obat = getObatById(obatId);
        if (obat == null) {
            Log.e(TAG, "Obat not found for adding stock. ID: " + obatId);
            return false;
        }

        try {
            obat.setJumlahObat(obat.getJumlahObat() + jumlah);
            int result = updateObat(obat);

            if (result > 0) {
                Log.d(TAG, "Stock added successfully. Obat ID: " + obatId + ", Added: " + jumlah);
                return true;
            } else {
                Log.e(TAG, "Failed to update stock for obat ID: " + obatId);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding obat stock: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Mengecek apakah obat masih memiliki stok
     * @param obatId ID obat yang dicek
     * @return true jika masih ada stok, false jika habis atau tidak ditemukan
     */
    public boolean hasStock(long obatId) {
        Obat obat = getObatById(obatId);
        return obat != null && obat.getJumlahObat() > 0;
    }

    /**
     * Mendapatkan obat dengan stok rendah (kurang dari minimum)
     * @param minimumStock Jumlah minimum stok
     * @return List obat dengan stok rendah
     */
    @NonNull
    public List<Obat> getObatWithLowStock(int minimumStock) {
        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT * FROM " + DbHelper.TABLE_OBAT +
                    " WHERE " + DbHelper.KEY_JUMLAH_OBAT + " < ? AND " +
                    DbHelper.KEY_OBAT_IS_AKTIF + " = 1" +
                    " ORDER BY " + DbHelper.KEY_JUMLAH_OBAT + " ASC";

            Cursor cursor = database.rawQuery(selectQuery, new String[]{String.valueOf(minimumStock)});
            List<Obat> result = CursorHelper.cursorToObatList(cursor);

            Log.d(TAG, "Found " + result.size() + " obat with low stock (< " + minimumStock + ")");
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting obat with low stock: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting obat with low stock: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        }
    }

    /**
     * Search obat berdasarkan nama
     * @param searchQuery Query pencarian
     * @return List obat yang cocok dengan pencarian
     */
    @NonNull
    public List<Obat> searchObatByName(@NonNull String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            return getAllObat();
        }

        ensureDatabaseOpen();

        try {
            String selectQuery = "SELECT * FROM " + DbHelper.TABLE_OBAT +
                    " WHERE " + DbHelper.KEY_NAMA_OBAT + " LIKE ? AND " +
                    DbHelper.KEY_OBAT_IS_AKTIF + " = 1" +
                    " ORDER BY " + DbHelper.KEY_NAMA_OBAT + " ASC";

            String searchPattern = "%" + searchQuery.trim() + "%";
            Cursor cursor = database.rawQuery(selectQuery, new String[]{searchPattern});
            List<Obat> result = CursorHelper.cursorToObatList(cursor);

            Log.d(TAG, "Found " + result.size() + " obat matching search: " + searchQuery);
            return result;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error searching obat: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error searching obat: " + e.getMessage(), e);
            return new java.util.ArrayList<>();
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
     * Mendapatkan total jumlah obat aktif
     * @return Jumlah total obat aktif
     */
    public int getTotalObatCount() {
        ensureDatabaseOpen();

        try {
            String countQuery = "SELECT COUNT(*) FROM " + DbHelper.TABLE_OBAT +
                    " WHERE " + DbHelper.KEY_OBAT_IS_AKTIF + " = 1";

            Cursor cursor = database.rawQuery(countQuery, null);
            int count = 0;

            if (cursor != null && cursor.moveToFirst()) {
                count = cursor.getInt(0);
                cursor.close();
            }

            Log.d(TAG, "Total active obat count: " + count);
            return count;

        } catch (SQLException e) {
            Log.e(TAG, "SQL error getting total obat count: " + e.getMessage(), e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error getting total obat count: " + e.getMessage(), e);
            return 0;
        }
    }
}