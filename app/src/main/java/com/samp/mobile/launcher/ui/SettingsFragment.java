package com.samp.mobile.launcher.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.samp.mobile.R;

import java.io.File;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "djava_settings";
    private static final String KEY_SHOW_FPS = "show_fps";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";
    private static final String KEY_VOLUME = "volume";

    private SharedPreferences prefs;
    private MaterialSwitch switchFps, switchVibration, switchAudio;
    private Slider sliderVolume;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        prefs = requireContext().getSharedPreferences(PREFS_NAME, 0);

        switchFps = view.findViewById(R.id.switch_show_fps);
        switchVibration = view.findViewById(R.id.switch_vibration);
        switchAudio = view.findViewById(R.id.switch_audio);
        sliderVolume = view.findViewById(R.id.slider_volume);
        MaterialButton btnDiscord = view.findViewById(R.id.btn_discord);
        MaterialButton btnClearCache = view.findViewById(R.id.btn_clear_cache);

        // Load saved preferences
        switchFps.setChecked(prefs.getBoolean(KEY_SHOW_FPS, false));
        switchVibration.setChecked(prefs.getBoolean(KEY_VIBRATION, true));
        switchAudio.setChecked(prefs.getBoolean(KEY_AUDIO_ENABLED, true));
        sliderVolume.setValue(prefs.getFloat(KEY_VOLUME, 80f));

        // Save on change
        switchFps.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_SHOW_FPS, checked).apply());
        switchVibration.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_VIBRATION, checked).apply());
        switchAudio.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean(KEY_AUDIO_ENABLED, checked).apply());
        sliderVolume.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) prefs.edit().putFloat(KEY_VOLUME, value).apply();
        });

        btnDiscord.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse(getString(R.string.settings_discord_url)));
            startActivity(intent);
        });

        btnClearCache.setOnClickListener(v -> clearDownloadedFiles());
    }

    private void clearDownloadedFiles() {
        File dir = requireContext().getExternalFilesDir(null);
        if (dir != null && dir.exists()) {
            deleteRecursive(dir);
            Toast.makeText(requireContext(), "Cache berhasil dihapus", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursive(child);
            }
        }
        file.delete();
    }
}
