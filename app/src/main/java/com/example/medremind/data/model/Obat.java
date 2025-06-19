package com.example.medremind.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Date;

public class Obat {
    private int id;
    private String namaObat;
    private String jenisObat;
    private String dosisObat;
    private String aturanMinum;
    private int jumlahObat;
    private String tipeJadwal; // 'harian' atau 'mingguan'
    private Date tanggalDibuat;
    private Date tanggalDiperbarui;
    private boolean isAktif; // untuk soft delete

    // Constructor kosong
    public Obat() {
        this.tanggalDibuat = new Date();
        this.tanggalDiperbarui = new Date();
        this.isAktif = true;
    }

    // Constructor lengkap dengan ID (untuk data dari database)
    public Obat(int id, @NonNull String namaObat, @NonNull String jenisObat,
                @NonNull String dosisObat, @NonNull String aturanMinum,
                int jumlahObat, @NonNull String tipeJadwal) {
        this();
        this.id = id;
        this.namaObat = namaObat;
        this.jenisObat = jenisObat;
        this.dosisObat = dosisObat;
        this.aturanMinum = aturanMinum;
        this.jumlahObat = jumlahObat;
        this.tipeJadwal = tipeJadwal;
    }

    // Constructor tanpa ID (untuk insert baru)
    public Obat(@NonNull String namaObat, @NonNull String jenisObat,
                @NonNull String dosisObat, @NonNull String aturanMinum,
                int jumlahObat, @NonNull String tipeJadwal) {
        this();
        this.namaObat = namaObat;
        this.jenisObat = jenisObat;
        this.dosisObat = dosisObat;
        this.aturanMinum = aturanMinum;
        this.jumlahObat = jumlahObat;
        this.tipeJadwal = tipeJadwal;
    }

    // Getters and Setters dengan validasi
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getNamaObat() {
        return namaObat != null ? namaObat : "";
    }

    public void setNamaObat(@NonNull String namaObat) {
        if (namaObat == null || namaObat.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama obat tidak boleh kosong");
        }
        this.namaObat = namaObat.trim();
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getJenisObat() {
        return jenisObat != null ? jenisObat : "";
    }

    public void setJenisObat(@NonNull String jenisObat) {
        if (jenisObat == null || jenisObat.trim().isEmpty()) {
            throw new IllegalArgumentException("Jenis obat tidak boleh kosong");
        }
        this.jenisObat = jenisObat.trim();
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getDosisObat() {
        return dosisObat != null ? dosisObat : "";
    }

    public void setDosisObat(@NonNull String dosisObat) {
        if (dosisObat == null || dosisObat.trim().isEmpty()) {
            throw new IllegalArgumentException("Dosis obat tidak boleh kosong");
        }
        this.dosisObat = dosisObat.trim();
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getAturanMinum() {
        return aturanMinum != null ? aturanMinum : "";
    }

    public void setAturanMinum(@NonNull String aturanMinum) {
        if (aturanMinum == null || aturanMinum.trim().isEmpty()) {
            throw new IllegalArgumentException("Aturan minum tidak boleh kosong");
        }
        this.aturanMinum = aturanMinum.trim();
        updateTanggalDiperbarui();
    }

    public int getJumlahObat() {
        return jumlahObat;
    }

    public void setJumlahObat(int jumlahObat) {
        if (jumlahObat < 0) {
            throw new IllegalArgumentException("Jumlah obat tidak boleh negatif");
        }
        this.jumlahObat = jumlahObat;
        updateTanggalDiperbarui();
    }

    @NonNull
    public String getTipeJadwal() {
        return tipeJadwal != null ? tipeJadwal : "";
    }

    public void setTipeJadwal(@NonNull String tipeJadwal) {
        if (tipeJadwal == null ||
                (!tipeJadwal.equals("harian") && !tipeJadwal.equals("mingguan"))) {
            throw new IllegalArgumentException("Tipe jadwal harus 'harian' atau 'mingguan'");
        }
        this.tipeJadwal = tipeJadwal;
        updateTanggalDiperbarui();
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

    public boolean isAktif() {
        return isAktif;
    }

    public void setAktif(boolean aktif) {
        this.isAktif = aktif;
        updateTanggalDiperbarui();
    }

    // Helper methods
    private void updateTanggalDiperbarui() {
        this.tanggalDiperbarui = new Date();
    }

    public boolean isValid() {
        return namaObat != null && !namaObat.trim().isEmpty() &&
                jenisObat != null && !jenisObat.trim().isEmpty() &&
                dosisObat != null && !dosisObat.trim().isEmpty() &&
                aturanMinum != null && !aturanMinum.trim().isEmpty() &&
                jumlahObat >= 0 &&
                (tipeJadwal != null && (tipeJadwal.equals("harian") || tipeJadwal.equals("mingguan")));
    }

    // Untuk debugging
    @Override
    public String toString() {
        return "Obat{" +
                "id=" + id +
                ", namaObat='" + namaObat + '\'' +
                ", jenisObat='" + jenisObat + '\'' +
                ", dosisObat='" + dosisObat + '\'' +
                ", aturanMinum='" + aturanMinum + '\'' +
                ", jumlahObat=" + jumlahObat +
                ", tipeJadwal='" + tipeJadwal + '\'' +
                ", isAktif=" + isAktif +
                '}';
    }

    // Untuk membandingkan objek
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Obat obat = (Obat) obj;
        return id == obat.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}