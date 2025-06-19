package com.example.medremind.data.helper;

import android.database.Cursor;

import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.util.ArrayList;
import java.util.List;

public class CursorHelper {
    public static Obat cursorToObat(Cursor cursor) {
        if (cursor == null) return null;

        Obat obat = new Obat();

        // Mendapatkan indeks kolom dengan pengecekan
        int idColIndex = cursor.getColumnIndex(DbHelper.KEY_OBAT_ID);
        int namaObatColIndex = cursor.getColumnIndex(DbHelper.KEY_NAMA_OBAT);
        int jenisObatColIndex = cursor.getColumnIndex(DbHelper.KEY_JENIS_OBAT);
        int dosisObatColIndex = cursor.getColumnIndex(DbHelper.KEY_DOSIS_OBAT);
        int aturanMinumColIndex = cursor.getColumnIndex(DbHelper.KEY_ATURAN_MINUM);
        int jumlahObatColIndex = cursor.getColumnIndex(DbHelper.KEY_JUMLAH_OBAT);
        int tipeJadwalColIndex = cursor.getColumnIndex(DbHelper.KEY_TIPE_JADWAL);

        // Mengisi objek dengan pengecekan indeks
        if (idColIndex != -1) obat.setId(cursor.getInt(idColIndex));
        if (namaObatColIndex != -1) obat.setNamaObat(cursor.getString(namaObatColIndex));
        if (jenisObatColIndex != -1) obat.setJenisObat(cursor.getString(jenisObatColIndex));
        if (dosisObatColIndex != -1) obat.setDosisObat(cursor.getString(dosisObatColIndex));
        if (aturanMinumColIndex != -1) obat.setAturanMinum(cursor.getString(aturanMinumColIndex));
        if (jumlahObatColIndex != -1) obat.setJumlahObat(cursor.getInt(jumlahObatColIndex));
        if (tipeJadwalColIndex != -1) obat.setTipeJadwal(cursor.getString(tipeJadwalColIndex));

        return obat;
    }


    public static List<Obat> cursorToObatList(Cursor cursor) {
        List<Obat> obatList = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                obatList.add(cursorToObat(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return obatList;
    }

    public static Jadwal cursorToJadwal(Cursor cursor) {
        if (cursor == null) return null;

        Jadwal jadwal = new Jadwal();

        // Mendapatkan indeks kolom dengan pengecekan
        int idColIndex = cursor.getColumnIndex(DbHelper.KEY_JADWAL_ID);
        int obatIdColIndex = cursor.getColumnIndex(DbHelper.KEY_OBAT_ID_FK);
        int hariColIndex = cursor.getColumnIndex(DbHelper.KEY_HARI);
        int waktuColIndex = cursor.getColumnIndex(DbHelper.KEY_WAKTU);
        int statusColIndex = cursor.getColumnIndex(DbHelper.KEY_STATUS);

        // Mengisi objek dengan pengecekan indeks
        if (idColIndex != -1) jadwal.setId(cursor.getInt(idColIndex));
        if (obatIdColIndex != -1) jadwal.setObatId(cursor.getInt(obatIdColIndex));
        if (hariColIndex != -1) jadwal.setHari(cursor.getString(hariColIndex));
        if (waktuColIndex != -1) jadwal.setWaktu(cursor.getString(waktuColIndex));
        if (statusColIndex != -1) jadwal.setStatus(cursor.getInt(statusColIndex));

        // Mencoba mendapatkan informasi obat terkait jika ada
        try {
            int namaObatColIndex = cursor.getColumnIndex("nama_obat");
            int dosisObatColIndex = cursor.getColumnIndex("dosis_obat");
            int aturanMinumColIndex = cursor.getColumnIndex("aturan_minum");

            if (namaObatColIndex != -1) jadwal.setTambahan("namaObat", cursor.getString(namaObatColIndex));
            if (dosisObatColIndex != -1) jadwal.setTambahan("dosisObat", cursor.getString(dosisObatColIndex));
            if (aturanMinumColIndex != -1) jadwal.setTambahan("aturanMinum", cursor.getString(aturanMinumColIndex));
        } catch (IllegalArgumentException e) {
            // Jika kolom tidak ditemukan, abaikan saja
        }

        return jadwal;
    }

    public static List<Jadwal> cursorToJadwalList(Cursor cursor) {
        List<Jadwal> jadwalList = new ArrayList<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                jadwalList.add(cursorToJadwal(cursor));
            } while (cursor.moveToNext());
            cursor.close();
        }

        return jadwalList;
    }
}