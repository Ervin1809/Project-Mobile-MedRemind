package com.example.medremind.data.model;

import java.util.HashMap;
import java.util.Map;

public class Jadwal {
    private int id;
    private int obatId;
    private String hari; // "daily" untuk jadwal harian, atau nama hari untuk jadwal mingguan
    private String waktu; // format "HH:MM"
    private int status; // 0: belum diminum, 1: sudah diminum
    private Map<String, String> tambahan; // untuk menyimpan data tambahan seperti nama obat

    // Constructor kosong
    public Jadwal() {
        tambahan = new HashMap<>();
    }

    // Constructor dengan parameter
    public Jadwal(int obatId, String hari, String waktu) {
        this.obatId = obatId;
        this.hari = hari;
        this.waktu = waktu;
        this.status = 0; // default belum diminum
        tambahan = new HashMap<>();
    }

    // Getters and Setters
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
        this.obatId = obatId;
    }

    public String getHari() {
        return hari;
    }

    public void setHari(String hari) {
        this.hari = hari;
    }

    public String getWaktu() {
        return waktu;
    }

    public void setWaktu(String waktu) {
        this.waktu = waktu;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setTambahan(String key, String value) {
        if (tambahan == null) {
            tambahan = new HashMap<>();
        }
        tambahan.put(key, value);
    }

    public String getTambahan(String key) {
        if (tambahan != null && tambahan.containsKey(key)) {
            return tambahan.get(key);
        }
        return null;
    }

    public Map<String, String> getAllTambahan() {
        return tambahan;
    }
}
