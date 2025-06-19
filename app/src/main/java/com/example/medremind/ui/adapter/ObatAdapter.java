package com.example.medremind.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medremind.R;
import com.example.medremind.data.model.Obat;

import java.util.ArrayList;
import java.util.List;

public class ObatAdapter extends RecyclerView.Adapter<ObatAdapter.ObatViewHolder> {

    private List<Obat> obatList;
    private ObatClickListener listener;

    public interface ObatClickListener {
        void onObatClick(Obat obat);
        void onLihatSelengkapnyaClick(Obat obat);
    }

    public ObatAdapter() {
        this.obatList = new ArrayList<>();
    }

    public void setObatClickListener(ObatClickListener listener) {
        this.listener = listener;
    }

    public void setObatList(List<Obat> obatList) {
        this.obatList = obatList;
        notifyDataSetChanged();
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

            // Set tipe jadwal dan jumlah makan
            String tipeJadwal = obat.getTipeJadwal();
            if (tipeJadwal != null) {
                if (tipeJadwal.equalsIgnoreCase("daily")) {
                    tvTipeJadwal.setText("Sehari");
                    tvJumlahMakan.setText("3 x"); // Asumsi 3x sehari
                } else {
                    tvTipeJadwal.setText("Seminggu");
                    tvJumlahMakan.setText("1 x"); // Asumsi 1x seminggu
                }
            }

            // Set daily progress
            // Ini adalah contoh statis, Anda perlu mengimplementasikan logika sebenarnya
            // untuk menghitung progress berdasarkan jadwal
            tvDailyProgress.setText("1/3");
        }
    }
}