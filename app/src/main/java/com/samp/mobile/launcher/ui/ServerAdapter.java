package com.samp.mobile.launcher.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.samp.mobile.R;
import com.samp.mobile.game.SAMP;

import java.util.ArrayList;
import java.util.List;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {

    private List<ServerItem> items = new ArrayList<>();
    private final Context context;

    public ServerAdapter(Context context) {
        this.context = context;
    }

    public void submitList(List<ServerItem> newList) {
        this.items = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_server, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ServerItem item = items.get(position);

        h.tvName.setText(item.hostname);
        h.tvGamemode.setText(item.gamemode + " • " + item.mapName);
        h.tvAddress.setText(item.getAddress());
        h.chipPlayers.setText(item.players + "/" + item.maxPlayers + " pemain");
        h.chipPing.setText(item.ping > 0 ? item.ping + " ms" : "— ms");
        h.ivLock.setVisibility(item.isPassworded ? View.VISIBLE : View.GONE);
        h.imgFavorite.setImageResource(item.isFavorite
                ? android.R.drawable.btn_star_big_on
                : android.R.drawable.btn_star_big_off);

        // Color-code ping chip
        switch (item.getPingCategory()) {
            case 0: // good
                h.chipPing.setChipBackgroundColorResource(R.color.md_theme_dark_primaryContainer);
                break;
            case 1: // medium
                h.chipPing.setChipBackgroundColorResource(R.color.md_theme_dark_tertiaryContainer);
                break;
            default: // bad
                h.chipPing.setChipBackgroundColorResource(R.color.md_theme_dark_surfaceVariant);
                break;
        }

        h.imgFavorite.setOnClickListener(v -> {
            item.isFavorite = !item.isFavorite;
            h.imgFavorite.setImageResource(item.isFavorite
                    ? android.R.drawable.btn_star_big_on
                    : android.R.drawable.btn_star_big_off);
        });

        h.btnConnect.setOnClickListener(v -> {
            Intent intent = new Intent(context, SAMP.class);
            intent.putExtra("server_ip", item.ip);
            intent.putExtra("server_port", item.port);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvGamemode, tvAddress;
        Chip chipPlayers, chipPing;
        ImageButton imgFavorite;
        ImageView ivLock;
        MaterialButton btnConnect;

        ViewHolder(@NonNull View view) {
            super(view);
            tvName = view.findViewById(R.id.tv_server_name);
            tvGamemode = view.findViewById(R.id.tv_gamemode);
            tvAddress = view.findViewById(R.id.tv_address);
            chipPlayers = view.findViewById(R.id.chip_players);
            chipPing = view.findViewById(R.id.chip_ping);
            imgFavorite = view.findViewById(R.id.img_favorite);
            ivLock = view.findViewById(R.id.iv_lock);
            btnConnect = view.findViewById(R.id.btn_connect);
        }
    }
}
