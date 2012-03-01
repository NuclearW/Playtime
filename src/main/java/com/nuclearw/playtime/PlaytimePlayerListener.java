package com.nuclearw.playtime;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlaytimePlayerListener extends PlayerListener {
	public static Playtime plugin;
	
	public PlaytimePlayerListener(Playtime instance) {
		plugin = instance;
	}

	public void onPlayerJoin(PlayerJoinEvent event) {
		plugin.onJoin(event.getPlayer());
	}
	public void onPlayerQuit(PlayerQuitEvent event) {
		plugin.onLeave(event.getPlayer().getName());
	}
	public void onPlayerKick(PlayerKickEvent event) {
		plugin.onLeave(event.getPlayer().getName());
	}
}
