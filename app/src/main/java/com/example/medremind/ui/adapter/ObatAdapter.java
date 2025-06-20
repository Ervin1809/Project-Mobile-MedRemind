package com.example.medremind.ui.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medremind.R;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.model.Jadwal;
import com.example.medremind.data.model.Obat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ObatAdapter extends RecyclerView.Adapter<ObatAdapter.ObatViewHolder> {
    private static final String TAG = "ObatAdapter";

    private List<Obat> obatList;
    private ObatClickListener listener;
    private Map<Integer, Integer> jumlahMakanMap = new HashMap<>();
    private Map<Integer, String> dailyProgressMap = new HashMap<>(); // 🔑 NEW
    private Context context; // 🔑 NEW

    public interface ObatClickListener {
        void onObatClick(Obat obat);
        void onLihatSelengkapnyaClick(Obat obat);
    }

    // 🔑 KEEP original constructor
    public ObatAdapter() {
        this.obatList = new ArrayList<>();
    }

    // 🔑 NEW METHOD untuk set context (dipanggil setelah constructor)
    public void setContext(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void setObatClickListener(ObatClickListener listener) {
        this.listener = listener;
    }

    public void setObatList(List<Obat> obatList) {
        this.obatList = obatList != null ? obatList : new ArrayList<>();

        // Only calculate progress if context is set
        if (context != null) {
            calculateAllDailyProgress(); // 🔑 Calculate progress saat set data
        }

        notifyDataSetChanged();
    }

    public void setJumlahMakanMap(Map<Integer, Integer> jumlahMakanMap) {
        this.jumlahMakanMap = jumlahMakanMap != null ? jumlahMakanMap : new HashMap<>();
        notifyDataSetChanged();
    }

    // 🔑 NEW METHOD untuk set daily progress map
    public void setDailyProgressMap(Map<Integer, String> dailyProgressMap) {
        this.dailyProgressMap = dailyProgressMap != null ? dailyProgressMap : new HashMap<>();
        notifyDataSetChanged();
    }

    // 🔑 NEW METHOD untuk calculate semua daily progress
    private void calculateAllDailyProgress() {
        if (obatList == null || obatList.isEmpty() || context == null) {
            return;
        }

        dailyProgressMap.clear();

        JadwalHelper jadwalHelper = new JadwalHelper(context);
        try {
            jadwalHelper.open();

            // Perform daily reset untuk konsistensi
            jadwalHelper.checkAndPerformDailyReset();
            jadwalHelper.autoMarkTerlewatJadwal();

            for (Obat obat : obatList) {
                String progress = calculateDailyProgressForObat(jadwalHelper, obat.getId());
                dailyProgressMap.put(obat.getId(), progress);
            }

            Log.d(TAG, "Calculated daily progress for " + obatList.size() + " obat");

        } catch (Exception e) {
            Log.e(TAG, "Error calculating daily progress: " + e.getMessage(), e);
        } finally {
            jadwalHelper.close();
        }
    }

    // 🔑 NEW METHOD untuk calculate daily progress untuk obat tertentu
    private String calculateDailyProgressForObat(@NonNull JadwalHelper jadwalHelper, int obatId) {
        try {
            // Get jadwal untuk obat ini
            List<Jadwal> allJadwal = jadwalHelper.getJadwalByObatId(obatId);

            if (allJadwal.isEmpty()) {
                return "0/0";
            }

            // Filter untuk jadwal hari ini
            List<Jadwal> todayJadwal = filterJadwalForToday(allJadwal);

            if (todayJadwal.isEmpty()) {
                return "0/0";
            }

            // Hitung completed vs total
            int completed = 0;
            int total = todayJadwal.size();

            for (Jadwal jadwal : todayJadwal) {
                if (jadwal.getStatus() == Jadwal.STATUS_SUDAH_DIMINUM) {
                    completed++;
                }
            }

            return completed + "/" + total;

        } catch (Exception e) {
            Log.e(TAG, "Error calculating progress for obat " + obatId + ": " + e.getMessage(), e);
            return "0/0";
        }
    }

    // 🔑 NEW METHOD untuk filter jadwal hari ini (sama seperti di DetailObatActivity)
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

    // 🔑 NEW METHOD untuk get nama hari saat ini
    private String getCurrentDayName(Calendar calendar) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", new Locale("id", "ID"));
        return dayFormat.format(calendar.getTime());
    }

    // 🔑 NEW METHOD untuk refresh progress (dipanggil dari fragment/activity)
    public void refreshDailyProgress() {
        if (context != null) {
            calculateAllDailyProgress();
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ObatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_obat, parent, false);
        return new ObatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ObatViewHolder holder, int position) {
        holder.bind(obatList.get(position));
    }

    @Override
    public int getItemCount() {
        return obatList.size();
    }

    public class ObatViewHolder extends RecyclerView.ViewHolder {
        private TextView tvJumlahMakan;
        private TextView tvTipeJadwal;
        private TextView tvNamaObat;
        private TextView tvDosisObat;
        private TextView tvDailyProgress;
        private TextView tvLihatSelengkapnya;

        public ObatViewHolder(@NonNull View itemView) {
            super(itemView);

            // Inisialisasi views
            tvJumlahMakan = itemView.findViewById(R.id.jumlah_makan);
            tvTipeJadwal = itemView.findViewById(R.id.tipe_jadwal);
            tvNamaObat = itemView.findViewById(R.id.nama_obat);
            tvDosisObat = itemView.findViewById(R.id.dosis_obat);
            tvDailyProgress = itemView.findViewById(R.id.daily_progress);
            tvLihatSelengkapnya = itemView.findViewById(R.id.lihat_selengkapnya);

            // Set click listeners
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onObatClick(obatList.get(position));
                }
            });

            tvLihatSelengkapnya.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onLihatSelengkapnyaClick(obatList.get(position));
                }
            });
        }

        public void bind(Obat obat) {
            // Set data ke views
            tvNamaObat.setText(obat.getNamaObat());
            tvDosisObat.setText(obat.getDosisObat());

            // Set tipe jadwal
            String tipeJadwal = obat.getTipeJadwal();
            if (tipeJadwal != null) {
                if (tipeJadwal.equalsIgnoreCase("harian") || tipeJadwal.equalsIgnoreCase("daily")) {
                    tvTipeJadwal.setText("Sehari");
                } else {
                    tvTipeJadwal.setText("Seminggu");
                }
            }

            // Set jumlah makan dari map
            int jumlahMakan = 0;
            if (jumlahMakanMap != null && jumlahMakanMap.containsKey(obat.getId())) {
                jumlahMakan = jumlahMakanMap.get(obat.getId());
            }
            tvJumlahMakan.setText(jumlahMakan + " x");

            // 🔑 Set daily progress dari map
            String dailyProgress = "0/0";
            if (dailyProgressMap != null && dailyProgressMap.containsKey(obat.getId())) {
                dailyProgress = dailyProgressMap.get(obat.getId());
            }
            tvDailyProgress.setText(dailyProgress);

            Log.d(TAG, "Bind obat " + obat.getNamaObat() + " - Progress: " + dailyProgress);
        }
    }
}