package com.example.medremind.data.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.medremind.data.model.Obat;

import java.util.List;

public class ObatHelper {
    private SQLiteDatabase database;
    private DbHelper dbHelper;

    public ObatHelper(Context context) {
        dbHelper = new DbHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (database != null && database.isOpen()) {
            database.close();
        }
        dbHelper.close();
    }

    public long tambahObat(Obat obat) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.KEY_NAMA_OBAT, obat.getNamaObat());
        values.put(DbHelper.KEY_JENIS_OBAT, obat.getJenisObat());
        values.put(DbHelper.KEY_DOSIS_OBAT, obat.getDosisObat());
        values.put(DbHelper.KEY_ATURAN_MINUM, obat.getAturanMinum());
        values.put(DbHelper.KEY_JUMLAH_OBAT, obat.getJumlahObat());
        values.put(DbHelper.KEY_TIPE_JADWAL, obat.getTipeJadwal());

        return database.insert(DbHelper.TABLE_OBAT, null, values);
    }

    public List<Obat> getAllObat() {
        String selectQuery = "SELECT * FROM " + DbHelper.TABLE_OBAT;
        Cursor cursor = database.rawQuery(selectQuery, null);
        return CursorHelper.cursorToObatList(cursor);
    }

    public Obat getObatById(long obatId) {
        Cursor cursor = database.query(
                DbHelper.TABLE_OBAT,
                null, // null berarti ambil semua kolom
                DbHelper.KEY_OBAT_ID + "=?",
                new String[] { String.valueOf(obatId) },
                null, null, null, null);

        Obat obat = null;
        if (cursor != null && cursor.moveToFirst()) {
            obat = CursorHelper.cursorToObat(cursor);
            cursor.close();
        }

        return obat;
    }

    public int updateObat(Obat obat) {
        ContentValues values = new ContentValues();
        values.put(DbHelper.KEY_NAMA_OBAT, obat.getNamaObat());
        values.put(DbHelper.KEY_JENIS_OBAT, obat.getJenisObat());
        values.put(DbHelper.KEY_DOSIS_OBAT, obat.getDosisObat());
        values.put(DbHelper.KEY_ATURAN_MINUM, obat.getAturanMinum());
        values.put(DbHelper.KEY_JUMLAH_OBAT, obat.getJumlahObat());
        values.put(DbHelper.KEY_TIPE_JADWAL, obat.getTipeJadwal());

        return database.update(DbHelper.TABLE_OBAT, values,
                DbHelper.KEY_OBAT_ID + " = ?",
                new String[] { String.valueOf(obat.getId()) });
    }

    public void deleteObat(long obatId) {
        database.delete(DbHelper.TABLE_OBAT,
                DbHelper.KEY_OBAT_ID + " = ?",
                new String[] { String.valueOf(obatId) });
    }

    public boolean kurangiJumlahObat(long obatId) {
        Obat obat = getObatById(obatId);
        if (obat != null && obat.getJumlahObat() > 0) {
            obat.setJumlahObat(obat.getJumlahObat() - 1);
            updateObat(obat);
            return true;
        }
        return false;
    }
}