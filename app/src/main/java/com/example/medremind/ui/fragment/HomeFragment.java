package com.example.medremind.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medremind.R;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.model.Obat;
import com.example.medremind.ui.adapter.ObatAdapter;

import java.util.List;

public class HomeFragment extends Fragment implements ObatAdapter.ObatClickListener {

    private RecyclerView rvObat;
    private TextView tvEmpty;
    private ObatAdapter adapter;
    private ObatHelper obatHelper;
    private JadwalHelper jadwalHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inisialisasi views
        rvObat = view.findViewById(R.id.rv_obat);
        tvEmpty = view.findViewById(R.id.tv_empty);

        // Inisialisasi helpers
        obatHelper = new ObatHelper(requireContext());
        jadwalHelper = new JadwalHelper(requireContext());

        // Setup RecyclerView
        setupRecyclerView();
    }

    private void setupRecyclerView() {
        adapter = new ObatAdapter();
        adapter.setObatClickListener(this);

        rvObat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvObat.setAdapter(adapter);
    }

    private void loadObatData() {
        obatHelper.open();
        try {
            List<Obat> obatList = obatHelper.getAllObat();

            if (obatList.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvObat.setVisibility(View.GONE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                rvObat.setVisibility(View.VISIBLE);
                adapter.setObatList(obatList);
            }
        } finally {
            obatHelper.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadObatData();
    }

    @Override
    public void onObatClick(Obat obat) {
        // Handle klik pada item obat
        Toast.makeText(requireContext(), "Obat: " + obat.getNamaObat(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLihatSelengkapnyaClick(Obat obat) {
        // Handle klik pada tombol "LIHAT SELENGKAPNYA"
        Toast.makeText(requireContext(), "Lihat detail jadwal untuk " + obat.getNamaObat(), Toast.LENGTH_SHORT).show();

        // Di sini Anda bisa menambahkan kode untuk berpindah ke fragment detail jadwal
        // Contoh dengan Navigation Component:
        // Bundle bundle = new Bundle();
        // bundle.putInt("obat_id", obat.getId());
        // Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_detailJadwalFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Pastikan koneksi database ditutup
        if (obatHelper != null) {
            obatHelper.close();
        }

        if (jadwalHelper != null) {
            jadwalHelper.close();
        }
    }
}

//package com.example.medremind.ui.fragment;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.medremind.R;
//import com.example.medremind.data.helper.ObatHelper;
//import com.example.medremind.data.helper.JadwalHelper;
//import com.example.medremind.data.model.Obat;
//import com.example.medremind.ui.adapter.ObatAdapter;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class HomeFragment extends Fragment implements ObatAdapter.ObatClickListener {
//
//    private RecyclerView rvObat;
//    private TextView tvEmpty;
//    private ObatAdapter adapter;
//    private ObatHelper obatHelper;
//    private JadwalHelper jadwalHelper;
//    private boolean isDataDummyAdded = false;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_home, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        // Inisialisasi views
//        rvObat = view.findViewById(R.id.rv_obat);
//        tvEmpty = view.findViewById(R.id.tv_empty);
//
//        // Inisialisasi helpers
//        obatHelper = new ObatHelper(requireContext());
//        jadwalHelper = new JadwalHelper(requireContext());
//
//        // Setup RecyclerView
//        setupRecyclerView();
//    }
//
//    private void setupRecyclerView() {
//        adapter = new ObatAdapter();
//        adapter.setObatClickListener(this);
//
//        rvObat.setLayoutManager(new LinearLayoutManager(requireContext()));
//        rvObat.setAdapter(adapter);
//    }
//
//    private void loadObatData() {
//        obatHelper.open();
//        try {
//            List<Obat> obatList = obatHelper.getAllObat();
//
//            if (obatList.isEmpty() && !isDataDummyAdded) {
//                // Tambahkan data dummy jika database kosong dan belum pernah menambahkan data dummy
//                obatList = addDummyData();
//                isDataDummyAdded = true;
//            }
//
//            if (obatList.isEmpty()) {
//                tvEmpty.setVisibility(View.VISIBLE);
//                rvObat.setVisibility(View.GONE);
//            } else {
//                tvEmpty.setVisibility(View.GONE);
//                rvObat.setVisibility(View.VISIBLE);
//                adapter.setObatList(obatList);
//            }
//        } finally {
//            obatHelper.close();
//        }
//    }
//
//    private List<Obat> addDummyData() {
//        List<Obat> dummyObatList = new ArrayList<>();
//
//        // Buat data obat dummy
//        Obat paracetamol = new Obat();
//        paracetamol.setId(1);
//        paracetamol.setNamaObat("Paracetamol");
//        paracetamol.setDosisObat("1 Tablet");
//        paracetamol.setJenisObat("Tablet");
//        paracetamol.setTipeJadwal("daily");
//        paracetamol.setJumlahObat(30);
//        paracetamol.setAturanMinum("Setelah makan");
//
//        // Tambahkan ke database
//        obatHelper.insertObat(paracetamol);
//
//        // Tambahkan juga ke list untuk dikembalikan
//        dummyObatList.add(paracetamol);
//
//        Toast.makeText(requireContext(), "Data dummy ditambahkan", Toast.LENGTH_SHORT).show();
//
//        return dummyObatList;
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        loadObatData();
//    }
//
//    @Override
//    public void onObatClick(Obat obat) {
//        // Handle klik pada item obat
//        Toast.makeText(requireContext(), "Obat: " + obat.getNamaObat(), Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void onLihatSelengkapnyaClick(Obat obat) {
//        // Handle klik pada tombol "LIHAT SELENGKAPNYA"
//        Toast.makeText(requireContext(), "Lihat detail jadwal untuk " + obat.getNamaObat(), Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//
//        // Pastikan koneksi database ditutup
//        if (obatHelper != null) {
//            obatHelper.close();
//        }
//
//        if (jadwalHelper != null) {
//            jadwalHelper.close();
//        }
//    }
//}