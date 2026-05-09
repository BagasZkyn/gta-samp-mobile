package com.samp.mobile.launcher.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.samp.mobile.R;
import com.samp.mobile.launcher.ui.DashboardFragment;
import com.samp.mobile.launcher.ui.DownloadCenterFragment;
import com.samp.mobile.launcher.ui.ServerListFragment;
import com.samp.mobile.launcher.ui.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_DASHBOARD = "tab_dashboard";
    private static final String TAG_SERVERS   = "tab_servers";
    private static final String TAG_DOWNLOADS = "tab_downloads";
    private static final String TAG_SETTINGS  = "tab_settings";
    private static final String KEY_TAB = "selected_tab";

    private BottomNavigationView bottomNav;
    private int currentTabId = R.id.nav_dashboard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_nav);

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            // First launch — add all fragments, hide all except dashboard
            fm.beginTransaction()
                    .add(R.id.fragment_container, new DashboardFragment(),  TAG_DASHBOARD)
                    .add(R.id.fragment_container, new ServerListFragment(),  TAG_SERVERS)
                    .add(R.id.fragment_container, new DownloadCenterFragment(), TAG_DOWNLOADS)
                    .add(R.id.fragment_container, new SettingsFragment(),    TAG_SETTINGS)
                    .hide(fm.findFragmentByTag(TAG_SERVERS) == null
                            ? getSupportFragmentManager().findFragmentByTag(TAG_SERVERS)
                            : fm.findFragmentByTag(TAG_SERVERS))
                    .commitNow();

            // After commitNow, hide the non-dashboard ones
            fm.beginTransaction()
                    .hide(fm.findFragmentByTag(TAG_SERVERS))
                    .hide(fm.findFragmentByTag(TAG_DOWNLOADS))
                    .hide(fm.findFragmentByTag(TAG_SETTINGS))
                    .commitNow();

            currentTabId = R.id.nav_dashboard;
        } else {
            currentTabId = savedInstanceState.getInt(KEY_TAB, R.id.nav_dashboard);
            switchToTab(fm, currentTabId);
        }

        bottomNav.setSelectedItemId(currentTabId);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id != currentTabId) {
                currentTabId = id;
                switchToTab(getSupportFragmentManager(), id);
            }
            return true;
        });
    }

    private void switchToTab(FragmentManager fm, int tabId) {
        FragmentTransaction ft = fm.beginTransaction();

        // Hide all
        hideIfExists(fm, ft, TAG_DASHBOARD);
        hideIfExists(fm, ft, TAG_SERVERS);
        hideIfExists(fm, ft, TAG_DOWNLOADS);
        hideIfExists(fm, ft, TAG_SETTINGS);

        // Show target
        String target = tagForId(tabId);
        Fragment f = fm.findFragmentByTag(target);
        if (f != null) ft.show(f);

        ft.commitNow();
    }

    private void hideIfExists(FragmentManager fm, FragmentTransaction ft, String tag) {
        Fragment f = fm.findFragmentByTag(tag);
        if (f != null) ft.hide(f);
    }

    private String tagForId(int id) {
        if (id == R.id.nav_servers)   return TAG_SERVERS;
        if (id == R.id.nav_downloads) return TAG_DOWNLOADS;
        if (id == R.id.nav_settings)  return TAG_SETTINGS;
        return TAG_DASHBOARD;
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_TAB, currentTabId);
    }
}
