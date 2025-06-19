package com.example.medremind.ui.fragment;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.medremind.R;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InputJadwalMingguanFragment extends Fragment {
    private static final String TAG = "InputJadwalMingguanFragment";

    // UI Components
    private GridLayout hariGrid;
    private MaterialButton btnSimpan;

    // Data
    private final String[] hariList = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
    private final Map<String, TextInputEditText> hariJamMap = new HashMap<>();
    private final Map<String, CheckBox> hariCheckBoxMap = new HashMap<>();

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
        return inflater.inflate(R.layout.fragment_input_jadwal_mingguan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        loadArgumentData();
        setupHariGrid();
        setupSaveButton();

        Log.d(TAG, "InputJadwalMingguanFragment initialized successfully");
    }

    private void initializeComponents(@NonNull View view) {
        // Initialize views
        hariGrid = view.findViewById(R.id.hari_grid_container);
        btnSimpan = view.findViewById(R.id.btn_simpan_mingguan);

        // Initialize database helpers
        obatHelper = new ObatHelper(requireContext());
        jadwalHelper = new JadwalHelper(requireContext());

        // Initialize executor
        executor = Executors.newSingleThreadExecutor();

        // Validate required views
        validateRequiredViews();
    }

    private void validateRequiredViews() {
        if (hariGrid == null || btnSimpan == null) {
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
            obatData.tipeJadwal = "mingguan"; // Force mingguan type

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

    private void setupHariGrid() {
        try {
            for (String hari : hariList) {
                View hariRow = LayoutInflater.from(getContext()).inflate(R.layout.item_hari_mingguan, hariGrid, false);

                CheckBox cbHari = hariRow.findViewById(R.id.cb_hari);
                TextInputEditText etJam = hariRow.findViewById(R.id.et_jam_hari);
                LinearLayout jamContainer = hariRow.findViewById(R.id.jam_input_container);
                MaterialButton btnClear = hariRow.findViewById(R.id.btn_clear_jam);

                if (cbHari == null || etJam == null || jamContainer == null || btnClear == null) {
                    Log.e(TAG, "Required views not found in item_hari_mingguan layout for: " + hari);
                    continue;
                }

                // Setup checkbox text
                cbHari.setText(hari);

                // Store references
                hariCheckBoxMap.put(hari, cbHari);

                // Setup checkbox listener
                cbHari.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    jamContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    if (!isChecked) {
                        etJam.setText("");
                        hariJamMap.remove(hari);
                    } else {
                        hariJamMap.put(hari, etJam);
                    }

                    Log.d(TAG, "Checkbox for " + hari + " changed to: " + isChecked);
                });

                // Setup time picker
                etJam.setOnClickListener(v1 -> showTimePickerDialog(etJam, hari));

                // Setup clear button
                btnClear.setOnClickListener(v -> {
                    cbHari.setChecked(false);
                    Log.d(TAG, "Clear button clicked for: " + hari);
                });

                // Set proper layout params for GridLayout
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = GridLayout.LayoutParams.MATCH_PARENT;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.setMargins(0, 0, 0, 8);
                hariRow.setLayoutParams(params);

                hariGrid.addView(hariRow);
            }

            Log.d(TAG, "Hari grid setup completed with " + hariList.length + " days");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up hari grid: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error setting up hari grid: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupSaveButton() {
        btnSimpan.setOnClickListener(v -> handleSimpanClick());
    }

    private void showTimePickerDialog(@NonNull TextInputEditText target, @NonNull String hari) {
        try {
            Calendar now = Calendar.getInstance();
            int hour = now.get(Calendar.HOUR_OF_DAY);
            int minute = now.get(Calendar.MINUTE);

            TimePickerDialog dialog = new TimePickerDialog(
                    getContext(),
                    (view, hourOfDay, selectedMinute) -> {
                        String jam = String.format("%02d:%02d", hourOfDay, selectedMinute);
                        target.setText(jam);

                        Log.d(TAG, "Time selected for " + hari + ": " + jam);
                    },
                    hour, minute, true
            );

            dialog.setTitle("Pilih Waktu untuk " + hari);
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing time picker for " + hari + ": " + e.getMessage(), e);
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

        // Collect schedule data
        Map<String, String> hasil = collectScheduleData();

        // Save in background thread
        executor.execute(() -> saveJadwalMingguan(hasil));
    }

    private boolean validateInput() {
        // Validate obat data
        if (obatData == null || !obatData.isValid()) {
            Toast.makeText(requireContext(), "Data obat tidak valid", Toast.LENGTH_LONG).show();
            return false;
        }

        // Validate schedule data
        Map<String, String> hasil = collectScheduleData();

        if (hasil.isEmpty()) {
            Toast.makeText(requireContext(), "Pilih minimal satu hari dan isi jam minum!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (hasil.size() > 7) {
            Toast.makeText(requireContext(), "Maksimal 7 hari dalam seminggu", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @NonNull
    private Map<String, String> collectScheduleData() {
        Map<String, String> hasil = new HashMap<>();

        for (Map.Entry<String, TextInputEditText> entry : hariJamMap.entrySet()) {
            String hari = entry.getKey();
            TextInputEditText et = entry.getValue();

            if (et != null) {
                String jam = et.getText().toString().trim();
                if (!TextUtils.isEmpty(jam)) {
                    hasil.put(hari, jam);
                }
            }
        }

        Log.d(TAG, "Collected schedule data: " + hasil.toString());
        return hasil;
    }

    private void saveJadwalMingguan(@NonNull Map<String, String> hasil) {
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

            // Insert jadwal for each selected day
            for (Map.Entry<String, String> entry : hasil.entrySet()) {
                String hari = entry.getKey();
                String jam = entry.getValue();

                Jadwal jadwal = new Jadwal((int) obatId, hari, jam);
                long jadwalId = jadwalHelper.tambahJadwal(jadwal);

                if (jadwalId != -1) {
                    successCount[0]++; // Access array element
                } else {
                    Log.w(TAG, "Failed to save schedule for " + hari + " at " + jam);
                }
            }

            // Check if all schedules were saved
            if (successCount[0] != hasil.size()) {
                Log.w(TAG, "Not all schedules saved. Expected: " + hasil.size() + ", Actual: " + successCount[0]);
            }

            Log.d(TAG, "Jadwal mingguan saved successfully. Obat ID: " + obatId + ", Schedules: " + successCount[0]);

            // Create final variables untuk lambda
            final int finalSuccessCount = successCount[0];
            final Map<String, String> finalHasil = new HashMap<>(hasil);

            // Update UI on main thread
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> handleSaveSuccess(finalSuccessCount, finalHasil));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving jadwal mingguan: " + e.getMessage(), e);

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

    private void handleSaveSuccess(int schedulesCount, @NonNull Map<String, String> hasil) {
        // Reset button state
        btnSimpan.setEnabled(true);
        btnSimpan.setText("Simpan Jadwal");

        // Show success message
        StringBuilder daysList = new StringBuilder();
        for (String hari : hasil.keySet()) {
            if (daysList.length() > 0) daysList.append(", ");
            daysList.append(hari);
        }

        String message = String.format("✅ Jadwal mingguan berhasil disimpan!\n%d jadwal untuk: %s",
                schedulesCount, daysList.toString());
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
        hariGrid = null;
        btnSimpan = null;
        obatHelper = null;
        jadwalHelper = null;
        hariJamMap.clear();
        hariCheckBoxMap.clear();
        obatData = null;

        Log.d(TAG, "InputJadwalMingguanFragment destroyed");
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