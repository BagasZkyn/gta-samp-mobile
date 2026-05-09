package com.samp.mobile.launcher.ui;

public class ServerItem {
    public String hostname;
    public String ip;
    public int port;
    public String gamemode;
    public String mapName;
    public int players;
    public int maxPlayers;
    public int ping;
    public boolean isPassworded;
    public boolean isFavorite;

    public ServerItem(String hostname, String ip, int port, String gamemode,
                      String mapName, int players, int maxPlayers, int ping, boolean isPassworded) {
        this.hostname = hostname;
        this.ip = ip;
        this.port = port;
        this.gamemode = gamemode;
        this.mapName = mapName;
        this.players = players;
        this.maxPlayers = maxPlayers;
        this.ping = ping;
        this.isPassworded = isPassworded;
        this.isFavorite = false;
    }

    public String getAddress() { return ip + ":" + port; }

    /** Returns ping color category: 0=good(<80ms), 1=medium(<150ms), 2=bad(>=150ms) */
    public int getPingCategory() {
        if (ping < 80) return 0;
        if (ping < 150) return 1;
        return 2;
    }
}
