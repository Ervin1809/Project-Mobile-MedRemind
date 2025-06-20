package com.example.medremind.ui.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.medremind.R;
import com.example.medremind.data.helper.ObatHelper;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.widget.SwitchCompat;

public class ProfileFragment extends Fragment {

    private TextInputEditText etNamaPengguna;
    private Button btnSimpanNama;
    private SwitchCompat switchDarkMode;
    private Button btnHapusData;

    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "MedRemindPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DARK_MODE = "dark_mode";
    private ObatHelper obatHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Initialize ObatHelper for database operations
        obatHelper = new ObatHelper(requireActivity());

        // Initialize views
        etNamaPengguna = view.findViewById(R.id.et_nama_pengguna);
        btnSimpanNama = view.findViewById(R.id.btn_simpan_nama);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        btnHapusData = view.findViewById(R.id.btn_hapus_data);

        // Set up the UI with saved data
        setupUI();

        // Set up click listeners
        setupListeners();

        return view;
    }

    private void setupUI() {
        // Load saved username
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        etNamaPengguna.setText(savedUsername);

        // Load dark mode preference
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(isDarkMode);
    }

    private void setupListeners() {
        // Username save button
        btnSimpanNama.setOnClickListener(v -> {
            String username = etNamaPengguna.getText().toString().trim();
            if (!username.isEmpty()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_USERNAME, username);
                editor.apply();
                Toast.makeText(requireActivity(), "Nama berhasil disimpan", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireActivity(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show();
            }
        });

        // Dark mode switch
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            setDarkMode(isChecked);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_DARK_MODE, isChecked);
            editor.apply();
        });

        // Delete all data button
        btnHapusData.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });
    }

    private void setDarkMode(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Hapus Semua Data")
                .setMessage("Anda yakin ingin menghapus semua data obat? Tindakan ini tidak dapat dibatalkan.")
                .setPositiveButton("Ya", (dialog, which) -> {
                    deleteAllMedicineData();
                })
                .setNegativeButton("Tidak", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteAllMedicineData() {
        obatHelper.open();
        boolean success = obatHelper.deleteAllObat();
        obatHelper.close();

        if (success) {
            Toast.makeText(requireActivity(), "Semua data obat berhasil dihapus", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireActivity(), "Gagal menghapus data obat", Toast.LENGTH_SHORT).show();
        }
    }
}