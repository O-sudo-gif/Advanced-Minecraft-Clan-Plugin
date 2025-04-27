package com.minecraft.clanplugin.commands;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import com.minecraft.clanplugin.wars.ClanWar;
import com.minecraft.clanplugin.wars.WarManager;
import com.minecraft.clanplugin.wars.WarStatus;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Command handler for clan war-related commands.
 */
public class ClanWarCommand implements CommandExecutor {
    
    private final ClanPlugin plugin;
    private final WarManager warManager;
    
    public ClanWarCommand(ClanPlugin plugin, WarManager warManager) {
        this.plugin = plugin;
        this.warManager = warManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use clan war commands!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "declare":
                return handleDeclare(player, args);
            case "status":
                return handleStatus(player);
            case "surrender":
                return handleSurrender(player);
            case "stats":
                return handleStats(player, args);
            case "leaderboard":
                return handleLeaderboard(player);
            case "help":
            default:
                sendHelpMessage(player);
                return true;
        }
    }
    
    /**
     * Handles the declare command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleDeclare(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /clan war declare <clan>");
            return true;
        }
        
        String targetClanName = args[1];
        
        // Check if player is in a clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (playerClanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to declare war!");
            return true;
        }
        
        // Check if player has permission
        Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
        ClanMember member = playerClan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.LEADER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can declare war!");
            return true;
        }
        
        // Check if target clan exists
        Clan targetClan = plugin.getStorageManager().getClanStorage().getClan(targetClanName);
        if (targetClan == null) {
            player.sendMessage(ChatColor.RED + "Clan " + targetClanName + " does not exist!");
            return true;
        }
        
        // Check if declaring war on own clan
        if (playerClanName.equalsIgnoreCase(targetClanName)) {
            player.sendMessage(ChatColor.RED + "You cannot declare war on your own clan!");
            return true;
        }
        
        // Check if already at war
        if (warManager.isAtWar(playerClanName)) {
            player.sendMessage(ChatColor.RED + "Your clan is already at war with another clan!");
            return true;
        }
        
        // Check if target clan is already at war
        if (warManager.isAtWar(targetClanName)) {
            player.sendMessage(ChatColor.RED + "The target clan is already at war with another clan!");
            return true;
        }
        
        // Declare war
        if (warManager.declareWar(playerClanName, targetClanName)) {
            // Send messages to both clans
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                String onlinePlayerClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(onlinePlayer.getUniqueId());
                
                if (onlinePlayerClan != null) {
                    if (onlinePlayerClan.equalsIgnoreCase(playerClanName)) {
                        onlinePlayer.sendMessage(ChatColor.RED + "Your clan has declared war on clan " + 
                                               ChatColor.GOLD + targetClanName + ChatColor.RED + "!");
                    } else if (onlinePlayerClan.equalsIgnoreCase(targetClanName)) {
                        onlinePlayer.sendMessage(ChatColor.RED + "Clan " + ChatColor.GOLD + playerClanName + 
                                               ChatColor.RED + " has declared war on your clan!");
                    }
                }
            }
            
            // Broadcast to server
            plugin.getServer().broadcastMessage(ChatColor.RED + "Clan " + ChatColor.GOLD + playerClanName + 
                                             ChatColor.RED + " has declared war on clan " + 
                                             ChatColor.GOLD + targetClanName + ChatColor.RED + "!");
            
            player.sendMessage(ChatColor.GREEN + "War has been declared! The war will last for 7 days.");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to declare war!");
        }
        
        return true;
    }
    
    /**
     * Handles the status command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleStatus(Player player) {
        // Check if player is in a clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (playerClanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to check war status!");
            return true;
        }
        
        // Check if at war
        if (!warManager.isAtWar(playerClanName)) {
            player.sendMessage(ChatColor.YELLOW + "Your clan is not currently at war.");
            return true;
        }
        
        ClanWar war = warManager.getWarForClan(playerClanName);
        String opposingClan = war.getOpposingClan(playerClanName);
        
        player.sendMessage(ChatColor.GOLD + "=== War Status ===");
        player.sendMessage(ChatColor.YELLOW + "War against: " + ChatColor.WHITE + opposingClan);
        
        // Display scores
        int yourScore, theirScore;
        if (playerClanName.equalsIgnoreCase(war.getInitiatingClan())) {
            yourScore = war.getInitiatingClanScore();
            theirScore = war.getTargetClanScore();
        } else {
            yourScore = war.getTargetClanScore();
            theirScore = war.getInitiatingClanScore();
        }
        
        player.sendMessage(ChatColor.YELLOW + "Score: " + ChatColor.GREEN + yourScore + 
                          ChatColor.YELLOW + " - " + ChatColor.RED + theirScore);
        
        // Display time remaining
        long timeRemaining = war.getTimeRemaining();
        String timeStr = warManager.formatTimeRemaining(timeRemaining);
        player.sendMessage(ChatColor.YELLOW + "Time remaining: " + ChatColor.WHITE + timeStr);
        
        // Display top killers
        player.sendMessage(ChatColor.YELLOW + "Top killers in your clan:");
        Map<UUID, Integer> topKillers = warManager.getTopKillersInWar(war.getWarId(), 3);
        
        if (topKillers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "  No kills yet");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topKillers.entrySet()) {
                UUID playerId = entry.getKey();
                int kills = entry.getValue();
                
                // Check if this player is in the player's clan
                String playerClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(playerId);
                if (playerClan != null && playerClan.equalsIgnoreCase(playerClanName)) {
                    String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();
                    player.sendMessage(ChatColor.GRAY + "  " + rank + ". " + 
                                      ChatColor.WHITE + playerName + ": " + 
                                      ChatColor.RED + kills + " kills");
                    rank++;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Handles the surrender command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleSurrender(Player player) {
        // Check if player is in a clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (playerClanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to surrender!");
            return true;
        }
        
        // Check if player has permission
        Clan playerClan = plugin.getStorageManager().getClanStorage().getClan(playerClanName);
        ClanMember member = playerClan.getMember(player.getUniqueId());
        
        if (member.getRole().getRoleLevel() < ClanRole.LEADER.getRoleLevel()) {
            player.sendMessage(ChatColor.RED + "Only clan leaders can surrender!");
            return true;
        }
        
        // Check if at war
        if (!warManager.isAtWar(playerClanName)) {
            player.sendMessage(ChatColor.RED + "Your clan is not currently at war!");
            return true;
        }
        
        ClanWar war = warManager.getWarForClan(playerClanName);
        String opposingClan = war.getOpposingClan(playerClanName);
        
        // Surrender
        if (warManager.surrenderWar(playerClanName)) {
            // Send messages to both clans
            for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                String onlinePlayerClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(onlinePlayer.getUniqueId());
                
                if (onlinePlayerClan != null) {
                    if (onlinePlayerClan.equalsIgnoreCase(playerClanName)) {
                        onlinePlayer.sendMessage(ChatColor.RED + "Your clan has surrendered the war against clan " + 
                                               ChatColor.GOLD + opposingClan + ChatColor.RED + "!");
                    } else if (onlinePlayerClan.equalsIgnoreCase(opposingClan)) {
                        onlinePlayer.sendMessage(ChatColor.GREEN + "Clan " + ChatColor.GOLD + playerClanName + 
                                               ChatColor.GREEN + " has surrendered the war against your clan!");
                    }
                }
            }
            
            // Broadcast to server
            plugin.getServer().broadcastMessage(ChatColor.YELLOW + "Clan " + ChatColor.GOLD + playerClanName + 
                                             ChatColor.YELLOW + " has surrendered the war against clan " + 
                                             ChatColor.GOLD + opposingClan + ChatColor.YELLOW + "!");
            
            player.sendMessage(ChatColor.GREEN + "Your clan has surrendered the war.");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to surrender!");
        }
        
        return true;
    }
    
    /**
     * Handles the stats command.
     * 
     * @param player The player
     * @param args The command arguments
     * @return True if the command was handled
     */
    private boolean handleStats(Player player, String[] args) {
        String clanName;
        
        if (args.length > 1) {
            clanName = args[1];
            
            if (plugin.getStorageManager().getClanStorage().getClan(clanName) == null) {
                player.sendMessage(ChatColor.RED + "Clan " + clanName + " does not exist!");
                return true;
            }
        } else {
            // Check if player is in a clan
            clanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
            if (clanName == null) {
                player.sendMessage(ChatColor.RED + "You are not in a clan! Use /clan war stats <clan> to view stats for a specific clan.");
                return true;
            }
        }
        
        Map<String, Integer> stats = warManager.getWarStats(clanName);
        
        player.sendMessage(ChatColor.GOLD + "=== War Statistics for " + clanName + " ===");
        player.sendMessage(ChatColor.YELLOW + "Total Wars: " + ChatColor.WHITE + stats.get("total"));
        player.sendMessage(ChatColor.YELLOW + "Victories: " + ChatColor.GREEN + stats.get("victories"));
        player.sendMessage(ChatColor.YELLOW + "Defeats: " + ChatColor.RED + stats.get("defeats"));
        player.sendMessage(ChatColor.YELLOW + "Draws: " + ChatColor.GRAY + stats.get("draws"));
        
        // Calculate win rate
        int total = stats.get("total");
        int wins = stats.get("victories");
        double winRate = total > 0 ? ((double) wins / total) * 100 : 0;
        
        player.sendMessage(ChatColor.YELLOW + "Win Rate: " + ChatColor.GOLD + String.format("%.1f%%", winRate));
        
        return true;
    }
    
    /**
     * Handles the leaderboard command.
     * 
     * @param player The player
     * @return True if the command was handled
     */
    private boolean handleLeaderboard(Player player) {
        // Check if player is in a clan
        String playerClanName = plugin.getStorageManager().getClanStorage().getPlayerClanName(player.getUniqueId());
        if (playerClanName == null) {
            player.sendMessage(ChatColor.RED + "You must be in a clan to view the war leaderboard!");
            return true;
        }
        
        // Check if at war
        if (!warManager.isAtWar(playerClanName)) {
            player.sendMessage(ChatColor.RED + "Your clan is not currently at war!");
            return true;
        }
        
        ClanWar war = warManager.getWarForClan(playerClanName);
        Map<UUID, Integer> topKillers = warManager.getTopKillersInWar(war.getWarId(), 10);
        
        player.sendMessage(ChatColor.GOLD + "=== War Leaderboard ===");
        
        if (topKillers.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "No kills recorded yet.");
        } else {
            int rank = 1;
            for (Map.Entry<UUID, Integer> entry : topKillers.entrySet()) {
                UUID playerId = entry.getKey();
                int kills = entry.getValue();
                
                String playerName = plugin.getServer().getOfflinePlayer(playerId).getName();
                String playerClan = plugin.getStorageManager().getClanStorage().getPlayerClanName(playerId);
                
                if (playerClan != null) {
                    ChatColor nameColor = playerClan.equalsIgnoreCase(playerClanName) ? ChatColor.GREEN : ChatColor.RED;
                    player.sendMessage(ChatColor.GRAY + "" + rank + ". " + 
                                      nameColor + playerName + ChatColor.GRAY + " (" + 
                                      playerClan + "): " + ChatColor.WHITE + kills + " kills");
                }
                
                rank++;
            }
        }
        
        return true;
    }
    
    /**
     * Sends the help message for war commands.
     * 
     * @param player The player
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Clan War Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/clan war declare <clan>" + ChatColor.WHITE + " - Declare war on another clan (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/clan war status" + ChatColor.WHITE + " - View the status of your clan's current war");
        player.sendMessage(ChatColor.YELLOW + "/clan war surrender" + ChatColor.WHITE + " - Surrender the current war (Leader only)");
        player.sendMessage(ChatColor.YELLOW + "/clan war stats [clan]" + ChatColor.WHITE + " - View war statistics for a clan");
        player.sendMessage(ChatColor.YELLOW + "/clan war leaderboard" + ChatColor.WHITE + " - View the current war's kill leaderboard");
    }
}