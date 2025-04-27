package com.minecraft.clanplugin.utils;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Manages player nametags for clan display in the TAB list.
 */
public class NametagManager {
    private final ClanPlugin plugin;
    private final Scoreboard scoreboard;
    
    /**
     * Creates a new nametag manager.
     * 
     * @param plugin The clan plugin instance
     */
    public NametagManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.scoreboard = plugin.getServer().getScoreboardManager().getMainScoreboard();
    }
    
    /**
     * Updates a player's nametag with their clan information.
     * 
     * @param player The player to update
     */
    public void updatePlayerNametag(Player player) {
        FileConfiguration config = plugin.getConfig();
        
        // Check if nametags are enabled in config
        if (!config.getBoolean("visual_identity.nametags.enabled", true)) {
            return;
        }
        
        Clan clan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        
        if (clan == null) {
            // Player is not in a clan, remove clan tag if present
            removePlayerFromClanTeam(player);
            return;
        }
        
        // Format: teamName = "clan_" + lowercase clan name with no spaces
        String teamName = "clan_" + clan.getName().toLowerCase().replace(" ", "_");
        
        // Truncate team name if too long (max 16 chars for team name)
        if (teamName.length() > 16) {
            teamName = teamName.substring(0, 16);
        }
        
        // Check if player already has this team
        boolean playerHadTeam = false;
        for (Team team : scoreboard.getTeams()) {
            if (team.hasEntry(player.getName())) {
                if (team.getName().equals(teamName)) {
                    playerHadTeam = true; // Player already has the correct team
                    break;
                }
            }
        }
        
        // Get or create the team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
            
            // Set team display name and color
            ChatColor clanColor = clan.getChatColor();
            team.setColor(clanColor);
            
            // Update player display name color for TAB list if enabled in config
            if (config.getBoolean("visual_identity.tab_list.colored_names", true)) {
                player.setDisplayName(clanColor + player.getName() + ChatColor.RESET);
                
                // New feature: Show clan name in tab list
                String format = config.getString("visual_identity.tab_list.format", "{clan} {player}");
                String tabName = format
                    .replace("{clan}", clanColor + "[" + clan.getName() + "]" + ChatColor.RESET + " ")
                    .replace("{player}", clanColor + player.getName());
                
                player.setPlayerListName(tabName);
            }
            
            // Add prefix if enabled in config
            if (config.getBoolean("visual_identity.nametags.display_clan_tag", true)) {
                team.setPrefix(clanColor + "[" + clan.getTag() + "] ");
            }
            
            // Set team settings from config
            team.setAllowFriendlyFire(config.getBoolean("visual_identity.nametags.clan_friendly_fire", true));
            team.setCanSeeFriendlyInvisibles(config.getBoolean("visual_identity.nametags.can_see_friendly_invisibles", true));
        }
        
        // Add player to the team
        team.addEntry(player.getName());
        
        // Always update player display name color for TAB list
        if (config.getBoolean("visual_identity.tab_list.colored_names", true)) {
            ChatColor clanColor = clan.getChatColor();
            player.setDisplayName(clanColor + player.getName() + ChatColor.RESET);
            
            // New feature: Show clan name in tab list
            String format = config.getString("visual_identity.tab_list.format", "{clan} {player}");
            String tabName = format
                .replace("{clan}", clanColor + "[" + clan.getName() + "]" + ChatColor.RESET + " ")
                .replace("{player}", clanColor + player.getName());
            
            player.setPlayerListName(tabName);
        }
        
        // Send a message to the player about their nametag update (only if it's a new assignment)
        if (!playerHadTeam) {
            MessageUtils.sendNametagUpdatedMessage(player, clan);
        }
    }
    
    /**
     * Removes a player from any clan team.
     * 
     * @param player The player to remove
     */
    public void removePlayerFromClanTeam(Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("clan_") && team.hasEntry(player.getName())) {
                team.removeEntry(player.getName());
            }
        }
        
        // Reset player display name when removing from clan team
        if (plugin.getConfig().getBoolean("visual_identity.tab_list.colored_names", true)) {
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
        }
    }
    
    /**
     * Updates team settings for all clans.
     */
    public void updateAllTeams() {
        // Clear empty clan teams first
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith("clan_") && team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
        
        // Update all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerNametag(player);
        }
    }
}