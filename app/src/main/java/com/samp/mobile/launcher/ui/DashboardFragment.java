package com.samp.mobile.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.samp.mobile.R;
import com.samp.mobile.game.SAMP;
import com.samp.mobile.launcher.other.Util;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private static final String SERVER_IP = "151.240.0.201";
    private static final int SERVER_PORT = 7798;

    private TextView tvPlayerCount, tvPing, tvMaxPlayers;
    private TextView tvServerHostname, tvServerGamemode, tvFileStatus;
    private Chip chipFileStatus;
    private MaterialButton btnLaunchGame, btnConnectMain, btnGoDownload;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPlayerCount = view.findViewById(R.id.tv_player_count);
        tvPing = view.findViewById(R.id.tv_ping);
        tvMaxPlayers = view.findViewById(R.id.tv_max_players);
        tvServerHostname = view.findViewById(R.id.tv_server_hostname);
        tvServerGamemode = view.findViewById(R.id.tv_server_gamemode);
        tvFileStatus = view.findViewById(R.id.tv_file_status);
        chipFileStatus = view.findViewById(R.id.chip_file_status);
        btnLaunchGame = view.findViewById(R.id.btn_launch_game);
        btnConnectMain = view.findViewById(R.id.btn_connect_main);
        btnGoDownload = view.findViewById(R.id.btn_go_download);

        btnLaunchGame.setOnClickListener(v -> launchGame());
        btnConnectMain.setOnClickListener(v -> launchGame());
        btnGoDownload.setOnClickListener(v -> navigateToDownloads());

        checkFileStatus();
        queryServer();
    }

    private void launchGame() {
        Intent intent = new Intent(requireContext(), SAMP.class);
        startActivity(intent);
    }

    private void navigateToDownloads() {
        // Switch bottom nav to downloads tab
        if (getActivity() instanceof com.samp.mobile.launcher.activity.MainActivity) {
            requireActivity().findViewById(R.id.bottom_nav)
                    .performClick(); // handled via BottomNav
            requireActivity().findViewById(R.id.nav_downloads).callOnClick();
        }
    }

    private void checkFileStatus() {
        // Check if game cache files exist
        File gameDir = requireContext().getExternalFilesDir(null);
        boolean cacheExists = gameDir != null && new File(gameDir, "cache").exists();

        if (cacheExists) {
            chipFileStatus.setText(getString(R.string.dashboard_files_ready));
            tvFileStatus.setText(getString(R.string.dashboard_files_ready));
            btnGoDownload.setVisibility(View.GONE);
        } else {
            chipFileStatus.setText(getString(R.string.dashboard_files_missing));
            tvFileStatus.setText(getString(R.string.dashboard_files_missing));
            btnGoDownload.setVisibility(View.VISIBLE);
        }
    }

    /** Query SA-MP server via UDP RCON packet to get player count, hostname, gamemode */
    private void queryServer() {
        executor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                DatagramSocket socket = new DatagramSocket();
                socket.setSoTimeout(3000);

                InetAddress address = InetAddress.getByName(SERVER_IP);
                // SA-MP server info query packet format
                // "SAMP" + IP bytes + port bytes + 'i' (info)
                byte[] ip = address.getAddress();
                int port = SERVER_PORT;

                byte[] packet = new byte[11];
                packet[0] = 'S'; packet[1] = 'A'; packet[2] = 'M'; packet[3] = 'P';
                packet[4] = ip[0]; packet[5] = ip[1]; packet[6] = ip[2]; packet[7] = ip[3];
                packet[8] = (byte)(port & 0xFF); packet[9] = (byte)((port >> 8) & 0xFF);
                packet[10] = 'i'; // info query

                DatagramPacket request = new DatagramPacket(packet, packet.length, address, port);
                socket.send(request);

                byte[] response = new byte[512];
                DatagramPacket responsePacket = new DatagramPacket(response, response.length);
                socket.receive(responsePacket);
                long pingMs = System.currentTimeMillis() - startTime;

                // Parse SA-MP info response (offset 11 = start of data)
                int offset = 11;
                // password (1 byte), players (2 bytes), maxplayers (2 bytes)
                // offset 11: isPassworded
                offset += 1;
                int players = (response[offset] & 0xFF) | ((response[offset + 1] & 0xFF) << 8);
                offset += 2;
                int maxPlayers = (response[offset] & 0xFF) | ((response[offset + 1] & 0xFF) << 8);
                offset += 2;
                // hostname length (4 bytes) then string
                int hostnameLen = (response[offset] & 0xFF) | ((response[offset+1] & 0xFF) << 8)
                        | ((response[offset+2] & 0xFF) << 16) | ((response[offset+3] & 0xFF) << 24);
                offset += 4;
                String hostname = new String(response, offset, Math.min(hostnameLen, 64));
                offset += hostnameLen;
                // gamemode
                int gamemodeLen = (response[offset] & 0xFF) | ((response[offset+1] & 0xFF) << 8)
                        | ((response[offset+2] & 0xFF) << 16) | ((response[offset+3] & 0xFF) << 24);
                offset += 4;
                String gamemode = new String(response, offset, Math.min(gamemodeLen, 32));

                socket.close();

                final int finalPlayers = players;
                final int finalMax = maxPlayers;
                final long finalPing = pingMs;
                final String finalHostname = hostname;
                final String finalGamemode = gamemode;

                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    tvPlayerCount.setText(String.valueOf(finalPlayers));
                    tvMaxPlayers.setText(String.valueOf(finalMax));
                    tvPing.setText(String.valueOf(finalPing));
                    tvServerHostname.setText(finalHostname);
                    tvServerGamemode.setText(finalGamemode);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded()) return;
                    tvServerHostname.setText(getString(R.string.dashboard_server_offline));
                    tvPing.setText("—");
                    tvPlayerCount.setText("—");
                    tvMaxPlayers.setText("—");
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        checkFileStatus();
        queryServer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
