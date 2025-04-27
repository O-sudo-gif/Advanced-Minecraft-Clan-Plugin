package com.minecraft.clanplugin.listeners;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.recruitment.RecruitmentMiniGame;
import com.minecraft.clanplugin.recruitment.RecruitmentMiniGame.RecruitmentChallenge;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.List;

/**
 * Handles events related to clan recruitment mini-games.
 */
public class RecruitmentListener implements Listener {

    private final ClanPlugin plugin;
    
    /**
     * Create a new recruitment listener.
     * 
     * @param plugin The plugin instance
     */
    public RecruitmentListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Handle clicks in recruitment-related GUIs.
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
        UUID playerId = player.getUniqueId();
        
        // Handle join clan GUI
        if (title.startsWith(ChatColor.GOLD + "Join ") && title.endsWith(" Clan")) {
            event.setCancelled(true);
            
            // Check if player is in recruitment session
            if (!plugin.getRecruitmentMiniGame().hasActiveSession(playerId)) {
                player.closeInventory();
                return;
            }
            
            // Check if clicked challenge
            if (event.getRawSlot() == 11 || event.getRawSlot() == 13 || event.getRawSlot() == 15) {
                RecruitmentMiniGame miniGame = plugin.getRecruitmentMiniGame();
                
                // Start a challenge based on the clicked slot
                player.sendMessage(ChatColor.YELLOW + "Starting challenge...");
                
                // Get current challenge from the player's session
                RecruitmentChallenge challenge = miniGame.getCurrentChallenge(playerId);
                if (challenge != null) {
                    player.sendMessage(ChatColor.GREEN + "Challenge: " + challenge.getName());
                    player.sendMessage(ChatColor.YELLOW + challenge.getDescription());
                }
            }
        }
        // Handle congrats screen
        else if (title.startsWith(ChatColor.GREEN + "Welcome to ")) {
            event.setCancelled(true);
            
            // Close on any click
            player.closeInventory();
        }
        // Handle challenge GUIs
        else if (title.startsWith(ChatColor.GOLD + "Quiz: Question")) {
            event.setCancelled(true);
            
            // Check if player is in active challenge
            RecruitmentChallenge challenge = plugin.getRecruitmentMiniGame().getCurrentChallenge(playerId);
            if (challenge != null && challenge.getType() == RecruitmentChallenge.ChallengeType.QUIZ) {
                // Handle quiz challenge
                player.sendMessage(ChatColor.GREEN + "You answered a quiz question!");
                plugin.getRecruitmentMiniGame().completeCurrentChallenge(playerId);
            }
        }
        else if (title.startsWith(ChatColor.GOLD + "Memory Game")) {
            event.setCancelled(true);
            
            // Check if player is in active challenge
            RecruitmentChallenge challenge = plugin.getRecruitmentMiniGame().getCurrentChallenge(playerId);
            if (challenge != null && challenge.getType() == RecruitmentChallenge.ChallengeType.QUIZ) {
                // Handle memory challenge (we're using QUIZ type for this)
                player.sendMessage(ChatColor.GREEN + "You completed a memory challenge!");
                plugin.getRecruitmentMiniGame().completeCurrentChallenge(playerId);
            }
        }
        else if (title.startsWith(ChatColor.GOLD + "Sequence Challenge")) {
            event.setCancelled(true);
            
            // Check if player is in active challenge
            RecruitmentChallenge challenge = plugin.getRecruitmentMiniGame().getCurrentChallenge(playerId);
            if (challenge != null && challenge.getType() == RecruitmentChallenge.ChallengeType.QUIZ) {
                // Handle sequence challenge (we're using QUIZ type for this)
                player.sendMessage(ChatColor.GREEN + "You completed a sequence challenge!");
                plugin.getRecruitmentMiniGame().completeCurrentChallenge(playerId);
            }
        }
    }
    
    /**
     * Handle inventory close events to clean up sessions when needed.
     * 
     * @param event The inventory close event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getPlayer();
        UUID playerId = player.getUniqueId();
        String title = event.getView().getTitle();
        
        // Check if it's a challenge GUI being closed
        if (title.startsWith(ChatColor.GOLD + "Quiz: Question") ||
            title.startsWith(ChatColor.GOLD + "Memory Game") ||
            title.startsWith(ChatColor.GOLD + "Sequence Challenge")) {
            
            // Skip the current challenge if player closes the inventory without completing
            if (plugin.getRecruitmentMiniGame().hasActiveSession(playerId)) {
                RecruitmentChallenge challenge = plugin.getRecruitmentMiniGame().getCurrentChallenge(playerId);
                if (challenge != null) {
                    plugin.getRecruitmentMiniGame().skipCurrentChallenge(playerId);
                    player.sendMessage(ChatColor.YELLOW + "Challenge skipped.");
                }
            }
        }
    }
    
    /**
     * Handle player quit to clean up sessions.
     * 
     * @param event The player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Remove any active sessions
        if (plugin.getRecruitmentMiniGame().hasActiveSession(playerId)) {
            plugin.getRecruitmentMiniGame().endSession(playerId, false);
        }
    }
}