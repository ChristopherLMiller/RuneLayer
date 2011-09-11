package com.moosemanstudios.Runelayer;

import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class RLPlayerListener extends PlayerListener{
	private RuneLayer plugin;
	
	RLPlayerListener(RuneLayer instance) {
		plugin = instance;
	}
	
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		
		// see if the player is in the offline list, if so query server for them and add to the rune users list
		Iterator<String> itor = plugin.offlinePlayers.iterator();
		while(itor.hasNext()) {
			String next = itor.next().toString();
			
			if (next.equals(player.getName())) {
				itor.remove();

				// make sure the player actually has permissions to use this, so no file editing can fool it
				if (player.hasPermission("runelayer.rune")) {
					plugin.runePlayers.add(player);
					plugin.log.info("[Runelayer] adding online user: " + player.getName());
				} else {
					player.sendMessage("I call hax!");
					plugin.log.info("[Runelayer] user not authorized to use rune: " + player.getName());
				}
			}
		}
	}
	
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		// check if the player has runelayer enabled
		if (plugin.runePlayers.contains(player)) {
			// remove the player from the rune users list and add to the offline users list
			plugin.runePlayers.remove(player);
			plugin.offlinePlayers.add(player.getName());
			plugin.log.info("[RuneLayer] removing online user: " + player.getName());
		}

	}
	
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		World world = player.getWorld();
		
		// see if they are in the hashmap of enabled users
		if (plugin.enabled(player)) {
			// see if they right clicked
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				// see if they are holding diamond block
				if (player.getItemInHand().getType() == Material.DIAMOND_BLOCK) {
					// see if the player has the necessary items in inventory for the rune
					PlayerInventory inventory = player.getInventory();
					
					// see if the player has enough materials
					if (inventory.contains(Material.DIAMOND_BLOCK, 4)) {
						if (inventory.contains(Material.REDSTONE_TORCH_ON, 4)) {
							if (inventory.contains(Material.REDSTONE_WIRE, 8)) {
								// player has the proper materials, time to prep the rune area
								
								//get the block the player clicked, this will be the center of the rune
								Block clickedBlock = event.getClickedBlock();
								Location blockLocation = clickedBlock.getLocation();
								int blockX = blockLocation.getBlockX();	
								int blockY = blockLocation.getBlockY(); // height in the world
								int blockZ = blockLocation.getBlockZ();

								create_dirt_platform(blockX, blockY, blockZ, world);
								
								// lay out the materials on the ground
								lay_rune(blockX, blockY, blockZ, world);
																
								// subtract out the materials from the users inventory
								inventory.removeItem(new ItemStack(Material.DIAMOND_BLOCK, 4), new ItemStack(Material.REDSTONE_TORCH_ON, 4), new ItemStack(Material.REDSTONE_WIRE, 8));
							} else if (inventory.contains(Material.REDSTONE, 8)) {
								// player has the proper materials, time to prep the rune area
								
								//get the block the player clicked, this will be the center of the rune
								Block clickedBlock = event.getClickedBlock();
								Location blockLocation = clickedBlock.getLocation();
								int blockX = blockLocation.getBlockX();	
								int blockY = blockLocation.getBlockY(); // height in the world
								int blockZ = blockLocation.getBlockZ();

								// create a dirt platform to build the rune on
								create_dirt_platform(blockX, blockY, blockZ, world);
								
								// lay out the materials on the ground
								lay_rune(blockX, blockY, blockZ, world);
																
								// subtract out the materials from the users inventory
								inventory.removeItem(new ItemStack(Material.DIAMOND_BLOCK, 4), new ItemStack(Material.REDSTONE_TORCH_ON, 4), new ItemStack(Material.REDSTONE, 8));
							} else {
								player.sendMessage(ChatColor.BLUE + "Not enough redstone wire/dust!");
							}
						} else {
							player.sendMessage(ChatColor.BLUE + "Not enough redstone torches!");
						}
					} else {
						player.sendMessage(ChatColor.BLUE + "Not enough diamond blocks!");
					}
				}
			}
		}
	}
	
	private void create_dirt_platform(int blockX, int blockY, int blockZ, World world) {
		for (int x = blockX-2; x <= blockX+2; x++) {
			for (int z = blockZ - 2; z <= blockZ + 2; z++) {
				if (!is_solid(blockX+x, blockY, blockZ+z, world)) {
					world.getBlockAt((x), (blockY), (z)).setType(Material.DIRT);
				}
				world.getBlockAt((x), (blockY+1), (z)).setType(Material.AIR);
				world.getBlockAt((x), (blockY+2), (z)).setType(Material.AIR);
			}
		}
	}
	
	private Boolean is_solid(int blockX, int blockY, int blockZ, World world) {
		 if ( (world.getBlockAt(blockX, blockY, blockZ).getType() == Material.AIR) || (world.getBlockAt(blockX, blockY, blockZ).getType() == Material.LAVA) || (world.getBlockAt(blockX, blockY, blockZ).getType() == Material.WATER) ) {
			 return false;
		 } else {
			 return true;
		 }
	}
	
	private void lay_rune(int blockX, int blockY, int blockZ, World world) {
		// lay the diamond blocks first
		world.getBlockAt(blockX - 1, blockY + 1, blockZ - 1).setType(Material.DIAMOND_BLOCK);
		world.getBlockAt(blockX - 1, blockY + 1, blockZ + 1).setType(Material.DIAMOND_BLOCK);
		world.getBlockAt(blockX + 1, blockY + 1, blockZ + 1).setType(Material.DIAMOND_BLOCK);
		world.getBlockAt(blockX + 1, blockY + 1, blockZ - 1).setType(Material.DIAMOND_BLOCK);
		
		// lay out the redstone torches next
		world.getBlockAt(blockX - 2, blockY + 1, blockZ).setType(Material.REDSTONE_TORCH_ON);
		world.getBlockAt(blockX + 2, blockY + 1, blockZ).setType(Material.REDSTONE_TORCH_ON);
		world.getBlockAt(blockX, blockY + 1, blockZ + 2).setType(Material.REDSTONE_TORCH_ON);
		world.getBlockAt(blockX, blockY + 1, blockZ - 2).setType(Material.REDSTONE_TORCH_ON);
		
		// lay out the redstone wire lastly
		world.getBlockAt(blockX - 2, blockY + 1, blockZ - 2).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX - 2, blockY + 1, blockZ + 2).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX + 2, blockY + 1, blockZ + 2).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX + 2, blockY + 1, blockZ - 2).setType(Material.REDSTONE_WIRE);
		
		world.getBlockAt(blockX - 1, blockY + 1, blockZ).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX + 1, blockY + 1, blockZ).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX, blockY + 1, blockZ + 1).setType(Material.REDSTONE_WIRE);
		world.getBlockAt(blockX, blockY + 1, blockZ - 1).setType(Material.REDSTONE_WIRE);
	}
}

