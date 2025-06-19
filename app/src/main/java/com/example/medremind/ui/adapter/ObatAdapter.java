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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObatAdapter extends RecyclerView.Adapter<ObatAdapter.ObatViewHolder> {

    private List<Obat> obatList;
    private ObatClickListener listener;
    private Map<Integer, Integer> jumlahMakanMap = new HashMap<>();

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
        this.obatList = obatList != null ? obatList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setJumlahMakanMap(Map<Integer, Integer> jumlahMakanMap) {
        this.jumlahMakanMap = jumlahMakanMap != null ? jumlahMakanMap : new HashMap<>();
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

            // Set daily progress (akan dibahas nanti)
            tvDailyProgress.setText("-");
        }
    }
}