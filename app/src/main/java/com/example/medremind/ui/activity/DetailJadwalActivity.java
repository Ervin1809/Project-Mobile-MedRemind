package com.example.medremind.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.medremind.R;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailJadwalActivity extends AppCompatActivity {
    private static final String TAG = "DetailJadwalActivity";

    // Intent extras
    public static final String EXTRA_OBAT_ID = "obat_id";
    public static final String EXTRA_OBAT_NAMA = "obat_nama";

    // UI Components
    private TextView tvJumlahMakan, tvTipeJadwal, tvNamaObat, tvDosisObat;
    private TextView tvAturanMinum, tvSisaObat, tvDailyProgress, tvTutup;
    private TableLayout tableJadwal;
    private MaterialButton btnSudahMinum, btnLewati, btnHapusObat;

    // Data
    private int obatId;
    private String obatNama;
    private Obat obatDetail, Obat1;
    private List<Jadwal> jadwalList;
    private List<Jadwal> todayJadwal;

    // Database helpers
    private ObatHelper obatHelper;
    private JadwalHelper jadwalHelper;

    // Background executor
    private ExecutorService executor;

    /**
     * Static method untuk start activity dengan data obat
     */
    public static void start(@NonNull Context context, int obatId, @NonNull String obatNama) {
        Intent intent = new Intent(context, DetailJadwalActivity.class);
        intent.putExtra(EXTRA_OBAT_ID, obatId);
        intent.putExtra(EXTRA_OBAT_NAMA, obatNama);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_jadwal);

        initializeComponents();
        loadIntentData();
        setupClickListeners();

        // Load data
        loadObatDetail();

        Log.d(TAG, "DetailObatActivity created for obat: " + obatNama);
    }

    private void initializeComponents() {
        // Initialize views
        tvJumlahMakan = findViewById(R.id.jumlah_makan);
        tvTipeJadwal = findViewById(R.id.tipe_jadwal);
        tvNamaObat = findViewById(R.id.nama_obat);
        tvDosisObat = findViewById(R.id.dosis_obat);
        tvAturanMinum = findViewById(R.id.aturan_minum);
        tvSisaObat = findViewById(R.id.sisa_obat);
        tvDailyProgress = findViewById(R.id.daily_progress);
        tvTutup = findViewById(R.id.tutup);
        tableJadwal = findViewById(R.id.table_jadwal);
        btnSudahMinum = findViewById(R.id.btn_sudah_minum);
        btnLewati = findViewById(R.id.btn_lewati);
        btnHapusObat = findViewById(R.id.btn_hapus_obat);

        // Initialize database helpers
        obatHelper = new ObatHelper(this);
        jadwalHelper = new JadwalHelper(this);

        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        // Initialize lists
        jadwalList = new ArrayList<>();
        todayJadwal = new ArrayList<>();
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            obatId = intent.getIntExtra(EXTRA_OBAT_ID, -1);
            obatNama = intent.getStringExtra(EXTRA_OBAT_NAMA);

            if (obatId == -1 || obatNama == null) {
                Log.e(TAG, "Invalid intent data. ObatId: " + obatId + ", ObatNama: " + obatNama);
                Toast.makeText(this, "Error: Data obat tidak valid", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d(TAG, "Loaded intent data - ObatId: " + obatId + ", ObatNama: " + obatNama);
        } else {
            Log.e(TAG, "Intent is null");
            Toast.makeText(this, "Error: Intent tidak ditemukan", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupClickListeners() {
        tvTutup.setOnClickListener(v -> {
            Log.d(TAG, "Tutup button clicked");
            finish(); // Close activity
        });

        btnSudahMinum.setOnClickListener(v -> handleSudahMinumClick());
        btnLewati.setOnClickListener(v -> handleLewatiClick());
        btnHapusObat.setOnClickListener(v -> showDeleteConfirmDialog());
    }

    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Obat")
                .setMessage("Apakah Anda yakin ingin menghapus obat '" + obatNama + "' secara permanen? Tindakan ini tidak dapat dibatalkan.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Ya, Hapus", (dialog, which) -> deleteCurrentObat())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteCurrentObat() {
        executor.execute(() -> {
            try {
                obatHelper.open();
                boolean success = obatHelper.deleteObat(obatId, true); // true for hard delete
                obatHelper.close();

                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(DetailJadwalActivity.this, "Obat '" + obatNama + "' berhasil dihapus.", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity after deletion
                    } else {
                        Toast.makeText(DetailJadwalActivity.this, "Gagal menghapus obat.", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error deleting obat: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(DetailJadwalActivity.this, "Terjadi error saat menghapus obat.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadObatDetail() {
        // Load data in background
        executor.execute(() -> {
            try {
                // Open database connections
                obatHelper.open();
                jadwalHelper.open();

                // 🔑 Perform daily reset dan auto-mark
                jadwalHelper.checkAndPerformDailyReset();
                jadwalHelper.autoMarkTerlewatJadwal();

                // Get obat detail
                obatDetail = obatHelper.getObatById(obatId);

                if (obatDetail == null) {
                    throw new Exception("Obat tidak ditemukan dengan ID: " + obatId);
                }

                // Get jadwal list
                jadwalList = jadwalHelper.getJadwalByObatId(obatId);

                // Update UI on main thread
                runOnUiThread(() -> updateUI(obatDetail, jadwalList));

            } catch (Exception e) {
                Log.e(TAG, "Error loading obat detail: " + e.getMessage(), e);
                runOnUiThread(() -> handleLoadError(e));
            } finally {
                // Close database connections
                try {
                    obatHelper.close();
                    jadwalHelper.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing database: " + e.getMessage(), e);
                }
            }
        });
    }

    private void updateUI(@NonNull Obat obat, @NonNull List<Jadwal> jadwalList) {
        try {
            // Update basic obat info
            tvNamaObat.setText(obat.getNamaObat());
            tvDosisObat.setText(obat.getDosisObat());
            tvAturanMinum.setText(obat.getAturanMinum().toUpperCase());
            tvSisaObat.setText(obat.getJumlahObat() + " Tablet");
            Obat1 = obat;

            // Set tipe jadwal dan jumlah makan
            String tipeJadwal = obat.getTipeJadwal();
            int jumlahMakan = jadwalList.size();

            if (tipeJadwal != null) {
                if (tipeJadwal.equalsIgnoreCase("harian") || tipeJadwal.equalsIgnoreCase("daily")) {
                    tvTipeJadwal.setText("Sehari");
                } else {
                    tvTipeJadwal.setText("Seminggu");
                }
            }

            tvJumlahMakan.setText(jumlahMakan + " x");

            // Populate jadwal table
            populateJadwalTable(jadwalList);

            // Calculate dan set daily progress
            calculateDailyProgress();

            // Update action buttons
            updateActionButtons();

            Log.d(TAG, "UI updated successfully with " + jadwalList.size() + " jadwal");

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            Toast.makeText(this, "Error menampilkan data", Toast.LENGTH_SHORT).show();
        }
    }

    private void populateJadwalTable(@NonNull List<Jadwal> jadwalList) {
        if (tableJadwal == null) return;

        // Clear existing rows (keep header)
        int childCount = tableJadwal.getChildCount();
        if (childCount > 1) {
            tableJadwal.removeViews(1, childCount - 1);
        }

        if (jadwalList.isEmpty()) {
            addJadwalRow("Tidak ada jadwal", "BELUM ADA", R.color.gray, -1);
            return;
        }

        // Filter jadwal untuk hari ini DAN hari lain dalam seminggu
        todayJadwal = filterJadwalForToday(jadwalList);
        List<Jadwal> weeklyJadwal = filterJadwalForWeek(jadwalList);

        // Sort jadwal by time untuk hari ini
        todayJadwal.sort((j1, j2) -> j1.getWaktu().compareTo(j2.getWaktu()));

        Calendar now = Calendar.getInstance();
        String currentDay = getCurrentDayName(now);

        // 🔑 Add rows untuk jadwal HARI INI dulu
        for (Jadwal jadwal : todayJadwal) {
            String status = calculateJadwalStatus(jadwal);
            int statusColor = getStatusColor(status);
            addJadwalRow(jadwal.getWaktu(), status, statusColor, jadwal.getId());
        }

        // 🔑 Add rows untuk jadwal HARI LAIN dalam seminggu
        for (Jadwal jadwal : weeklyJadwal) {
            if (!isJadwalForToday(jadwal, currentDay)) {
                // Untuk jadwal hari lain, tampilkan hari dan berapa hari lagi
                String hariJadwal = jadwal.getHari();
                int daysDiff = calculateDaysUntilHari(hariJadwal);

                String status = daysDiff + " Hari Lagi";
                int statusColor = R.color.purple; // Warna untuk jadwal masa depan

                addJadwalRow(hariJadwal, status, statusColor, jadwal.getId());
            }
        }

        // Jika tidak ada jadwal sama sekali
        if (todayJadwal.isEmpty() && weeklyJadwal.isEmpty()) {
            addJadwalRow("Tidak ada jadwal", "BELUM ADA", R.color.gray, -1);
        }
    }

    private List<Jadwal> filterJadwalForToday(@NonNull List<Jadwal> jadwalList) {
        List<Jadwal> todayJadwal = new ArrayList<>();

        // Get current day of week
        Calendar now = Calendar.getInstance();
        String currentDay = getCurrentDayName(now);

        for (Jadwal jadwal : jadwalList) {
            String jadwalHari = jadwal.getHari();

            // For daily schedule
            if (jadwalHari.equalsIgnoreCase("daily") ||
                    jadwalHari.equalsIgnoreCase("setiap hari") ||
                    jadwalHari.equalsIgnoreCase("harian")) {
                todayJadwal.add(jadwal);
            }
            // For weekly schedule - check if today matches
            else if (jadwalHari.equalsIgnoreCase(currentDay)) {
                todayJadwal.add(jadwal);
            }
        }

        return todayJadwal;
    }

    private String getCurrentDayName(Calendar calendar) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        return dayFormat.format(calendar.getTime());
    }

    private String calculateJadwalStatus(Jadwal jadwal) {
        try {
            // 🔑 Check status dari database (sudah di-reset daily)
            switch (jadwal.getStatus()) {
                case Jadwal.STATUS_SUDAH_DIMINUM:
                    return "DONE";
                case Jadwal.STATUS_TERLEWAT:
                    return "TERLEWAT";
                case Jadwal.STATUS_BELUM_DIMINUM:
                default:
                    // Calculate real-time status
                    return calculateRealTimeStatus(jadwal.getWaktu());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error calculating status: " + e.getMessage(), e);
            return "ERROR";
        }
    }

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

    /**
     * Filter jadwal untuk seluruh minggu (bukan hanya hari ini)
     */
    private List<Jadwal> filterJadwalForWeek(@NonNull List<Jadwal> jadwalList) {
        List<Jadwal> weeklyJadwal = new ArrayList<>();

        String[] validDays = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};

        for (Jadwal jadwal : jadwalList) {
            String jadwalHari = jadwal.getHari();

            // Skip daily schedule (sudah dihandle di filterJadwalForToday)
            if (jadwalHari.equalsIgnoreCase("daily") ||
                    jadwalHari.equalsIgnoreCase("setiap hari") ||
                    jadwalHari.equalsIgnoreCase("harian")) {
                continue;
            }

            // Check if it's a valid day name
            for (String day : validDays) {
                if (jadwalHari.equalsIgnoreCase(day)) {
                    weeklyJadwal.add(jadwal);
                    break;
                }
            }
        }

        return weeklyJadwal;
    }

    /**
     * Check apakah jadwal ini untuk hari ini
     */
    private boolean isJadwalForToday(Jadwal jadwal, String currentDay) {
        String jadwalHari = jadwal.getHari();

        // Daily schedule selalu untuk hari ini
        if (jadwalHari.equalsIgnoreCase("daily") ||
                jadwalHari.equalsIgnoreCase("setiap hari") ||
                jadwalHari.equalsIgnoreCase("harian")) {
            return true;
        }

        // Weekly schedule - check if today matches
        return jadwalHari.equalsIgnoreCase(currentDay);
    }

    /**
     * Hitung berapa hari sampai hari tertentu dalam seminggu
     */
    private int calculateDaysUntilHari(@NonNull String targetDay) {
        try {
            // Map nama hari ke nomor hari (Calendar format)
            Map<String, Integer> dayMap = new HashMap<>();
            dayMap.put("minggu", Calendar.SUNDAY);    // 1
            dayMap.put("senin", Calendar.MONDAY);     // 2
            dayMap.put("selasa", Calendar.TUESDAY);   // 3
            dayMap.put("rabu", Calendar.WEDNESDAY);   // 4
            dayMap.put("kamis", Calendar.THURSDAY);   // 5
            dayMap.put("jumat", Calendar.FRIDAY);     // 6
            dayMap.put("sabtu", Calendar.SATURDAY);   // 7

            Integer targetDayNum = dayMap.get(targetDay.toLowerCase());
            if (targetDayNum == null) {
                Log.w(TAG, "Unknown day: " + targetDay);
                return 0;
            }

            Calendar now = Calendar.getInstance();
            int currentDayNum = now.get(Calendar.DAY_OF_WEEK);

            // Hitung selisih hari
            int daysDiff = targetDayNum - currentDayNum;

            // Jika hari target sudah lewat minggu ini, hitung untuk minggu depan
            if (daysDiff <= 0) {
                daysDiff += 7; // Tambah 7 hari untuk minggu depan
            }

            Log.d(TAG, "Days until " + targetDay + ": " + daysDiff +
                    " (current: " + currentDayNum + ", target: " + targetDayNum + ")");

            return daysDiff;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating days until " + targetDay + ": " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Update getStatusColor untuk handle status baru
     */
    private int getStatusColor(String status) {
        switch (status) {
            case "DONE":
                return R.color.green;
            case "TERLEWAT":
                return R.color.red;
            case "TERLAMBAT":
                return R.color.orange;
            case "WAKTUNYA":
                return R.color.blue;
            default:
                // Handle "X Hari Lagi" status
                if (status.contains("Hari Lagi")) {
                    return R.color.purple;
                }
                // Handle "X Menit Lagi" atau "X Jam Lagi"
                if (status.contains("Lagi")) {
                    return R.color.light_blue; // Warna berbeda untuk waktu
                }
                return R.color.black;
        }
    }

    private void addJadwalRow(String waktu, String status, int statusColorRes, int jadwalId) {
        TableRow row = new TableRow(this);

        // Create waktu TextView
        TextView waktuText = new TextView(this);
        waktuText.setText(waktu);
        waktuText.setTextSize(16);
        waktuText.setPadding(12, 12, 12, 12);

        // Create status TextView
        TextView statusText = new TextView(this);
        statusText.setText(status);
        statusText.setTextSize(16);
        statusText.setPadding(12, 12, 12, 12);
        statusText.setTextColor(ContextCompat.getColor(this, statusColorRes));
        statusText.setGravity(Gravity.CENTER);

        // Add click listener untuk aksi jika status bisa diubah
//        if (jadwalId > 0 && canChangeStatus(status)) {
//            row.setOnClickListener(v -> showJadwalDialog(jadwalId, waktu, status));
//            row.setBackgroundResource(android.R.attr.selectableItemBackground);
//        }

        row.addView(waktuText);
        row.addView(statusText);

        tableJadwal.addView(row);
    }

    private boolean canChangeStatus(String status) {
        return !status.equals("TERLEWAT") &&
                !status.equals("BESOK") &&
                !status.contains("Jam Lagi") &&
                !status.contains("Menit Lagi") &&
                !status.equals("DONE");
    }

    private void calculateDailyProgress() {
        if (todayJadwal.isEmpty()) {
            tvDailyProgress.setText("0/0");
            return;
        }

        int completed = 0;
        int total = todayJadwal.size();

        for (Jadwal jadwal : todayJadwal) {
            if (jadwal.getStatus() == Jadwal.STATUS_SUDAH_DIMINUM) {
                completed++;
            }
        }
        tvSisaObat.setText(Obat1.getJumlahObat() + " " + Obat1.getJenisObat());
        tvDailyProgress.setText(completed + "/" + total);
    }

    private void updateActionButtons() {
        // Check if ada jadwal yang bisa diubah statusnya hari ini
        boolean hasActionableJadwal = false;

        for (Jadwal jadwal : todayJadwal) {
            String status = calculateJadwalStatus(jadwal);
            if (canChangeStatus(status)) {
                hasActionableJadwal = true;
                break;
            }
        }

        btnSudahMinum.setVisibility(hasActionableJadwal ? View.VISIBLE : View.GONE);
        btnLewati.setVisibility(hasActionableJadwal ? View.VISIBLE : View.GONE);
    }

    private void handleSudahMinumClick() {
        // Find next actionable jadwal
        Jadwal nextJadwal = findNextActionableJadwal();
        if (nextJadwal != null) {
            updateJadwalStatus(nextJadwal.getId(), Jadwal.STATUS_SUDAH_DIMINUM, "Manual - Sudah Minum");
        } else {
            Toast.makeText(this, "Tidak ada jadwal yang bisa ditandai sudah minum", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleLewatiClick() {
        // Find next actionable jadwal
        Jadwal nextJadwal = findNextActionableJadwal();
        if (nextJadwal != null) {
            showLewatiConfirmDialog(nextJadwal);
        } else {
            Toast.makeText(this, "Tidak ada jadwal yang bisa dilewati", Toast.LENGTH_SHORT).show();
        }
    }

    private Jadwal findNextActionableJadwal() {
        for (Jadwal jadwal : todayJadwal) {
            String status = calculateJadwalStatus(jadwal);
            if (canChangeStatus(status)) {
                return jadwal;
            }
        }
        return null;
    }

    private void showJadwalDialog(int jadwalId, String waktu, String currentStatus) {
        new AlertDialog.Builder(this)
                .setTitle("Jadwal " + waktu)
                .setMessage("Status saat ini: " + currentStatus + "\n\nPilih aksi:")
                .setPositiveButton("Sudah Minum", (dialog, which) ->
                        updateJadwalStatus(jadwalId, Jadwal.STATUS_SUDAH_DIMINUM, "Manual - Sudah Minum"))
                .setNeutralButton("Lewati", (dialog, which) ->
                        updateJadwalStatus(jadwalId, Jadwal.STATUS_TERLEWAT, "Manual - Dilewati"))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void showLewatiConfirmDialog(Jadwal jadwal) {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi")
                .setMessage("Apakah Anda yakin ingin melewati jadwal " + jadwal.getWaktu() + "?")
                .setPositiveButton("Ya, Lewati", (dialog, which) ->
                        updateJadwalStatus(jadwal.getId(), Jadwal.STATUS_TERLEWAT, "Manual - Dilewati"))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void updateJadwalStatus(int jadwalId, int newStatus, String catatan) {
        jadwalHelper.open();
        obatHelper.open();
        int rowsAffected = jadwalHelper.updateJadwalStatus(jadwalId, newStatus, catatan);
        executor.execute(() -> {
            try {


                if (rowsAffected > 0) {
                    // Reload data
                    jadwalList = jadwalHelper.getJadwalByObatId(obatId);
                    obatDetail = obatHelper.getObatById(obatId);

                    runOnUiThread(() -> {
                        Obat1 = obatDetail; // Update Obat1 reference
                        populateJadwalTable(jadwalList);
                        calculateDailyProgress();
                        updateActionButtons();

                        String statusMsg = newStatus == Jadwal.STATUS_SUDAH_DIMINUM ? "Sudah Diminum" : "Dilewati";
                        Toast.makeText(this, "Status berhasil diubah: " + statusMsg, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Gagal mengubah status", Toast.LENGTH_SHORT).show());
                }

                jadwalHelper.close();

            } catch (Exception e) {
                Log.e(TAG, "Error updating jadwal status: " + e.getMessage(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void handleLoadError(@NonNull Exception e) {
        Log.e(TAG, "Failed to load obat detail: " + e.getMessage(), e);

        Toast.makeText(this,
                "Gagal memuat data: " + e.getMessage(),
                Toast.LENGTH_LONG).show();

        // Show basic info if available
        if (obatNama != null) {
            tvNamaObat.setText(obatNama);
        }

        // Add error row to table
        addJadwalRow("Error memuat data", "ERROR", R.color.red, -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }

        // Close database connections if still open
        try {
            if (obatHelper != null && obatHelper.isOpen()) {
                obatHelper.close();
            }
            if (jadwalHelper != null && jadwalHelper.isOpen()) {
                jadwalHelper.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing database in onDestroy: " + e.getMessage(), e);
        }

        Log.d(TAG, "DetailObatActivity destroyed");
    }
}