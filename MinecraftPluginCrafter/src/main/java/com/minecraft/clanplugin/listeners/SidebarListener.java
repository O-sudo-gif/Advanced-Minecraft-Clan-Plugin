package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.utils.SidebarManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.event.server.PluginEnableEvent;
import java.util.UUID;

/**
 * Listener for sidebar-related events.
 */
public class SidebarListener implements Listener {

    private final ClanPlugin plugin;
    private final SidebarManager sidebarManager;
    
    /**
     * Creates a new sidebar listener.
     * 
     * @param plugin The clan plugin instance
     * @param sidebarManager The sidebar manager instance
     */
    public SidebarListener(ClanPlugin plugin, SidebarManager sidebarManager) {
        this.plugin = plugin;
        this.sidebarManager = sidebarManager;
    }
    
    /**
     * Handles player join events to set up the sidebar.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        
        // Delay sidebar creation to ensure all data is loaded
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                sidebarManager.initializeSidebar(player);
            }
        }, 10L); // 10 ticks (0.5 seconds) delay
    }
    
    /**
     * Handles player quit events to clean up sidebar data.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        sidebarManager.removeSidebar(player);
    }
    
    /**
     * Updates sidebars for all members of a specific clan.
     *
     * @param clan The clan whose members need sidebar updates
     */
    public void updateClanSidebars(Clan clan) {
        if (clan == null) return;
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            if (clan.isMember(playerUuid)) {
                sidebarManager.updateSidebar(player);
            }
        }
    }
    
    /**
     * Updates the sidebar for all online players in response to economy changes.
     */
    public void updateAllEconomySidebars() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            sidebarManager.updateSidebar(player);
        }
    }
    
    /**
     * Initializes sidebars for all online players when the plugin is enabled.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().equals(plugin)) {
            // Wait 5 ticks to ensure server is fully started
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    sidebarManager.initializeSidebar(player);
                }
            }, 5L);
        }
    }
}