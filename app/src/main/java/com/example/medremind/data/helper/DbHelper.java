package com.example.medremind.data.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "DbHelper";

    // Database Info
    public static final String DATABASE_NAME = "MediReminderDatabase";
    public static final int DATABASE_VERSION = 3; // 🔑 INCREMENT untuk daily reset

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
    public static final String KEY_TIPE_JADWAL = "tipe_jadwal";
    public static final String KEY_OBAT_TANGGAL_DIBUAT = "tanggal_dibuat";
    public static final String KEY_OBAT_TANGGAL_DIPERBARUI = "tanggal_diperbarui";
    public static final String KEY_OBAT_IS_AKTIF = "is_aktif";

    // Table Jadwal - Column Names
    public static final String KEY_JADWAL_ID = "id";
    public static final String KEY_OBAT_ID_FK = "obat_id";
    public static final String KEY_HARI = "hari";
    public static final String KEY_WAKTU = "waktu";
    public static final String KEY_STATUS = "status";
    public static final String KEY_JADWAL_TANGGAL_DIBUAT = "tanggal_dibuat";
    public static final String KEY_JADWAL_TANGGAL_DIPERBARUI = "tanggal_diperbarui";
    public static final String KEY_JADWAL_TANGGAL_DIMINUM = "tanggal_diminum";
    public static final String KEY_JADWAL_CATATAN = "catatan";
    public static final String KEY_LAST_RESET_DATE = "last_reset_date"; // 🔑 NEW COLUMN

    // Table Create Statements dengan constraints yang lebih baik
    private static final String CREATE_TABLE_OBAT = "CREATE TABLE " + TABLE_OBAT + "("
            + KEY_OBAT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_NAMA_OBAT + " TEXT NOT NULL,"
            + KEY_JENIS_OBAT + " TEXT NOT NULL,"
            + KEY_DOSIS_OBAT + " TEXT NOT NULL,"
            + KEY_ATURAN_MINUM + " TEXT NOT NULL,"
            + KEY_JUMLAH_OBAT + " INTEGER NOT NULL DEFAULT 0 CHECK(" + KEY_JUMLAH_OBAT + " >= 0),"
            + KEY_TIPE_JADWAL + " TEXT NOT NULL CHECK(" + KEY_TIPE_JADWAL + " IN ('harian', 'mingguan')),"
            + KEY_OBAT_TANGGAL_DIBUAT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + KEY_OBAT_TANGGAL_DIPERBARUI + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + KEY_OBAT_IS_AKTIF + " INTEGER NOT NULL DEFAULT 1 CHECK(" + KEY_OBAT_IS_AKTIF + " IN (0, 1))"
            + ")";

    private static final String CREATE_TABLE_JADWAL = "CREATE TABLE " + TABLE_JADWAL + "("
            + KEY_JADWAL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_OBAT_ID_FK + " INTEGER NOT NULL,"
            + KEY_HARI + " TEXT NOT NULL,"
            + KEY_WAKTU + " TEXT NOT NULL,"
            + KEY_STATUS + " INTEGER NOT NULL DEFAULT 0 CHECK(" + KEY_STATUS + " IN (0, 1, 2)),"
            + KEY_JADWAL_TANGGAL_DIBUAT + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + KEY_JADWAL_TANGGAL_DIPERBARUI + " INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),"
            + KEY_JADWAL_TANGGAL_DIMINUM + " INTEGER,"
            + KEY_JADWAL_CATATAN + " TEXT,"
            + KEY_LAST_RESET_DATE + " TEXT DEFAULT NULL," // 🔑 NEW COLUMN
            + "FOREIGN KEY(" + KEY_OBAT_ID_FK + ") REFERENCES " + TABLE_OBAT + "(" + KEY_OBAT_ID + ") ON DELETE CASCADE"
            + ")";

    // Indexes untuk performa yang lebih baik
    private static final String CREATE_INDEX_JADWAL_OBAT_ID =
            "CREATE INDEX idx_jadwal_obat_id ON " + TABLE_JADWAL + "(" + KEY_OBAT_ID_FK + ")";

    private static final String CREATE_INDEX_JADWAL_HARI_WAKTU =
            "CREATE INDEX idx_jadwal_hari_waktu ON " + TABLE_JADWAL + "(" + KEY_HARI + ", " + KEY_WAKTU + ")";

    private static final String CREATE_INDEX_JADWAL_STATUS =
            "CREATE INDEX idx_jadwal_status ON " + TABLE_JADWAL + "(" + KEY_STATUS + ")";

    // 🔑 NEW INDEX untuk daily reset
    private static final String CREATE_INDEX_JADWAL_RESET_DATE =
            "CREATE INDEX idx_jadwal_reset_date ON " + TABLE_JADWAL + "(" + KEY_LAST_RESET_DATE + ")";

    // Triggers untuk auto-update timestamp
    private static final String CREATE_TRIGGER_OBAT_UPDATE =
            "CREATE TRIGGER trigger_obat_update AFTER UPDATE ON " + TABLE_OBAT + " " +
                    "BEGIN " +
                    "UPDATE " + TABLE_OBAT + " SET " + KEY_OBAT_TANGGAL_DIPERBARUI + " = strftime('%s', 'now') " +
                    "WHERE " + KEY_OBAT_ID + " = NEW." + KEY_OBAT_ID + "; " +
                    "END";

    private static final String CREATE_TRIGGER_JADWAL_UPDATE =
            "CREATE TRIGGER trigger_jadwal_update AFTER UPDATE ON " + TABLE_JADWAL + " " +
                    "BEGIN " +
                    "UPDATE " + TABLE_JADWAL + " SET " + KEY_JADWAL_TANGGAL_DIPERBARUI + " = strftime('%s', 'now') " +
                    "WHERE " + KEY_JADWAL_ID + " = NEW." + KEY_JADWAL_ID + "; " +
                    "END";

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");

            // Creating required tables
            db.execSQL(CREATE_TABLE_OBAT);
            db.execSQL(CREATE_TABLE_JADWAL);

            // Creating indexes
            db.execSQL(CREATE_INDEX_JADWAL_OBAT_ID);
            db.execSQL(CREATE_INDEX_JADWAL_HARI_WAKTU);
            db.execSQL(CREATE_INDEX_JADWAL_STATUS);
            db.execSQL(CREATE_INDEX_JADWAL_RESET_DATE); // 🔑 NEW INDEX

            // Creating triggers
            db.execSQL(CREATE_TRIGGER_OBAT_UPDATE);
            db.execSQL(CREATE_TRIGGER_JADWAL_UPDATE);

            Log.d(TAG, "Database created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database: " + e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        try {
            // Migration logic berdasarkan versi
            if (oldVersion < 2) {
                // Migration dari versi 1 ke 2
                migrateFromV1ToV2(db);
            }
            if (oldVersion < 3) {
                // 🔑 Migration dari versi 2 ke 3 (Daily Reset)
                migrateFromV2ToV3(db);
            }

            Log.d(TAG, "Database upgrade completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage(), e);
            // Fallback: recreate tables
            recreateTables(db);
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key constraints every time database is opened
        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    private void migrateFromV1ToV2(SQLiteDatabase db) {
        try {
            // Add new columns to existing tables
            db.execSQL("ALTER TABLE " + TABLE_OBAT + " ADD COLUMN " + KEY_OBAT_TANGGAL_DIBUAT + " INTEGER DEFAULT (strftime('%s', 'now'))");
            db.execSQL("ALTER TABLE " + TABLE_OBAT + " ADD COLUMN " + KEY_OBAT_TANGGAL_DIPERBARUI + " INTEGER DEFAULT (strftime('%s', 'now'))");
            db.execSQL("ALTER TABLE " + TABLE_OBAT + " ADD COLUMN " + KEY_OBAT_IS_AKTIF + " INTEGER DEFAULT 1");

            db.execSQL("ALTER TABLE " + TABLE_JADWAL + " ADD COLUMN " + KEY_JADWAL_TANGGAL_DIBUAT + " INTEGER DEFAULT (strftime('%s', 'now'))");
            db.execSQL("ALTER TABLE " + TABLE_JADWAL + " ADD COLUMN " + KEY_JADWAL_TANGGAL_DIPERBARUI + " INTEGER DEFAULT (strftime('%s', 'now'))");
            db.execSQL("ALTER TABLE " + TABLE_JADWAL + " ADD COLUMN " + KEY_JADWAL_TANGGAL_DIMINUM + " INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_JADWAL + " ADD COLUMN " + KEY_JADWAL_CATATAN + " TEXT");

            // Create indexes and triggers
            db.execSQL(CREATE_INDEX_JADWAL_OBAT_ID);
            db.execSQL(CREATE_INDEX_JADWAL_HARI_WAKTU);
            db.execSQL(CREATE_INDEX_JADWAL_STATUS);
            db.execSQL(CREATE_TRIGGER_OBAT_UPDATE);
            db.execSQL(CREATE_TRIGGER_JADWAL_UPDATE);

            Log.d(TAG, "Migration from V1 to V2 completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in migration from V1 to V2: " + e.getMessage(), e);
            throw e;
        }
    }

    // 🔑 NEW MIGRATION METHOD
    private void migrateFromV2ToV3(SQLiteDatabase db) {
        try {
            // Add last_reset_date column for daily reset functionality
            db.execSQL("ALTER TABLE " + TABLE_JADWAL + " ADD COLUMN " + KEY_LAST_RESET_DATE + " TEXT DEFAULT NULL");

            // Create index for reset date
            db.execSQL(CREATE_INDEX_JADWAL_RESET_DATE);

            Log.d(TAG, "Migration from V2 to V3 completed - Added daily reset functionality");
        } catch (Exception e) {
            Log.e(TAG, "Error in migration from V2 to V3: " + e.getMessage(), e);
            throw e;
        }
    }

    private void recreateTables(SQLiteDatabase db) {
        // Drop existing tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_JADWAL);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_OBAT);

        // Recreate tables
        onCreate(db);
    }

    // Utility method untuk mendapatkan database version info
    public static String getDatabaseInfo() {
        return "Database: " + DATABASE_NAME + ", Version: " + DATABASE_VERSION;
    }
}