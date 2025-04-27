package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.mapping.TerritoryMap;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

/**
 * Handles events for the dynamic territory map.
 */
public class TerritoryVisualizationListener implements Listener {
    
    private final ClanPlugin plugin;
    
    public TerritoryVisualizationListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handles player interactions with the territory map.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.getType().equals(Material.FILLED_MAP)) {
            return;
        }
        
        if (!(item.getItemMeta() instanceof MapMeta)) {
            return;
        }
        
        MapMeta meta = (MapMeta) item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        // Check if this is our territory map
        if (!meta.getDisplayName().contains("Clan Territory Map")) {
            return;
        }
        
        // Right-click (refresh action)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true); // Prevent normal map interactions
            
            // Display territory info for current chunk
            plugin.getTerritoryMap().displayTerritoryInfo(player, player.getLocation().getChunk());
            
            // Refresh map view
            MapView view = meta.getMapView();
            if (view != null) {
                view.setCenterX(player.getLocation().getBlockX());
                view.setCenterZ(player.getLocation().getBlockZ());
            }
        }
    }
    
    /**
     * Handles player dropping the territory map.
     */
    @EventHandler
    public void onPlayerDropMap(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (item.getType() != Material.FILLED_MAP) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        // Check if this is our territory map
        if (meta.getDisplayName().contains("Clan Territory Map")) {
            // Deactivate the map
            Player player = event.getPlayer();
            plugin.getTerritoryMap().stopMapUpdates(player.getUniqueId());
            
            // Notify player (but allow them to drop it)
            player.sendMessage(ChatColor.YELLOW + "Territory map deactivated.");
        }
    }
    
    /**
     * Handles player death with a territory map.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Stop any active map updates for this player
        plugin.getTerritoryMap().stopMapUpdates(player.getUniqueId());
    }
    
    /**
     * Handles player quit event to clean up map resources.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Stop any active map updates for this player
        plugin.getTerritoryMap().stopMapUpdates(player.getUniqueId());
    }
    
    /**
     * Handles when a hopper or other container picks up a dropped map.
     */
    @EventHandler
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        
        if (item.getType() != Material.FILLED_MAP) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        // Check if this is our territory map
        if (meta.getDisplayName().contains("Clan Territory Map")) {
            // Don't allow automated systems to pick up active maps
            event.setCancelled(true);
            event.getItem().remove();
        }
    }
}