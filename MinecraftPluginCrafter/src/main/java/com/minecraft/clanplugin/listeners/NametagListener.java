package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.utils.NametagManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for nametag-related events.
 */
public class NametagListener implements Listener {

    private final ClanPlugin plugin;
    private final NametagManager nametagManager;
    
    /**
     * Creates a new nametag listener.
     * 
     * @param plugin The clan plugin instance
     * @param nametagManager The nametag manager instance
     */
    public NametagListener(ClanPlugin plugin, NametagManager nametagManager) {
        this.plugin = plugin;
        this.nametagManager = nametagManager;
    }
    
    /**
     * Handles player join events to set up nametags.
     * Has a lower priority than PlayerListener to ensure data is loaded first.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Schedule nametag update after a short delay to ensure all data is loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            nametagManager.updatePlayerNametag(player);
        }, 5L); // 5 ticks (0.25 seconds) delay
    }
    
    /**
     * Handles player quit events to clean up nametag data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        nametagManager.removePlayerFromClanTeam(player);
    }
}