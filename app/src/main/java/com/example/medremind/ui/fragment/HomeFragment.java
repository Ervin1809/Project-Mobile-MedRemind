package com.example.medremind.ui.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.medremind.R;
import com.example.medremind.data.helper.ObatHelper;
import com.example.medremind.data.helper.JadwalHelper;
import com.example.medremind.data.model.Obat;
import com.example.medremind.ui.activity.DetailJadwalActivity;
import com.example.medremind.ui.activity.MainActivity;
import com.example.medremind.ui.adapter.ObatAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements ObatAdapter.ObatClickListener {
    private static final String TAG = "HomeFragment";

    // UI Components
    private RecyclerView rvObat;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ObatAdapter adapter;

    // Database helpers
    private ObatHelper obatHelper;
    private JadwalHelper jadwalHelper;

    // Background executor
    private ExecutorService executor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents(view);
        setupRecyclerView();
        setupSwipeRefresh();

        Log.d(TAG, "HomeFragment initialized successfully");
    }

    private void initializeComponents(@NonNull View view) {
        // Initialize views
        rvObat = view.findViewById(R.id.rv_obat);
        tvEmpty = view.findViewById(R.id.tv_empty);

//        // SwipeRefreshLayout adalah optional - jika tidak ada di layout, skip
//        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
//        if (swipeRefreshLayout == null) {
//            Log.d(TAG, "SwipeRefreshLayout not found in layout, using manual refresh");
//        }

        // Initialize database helpers
        obatHelper = new ObatHelper(requireContext());
        jadwalHelper = new JadwalHelper(requireContext());

        // Initialize executor for background tasks
        executor = Executors.newSingleThreadExecutor();
    }

    private void setupRecyclerView() {
        adapter = new ObatAdapter();
        adapter.setObatClickListener(this);

        // 🔑 Set context ke adapter setelah constructor
        adapter.setContext(requireContext());

        rvObat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvObat.setAdapter(adapter);

        // Add item decoration if needed
        // rvObat.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadObatData);
            swipeRefreshLayout.setColorSchemeResources(
                    R.color.green,
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light
            );
        }
    }

    private void loadObatData() {
        // 🔑 CHECK: Fragment masih attached?
        if (!isAdded() || getContext() == null) {
            Log.w(TAG, "Fragment not attached, skipping data load");
            return;
        }

        // Show loading indicator
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }

        // Load data in background thread
        executor.execute(() -> {
            try {
                // Open database connections
                obatHelper.open();
                jadwalHelper.open();

                // Get obat list
                List<Obat> obatList = obatHelper.getAllObat(true); // Only active obat

                // Calculate jadwal count for each obat
                Map<Integer, Integer> jumlahMakanMap = new HashMap<>();
                for (Obat obat : obatList) {
                    int count = jadwalHelper.getJadwalByObatId(obat.getId()).size();
                    jumlahMakanMap.put(obat.getId(), count);
                }

                // 🔑 CHECK: Fragment masih attached sebelum update UI?
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> updateUI(obatList, jumlahMakanMap));
                } else {
                    Log.w(TAG, "Fragment detached during data loading");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading obat data: " + e.getMessage(), e);

                // 🔑 SAFE: Only update UI if fragment still attached
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> handleLoadError(e));
                }
            } finally {
                // Close database connections
                try {
                    obatHelper.close();
                    jadwalHelper.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing database: " + e.getMessage(), e);
                }

                // 🔑 SAFE: Hide loading indicator jika fragment masih attached
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
                }
            }
        });
    }

    private void updateUI(@NonNull List<Obat> obatList, @NonNull Map<Integer, Integer> jumlahMakanMap) {
        try {
            // 🔑 CHECK: Fragment masih attached?
            if (!isAdded() || getContext() == null) {
                Log.w(TAG, "Fragment not attached, skipping UI update");
                return;
            }

            // 🔑 CHECK: Views masih valid?
            if (tvEmpty == null || rvObat == null) {
                Log.w(TAG, "Views are null, skipping UI update");
                return;
            }

            if (obatList.isEmpty()) {
                showEmptyState();
            } else {
                showObatList(obatList, jumlahMakanMap);
            }

            Log.d(TAG, "UI updated successfully with " + obatList.size() + " obat");

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);

            // 🔑 SAFE: Only show toast if fragment still attached
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Error menampilkan data", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showEmptyState() {
        try {
            // 🔑 CHECK: Views exist?
            if (tvEmpty == null || rvObat == null) {
                Log.w(TAG, "Cannot show empty state - views are null");
                return;
            }

            tvEmpty.setVisibility(View.VISIBLE);
            rvObat.setVisibility(View.GONE);

            // Set empty message
            tvEmpty.setText("Belum ada obat yang ditambahkan.\nTambahkan obat baru untuk memulai.");

            Log.d(TAG, "Empty state shown");

        } catch (Exception e) {
            Log.e(TAG, "Error showing empty state: " + e.getMessage(), e);
        }
    }

    private void showObatList(@NonNull List<Obat> obatList, @NonNull Map<Integer, Integer> jumlahMakanMap) {
        try {
            // 🔑 CHECK: Views exist?
            if (tvEmpty == null || rvObat == null || adapter == null) {
                Log.w(TAG, "Cannot show obat list - views or adapter are null");
                return;
            }

            tvEmpty.setVisibility(View.GONE);
            rvObat.setVisibility(View.VISIBLE);

            adapter.setObatList(obatList);
            adapter.setJumlahMakanMap(jumlahMakanMap);

            Log.d(TAG, "Obat list shown with " + obatList.size() + " items");

        } catch (Exception e) {
            Log.e(TAG, "Error showing obat list: " + e.getMessage(), e);
        }
    }

    private void handleLoadError(@NonNull Exception e) {
        try {
            Log.e(TAG, "Failed to load obat data: " + e.getMessage(), e);

            // 🔑 SAFE: Only show toast if fragment attached
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(),
                        "Gagal memuat data: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }

            // 🔑 CHECK: Views exist before showing error state?
            if (tvEmpty != null && rvObat != null) {
                tvEmpty.setVisibility(View.VISIBLE);
                rvObat.setVisibility(View.GONE);
                tvEmpty.setText("Error memuat data.\nTarik ke bawah untuk mencoba lagi.");
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error in handleLoadError: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // 🔑 CHECK: Fragment attached dan views initialized?
        if (isAdded() && getContext() != null && tvEmpty != null && rvObat != null) {
            loadObatData();
            refreshNotificationAlarms();
            refreshObatProgress();
            Log.d(TAG, "HomeFragment resumed, loading data and refreshing notifications");
        } else {
            Log.w(TAG, "HomeFragment resumed but not ready for data loading");
        }
    }

    @Override
    public void onObatClick(@NonNull Obat obat) {
        try {
            Log.d(TAG, "Obat clicked: " + obat.getNamaObat());

            // Show obat info
            String message = String.format("Obat: %s\nDosis: %s\nJumlah: %d",
                    obat.getNamaObat(), obat.getDosisObat(), obat.getJumlahObat());

            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

            // TODO: Navigate to obat detail fragment
            // navigateToObatDetail(obat);

        } catch (Exception e) {
            Log.e(TAG, "Error handling obat click: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLihatSelengkapnyaClick(@NonNull Obat obat) {
        try {
            Log.d(TAG, "Lihat selengkapnya clicked for: " + obat.getNamaObat());

            // Navigate to DetailObatActivity
            DetailJadwalActivity.start(requireContext(), obat.getId(), obat.getNamaObat());

        } catch (Exception e) {
            Log.e(TAG, "Error handling lihat selengkapnya click: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void refreshNotificationAlarms() {
        try {
            // 🔑 CHECK: Fragment masih attached?
            if (!isAdded() || getActivity() == null) {
                Log.w(TAG, "Fragment not attached, skipping notification refresh");
                return;
            }

            if (getActivity() instanceof MainActivity) {
                MainActivity mainActivity = (MainActivity) getActivity();
                if (mainActivity.getAlarmScheduler() != null) {
                    mainActivity.getAlarmScheduler().scheduleAllMedicationReminders();
                    Log.d(TAG, "Notification alarms refreshed");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing notification alarms: " + e.getMessage(), e);
        }
    }

    private void refreshObatProgress() {
        try {
            if (adapter != null && adapter.getClass().getMethod("refreshDailyProgress") != null) {
                adapter.refreshDailyProgress();
                Log.d(TAG, "Obat progress refreshed");
            }
        } catch (NoSuchMethodException e) {
            Log.d(TAG, "refreshDailyProgress method not found in adapter");
        } catch (Exception e) {
            Log.e(TAG, "Error refreshing obat progress: " + e.getMessage(), e);
        }
    }

    private void navigateToJadwalDetail(@NonNull Obat obat) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("obat_id", obat.getId());
            bundle.putString("obat_nama", obat.getNamaObat());

            // Navigation.findNavController(requireView())
            //         .navigate(R.id.action_homeFragment_to_detailJadwalFragment, bundle);

            // Temporary: Show toast until detail fragment is implemented
            Toast.makeText(requireContext(),
                    "Detail jadwal untuk " + obat.getNamaObat() + " (ID: " + obat.getId() + ")",
                    Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error navigasi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Shutdown executor
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }

        // Clear adapter
        if (adapter != null) {
            adapter.setObatClickListener(null);
            adapter = null;
        }

        // Close database connections if still open
        try {
            if (obatHelper != null && obatHelper.isOpen()) {
                obatHelper.close();
            }
            if (jadwalHelper != null && jadwalHelper.isOpen()) {
                jadwalHelper.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing database in onDestroyView: " + e.getMessage(), e);
        }

        // Clear references
        rvObat = null;
        tvEmpty = null;
        swipeRefreshLayout = null;
        obatHelper = null;
        jadwalHelper = null;

        Log.d(TAG, "HomeFragment destroyed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Final cleanup
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }
}