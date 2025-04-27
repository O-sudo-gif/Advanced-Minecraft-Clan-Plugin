package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.bounty.BountyManager;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

/**
 * Listener for bounty-related events.
 */
public class BountyListener implements Listener {
    
    private final ClanPlugin plugin;
    private final BountyManager bountyManager;
    
    /**
     * Creates a new bounty listener.
     * 
     * @param plugin The clan plugin instance
     * @param bountyManager The bounty manager
     */
    public BountyListener(ClanPlugin plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }
    
    /**
     * Handles player death events for bounty claims.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // If there's no killer (e.g., died from fall damage), or the killer is the same as the victim, do nothing
        if (killer == null || killer.equals(victim)) {
            return;
        }
        
        UUID victimUUID = victim.getUniqueId();
        UUID killerUUID = killer.getUniqueId();
        
        // Check if the victim has a bounty
        double bountyAmount = bountyManager.getTotalBountyValue(victimUUID);
        if (bountyAmount <= 0) {
            return; // No bounty to claim
        }
        
        // Check if clan sharing is enabled
        if (bountyManager.isClanSharingEnabled()) {
            // Check if the killer and victim are in the same clan
            Clan killerClan = plugin.getStorageManager().getPlayerClan(killerUUID);
            Clan victimClan = plugin.getStorageManager().getPlayerClan(victimUUID);
            
            if (killerClan != null && victimClan != null && killerClan.equals(victimClan)) {
                // Players are in the same clan, bounty can't be claimed
                killer.sendMessage(ChatColor.RED + "You cannot claim bounties on members of your own clan!");
                return;
            }
        }
        
        // Claim the bounty
        double claimedAmount = bountyManager.claimBounty(killerUUID, victimUUID);
        
        // This is just a fallback - the actual success message is handled in the bounty manager
        if (claimedAmount > 0) {
            // Add death message about the bounty claim
            String deathMessage = event.getDeathMessage();
            if (deathMessage != null) {
                event.setDeathMessage(deathMessage + ChatColor.GOLD + " (Bounty: $" + 
                                     String.format("%.2f", claimedAmount) + ")");
            }
        }
    }
}