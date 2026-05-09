package com.samp.mobile.launcher.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.samp.mobile.R;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ServerListFragment extends Fragment {

    // Default: Djava Roleplay as primary, plus a few well-known servers
    private static final String[][] KNOWN_SERVERS = {
            {"151.240.0.201", "7798"},   // Djava Roleplay (primary)
            {"5.135.125.143", "7777"},   // Example SA-MP server 2
            {"198.50.176.134", "7777"},  // Example SA-MP server 3
    };

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private TextView tvEmptyMessage;
    private MaterialButton btnRetry;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;

    private ServerAdapter adapter;
    private List<ServerItem> allServers = new ArrayList<>();
    private String currentQuery = "";
    private int currentFilter = 0; // 0=all, 1=favorites, 2=lowping

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_servers);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        btnRetry = view.findViewById(R.id.btn_retry);
        etSearch = view.findViewById(R.id.et_search);
        chipGroupFilter = view.findViewById(R.id.chip_group_filter);

        adapter = new ServerAdapter(requireContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.md_theme_dark_primary);
        swipeRefresh.setOnRefreshListener(this::loadServers);

        btnRetry.setOnClickListener(v -> loadServers());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {
                currentQuery = s.toString().toLowerCase();
                applyFilter();
            }
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_all) currentFilter = 0;
            else if (id == R.id.chip_favorites) currentFilter = 1;
            else if (id == R.id.chip_low_ping) currentFilter = 2;
            applyFilter();
        });

        loadServers();
    }

    private void loadServers() {
        swipeRefresh.setRefreshing(true);
        allServers.clear();
        layoutEmpty.setVisibility(View.GONE);

        for (String[] server : KNOWN_SERVERS) {
            String ip = server[0];
            int port = Integer.parseInt(server[1]);
            executor.execute(() -> queryServer(ip, port));
        }

        // After a timeout, stop refresh indicator
        mainHandler.postDelayed(() -> {
            if (isAdded()) {
                swipeRefresh.setRefreshing(false);
                if (allServers.isEmpty()) {
                    showEmpty(getString(R.string.servers_error));
                }
            }
        }, 4000);
    }

    private void queryServer(String ip, int port) {
        try {
            long t = System.currentTimeMillis();
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            InetAddress address = InetAddress.getByName(ip);
            byte[] ipBytes = address.getAddress();

            byte[] pkt = new byte[11];
            pkt[0] = 'S'; pkt[1] = 'A'; pkt[2] = 'M'; pkt[3] = 'P';
            pkt[4] = ipBytes[0]; pkt[5] = ipBytes[1]; pkt[6] = ipBytes[2]; pkt[7] = ipBytes[3];
            pkt[8] = (byte)(port & 0xFF); pkt[9] = (byte)((port >> 8) & 0xFF);
            pkt[10] = 'i';

            socket.send(new DatagramPacket(pkt, pkt.length, address, port));
            byte[] resp = new byte[512];
            DatagramPacket respPkt = new DatagramPacket(resp, resp.length);
            socket.receive(respPkt);
            int ping = (int)(System.currentTimeMillis() - t);

            // Parse response
            int off = 11;
            off += 1; // isPassworded
            int players = (resp[off] & 0xFF) | ((resp[off+1] & 0xFF) << 8); off += 2;
            int maxP = (resp[off] & 0xFF) | ((resp[off+1] & 0xFF) << 8); off += 2;
            int hnLen = (resp[off] & 0xFF) | ((resp[off+1] & 0xFF) << 8)
                    | ((resp[off+2] & 0xFF) << 16) | ((resp[off+3] & 0xFF) << 24); off += 4;
            String hostname = new String(resp, off, Math.min(hnLen, 100)); off += hnLen;
            int gmLen = (resp[off] & 0xFF) | ((resp[off+1] & 0xFF) << 8)
                    | ((resp[off+2] & 0xFF) << 16) | ((resp[off+3] & 0xFF) << 24); off += 4;
            String gamemode = new String(resp, off, Math.min(gmLen, 64)); off += gmLen;
            int mapLen = (resp[off] & 0xFF) | ((resp[off+1] & 0xFF) << 8)
                    | ((resp[off+2] & 0xFF) << 16) | ((resp[off+3] & 0xFF) << 24); off += 4;
            String mapName = new String(resp, off, Math.min(mapLen, 64));

            socket.close();

            ServerItem item = new ServerItem(hostname, ip, port, gamemode, mapName,
                    players, maxP, ping, resp[10] == 1);

            mainHandler.post(() -> {
                if (!isAdded()) return;
                allServers.add(item);
                allServers.sort((a, b) -> {
                    // Djava Roleplay always first
                    if (a.ip.equals("151.240.0.201")) return -1;
                    if (b.ip.equals("151.240.0.201")) return 1;
                    return Integer.compare(b.players, a.players);
                });
                swipeRefresh.setRefreshing(false);
                applyFilter();
            });

        } catch (Exception e) {
            mainHandler.post(() -> {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
            });
        }
    }

    private void applyFilter() {
        List<ServerItem> filtered = new ArrayList<>(allServers);

        // Search query
        if (!currentQuery.isEmpty()) {
            List<ServerItem> result = new ArrayList<>();
            for (ServerItem s : filtered) {
                if (s.hostname.toLowerCase().contains(currentQuery)
                        || s.gamemode.toLowerCase().contains(currentQuery)
                        || s.getAddress().contains(currentQuery)) {
                    result.add(s);
                }
            }
            filtered = result;
        }

        // Filter chip
        if (currentFilter == 1) {
            List<ServerItem> favs = new ArrayList<>();
            for (ServerItem s : filtered) { if (s.isFavorite) favs.add(s); }
            filtered = favs;
        } else if (currentFilter == 2) {
            List<ServerItem> low = new ArrayList<>();
            for (ServerItem s : filtered) { if (s.ping < 80) low.add(s); }
            filtered = low;
        }

        adapter.submitList(filtered);

        if (filtered.isEmpty()) {
            showEmpty(currentFilter == 1
                    ? "Belum ada server favorit"
                    : getString(R.string.servers_empty));
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showEmpty(String msg) {
        recyclerView.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptyMessage.setText(msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
