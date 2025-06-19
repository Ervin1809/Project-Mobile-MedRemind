package com.example.medremind.data.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class JadwalHelper {
    private SQLiteDatabase database;
    private DbHelper dbHelper;
    private ObatHelper obatHelper;

    public JadwalHelper(Context context) {
        dbHelper = new DbHelper(context);
        obatHelper = new ObatHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
        obatHelper.open();
    }

    public void close() {
        obatHelper.close();
        if (database != null && database.isOpen()) {
            database.close();
        }
    }

    public long tambahJadwal(Jadwal jadwal) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.KEY_OBAT_ID_FK, jadwal.getObatId());
        values.put(DbHelper.KEY_HARI, jadwal.getHari());
        values.put(DbHelper.KEY_WAKTU, jadwal.getWaktu());
        values.put(DbHelper.KEY_STATUS, jadwal.getStatus());

        return database.insert(DbHelper.TABLE_JADWAL, null, values);
    }


    public List<Jadwal> getAllJadwal() {
        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "ORDER BY j.waktu";

        Cursor cursor = database.rawQuery(selectQuery, null);
        return CursorHelper.cursorToJadwalList(cursor);
    }


    public List<Jadwal> getJadwalByObatId(long obatId) {
        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "WHERE j.obat_id = ? " +
                "ORDER BY j.waktu";

        Cursor cursor = database.rawQuery(selectQuery, new String[]{String.valueOf(obatId)});
        return CursorHelper.cursorToJadwalList(cursor);
    }


    public List<Jadwal> getJadwalHariIni() {
        // Mendapatkan nama hari dalam bahasa Indonesia
        String[] hariNames = {"Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"};
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String hariIni = hariNames[dayOfWeek - 1]; // Calendar.SUNDAY = 1, dll

        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "WHERE j.hari = 'daily' OR j.hari = ? " +
                "ORDER BY j.waktu";

        Cursor cursor = database.rawQuery(selectQuery, new String[]{hariIni});
        return CursorHelper.cursorToJadwalList(cursor);
    }

    public List<Jadwal> getJadwalHarian() {
        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "WHERE j.hari = 'daily' " +
                "ORDER BY j.waktu";

        Cursor cursor = database.rawQuery(selectQuery, null);
        return CursorHelper.cursorToJadwalList(cursor);
    }

    public List<Jadwal> getJadwalMingguan(String hari) {
        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "WHERE j.hari = ? " +
                "ORDER BY j.waktu";

        Cursor cursor = database.rawQuery(selectQuery, new String[]{hari});
        return CursorHelper.cursorToJadwalList(cursor);
    }

    public Jadwal getJadwalById(long jadwalId) {
        String selectQuery = "SELECT j.*, o.nama_obat, o.dosis_obat, o.aturan_minum " +
                "FROM " + DbHelper.TABLE_JADWAL + " j " +
                "JOIN " + DbHelper.TABLE_OBAT + " o ON j.obat_id = o.id " +
                "WHERE j.id = ?";

        Cursor cursor = database.rawQuery(selectQuery, new String[]{String.valueOf(jadwalId)});

        Jadwal jadwal = null;
        if (cursor != null && cursor.moveToFirst()) {
            jadwal = CursorHelper.cursorToJadwal(cursor);
            cursor.close();
        }

        return jadwal;
    }

    public int updateJadwalStatus(long jadwalId, int status) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.KEY_STATUS, status);

        // Jika status = 1 (sudah diminum), kurangi jumlah obat
        if (status == 1) {
            Jadwal jadwal = getJadwalById(jadwalId);
            if (jadwal != null) {
                obatHelper.kurangiJumlahObat(jadwal.getObatId());
            }
        }

        return database.update(DbHelper.TABLE_JADWAL, values,
                DbHelper.KEY_JADWAL_ID + " = ?",
                new String[] { String.valueOf(jadwalId) });
    }

    public int updateJadwal(Jadwal jadwal) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.KEY_OBAT_ID_FK, jadwal.getObatId());
        values.put(DbHelper.KEY_HARI, jadwal.getHari());
        values.put(DbHelper.KEY_WAKTU, jadwal.getWaktu());
        values.put(DbHelper.KEY_STATUS, jadwal.getStatus());

        return database.update(DbHelper.TABLE_JADWAL, values,
                DbHelper.KEY_JADWAL_ID + " = ?",
                new String[] { String.valueOf(jadwal.getId()) });
    }

    public void deleteJadwal(long jadwalId) {
        database.delete(DbHelper.TABLE_JADWAL,
                DbHelper.KEY_JADWAL_ID + " = ?",
                new String[] { String.valueOf(jadwalId) });
    }

    public void deleteJadwalByObatId(long obatId) {
        database.delete(DbHelper.TABLE_JADWAL,
                DbHelper.KEY_OBAT_ID_FK + " = ?",
                new String[] { String.valueOf(obatId) });
    }
}