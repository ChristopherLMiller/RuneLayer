package com.moosemanstudios.Runelayer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;

public class RuneLayer extends JavaPlugin{
	Logger log = Logger.getLogger("minecraft");
	public ArrayList<Player> runePlayers = new ArrayList<Player>();
	public ArrayList<String> offlinePlayers = new ArrayList<String>();
	private final RLPlayerListener playerlistener = new RLPlayerListener(this);
	static String mainDirectory = "plugins/RuneLayer";
	
	public void onEnable() {
		// register the event
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerlistener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerlistener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerlistener, Priority.Normal, this);
		
		// load list of players with rune layer enabled
		new File(mainDirectory).mkdir();
		load_players();
		
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info("[" + pdfFile.getName() +  "] version " + pdfFile.getVersion() + " is enabled");
	}
	
	public void onDisable() {
		// save list of players with rune layer enabled
		save_players();
		
		log.info("[RuneLayer] is disabled");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		Player player = (Player) sender;
		
		// see if they entered the command
		if (commandLabel.equalsIgnoreCase("rune")) {
			if (player.hasPermission("runelayer.rune")) {
				togglePlayer(player);
				return true;
			} else if (player.isOp()) {
				togglePlayer(player);
				return true;
			} else {
				player.sendMessage(ChatColor.RED + "You don't have permissions to do that");
				return false;
			}
		}
		
		return false;
	}
	
	private void load_players() {
		try {
			// see if hte file even exists
			if ((new File(mainDirectory+"/players.txt").exists())) {
				BufferedReader in = new BufferedReader(new FileReader(mainDirectory + "/players.txt"));
				
				runePlayers.clear();
				
				// loop through the file to get all the players
				String line = null;
				while ((line = in.readLine()) != null) {
					Player player = this.getServer().getPlayer(line);
					if (player == null) {
						this.offlinePlayers.add(line);
						log.info("[RuneLayer] offline player added: " + line);
					} else {
						this.runePlayers.add(player);
						log.info("[Runelayer] online player added: " + player.getName());
					}
				}
				
				in.close();
				log.info("[RuneLayer] Loaded successfully!");
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void save_players() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(mainDirectory + "/players.txt"));
			
			// save the online users first
			for (Player player: runePlayers) {
				out.write(player.getName());
				out.newLine();
				log.info("[RuneLayer] online player saved: " + player.getName());
			}
			
			// now save the offline users
			for (String offlinePlayer : offlinePlayers) {
				out.write(offlinePlayer);
				out.newLine();
				log.info("[RuneLayer] offline player saved: " + offlinePlayer);
			}
			
			out.close();
			log.info("[RuneLayer] Saved successfully!");
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	private void togglePlayer(Player player) {
		if (enabled(player)) {
			this.runePlayers.remove(player);
			player.sendMessage(ChatColor.BLUE + "Rune Layer is disabled");
			log.info("[Runelayer] disabled for player " + player.getName());
		} else {
			this.runePlayers.add(player);
			player.sendMessage(ChatColor.BLUE + "Rune Layer is enabled");
			log.info("[Runelayer] enabled for player " + player.getName());
		}
	}
	
	public boolean enabled(Player player) {
		return this.runePlayers.contains(player);
	}
}
