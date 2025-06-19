package com.example.medremind.data.model;

public class Obat {
    private int id;
    private String namaObat;
    private String jenisObat;
    private String dosisObat;
    private String aturanMinum;
    private int jumlahObat;
    private String tipeJadwal; // 'harian' atau 'mingguan'

    // Constructor kosong
    public Obat() {
    }

    // Constructor dengan parameter
    public Obat(int id,String namaObat, String jenisObat, String dosisObat, String aturanMinum, int jumlahObat, String tipeJadwal) {
        this.id = id;
        this.namaObat = namaObat;
        this.jenisObat = jenisObat;
        this.dosisObat = dosisObat;
        this.aturanMinum = aturanMinum;
        this.jumlahObat = jumlahObat;
        this.tipeJadwal = tipeJadwal;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNamaObat() {
        return namaObat;
    }

    public void setNamaObat(String namaObat) {
        this.namaObat = namaObat;
    }

    public String getJenisObat() {
        return jenisObat;
    }

    public void setJenisObat(String jenisObat) {
        this.jenisObat = jenisObat;
    }

    public String getDosisObat() {
        return dosisObat;
    }

    public void setDosisObat(String dosisObat) {
        this.dosisObat = dosisObat;
    }

    public String getAturanMinum() {
        return aturanMinum;
    }

    public void setAturanMinum(String aturanMinum) {
        this.aturanMinum = aturanMinum;
    }

    public int getJumlahObat() {
        return jumlahObat;
    }

    public void setJumlahObat(int jumlahObat) {
        this.jumlahObat = jumlahObat;
    }

    public String getTipeJadwal() {
        return tipeJadwal;
    }

    public void setTipeJadwal(String tipeJadwal) {
        this.tipeJadwal = tipeJadwal;
    }
}
