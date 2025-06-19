package com.example.medremind.data.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    // Database Info
    public static final String DATABASE_NAME = "MediReminderDatabase";
    public static final int DATABASE_VERSION = 1;

    // Table Names
    public static final String TABLE_OBAT = "obat";
    public static final String TABLE_JADWAL = "jadwal";

    // Table Obat - Column Names
    public static final String KEY_OBAT_ID = "id";
    public static final String KEY_NAMA_OBAT = "nama_obat";
    public static final String KEY_JENIS_OBAT = "jenis_obat";
    public static final String KEY_DOSIS_OBAT = "dosis_obat";
    public static final String KEY_ATURAN_MINUM = "aturan_minum";
    public static final String KEY_JUMLAH_OBAT = "jumlah_obat";
    public static final String KEY_TIPE_JADWAL = "tipe_jadwal"; // 'harian' atau 'mingguan'

    // Table Jadwal - Column Names
    public static final String KEY_JADWAL_ID = "id";
    public static final String KEY_OBAT_ID_FK = "obat_id";
    public static final String KEY_HARI = "hari"; // 'daily' untuk jadwal harian, atau nama hari untuk jadwal mingguan
    public static final String KEY_WAKTU = "waktu"; // format waktu HH:MM
    public static final String KEY_STATUS = "status"; // 0: belum diminum, 1: sudah diminum

    // Table Create Statements
    // Table Obat create statement
    private static final String CREATE_TABLE_OBAT = "CREATE TABLE " + TABLE_OBAT + "("
            + KEY_OBAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_NAMA_OBAT + " TEXT NOT NULL,"
            + KEY_JENIS_OBAT + " TEXT,"
            + KEY_DOSIS_OBAT + " TEXT,"
            + KEY_ATURAN_MINUM + " TEXT,"
            + KEY_JUMLAH_OBAT + " INTEGER,"
            + KEY_TIPE_JADWAL + " TEXT"
            + ")";

    // Table Jadwal create statement
    private static final String CREATE_TABLE_JADWAL = "CREATE TABLE " + TABLE_JADWAL + "("
            + KEY_JADWAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_OBAT_ID_FK + " INTEGER,"
            + KEY_HARI + " TEXT,"
            + KEY_WAKTU + " TEXT,"
            + KEY_STATUS + " INTEGER DEFAULT 0,"
            + "FOREIGN KEY(" + KEY_OBAT_ID_FK + ") REFERENCES " + TABLE_OBAT + "(" + KEY_OBAT_ID + ")"
            + ")";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creating required tables
        db.execSQL(CREATE_TABLE_OBAT);
        db.execSQL(CREATE_TABLE_JADWAL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // On upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JADWAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OBAT);

        // Create new tables
        onCreate(db);
    }
}