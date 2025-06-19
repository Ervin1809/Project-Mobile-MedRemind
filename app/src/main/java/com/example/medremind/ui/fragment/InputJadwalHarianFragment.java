package com.example.medremind.ui.fragment;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.medremind.R;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InputJadwalHarianFragment extends Fragment {
    private static final String TAG = "InputJadwalHarianFragment";
    private static final String HARI_DAILY = "daily";

    // UI Components
    private LinearLayout jamContainer;
    private Button btnTambahJam, btnSimpan;

    // Data
    private ArrayList<String> jamList = new ArrayList<>();
    private Set<String> uniqueTimeSet = new HashSet<>(); // Prevent duplicate times

    // Database helpers
    private ObatHelper obatHelper;
    private JadwalHelper jadwalHelper;

    // Background executor
    private ExecutorService executor;

    // Input data from previous fragment
    private ObatInputData obatData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_input_jadwal_harian, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        loadArgumentData();
        setupClickListeners();

        // Add first time picker by default
        addTimePickerRow();

        Log.d(TAG, "InputJadwalHarianFragment initialized successfully");
    }

    private void initializeComponents(@NonNull View view) {
        // Initialize views
        jamContainer = view.findViewById(R.id.jam_container);
        btnTambahJam = view.findViewById(R.id.btn_tambah_jam);
        btnSimpan = view.findViewById(R.id.btn_simpan_harian);

        // Initialize database helpers
        obatHelper = new ObatHelper(requireContext());
        jadwalHelper = new JadwalHelper(requireContext());

        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        // Validate required views
        validateRequiredViews();
    }

    private void validateRequiredViews() {
        if (jamContainer == null || btnTambahJam == null || btnSimpan == null) {
            Log.e(TAG, "Required views not found in layout");
            Toast.makeText(requireContext(), "Error: Layout tidak lengkap", Toast.LENGTH_LONG).show();
        }
    }

    private void loadArgumentData() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            obatData = new ObatInputData();
            obatData.namaObat = arguments.getString("namaObat", "");
            obatData.jenisObat = arguments.getString("jenisObat", "");
            obatData.dosisObat = arguments.getString("dosisObat", "");
            obatData.aturanMinum = arguments.getString("aturanMinum", "");
            obatData.tipeJadwal = "harian"; // Force harian type

            try {
                obatData.jumlahObat = Integer.parseInt(arguments.getString("jumlahObat", "0"));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid jumlahObat: " + arguments.getString("jumlahObat"), e);
                obatData.jumlahObat = 0;
            }

            Log.d(TAG, "Loaded obat data: " + obatData.toString());
        } else {
            Log.e(TAG, "No arguments provided to fragment");
            Toast.makeText(requireContext(), "Error: Data obat tidak ditemukan", Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListeners() {
        btnTambahJam.setOnClickListener(v -> addTimePickerRow());
        btnSimpan.setOnClickListener(v -> handleSimpanClick());
    }

    private void addTimePickerRow() {
        if (jamContainer.getChildCount() >= 10) { // Limit maksimal 10 jam per hari
            Toast.makeText(requireContext(), "Maksimal 10 jadwal per hari", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            View jamRow = LayoutInflater.from(getContext()).inflate(R.layout.item_jam, jamContainer, false);
            EditText etJam = jamRow.findViewById(R.id.et_jam);
            ImageButton btnHapus = jamRow.findViewById(R.id.btn_hapus_jam);

            if (etJam == null || btnHapus == null) {
                Log.e(TAG, "Required views not found in item_jam layout");
                Toast.makeText(requireContext(), "Error: Layout item tidak lengkap", Toast.LENGTH_SHORT).show();
                return;
            }

            // Setup click listeners
            etJam.setOnClickListener(v -> showTimePickerDialog(etJam));
            btnHapus.setOnClickListener(v -> removeTimePickerRow(jamRow, etJam));

            // Add to container
            jamContainer.addView(jamRow);

            Log.d(TAG, "Time picker row added. Total rows: " + jamContainer.getChildCount());

        } catch (Exception e) {
            Log.e(TAG, "Error adding time picker row: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error menambah jadwal: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void removeTimePickerRow(@NonNull View jamRow, @NonNull EditText etJam) {
        try {
            // Remove time from unique set if it was added
            String timeText = etJam.getText().toString().trim();
            if (!TextUtils.isEmpty(timeText)) {
                uniqueTimeSet.remove(timeText);
            }

            // Remove view from container
            jamContainer.removeView(jamRow);

            Log.d(TAG, "Time picker row removed. Total rows: " + jamContainer.getChildCount());

            // Ensure at least one time picker exists
            if (jamContainer.getChildCount() == 0) {
                addTimePickerRow();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error removing time picker row: " + e.getMessage(), e);
        }
    }

    private void showTimePickerDialog(@NonNull EditText target) {
        try {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    getContext(),
                    (view, hourOfDay, selectedMinute) -> {
                        String jam = String.format("%02d:%02d", hourOfDay, selectedMinute);

                        // Check for duplicate times
                        if (uniqueTimeSet.contains(jam)) {
                            Toast.makeText(requireContext(),
                                    "Waktu " + jam + " sudah dipilih",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Remove old time from set if exists
                        String oldTime = target.getText().toString().trim();
                        if (!TextUtils.isEmpty(oldTime)) {
                            uniqueTimeSet.remove(oldTime);
                        }

                        // Add new time
                        target.setText(jam);
                        uniqueTimeSet.add(jam);

                        Log.d(TAG, "Time selected: " + jam);
                    },
                    hour, minute, true
            );

            dialog.setTitle("Pilih Waktu Minum Obat");
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error menampilkan time picker", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSimpanClick() {
        if (!validateInput()) {
            return;
        }

        // Show loading
        btnSimpan.setEnabled(false);
        btnSimpan.setText("Menyimpan...");

        // Collect time data
        collectTimeData();

        // Save in background thread
        executor.execute(this::saveJadwalHarian);
    }

    private boolean validateInput() {
        // Validate obat data
        if (obatData == null || !obatData.isValid()) {
            Toast.makeText(requireContext(), "Data obat tidak valid", Toast.LENGTH_LONG).show();
            return false;
        }

        // Collect and validate time data
        collectTimeData();

        if (jamList.isEmpty()) {
            Toast.makeText(requireContext(), "Isi minimal satu jam minum!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (jamList.size() > 10) {
            Toast.makeText(requireContext(), "Maksimal 10 jadwal per hari", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void collectTimeData() {
        jamList.clear();
        uniqueTimeSet.clear();

        for (int i = 0; i < jamContainer.getChildCount(); i++) {
            View child = jamContainer.getChildAt(i);
            EditText etJam = child.findViewById(R.id.et_jam);

            if (etJam != null) {
                String jam = etJam.getText().toString().trim();
                if (!TextUtils.isEmpty(jam) && !jamList.contains(jam)) {
                    jamList.add(jam);
                    uniqueTimeSet.add(jam);
                }
            }
        }

        // Sort times in chronological order
        jamList.sort(String::compareTo);

        Log.d(TAG, "Collected " + jamList.size() + " unique times: " + jamList.toString());
    }

    private void saveJadwalHarian() {
        try {
            // Open database connections
            obatHelper.open();
            jadwalHelper.open();

            // Create Obat object
            Obat obat = new Obat(
                    obatData.namaObat,
                    obatData.jenisObat,
                    obatData.dosisObat,
                    obatData.aturanMinum,
                    obatData.jumlahObat,
                    obatData.tipeJadwal
            );

            // Insert obat first
            long obatId = obatHelper.insertObat(obat);

            if (obatId == -1) {
                throw new Exception("Gagal menyimpan data obat");
            }

            // Use array wrapper untuk variable yang akan diakses di lambda
            final int[] successCount = {0}; // Array wrapper

            // Insert jadwal for each time
            for (String jam : jamList) {
                Jadwal jadwal = new Jadwal((int) obatId, HARI_DAILY, jam);
                long jadwalId = jadwalHelper.tambahJadwal(jadwal);

                if (jadwalId != -1) {
                    successCount[0]++; // Access array element
                }
            }

            // Check if all schedules were saved
            if (successCount[0] != jamList.size()) {
                Log.w(TAG, "Not all schedules saved. Expected: " + jamList.size() + ", Actual: " + successCount[0]);
            }

            Log.d(TAG, "Jadwal harian saved successfully. Obat ID: " + obatId + ", Schedules: " + successCount[0]);

            // Create final variable untuk lambda
            final int finalSuccessCount = successCount[0];

            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> handleSaveSuccess(finalSuccessCount));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving jadwal harian: " + e.getMessage(), e);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> handleSaveError(e));
            }
        } finally {
            // Close database connections
            try {
                obatHelper.close();
                jadwalHelper.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing database: " + e.getMessage(), e);
            }
        }
    }

    private void handleSaveSuccess(int schedulesCount) {
        // Reset button state
        btnSimpan.setEnabled(true);
        btnSimpan.setText("Simpan Jadwal");

        // Show success message
        String message = String.format("✅ Jadwal harian berhasil disimpan!\n%d jadwal minum per hari", schedulesCount);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

        // Kirim signal ke AddObatFragment bahwa save berhasil
        sendSuccessSignalToAddObat();

        // Navigate back
        navigateBack();
    }

    private void handleSaveError(@NonNull Exception e) {
        // Reset button state
        btnSimpan.setEnabled(true);
        btnSimpan.setText("Simpan Jadwal");

        // Show error message
        String errorMessage = "❌ Gagal menyimpan jadwal: " + e.getMessage();
        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();

        Log.e(TAG, "Save error handled: " + errorMessage);
    }

    private void navigateBack() {
        try {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating back: " + e.getMessage(), e);

            // Fallback: try navigation controller
            try {
                NavController navController = Navigation.findNavController(requireView());
                navController.popBackStack();
            } catch (Exception navError) {
                Log.e(TAG, "Error with navigation controller: " + navError.getMessage(), navError);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
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
            Log.e(TAG, "Error closing database in onDestroyView: " + e.getMessage(), e);
        }

        // Clear references
        jamContainer = null;
        btnTambahJam = null;
        btnSimpan = null;
        obatHelper = null;
        jadwalHelper = null;
        jamList = null;
        uniqueTimeSet = null;
        obatData = null;

        Log.d(TAG, "InputJadwalHarianFragment destroyed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Final cleanup
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    // Helper class untuk menyimpan data obat
    private static class ObatInputData {
        String namaObat;
        String jenisObat;
        String dosisObat;
        String aturanMinum;
        String tipeJadwal;
        int jumlahObat;

        boolean isValid() {
            return !TextUtils.isEmpty(namaObat) &&
                    !TextUtils.isEmpty(jenisObat) &&
                    !TextUtils.isEmpty(dosisObat) &&
                    !TextUtils.isEmpty(aturanMinum) &&
                    !TextUtils.isEmpty(tipeJadwal) &&
                    jumlahObat > 0;
        }

        @Override
        public String toString() {
            return "ObatInputData{" +
                    "namaObat='" + namaObat + '\'' +
                    ", jenisObat='" + jenisObat + '\'' +
                    ", dosisObat='" + dosisObat + '\'' +
                    ", aturanMinum='" + aturanMinum + '\'' +
                    ", tipeJadwal='" + tipeJadwal + '\'' +
                    ", jumlahObat=" + jumlahObat +
                    '}';
        }
    }

    private void sendSuccessSignalToAddObat() {
        try {
            // Simpan flag di SharedPreferences (simple solution)
            requireContext().getSharedPreferences("medremind_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("should_reset_add_obat_form", true)
                    .apply();

            Log.d(TAG, "Success signal sent to AddObatFragment");
        } catch (Exception e) {
            Log.e(TAG, "Error sending success signal: " + e.getMessage(), e);
        }
    }
}