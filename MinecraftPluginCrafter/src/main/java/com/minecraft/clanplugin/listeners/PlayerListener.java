package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for player-related events.
 */
public class PlayerListener implements Listener {

    private final ClanPlugin plugin;

    /**
     * Creates a new player listener.
     * 
     * @param plugin The clan plugin instance
     */
    public PlayerListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan != null) {
            // Update player name in case it changed
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null) {
                member.setPlayerName(player.getName());
            }
        }
        
        // Update player's visual elements (nametag and armor colors)
        // Schedule this to run 1 tick later to ensure the player is fully loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            updatePlayerClanVisuals(player);
        }, 1L);
    }

    /**
     * Handles player quit events.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // No action needed when player quits
        // All clan data is maintained in memory
    }
    
    /**
     * Helper method to update a player's visual elements when their clan status changes.
     * This should be called whenever a player joins, leaves, or changes clans.
     * 
     * @param player The player to update
     */
    public void updatePlayerClanVisuals(Player player) {
        // Update the player's nametag
        plugin.getNametagManager().updatePlayerNametag(player);
        
        // Update armor colors
        plugin.getArmorListener().colorPlayerArmor(player);
    }
}
