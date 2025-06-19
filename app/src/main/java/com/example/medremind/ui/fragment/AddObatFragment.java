package com.example.medremind.ui.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.medremind.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AddObatFragment extends Fragment {
    private static final String TAG = "AddObatFragment";

    // UI Components
    private TextInputEditText etNamaObat, etDosisObat, etJumlah;
    private RadioGroup rgJenisObat, rgTipeJadwal, rgAturan;
    private MaterialButton btnSelanjutnya;

    // Constants untuk validasi
    private static final String TIPE_HARIAN = "harian";
    private static final String TIPE_MINGGUAN = "mingguan";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_obat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners(view);

        Log.d(TAG, "AddObatFragment initialized successfully");
    }

    private void initializeViews(@NonNull View view) {
        etNamaObat = view.findViewById(R.id.et_nama_obat);
        etDosisObat = view.findViewById(R.id.et_dosis_obat);
        etJumlah = view.findViewById(R.id.et_jumlah);
        rgJenisObat = view.findViewById(R.id.rg_jenis_obat);
        rgTipeJadwal = view.findViewById(R.id.rg_tipe_jadwal);
        rgAturan = view.findViewById(R.id.rg_aturan);
        btnSelanjutnya = view.findViewById(R.id.btn_selanjutnya);

        // Set default values atau validasi komponen
        validateRequiredViews();
    }

    private void validateRequiredViews() {
        if (etNamaObat == null || etDosisObat == null || etJumlah == null ||
                rgJenisObat == null || rgTipeJadwal == null || btnSelanjutnya == null) {
            Log.e(TAG, "Required views not found in layout");
            Toast.makeText(requireContext(), "Error: Layout tidak lengkap", Toast.LENGTH_LONG).show();
        }
    }

    private void setupClickListeners(@NonNull View view) {
        btnSelanjutnya.setOnClickListener(v -> handleSelanjutnyaClick(view));
    }

    private void handleSelanjutnyaClick(@NonNull View view) {
        if (!validateInput()) {
            return;
        }

        try {
            // Collect input data
            ObatInputData inputData = collectInputData(view);

            if (inputData == null) {
                Toast.makeText(requireContext(), "Error: Gagal mengumpulkan data", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create bundle for navigation
            Bundle bundle = createNavigationBundle(inputData);

            // Navigate berdasarkan tipe jadwal
            navigateToScheduleFragment(view, inputData.tipeJadwal, bundle);

        } catch (Exception e) {
            Log.e(TAG, "Error handling selanjutnya click: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private ObatInputData collectInputData(@NonNull View view) {
        try {
            ObatInputData data = new ObatInputData();

            // Ambil text input
            data.namaObat = etNamaObat.getText() != null ? etNamaObat.getText().toString().trim() : "";
            data.dosisObat = etDosisObat.getText() != null ? etDosisObat.getText().toString().trim() : "";
            data.jumlahObat = etJumlah.getText() != null ? etJumlah.getText().toString().trim() : "";

            // Ambil radio button selections
            data.jenisObat = getSelectedRadioButtonText(view, rgJenisObat);
            data.tipeJadwal = getSelectedRadioButtonText(view, rgTipeJadwal);
            data.aturanMinum = rgAturan != null ? getSelectedRadioButtonText(view, rgAturan) : "";

            // Normalize tipe jadwal
            data.tipeJadwal = normalizeTipeJadwal(data.tipeJadwal);

            return data;

        } catch (Exception e) {
            Log.e(TAG, "Error collecting input data: " + e.getMessage(), e);
            return null;
        }
    }

    private String getSelectedRadioButtonText(@NonNull View parentView, @Nullable RadioGroup radioGroup) {
        if (radioGroup == null) {
            return "";
        }

        int selectedId = radioGroup.getCheckedRadioButtonId();
        if (selectedId == -1) {
            return "";
        }

        RadioButton selectedRadioButton = parentView.findViewById(selectedId);
        return selectedRadioButton != null ? selectedRadioButton.getText().toString().trim() : "";
    }

    private String normalizeTipeJadwal(@NonNull String tipeJadwal) {
        if (tipeJadwal.equalsIgnoreCase("Setiap Hari") ||
                tipeJadwal.equalsIgnoreCase("Harian") ||
                tipeJadwal.equalsIgnoreCase("Daily")) {
            return TIPE_HARIAN;
        } else if (tipeJadwal.equalsIgnoreCase("Mingguan") ||
                tipeJadwal.equalsIgnoreCase("Weekly")) {
            return TIPE_MINGGUAN;
        }
        return tipeJadwal.toLowerCase();
    }

    private Bundle createNavigationBundle(@NonNull ObatInputData data) {
        Bundle bundle = new Bundle();
        bundle.putString("namaObat", data.namaObat);
        bundle.putString("dosisObat", data.dosisObat);
        bundle.putString("jumlahObat", data.jumlahObat);
        bundle.putString("jenisObat", data.jenisObat);
        bundle.putString("aturanMinum", data.aturanMinum);
        bundle.putString("tipeJadwal", data.tipeJadwal);

        Log.d(TAG, "Navigation bundle created: " + bundle.toString());
        return bundle;
    }

    private void navigateToScheduleFragment(@NonNull View view, @NonNull String tipeJadwal, @NonNull Bundle bundle) {
        try {
            NavController navController = Navigation.findNavController(view);

            if (TIPE_HARIAN.equals(tipeJadwal)) {
                navController.navigate(R.id.action_addObatFragment_to_inputJadwalHarianFragment, bundle);
                Log.d(TAG, "Navigating to harian fragment");
            } else if (TIPE_MINGGUAN.equals(tipeJadwal)) {
                navController.navigate(R.id.action_addObatFragment_to_inputJadwalMingguanFragment, bundle);
                Log.d(TAG, "Navigating to mingguan fragment");
            } else {
                Toast.makeText(requireContext(), "Tipe jadwal tidak valid: " + tipeJadwal, Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Invalid tipe jadwal: " + tipeJadwal);
            }

        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error navigasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput() {
        // Reset previous errors
        clearInputErrors();

        boolean isValid = true;

        // Validate nama obat
        if (TextUtils.isEmpty(etNamaObat.getText())) {
            etNamaObat.setError("Nama obat wajib diisi");
            isValid = false;
        } else if (etNamaObat.getText().toString().trim().length() < 2) {
            etNamaObat.setError("Nama obat minimal 2 karakter");
            isValid = false;
        }

        // Validate dosis
        if (TextUtils.isEmpty(etDosisObat.getText())) {
            etDosisObat.setError("Dosis wajib diisi");
            isValid = false;
        }

        // Validate jumlah
        if (TextUtils.isEmpty(etJumlah.getText())) {
            etJumlah.setError("Jumlah wajib diisi");
            isValid = false;
        } else {
            try {
                int jumlah = Integer.parseInt(etJumlah.getText().toString().trim());
                if (jumlah <= 0) {
                    etJumlah.setError("Jumlah harus lebih dari 0");
                    isValid = false;
                } else if (jumlah > 1000) {
                    etJumlah.setError("Jumlah terlalu besar (maksimal 1000)");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                etJumlah.setError("Jumlah harus berupa angka");
                isValid = false;
            }
        }

        // Validate radio groups
        if (rgJenisObat.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Pilih jenis obat", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (rgTipeJadwal.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Pilih tipe jadwal", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (rgAturan != null && rgAturan.getCheckedRadioButtonId() == -1) {
            Toast.makeText(requireContext(), "Pilih aturan minum", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }
    @Override
    public void onResume() {
        super.onResume();

        // Cek apakah ada signal untuk reset form setelah save berhasil
        checkForResetSignal();
    }
    private void checkForResetSignal() {
        try {
            SharedPreferences prefs = requireContext().getSharedPreferences("medremind_prefs", Context.MODE_PRIVATE);
            boolean shouldReset = prefs.getBoolean("should_reset_add_obat_form", false);

            if (shouldReset) {
                // Reset form karena save berhasil
                resetForm();

                // Clear flag
                prefs.edit().remove("should_reset_add_obat_form").apply();

                // Show success message
                Toast.makeText(requireContext(), "✅ Obat dan jadwal berhasil disimpan!", Toast.LENGTH_LONG).show();

                Log.d(TAG, "Form reset after successful save");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking reset signal: " + e.getMessage(), e);
        }
    }

    private void resetForm() {
        try {
            // Clear text inputs
            if (etNamaObat != null) {
                etNamaObat.setText("");
                etNamaObat.setError(null);
            }

            if (etDosisObat != null) {
                etDosisObat.setText("");
                etDosisObat.setError(null);
            }

            if (etJumlah != null) {
                etJumlah.setText("");
                etJumlah.setError(null);
            }

            // Clear radio button selections
            if (rgJenisObat != null) {
                rgJenisObat.clearCheck();
            }

            if (rgTipeJadwal != null) {
                rgTipeJadwal.clearCheck();
            }

            if (rgAturan != null) {
                rgAturan.clearCheck();
            }

            // Reset focus ke input pertama
            if (etNamaObat != null) {
                etNamaObat.requestFocus();
            }

            Log.d(TAG, "Form reset successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error resetting form: " + e.getMessage(), e);
        }
    }

    private void clearInputErrors() {
        etNamaObat.setError(null);
        etDosisObat.setError(null);
        etJumlah.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clear references to prevent memory leaks
        etNamaObat = null;
        etDosisObat = null;
        etJumlah = null;
        rgJenisObat = null;
        rgTipeJadwal = null;
        rgAturan = null;
        btnSelanjutnya = null;

        Log.d(TAG, "AddObatFragment destroyed");
    }

    // Helper class untuk menyimpan data input
    private static class ObatInputData {
        String namaObat;
        String dosisObat;
        String jumlahObat;
        String jenisObat;
        String tipeJadwal;
        String aturanMinum;

        @Override
        public String toString() {
            return "ObatInputData{" +
                    "namaObat='" + namaObat + '\'' +
                    ", dosisObat='" + dosisObat + '\'' +
                    ", jumlahObat='" + jumlahObat + '\'' +
                    ", jenisObat='" + jenisObat + '\'' +
                    ", tipeJadwal='" + tipeJadwal + '\'' +
                    ", aturanMinum='" + aturanMinum + '\'' +
                    '}';
        }
    }
}