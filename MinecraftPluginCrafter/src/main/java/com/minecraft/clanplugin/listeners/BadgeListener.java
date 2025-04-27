package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Handles events related to clan member badges.
 */
public class BadgeListener implements Listener {

    private final ClanPlugin plugin;
    
    /**
     * Create a new badge listener.
     * 
     * @param plugin The plugin instance
     */
    public BadgeListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle clicks in badge-related GUIs.
     * 
     * @param event The inventory click event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Handle badge GUIs
        String goldPrefix = ChatColor.GOLD.toString();
        if (title.equals(goldPrefix + "Your Badges")) {
            event.setCancelled(true);
            plugin.getBadgeManager().handleBadgesGUIClick(player, event.getRawSlot(), true);
        } else if (title.startsWith(goldPrefix) && title.endsWith("'s Badges")) {
            event.setCancelled(true);
            // Allow viewing but not interaction
        } else if (title.equals(goldPrefix + "New Badge Earned!")) {
            event.setCancelled(true);
            // Close the animation on any click
            player.closeInventory();
        }
    }
    
    /**
     * Check for badge awards on player join.
     * 
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if player is in a clan
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            // Schedule badge checks after a short delay to ensure all data is loaded
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                plugin.getBadgeManager().checkAndAwardRoleBadges(player, clan);
                plugin.getBadgeManager().checkAndAwardActivityBadges(player, clan);
            }, 40); // 2 second delay
        }
    }
}