package com.nuclearw.playtime;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Playtime extends JavaPlugin {	
	static String mainDirectory = "plugins" + File.separator + "Playtime";
	static String tempFile = mainDirectory + File.separator + "temp";
	static String totalFile = mainDirectory + File.separator + "totals";
	static File versionFile = new File(mainDirectory + File.separator + "VERSION");
	static File languageFile = new File(mainDirectory + File.separator + "lang");

	private final PlaytimePlayerListener playerListener = new PlaytimePlayerListener(this);

	Logger log = Logger.getLogger("Minecraft");

	Properties prop = new Properties();

	String[] language = new String[11];

	public HashMap<String, Long> joinTime = new HashMap<String, Long>();
	public HashMap<String, Long> totalTime = new HashMap<String, Long>();

	@SuppressWarnings("unchecked")
	public void onEnable() {
		new File(mainDirectory).mkdir();

		if(!versionFile.exists()) {
			updateVersion();
		} else {
			String vnum = readVersion();
			if(vnum.equalsIgnoreCase("0.1")) updateVersion();
			if(vnum.equalsIgnoreCase("0.2")) updateVersion();
			if(vnum.equalsIgnoreCase("0.2.1")) updateVersion();
			if(vnum.equalsIgnoreCase("0.3")) updateVersion();
			if(vnum.equalsIgnoreCase("0.4")) updateVersion();
			if(vnum.equalsIgnoreCase("0.5")) updateVersion();
		}

		if(!languageFile.exists()) tryMakeLangFile();

		tryLoadLangFile();

		if(!prop.containsKey("online") || !prop.containsKey("online-other") || !prop.containsKey("not-online")
				 || !prop.containsKey("day") || !prop.containsKey("days") || !prop.containsKey("hour") || !prop.containsKey("hours")
				 || !prop.containsKey("minute") || !prop.containsKey("minutes") || !prop.containsKey("second") || !prop.containsKey("seconds")) {
			this.log.severe("[Playtime] Lang file not complete! Restoring to default!");
			tryMakeLangFile();
			tryLoadLangFile();
		}

		this.language[0] = prop.getProperty("online");
		this.language[1] = prop.getProperty("online-other");
		this.language[2] = prop.getProperty("not-online");
		this.language[3] = prop.getProperty("day");
		this.language[4] = prop.getProperty("days");
		this.language[5] = prop.getProperty("hour");
		this.language[6] = prop.getProperty("hours");
		this.language[7] = prop.getProperty("minute");
		this.language[8] = prop.getProperty("minutes");
		this.language[9] = prop.getProperty("second");
		this.language[10] = prop.getProperty("seconds");

		PluginManager pluginManager = getServer().getPluginManager();

		pluginManager.registerEvents(playerListener, this);

		log.addHandler(new Handler() {
        	public void publish(LogRecord logRecord) {
        		String mystring = logRecord.getMessage();
        		if(mystring.contains(" lost connection: ")) {
        			String myarray[] = mystring.split(" ");
        			String playerName = myarray[0];
        			String DisconnectMessage = myarray[3];
        			if(DisconnectMessage.equals("disconnect.quitting")) return;
        			onLeave(playerName);
        		}
        	}
        	public void flush() {}
        	public void close() {
        	}
        });

		if(getServer().getOnlinePlayers().length != 0) {
			if(new File(tempFile).exists()) {
				try {
					ObjectInputStream obj = new ObjectInputStream(new FileInputStream(tempFile));
					joinTime = (HashMap<String, Long>)obj.readObject();
				} catch (FileNotFoundException e) { e.printStackTrace();
				} catch (IOException e) { e.printStackTrace();
				} catch (ClassNotFoundException e) { e.printStackTrace(); }
				new File(tempFile).delete();
			}
		} else {
			new File(tempFile).delete();
		}

		if(new File(totalFile).exists()) {
			try {
				ObjectInputStream obj = new ObjectInputStream(new FileInputStream(totalFile));
				totalTime = (HashMap<String, Long>)obj.readObject();
			} catch (FileNotFoundException e) { e.printStackTrace();
			} catch (IOException e) { e.printStackTrace();
			} catch (ClassNotFoundException e) { e.printStackTrace(); }
		}

		this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new PlaytimeWriteTimer(this), 6000L, 6000L);

		this.log.info("[Playtime] Playtime version "+this.getDescription().getVersion()+" loaded.");
	}

	public void onDisable() {
		try {
			new File(tempFile).createNewFile();
			ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(tempFile));
			obj.writeObject(joinTime);
			obj.close();
		} catch (FileNotFoundException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }

		Iterator<String> i = joinTime.keySet().iterator();
		while (i.hasNext()) {
			String player = i.next();
			long add = System.currentTimeMillis() - joinTime.get(player).longValue();
			Long addTo = totalTime.get(player);
			if(addTo == null) addTo = Long.parseLong("0");
			totalTime.put(player, add + addTo.longValue());
		}
		joinTime.clear();

		try {
			new File(totalFile).createNewFile();
			ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(totalFile));
			obj.writeObject(totalTime);
			obj.close();
		} catch (FileNotFoundException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
		totalTime.clear();

		this.log.info("[Playtime] Playtime version "+this.getDescription().getVersion()+" unloaded.");
	}

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if(cmd.getName().equalsIgnoreCase("playtime")) {
			if(args.length == 0) {
				if(!isPlayer(sender)) return false;
				if(!((Player)sender).hasPermission("playtime.self")) return true;
				String elapsedTimeString = getElapsedTimeString(System.currentTimeMillis(), ((Player) sender).getName());
				sender.sendMessage(this.language[0] + elapsedTimeString);
			} else if(args.length == 1) {
				if(isPlayer(sender) && !((Player)sender).hasPermission("playtime.other")) return true;
				if(getServer().getPlayer(args[0]) == null) {
					String parsedNotOnline = this.language[2].replace("<player>", args[0]);
					sender.sendMessage(parsedNotOnline);
					return true;
				}
				String elapsedTimeString = getElapsedTimeString(System.currentTimeMillis(), getServer().getPlayer(args[0]).getName());
				String parsedOnlineOther = this.language[1].replace("<player>", getServer().getPlayer(args[0]).getName());
				sender.sendMessage(parsedOnlineOther + elapsedTimeString);
			} else return false;
		}
		if(cmd.getName().equalsIgnoreCase("totalplaytime")) {
			if(args.length == 0) {
				if(!isPlayer(sender)) return false;
				if(!((Player)sender).hasPermission("playtime.total.self")) return true;
				String elapsedTimeString = null;
				if(totalTime.get(((Player) sender).getName()) == null) {
					elapsedTimeString = getElapsedTimeString(System.currentTimeMillis(), ((Player) sender).getName());
				} else {
					long add = getElapsedTimeLong(System.currentTimeMillis(), ((Player) sender).getName());
					Long addTo = totalTime.get(((Player) sender).getName());
					if(addTo == null) addTo = Long.parseLong("0");
					elapsedTimeString = getElapsedTimeString(add + addTo.longValue());
				}
				sender.sendMessage(this.language[0] + elapsedTimeString);
			} else if(args.length == 1) {
				if(isPlayer(sender) && !((Player)sender).hasPermission("playtime.total.other")) return true;
				if(getServer().getPlayer(args[0]) == null && !totalTime.containsKey(args[0])) {
					String parsedNotOnline = this.language[2].replace("<player>", args[0]);
					sender.sendMessage(parsedNotOnline);
					return true;
				}
				String elapsedTimeString = null;
				String parsedOnlineOther = null;
				if(getServer().getPlayer(args[0]) == null && totalTime.containsKey(args[0])) {
					elapsedTimeString = getElapsedTimeString(totalTime.get(args[0]));
					parsedOnlineOther = this.language[1].replace("<player>", args[0]);
					sender.sendMessage(parsedOnlineOther + elapsedTimeString);
				} else {
					long add = getElapsedTimeLong(System.currentTimeMillis(), getServer().getPlayer(args[0]).getName());
					Long addTo = totalTime.get(getServer().getPlayer(args[0]).getName());
					if(addTo == null) addTo = Long.parseLong("0");
					elapsedTimeString = getElapsedTimeString(add + addTo.longValue());
					parsedOnlineOther = this.language[1].replace("<player>", getServer().getPlayer(args[0]).getName());
					sender.sendMessage(parsedOnlineOther + elapsedTimeString);
				}
			} else return false;
		}
		return true;
	}

	public void onJoin(Player player) {
		joinTime.put(player.getName(), System.currentTimeMillis());
	}

	public void onLeave(String player) {
		if(!joinTime.containsKey(player)) return;
		long add = System.currentTimeMillis() - joinTime.get(player).longValue();
		Long addTo = totalTime.get(player);
		if(addTo == null) addTo = Long.parseLong("0");
		totalTime.put(player, add + addTo.longValue());
		joinTime.remove(player);
	}

	public String getElapsedTimeString(long diff) {
		long secondInMillis = 1000;
		long minuteInMillis = secondInMillis * 60;
		long hourInMillis = minuteInMillis * 60;
		long dayInMillis = hourInMillis * 24;
		long yearInMillis = dayInMillis * 365;

		diff = diff % yearInMillis;
		long elapsedDays = diff / dayInMillis;
		diff = diff % dayInMillis;
		long elapsedHours = diff / hourInMillis;
		diff = diff % hourInMillis;
		long elapsedMinutes = diff / minuteInMillis;
		diff = diff % minuteInMillis;
		long elapsedSeconds = diff / secondInMillis;

		String compose = " ";

		if(elapsedDays == 1) {
			compose = compose + "1 " +  this.language[3] + ", ";
		} else if(elapsedDays > 1) {
			compose = compose + Long.toString(elapsedDays) + " " + language[4]+ ", ";
		}

		if(elapsedHours == 1) {
			compose = compose + "1 " +  this.language[5]+ ", ";
		} else if(elapsedHours > 1) {
			compose = compose + Long.toString(elapsedHours) + " " + language[6]+ ", ";
		}

		if(elapsedMinutes == 1) {
			compose = compose + "1 " +  this.language[7]+ ", ";
		} else if(elapsedMinutes > 1) {
			compose = compose + Long.toString(elapsedMinutes) + " " + language[8]+ ", ";
		}

		if(elapsedSeconds == 1) {
			compose = compose + "1 " +  this.language[9]+ ".";
		} else if(elapsedSeconds > 1) {
			compose = compose + Long.toString(elapsedSeconds) + " " + language[10]+ ".";
		}

		return compose;
	}

	public String getElapsedTimeString(long currentTime, String playerName) {
		//log.info("Current time: " + Long.toString(currentTime));
		//log.info("Join time: " + Long.toString(joinTime.get(playerName).longValue()));
		long diff = currentTime - joinTime.get(playerName).longValue();
		return getElapsedTimeString(diff);
	}

	public long getElapsedTimeLong(long currentTime, String playerName) {
		long diff = currentTime - joinTime.get(playerName).longValue();
		return diff;
	}

	public void tryLoadLangFile() {
		FileInputStream langin;
		try {
			langin = new FileInputStream(languageFile);
			this.prop.load(langin);
			langin.close();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void tryMakeLangFile() {
		try {
			languageFile.createNewFile();
			FileOutputStream out = new FileOutputStream(languageFile);
			this.prop.put("online", "You have been online for");
			this.prop.put("online-other", "<player> has been online for");
			this.prop.put("not-online", "<player> is not online.");
			this.prop.put("day", "day");
			this.prop.put("days", "days");
			this.prop.put("hour", "hour");
			this.prop.put("hours", "hours");
			this.prop.put("minute", "minute");
			this.prop.put("minutes", "minutes");
			this.prop.put("second", "second");
			this.prop.put("seconds", "seconds");
			this.prop.store(out, "Loaclization.");
			out.flush();
			out.close();
			this.prop.clear();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public void updateVersion() {
		try {
			versionFile.createNewFile();
			BufferedWriter vout = new BufferedWriter(new FileWriter(versionFile));
			vout.write(this.getDescription().getVersion());
			vout.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (SecurityException ex) {
			ex.printStackTrace();
		}
	}

	public String readVersion() {
		byte[] buffer = new byte[(int) versionFile.length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(versionFile));
			f.read(buffer);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (f != null) try { f.close(); } catch (IOException ignored) { }
		}

		return new String(buffer);
	}

    public boolean isPlayer(CommandSender sender) {
        return sender != null && sender instanceof Player;
    }
}
