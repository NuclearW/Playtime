package com.nuclearw.playtime;

import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;

public class PlaytimePluginListener extends ServerListener {
	public static Playtime plugin;
	
	public PlaytimePluginListener(Playtime instance) {
		plugin = instance;
	}
	
    public void onPluginEnable(PluginEnableEvent event) {
        PlaytimePermissionsHandler.onEnable(event.getPlugin());
    }
	
}
