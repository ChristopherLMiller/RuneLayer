package com.moosemanstudios.Runelayer;

import java.util.ArrayList;
import java.util.HashMap;
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

@SuppressWarnings("unused")
public class RLPlayerListener extends PlayerListener{
	private RuneLayer plugin;
	private final HashMap<Material, String> runeMaterials = new HashMap<Material, String>();
	private Material playerInHand;
	
	RLPlayerListener(RuneLayer instance) {
		plugin = instance;
		
		// tier 6 materials
		runeMaterials.put(Material.DIAMOND_BLOCK, "diamond block");
		
		// tier 5 materials
		runeMaterials.put(Material.GOLD_BLOCK, "gold block");
		runeMaterials.put(Material.JUKEBOX, "jukebox");
		runeMaterials.put(Material.OBSIDIAN, "obsidian");
		runeMaterials.put(Material.DIAMOND_ORE, "diamond ore");
		runeMaterials.put(Material.PORTAL, "portal");
		
		// tier 4 materials
		runeMaterials.put(Material.BEDROCK, "bedrock");
		runeMaterials.put(Material.BOOKSHELF, "bookshelf");
		runeMaterials.put(Material.GOLD_ORE, "gold ore");
		runeMaterials.put(Material.IRON_BLOCK, "iron block");
		runeMaterials.put(Material.LAPIS_BLOCK, "lapis lazuli block");
		runeMaterials.put(Material.MOSSY_COBBLESTONE, "mossy cobble");
		runeMaterials.put(Material.TNT, "TNT");
		
		// tier 3 materials
		runeMaterials.put(Material.BRICK, "brick block");
		runeMaterials.put(Material.CACTUS, "cactus");
		runeMaterials.put(Material.CLAY, "clay");
		runeMaterials.put(Material.IRON_ORE, "iron ore");
		runeMaterials.put(Material.COAL_ORE, "coal ore");
		runeMaterials.put(Material.REDSTONE_ORE, "redstone ore");
		runeMaterials.put(Material.TORCH, "torch");
		runeMaterials.put(Material.LAPIS_ORE, "lapis lazuli ore");
		runeMaterials.put(Material.CAKE, "cake");
		runeMaterials.put(Material.JACK_O_LANTERN, "jack-o-lantern");
		runeMaterials.put(Material.RAILS, "rail");	
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
		Player player = event.getPlayer();	// the player who triggered the event
		World world = player.getWorld();	// the world the player triggered the event on
		playerInHand = player.getItemInHand().getType();
		
		// see if the world the player is on is enabled
		if (!worldEnabled(player)) {
			 player.sendMessage(ChatColor.RED + "RuneLayer not enabled for this world");
			 return;
		 }
		
		//see if the player is the hashmap of enabled users
		if (plugin.enabled(player)) {
			
			// see if they left clicked
			if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
				
				// compare what the user is holding in there hand to the list of possible materials
				 if (valid_block_in_hand(playerInHand)) {
					 
					 // get the block they clicked on
					 Location blockLocation = event.getClickedBlock().getLocation();
					 
					 // get players inventory, and count the materials needed
					 PlayerInventory inventory = player.getInventory();
					 int numRedstoneTorchOn = check_inventory(Material.REDSTONE_TORCH_ON, inventory);
					 int numRedstoneTorchOff = check_inventory(Material.REDSTONE_TORCH_OFF, inventory);
					 int numRedstoneWire = check_inventory(Material.REDSTONE_WIRE, inventory);
					 int numRedstoneDust = check_inventory(Material.REDSTONE, inventory);
					 int numRuneBlocks = check_inventory(playerInHand, inventory);
					 String message = "Not enough of material: ";
					 Boolean enoughMaterials = true;
					 
					 // verify they have enough materials
					 if ((numRedstoneTorchOn + numRedstoneTorchOff) < 4) {
						 message = message.concat("redstone torches, ");
						 enoughMaterials = false;
					 }
					 if ((numRedstoneWire + numRedstoneDust) < 8) {
						 message = message.concat("redstone dust, ");
						 enoughMaterials = false;
					 }
					 if (numRuneBlocks < 4) {
						 message = message.concat(get_human_readable(playerInHand));
						 enoughMaterials = false;
					 }
					 
					 // return if they don't have enough materials
					 if (!enoughMaterials) {
						player.sendMessage(ChatColor.BLUE + message);
						return;
					 }
					 
					 // at this point, world is enabled to lay rune, player has permission and materials, lay it!
					 create_dirt_platform(blockLocation, world);
					 lay_rune(blockLocation, world);
					 
					 // subtract out player inventory
					 subtract_inventory(player);
				 }
			}
		}
	}
	
	private String get_human_readable(Material inHand) {
		return runeMaterials.get(inHand);
	}
	private Boolean valid_block_in_hand(Material inHand) {
		// see if the player has the item needed in there inventory
		if (runeMaterials.containsKey(inHand)) {
			return true;
		} else {
			return false;
		}
	}
	
	private void subtract_inventory(Player player) {
		PlayerInventory inventory = player.getInventory();
		 int numRedstoneTorchOn = check_inventory(Material.REDSTONE_TORCH_ON, inventory);
		 int numRedstoneTorchOff = check_inventory(Material.REDSTONE_TORCH_OFF, inventory);
		 int numRedstoneWire = check_inventory(Material.REDSTONE_WIRE, inventory);
		 int numRedstoneDust = check_inventory(Material.REDSTONE, inventory);
		 int numRuneBlocks = check_inventory(playerInHand, inventory);
		 
		 // remove 4 torches, either off or on
		 int remainingTorches = 4;
		 if (numRedstoneTorchOn >= remainingTorches) {
			 // they have enough torches, go ahead and take some
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE_TORCH_ON, remainingTorches));
		 } else {
			 // not enough torches, subtract out how many they have, then get the rest in the other kind of torches
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE_TORCH_ON, numRedstoneTorchOn));
			 remainingTorches = remainingTorches - numRedstoneTorchOn;
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE_TORCH_OFF, remainingTorches));
		 }
		 
		 // remove 8 redstone wire/dust
		 int remainingRedstone = 8;
		 if (numRedstoneDust >= remainingRedstone) {
			 // they have enough dust, go ahead and take whats needed
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE, remainingRedstone));
		 } else {
			 // not enough dust, subtract out what they have, then take the rest on wire
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE, numRedstoneDust));
			 remainingRedstone = remainingRedstone - numRedstoneDust;
			 player.getInventory().removeItem(new ItemStack(Material.REDSTONE_WIRE, remainingRedstone));
		 }
		 
		 // remove 4 of the rune block
		 ItemStack inHand = player.getItemInHand();
		 if ((inHand.getAmount() - 4) > 0) {
			 inHand.setAmount(inHand.getAmount() - 4);
			 player.setItemInHand(inHand);
		 } else {
			 player.getItemInHand().setType(Material.AIR);
			 player.setItemInHand(null);
		 }
	}
	
	private int check_inventory(Material material, PlayerInventory inventory) {
		int amt = 0;
        for (ItemStack item: inventory.getContents()) {
            if (item != null && item.getType() == material) {
                amt += item.getAmount();
            }
        }
        return amt;	
	}
	
	private Boolean worldEnabled(Player player) {
		String world = player.getWorld().getName();
		
		for (String worldList : plugin.worlds){
			if (worldList.equalsIgnoreCase(world)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void create_dirt_platform(Location location, World world) {
		int blockX = location.getBlockX();
		int blockY = location.getBlockY();
		int blockZ = location.getBlockZ();
		
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
	
	private void lay_rune(Location location, World world) {
		int blockX = location.getBlockX();
		int blockY = location.getBlockY();
		int blockZ = location.getBlockZ();
		// lay the diamond blocks first
		world.getBlockAt(blockX - 1, blockY + 1, blockZ - 1).setType(playerInHand);
		world.getBlockAt(blockX - 1, blockY + 1, blockZ + 1).setType(playerInHand);
		world.getBlockAt(blockX + 1, blockY + 1, blockZ + 1).setType(playerInHand);
		world.getBlockAt(blockX + 1, blockY + 1, blockZ - 1).setType(playerInHand);
		
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

