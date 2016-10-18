package com.civclassic.oldenchanting;

import java.util.Random;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftInventoryView;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import net.minecraft.server.v1_10_R1.ContainerEnchantTable;

public class OldEnchanting extends JavaPlugin implements Listener {

	private static Random rand = new Random();
	
	private boolean hideEnchants;
	private boolean fillLapis;
	private boolean randomize;
	private double xpMod;
	private double lootMod;
	private boolean emeraldCrafting;
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		hideEnchants = getConfig().getBoolean("hide_enchants");
		fillLapis = getConfig().getBoolean("fill_lapis");
		randomize = getConfig().getBoolean("randomize_enchants");
		xpMod = getConfig().getDouble("xpmod");
		lootMod = getConfig().getDouble("loot_mult");
		emeraldCrafting = getConfig().getBoolean("emerald_crafting");
		if(emeraldCrafting) {
			registerRecipes();
		}
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private void registerRecipes() {
		ItemStack emerald = new ItemStack(Material.EMERALD, 1);
		ShapedRecipe expToEmerald = new ShapedRecipe(emerald);
		expToEmerald.shape("xxx", "xxx", "xxx"); 
		expToEmerald.setIngredient('x', Material.EXP_BOTTLE);
		getServer().addRecipe(expToEmerald);
		ItemStack bottles = new ItemStack(Material.EXP_BOTTLE, 9);
		ShapelessRecipe emeraldsToExp = new ShapelessRecipe(bottles);
		emeraldsToExp.addIngredient(Material.EMERALD);
		getServer().addRecipe(emeraldsToExp);
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if(entity.getType() == EntityType.PLAYER) return;
		int xp = event.getDroppedExp();
		if(xp == 0) return;
		xp *= xpMod;
		if(entity.getKiller() != null) {
			Player killer = entity.getKiller();
			if(killer.getInventory().getItemInMainHand() != null) {
				if(killer.getInventory().getItemInMainHand().hasItemMeta() && killer.getInventory().getItemInMainHand().getItemMeta().hasEnchant(Enchantment.LOOT_BONUS_MOBS)) {
					double mod = lootMod * killer.getInventory().getItemInMainHand().getItemMeta().getEnchantLevel(Enchantment.LOOT_BONUS_MOBS);
					xp *= mod;
				}
			}
		}
		event.setDroppedExp(xp);
	}
	
	@EventHandler
	public void onBlockExp(BlockExpEvent event) {
		event.setExpToDrop((int) (event.getExpToDrop() * xpMod));
	}
	
	@EventHandler
	public void onEnchantItem(EnchantItemEvent event) {
		Player player = event.getEnchanter();
		player.setLevel(player.getLevel() - event.getExpLevelCost() + event.whichButton() + 1);
		if(fillLapis) {
			event.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
		}
	}
	
	@EventHandler
	public void onPrepareItemEnchant(PrepareItemEnchantEvent event) {
		CraftInventoryView view = (CraftInventoryView) event.getView();
		ContainerEnchantTable table = (ContainerEnchantTable) view.getHandle();
		if(randomize) {
			table.f = rand.nextInt();
		}
		if(hideEnchants) {
			getServer().getScheduler().scheduleSyncDelayedTask(this, ()-> {
				table.h[0] = (-1 | 0 << 8);
				table.h[1] = (-1 | 0 << 8);
				table.h[2] = (-1 | 0 << 8);
			}, 1);
		}
	}
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if(fillLapis && event.getClickedInventory().getType() == InventoryType.ENCHANTING && event.getRawSlot() == 1) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onInventoryOpen(InventoryOpenEvent event) {
		if(fillLapis && event.getInventory().getType() == InventoryType.ENCHANTING) {
			event.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
		}
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if(fillLapis && event.getInventory().getType() == InventoryType.ENCHANTING) {
			event.getInventory().setItem(1, null);
		}
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if(event.getBlock().getType() == Material.ENCHANTMENT_TABLE) {
			for(ItemStack item : event.getBlock().getDrops()) {
				if(item.getType() == Material.INK_SACK) {
					event.getBlock().getDrops().remove(item);
				}
			}
		}
	}
}