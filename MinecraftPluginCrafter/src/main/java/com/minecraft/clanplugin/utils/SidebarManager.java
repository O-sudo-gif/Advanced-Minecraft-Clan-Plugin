package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.economy.ClanEconomy;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the player information sidebar display.
 */
public class SidebarManager {
    
    private final ClanPlugin plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    
    // The key for the blank space lines (each needs to be unique)
    private static final String[] BLANK_KEYS = {
        "blank_a", "blank_b", "blank_c", "blank_d", 
        "blank_e", "blank_f", "blank_g", "blank_h"
    };
    
    /**
     * Creates a new sidebar manager.
     * 
     * @param plugin The clan plugin instance
     */
    public SidebarManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
    }
    
    /**
     * Initializes the sidebar for a player.
     * 
     * @param player The player to initialize sidebar for
     */
    public void initializeSidebar(Player player) {
        if (!plugin.getConfig().getBoolean("ui.sidebar.enabled", true)) {
            return;
        }
        
        // Create a new scoreboard for this player
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        
        // Create sidebar objective
        String title = ChatColor.GOLD + "• " + ChatColor.YELLOW + "Clan Info" + ChatColor.GOLD + " •";
        Objective sidebar = scoreboard.registerNewObjective("claninfo", "dummy", title);
        
        // Set sidebar position based on config
        setSidebarPosition(sidebar);
        
        // Store player's scoreboard
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        
        // Set the player's scoreboard
        player.setScoreboard(scoreboard);
        
        // Update the sidebar content
        updateSidebar(player);
    }
    
    /**
     * Updates the sidebar content for a player.
     * 
     * @param player The player to update sidebar for
     */
    public void updateSidebar(Player player) {
        if (!plugin.getConfig().getBoolean("ui.sidebar.enabled", true)) {
            return;
        }
        
        UUID playerUuid = player.getUniqueId();
        
        // Get player's scoreboard
        Scoreboard scoreboard = playerScoreboards.get(playerUuid);
        if (scoreboard == null) {
            initializeSidebar(player);
            scoreboard = playerScoreboards.get(playerUuid);
        }
        
        // Get sidebar objective
        Objective sidebar = scoreboard.getObjective("claninfo");
        if (sidebar == null) {
            initializeSidebar(player);
            return;
        }
        
        // Clear existing teams for this player
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
        
        // Get player data
        Clan clan = plugin.getStorageManager().getPlayerClan(playerUuid);
        double balance = 0;
        
        // Check if Vault economy is enabled
        if (plugin.getVaultEconomy() != null) {
            balance = plugin.getVaultEconomy().getBalance(player);
        }
        
        // Create lines for the sidebar
        int line = 1;
        
        // General player information
        registerTeam(scoreboard, "money", ChatColor.GREEN + "Balance: " + ChatColor.WHITE + "$" + formatMoney(balance), line++);
        
        // Add bounty information if bounty manager is available
        if (plugin.getBountyManager() != null) {
            // Check if the player has any bounties on them
            double bountyValue = plugin.getBountyManager().getTotalBountyValue(playerUuid);
            if (bountyValue > 0) {
                registerTeam(scoreboard, "bounty", ChatColor.RED + "Bounty: " + ChatColor.GOLD + "$" + formatMoney(bountyValue), line++);
            }
        }
        
        // Add playtime reward info
        long timeUntilReward = plugin.getPlaytimeRewardManager().getTimeUntilNextReward(player);
        if (timeUntilReward > 0) {
            int minutesLeft = (int) (timeUntilReward / (1000 * 60));
            registerTeam(scoreboard, "nextreward", ChatColor.YELLOW + "Next Reward: " + ChatColor.WHITE + minutesLeft + "m", line++);
        } else {
            registerTeam(scoreboard, "nextreward", ChatColor.YELLOW + "Next Reward: " + ChatColor.GREEN + "Ready!", line++);
        }
        
        registerTeam(scoreboard, BLANK_KEYS[0], "", line++);
        
        // Clan information section
        if (clan != null) {
            registerTeam(scoreboard, "clan", ChatColor.GOLD + "Clan: " + clan.getChatColor() + clan.getName(), line++);
            registerTeam(scoreboard, "level", ChatColor.YELLOW + "Level: " + ChatColor.WHITE + clan.getLevel(), line++);
            
            // Add clan bank balance if player is leader or officer
            ClanMember member = clan.getMember(playerUuid);
            if (member != null && 
                (member.getRole() == ClanRole.LEADER || member.getRole() == ClanRole.OFFICER)) {
                
                double clanBalance = 0;
                ClanEconomy clanEconomy = plugin.getEconomy();
                if (clanEconomy != null) {
                    clanBalance = clanEconomy.getClanBalance(clan.getName());
                }
                
                registerTeam(scoreboard, "clanbank", ChatColor.AQUA + "Clan Bank: " + ChatColor.WHITE + "$" + formatMoney(clanBalance), line++);
            }
            
            // Clan territories
            int territories = plugin.getStorageManager().getTerritoryManager().getClanTerritoryCount(clan.getName());
            registerTeam(scoreboard, "territory", ChatColor.YELLOW + "Territories: " + ChatColor.WHITE + territories, line++);
            
            // Clan members
            registerTeam(scoreboard, "members", ChatColor.YELLOW + "Members: " + ChatColor.WHITE + clan.getMembers().size(), line++);
            
            // Experience progress to next level
            int currentXP = clan.getExperience();
            int nextLevelXP = plugin.getProgressionManager().getRequiredExperienceForLevel(clan.getLevel() + 1);
            int xpNeeded = Math.max(0, nextLevelXP - currentXP);
            
            registerTeam(scoreboard, BLANK_KEYS[1], "", line++);
            registerTeam(scoreboard, "xp", ChatColor.LIGHT_PURPLE + "Next Level: " + ChatColor.WHITE + xpNeeded + " XP", line++);
        } else {
            registerTeam(scoreboard, "noclan", ChatColor.RED + "Not in a clan", line++);
            registerTeam(scoreboard, "joinclan", ChatColor.GRAY + "Use /clan join to join one", line++);
        }
        
        // Footer
        registerTeam(scoreboard, BLANK_KEYS[2], "", line++);
        String server = plugin.getConfig().getString("ui.sidebar.server_name", "Minecraft Server");
        registerTeam(scoreboard, "server", ChatColor.DARK_GRAY + server, line++);
    }
    
    /**
     * Registers a team for a scoreboard line.
     * 
     * @param scoreboard The scoreboard to register the team on
     * @param name The team name
     * @param text The text to display
     * @param score The score (line position)
     */
    private void registerTeam(Scoreboard scoreboard, String name, String text, int score) {
        Team team = scoreboard.registerNewTeam(name);
        String entry = getColorCodeForLine(score);
        team.addEntry(entry);
        team.setPrefix(text);
        
        Objective sidebar = scoreboard.getObjective("claninfo");
        sidebar.getScore(entry).setScore(score);
    }
    
    /**
     * Gets a unique color code for a sidebar line.
     * 
     * @param line The line number
     * @return A unique color code for that line
     */
    private String getColorCodeForLine(int line) {
        // Use color codes as unique entry identifiers for scoreboard lines
        ChatColor[] colors = ChatColor.values();
        return colors[line % colors.length].toString();
    }
    
    /**
     * Removes the sidebar for a player.
     * 
     * @param player The player to remove sidebar for
     */
    public void removeSidebar(Player player) {
        UUID playerUuid = player.getUniqueId();
        playerScoreboards.remove(playerUuid);
        
        // Reset to server's main scoreboard
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
    
    /**
     * Updates all players' sidebars.
     */
    public void updateAllSidebars() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updateSidebar(player);
        }
    }
    
    /**
     * Formats money values to a readable string.
     * 
     * @param amount The amount to format
     * @return The formatted amount
     */
    private String formatMoney(double amount) {
        if (amount >= 1000000) {
            return String.format("%.1fM", amount / 1000000.0);
        } else if (amount >= 1000) {
            return String.format("%.1fK", amount / 1000.0);
        } else {
            return String.format("%.1f", amount);
        }
    }
    
    /**
     * Sets the sidebar position based on the configuration.
     * 
     * @param sidebar The sidebar objective to position
     */
    private void setSidebarPosition(Objective sidebar) {
        String position = plugin.getConfig().getString("ui.sidebar.position", "RIGHT");
        
        // Default position is SIDEBAR (right side of the screen)
        DisplaySlot slot = DisplaySlot.SIDEBAR;
        
        // Apply custom position if specified
        switch (position.toUpperCase()) {
            case "CENTER_LEFT":
                // Use player list to show in the center-left position
                slot = DisplaySlot.PLAYER_LIST;
                break;
            case "LEFT":
                // Use below name to show on the left side
                slot = DisplaySlot.BELOW_NAME;
                break;
            case "CENTER_RIGHT":
                // Currently not directly supported in vanilla Minecraft
                // Using sidebar as it's the closest (right side)
                slot = DisplaySlot.SIDEBAR;
                break;
            default:
                // Use sidebar (right side) as default
                slot = DisplaySlot.SIDEBAR;
        }
        
        // Set the display slot
        sidebar.setDisplaySlot(slot);
        
        // Log the position setting
        plugin.getLogger().info("Setting sidebar position to: " + position);
    }
}