package com.moosemanstudios.Runelayer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Event;

public class RuneLayer extends JavaPlugin{
	Logger log = Logger.getLogger("minecraft");	// main plugin logger
	public ArrayList<Player> runePlayers = new ArrayList<Player>();	// list of players online able to use runelayer
	public ArrayList<String> offlinePlayers = new ArrayList<String>();	// list of players offline able to use runelayer
	private final RLPlayerListener playerlistener = new RLPlayerListener(this);	// player listener class
	static String mainDirectory = "plugins/RuneLayer";	// main directory for config purposes
	public Configuration config;	// configuration file for the plugin
	public ArrayList<String> worlds = new ArrayList<String>();	// list of worlds for the config
	
	public void onEnable() {
		// register the event
		PluginManager pm = this.getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerlistener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerlistener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerlistener, Priority.Normal, this);
		
		// load list of players with rune layer enabled
		new File(mainDirectory).mkdir();
		load_players();
		
		// get the config
		config = this.getConfig();
		
		// load the config
		loadConfig();
		
		// reload the config to get the file if it didn't exist
		reloadConfig();
		
		PluginDescriptionFile pdfFile = this.getDescription();
		log.info("[" + pdfFile.getName() +  "] version " + pdfFile.getVersion() + " is enabled");
	}
	
	public void onDisable() {
		// save list of players with rune layer enabled
		save_players();
		
		log.info("[RuneLayer] is disabled");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		String[] split = args;
		String commandName = cmd.getName().toLowerCase();
		
		if (commandName.equalsIgnoreCase("rune")) {
			if (split.length == 0) {
				sender.sendMessage(ChatColor.RED + "Type " + ChatColor.WHITE + "/rune help" + ChatColor.RED + " for help");
				return true;
			}
			
			if (split[0].equalsIgnoreCase("help")) {
				sender.sendMessage(ChatColor.RED + "RuneLayer Help");
				sender.sendMessage("----------------------------------------");
				sender.sendMessage(ChatColor.RED + "/rune help" + ChatColor.WHITE + ": Displays this help screen");
				
				// display help menu based on user permissions
				if (sender.hasPermission("runelayer.rune")) {
					sender.sendMessage(ChatColor.RED + "/rune (enable/disable)" + ChatColor.WHITE + ": Enable/disable rune laying");
				}
				if (sender.hasPermission("runelayer.reload")) {
					sender.sendMessage(ChatColor.RED + "/rune reload" + ChatColor.WHITE + ": Reload RuneLayer config file");
				}
				if (sender.hasPermission("runelayer.change")) {
					sender.sendMessage(ChatColor.RED + "/rune (add/remove) [WORLD]" + ChatColor.WHITE + ": Enable/disable specified world");
				}
				
				return true;
			}
			
			if (split[0].equalsIgnoreCase("disable") || split[0].equalsIgnoreCase("enable")) {
				// this will vary if its the user or the console
				if (sender instanceof Player) {
					if (sender.hasPermission("runelayer.rune")) {
						if (split[0].equalsIgnoreCase("enable")) {
							if (enabled((Player) sender)) {
								sender.sendMessage(ChatColor.BLUE + "RuneLayer is enabled");
							} else {
								togglePlayer((Player) sender);
							}
						} else {
							if (enabled((Player) sender)) {
								togglePlayer((Player) sender);
							} else {
								sender.sendMessage(ChatColor.BLUE + "RuneLayer is disabled");
							}
						}
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permissions to do that!");
					}
				} else {
					sender.sendMessage("You are not a player! I can't allow this.");
				}
				return true;
			}
			
			if (split[0].equalsIgnoreCase("reload")) {
				if (sender.hasPermission("runelayer.reload")) {
					reloadConfig();
					sender.sendMessage("RuneLayer config reloaded");
				} else {
					sender.sendMessage(ChatColor.RED + "You don't have permissions to do that!");
				}
				return true;
			}
			
			if ((split[0].equalsIgnoreCase("add")) || (split[0].equalsIgnoreCase("remove")) ) {
				// see if they provided a world name
				if (split.length == 2) {
					if (sender.hasPermission("runelayer.change")) {
						if (split[0].equalsIgnoreCase("add")) {
							// see if the world is in the list already
							if (worlds.contains(split[1])){
								sender.sendMessage("World already enabled");
							} else {
								worlds.add(split[1]);
								config.set("worlds", worlds);
								saveConfig();
								sender.sendMessage("World added");
							}
						} else {
							if (worlds.contains(split[1])) {
								worlds.remove(split[1]);
								config.set("worlds", worlds);
								saveConfig();
								sender.sendMessage("World removed");
							} else {
								sender.sendMessage("World already disabled");
							}
						}
					} else {
						sender.sendMessage(ChatColor.RED + "You don't have permissions to do that!");
					}
				} else {
					sender.sendMessage(ChatColor.RED + "World name not provided, use /rune help for more info");
				}
				return true;
			}
		}
		
		return false;
	}
	
	private void reloadConfig() {
		@SuppressWarnings("unchecked")
		List<String> worldList = config.getList("worlds");
		// wipe the worlds list first
		worlds.clear();
		
		// iterate and add the worlds to the list
		for (String world : worldList) {
			// check that the world actually exists on the server
			World worldWorld = this.getServer().getWorld(world);
			if (worldWorld != null) {
				worlds.add(world);
				log.info("[RuneLayer] world loaded: " + world);
			}
		}
	}
	
	private void loadConfig() {
		// see if the value exists
		if (!config.contains("worlds")) {
			List<World> tempWorlds = Bukkit.getServer().getWorlds();
			for (World world : tempWorlds) {
				if(world != null) {
					worlds.add(world.getName());
				}
			}
			config.set("worlds", worlds);
		}
		saveConfig();
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
			player.sendMessage(ChatColor.BLUE + "RuneLayer is disabled");
			log.info("[Runelayer] disabled for player " + player.getName());
		} else {
			this.runePlayers.add(player);
			player.sendMessage(ChatColor.BLUE + "RuneLayer is enabled");
			log.info("[Runelayer] enabled for player " + player.getName());
		}
	}
	
	public boolean enabled(Player player) {
		return this.runePlayers.contains(player);
	}
}
