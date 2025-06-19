package com.example.medremind.data.helper;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CursorHelper {
    private static final String TAG = "CursorHelper";

    @Nullable
    public static Obat cursorToObat(@Nullable Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            Log.w(TAG, "Cursor is null or closed");
            return null;
        }

        try {
            Obat obat = new Obat();

            // Mengisi data dengan safe column access
            obat.setId(getIntValue(cursor, DbHelper.KEY_OBAT_ID, 0));
            obat.setNamaObat(getStringValue(cursor, DbHelper.KEY_NAMA_OBAT, ""));
            obat.setJenisObat(getStringValue(cursor, DbHelper.KEY_JENIS_OBAT, ""));
            obat.setDosisObat(getStringValue(cursor, DbHelper.KEY_DOSIS_OBAT, ""));
            obat.setAturanMinum(getStringValue(cursor, DbHelper.KEY_ATURAN_MINUM, ""));
            obat.setJumlahObat(getIntValue(cursor, DbHelper.KEY_JUMLAH_OBAT, 0));
            obat.setTipeJadwal(getStringValue(cursor, DbHelper.KEY_TIPE_JADWAL, "harian"));

            // Set timestamp jika ada
            long tanggalDibuat = getLongValue(cursor, DbHelper.KEY_OBAT_TANGGAL_DIBUAT, 0);
            if (tanggalDibuat > 0) {
                obat.setTanggalDibuat(new Date(tanggalDibuat * 1000));
            }

            long tanggalDiperbarui = getLongValue(cursor, DbHelper.KEY_OBAT_TANGGAL_DIPERBARUI, 0);
            if (tanggalDiperbarui > 0) {
                obat.setTanggalDiperbarui(new Date(tanggalDiperbarui * 1000));
            }

            obat.setAktif(getIntValue(cursor, DbHelper.KEY_OBAT_IS_AKTIF, 1) == 1);

            return obat;

        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Obat: " + e.getMessage(), e);
            return null;
        }
    }

    @NonNull
    public static List<Obat> cursorToObatList(@Nullable Cursor cursor) {
        List<Obat> obatList = new ArrayList<>();

        if (cursor == null) {
            Log.w(TAG, "Cursor is null");
            return obatList;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    Obat obat = cursorToObat(cursor);
                    if (obat != null) {
                        obatList.add(obat);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Obat list: " + e.getMessage(), e);
        } finally {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        return obatList;
    }

    @Nullable
    public static Jadwal cursorToJadwal(@Nullable Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            Log.w(TAG, "Cursor is null or closed");
            return null;
        }

        try {
            Jadwal jadwal = new Jadwal();

            // Mengisi data dengan safe column access
            jadwal.setId(getIntValue(cursor, DbHelper.KEY_JADWAL_ID, 0));
            jadwal.setObatId(getIntValue(cursor, DbHelper.KEY_OBAT_ID_FK, 0));
            jadwal.setHari(getStringValue(cursor, DbHelper.KEY_HARI, ""));
            jadwal.setWaktu(getStringValue(cursor, DbHelper.KEY_WAKTU, ""));
            jadwal.setStatus(getIntValue(cursor, DbHelper.KEY_STATUS, 0));
            jadwal.setCatatan(getStringValue(cursor, DbHelper.KEY_JADWAL_CATATAN, null));

            // Set timestamp jika ada
            long tanggalDibuat = getLongValue(cursor, DbHelper.KEY_JADWAL_TANGGAL_DIBUAT, 0);
            if (tanggalDibuat > 0) {
                jadwal.setTanggalDibuat(new Date(tanggalDibuat * 1000));
            }

            long tanggalDiperbarui = getLongValue(cursor, DbHelper.KEY_JADWAL_TANGGAL_DIPERBARUI, 0);
            if (tanggalDiperbarui > 0) {
                jadwal.setTanggalDiperbarui(new Date(tanggalDiperbarui * 1000));
            }

            long tanggalDiminum = getLongValue(cursor, DbHelper.KEY_JADWAL_TANGGAL_DIMINUM, 0);
            if (tanggalDiminum > 0) {
                jadwal.setTanggalDiminum(new Date(tanggalDiminum * 1000));
            }

            // Mencoba mendapatkan informasi obat terkait jika ada (dari JOIN)
            String namaObat = getStringValue(cursor, "nama_obat", null);
            if (namaObat != null) {
                jadwal.setTambahan("namaObat", namaObat);
                jadwal.setTambahan("dosisObat", getStringValue(cursor, "dosis_obat", ""));
                jadwal.setTambahan("aturanMinum", getStringValue(cursor, "aturan_minum", ""));
                jadwal.setTambahan("jenisObat", getStringValue(cursor, "jenis_obat", ""));
            }

            return jadwal;

        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Jadwal: " + e.getMessage(), e);
            return null;
        }
    }

    @NonNull
    public static List<Jadwal> cursorToJadwalList(@Nullable Cursor cursor) {
        List<Jadwal> jadwalList = new ArrayList<>();

        if (cursor == null) {
            Log.w(TAG, "Cursor is null");
            return jadwalList;
        }

        try {
            if (cursor.moveToFirst()) {
                do {
                    Jadwal jadwal = cursorToJadwal(cursor);
                    if (jadwal != null) {
                        jadwalList.add(jadwal);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error converting cursor to Jadwal list: " + e.getMessage(), e);
        } finally {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }

        return jadwalList;
    }

    // Helper methods untuk safe column access
    private static String getStringValue(@NonNull Cursor cursor, @NonNull String columnName, @Nullable String defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
            return cursor.getString(columnIndex);
        }
        return defaultValue;
    }

    private static int getIntValue(@NonNull Cursor cursor, @NonNull String columnName, int defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
            return cursor.getInt(columnIndex);
        }
        return defaultValue;
    }

    private static long getLongValue(@NonNull Cursor cursor, @NonNull String columnName, long defaultValue) {
        int columnIndex = cursor.getColumnIndex(columnName);
        if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
            return cursor.getLong(columnIndex);
        }
        return defaultValue;
    }
}