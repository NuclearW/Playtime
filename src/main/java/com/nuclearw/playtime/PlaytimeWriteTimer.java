package com.nuclearw.playtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;

public class PlaytimeWriteTimer implements Runnable {
	Playtime plugin;
	
	public PlaytimeWriteTimer(Playtime instance) {
		plugin = instance;
	}

	@Override
	public void run() {
		Iterator<String> i = plugin.joinTime.keySet().iterator();
		while (i.hasNext()) {
			String player = i.next();
			long add = System.currentTimeMillis() - plugin.joinTime.get(player).longValue();
			Long addTo = plugin.totalTime.get(player);
			if(addTo == null) addTo = Long.parseLong("0");
			plugin.totalTime.put(player, add + addTo.longValue());
		}
		
		try {
			new File(Playtime.totalFile).createNewFile();
			ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(Playtime.totalFile));
			obj.writeObject(plugin.totalTime);
			obj.close();
		} catch (FileNotFoundException e) { e.printStackTrace();
		} catch (IOException e) { e.printStackTrace(); }
	}

}
