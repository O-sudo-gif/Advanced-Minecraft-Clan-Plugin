package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.economy.PlaytimeRewardManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Listener for playtime reward events.
 */
public class PlaytimeRewardListener implements Listener {
    
    private final ClanPlugin plugin;
    private final PlaytimeRewardManager rewardManager;
    
    /**
     * Creates a new playtime reward listener.
     * 
     * @param plugin The clan plugin instance
     * @param rewardManager The playtime reward manager
     */
    public PlaytimeRewardListener(ClanPlugin plugin, PlaytimeRewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }
    
    /**
     * Tracks player session start on join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        rewardManager.trackPlayerSession(player);
    }
    
    /**
     * Ends tracking for a player session on quit.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        rewardManager.endPlayerSession(player);
    }
}