package com.example.medremind.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Jadwal {
    // Constants untuk status
    public static final int STATUS_BELUM_DIMINUM = 0;
    public static final int STATUS_SUDAH_DIMINUM = 1;
    public static final int STATUS_TERLEWAT = 2;

    // Constants untuk hari
    public static final String HARI_DAILY = "daily";

    private int id;
    private int obatId;
    private String hari; // "daily" untuk jadwal harian, atau nama hari untuk jadwal mingguan
    private String waktu; // format "HH:MM"
    private int status; // 0: belum diminum, 1: sudah diminum, 2: terlewat
    private Date tanggalDibuat;
    private Date tanggalDiperbarui;
    private Date tanggalDiminum; // kapan obat diminum (jika sudah)
    private String catatan; // catatan tambahan
    private Map<String, String> tambahan; // untuk menyimpan data tambahan

    // Constructor kosong
    public Jadwal() {
        this.status = STATUS_BELUM_DIMINUM;
        this.tanggalDibuat = new Date();
        this.tanggalDiperbarui = new Date();
        this.tambahan = new HashMap<>();
    }

    // Constructor lengkap dengan ID (dari database)
    public Jadwal(int id, int obatId, @NonNull String hari, @NonNull String waktu) {
        this();
        this.id = id;
        this.obatId = obatId;
        this.hari = hari;
        this.waktu = waktu;
    }

    // Constructor tanpa ID (untuk insert baru)
    public Jadwal(int obatId, @NonNull String hari, @NonNull String waktu) {
        this();
        this.obatId = obatId;
        this.hari = hari;
        this.waktu = waktu;
    }

    // Constructor minimal (untuk compatibility)
    public Jadwal(@NonNull String hari, @NonNull String waktu) {
        this();
        this.hari = hari;
        this.waktu = waktu;
    }

    // Getters and Setters dengan validasi
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getObatId() {
        return obatId;
    }

    public void setObatId(int obatId) {
        if (obatId <= 0) {
            throw new IllegalArgumentException("Obat ID harus lebih dari 0");
        }
        this.obatId = obatId;
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getHari() {
        return hari != null ? hari : "";
    }

    public void setHari(@NonNull String hari) {
        if (hari == null || hari.trim().isEmpty()) {
            throw new IllegalArgumentException("Hari tidak boleh kosong");
        }

        // Validasi format hari
        if (!isValidHari(hari)) {
            throw new IllegalArgumentException("Format hari tidak valid: " + hari);
        }

        this.hari = hari.trim();
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getWaktu() {
        return waktu != null ? waktu : "";
    }

    public void setWaktu(@NonNull String waktu) {
        if (waktu == null || waktu.trim().isEmpty()) {
            throw new IllegalArgumentException("Waktu tidak boleh kosong");
        }

        // Validasi format waktu (HH:MM)
        if (!isValidTimeFormat(waktu)) {
            throw new IllegalArgumentException("Format waktu harus HH:MM");
        }

        this.waktu = waktu.trim();
        updateTanggalDiperbarui();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        if (status < STATUS_BELUM_DIMINUM || status > STATUS_TERLEWAT) {
            throw new IllegalArgumentException("Status tidak valid: " + status);
        }
        this.status = status;
        updateTanggalDiperbarui();

        // Set tanggal diminum jika status berubah ke sudah diminum
        if (status == STATUS_SUDAH_DIMINUM && tanggalDiminum == null) {
            this.tanggalDiminum = new Date();
        }
    }

    @Nullable
    public Date getTanggalDibuat() {
        return tanggalDibuat;
    }

    public void setTanggalDibuat(@Nullable Date tanggalDibuat) {
        this.tanggalDibuat = tanggalDibuat;
    }

    @Nullable
    public Date getTanggalDiperbarui() {
        return tanggalDiperbarui;
    }

    public void setTanggalDiperbarui(@Nullable Date tanggalDiperbarui) {
        this.tanggalDiperbarui = tanggalDiperbarui;
    }

    @Nullable
    public Date getTanggalDiminum() {
        return tanggalDiminum;
    }

    public void setTanggalDiminum(@Nullable Date tanggalDiminum) {
        this.tanggalDiminum = tanggalDiminum;
    }

    @Nullable
    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(@Nullable String catatan) {
        this.catatan = catatan;
        updateTanggalDiperbarui();
    }

    // Methods untuk tambahan data
    public void setTambahan(@NonNull String key, @Nullable String value) {
        if (tambahan == null) {
            tambahan = new HashMap<>();
        }
        tambahan.put(key, value);
    }

    @Nullable
    public String getTambahan(@NonNull String key) {
        if (tambahan != null && tambahan.containsKey(key)) {
            return tambahan.get(key);
        }
        return null;
    }

    @NonNull
    public Map<String, String> getAllTambahan() {
        return tambahan != null ? new HashMap<>(tambahan) : new HashMap<>();
    }

    // Helper methods
    private void updateTanggalDiperbarui() {
        this.tanggalDiperbarui = new Date();
    }

    private boolean isValidHari(@NonNull String hari) {
        String[] validHari = {"daily", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        for (String h : validHari) {
            if (h.equalsIgnoreCase(hari)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidTimeFormat(@NonNull String waktu) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(waktu);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public boolean isValid() {
        return obatId > 0 &&
                hari != null && !hari.trim().isEmpty() && isValidHari(hari) &&
                waktu != null && !waktu.trim().isEmpty() && isValidTimeFormat(waktu) &&
                status >= STATUS_BELUM_DIMINUM && status <= STATUS_TERLEWAT;
    }

    // Method untuk menandai sudah diminum
    public void markAsSudahDiminum() {
        setStatus(STATUS_SUDAH_DIMINUM);
        this.tanggalDiminum = new Date();
    }

    // Method untuk menandai terlewat
    public void markAsTerlewat() {
        setStatus(STATUS_TERLEWAT);
    }

    // Method untuk mendapatkan status dalam string
    @NonNull
    public String getStatusString() {
        switch (status) {
            case STATUS_SUDAH_DIMINUM:
                return "Sudah Diminum";
            case STATUS_TERLEWAT:
                return "Terlewat";
            case STATUS_BELUM_DIMINUM:
            default:
                return "Belum Diminum";
        }
    }

    // Untuk debugging
    @Override
    public String toString() {
        return "Jadwal{" +
                "id=" + id +
                ", obatId=" + obatId +
                ", hari='" + hari + '\'' +
                ", waktu='" + waktu + '\'' +
                ", status=" + status +
                ", statusString='" + getStatusString() + '\'' +
                '}';
    }

    // Untuk membandingkan objek
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Jadwal jadwal = (Jadwal) obj;
        return id == jadwal.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}