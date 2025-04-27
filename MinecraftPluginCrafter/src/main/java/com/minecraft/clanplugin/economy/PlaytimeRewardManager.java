package com.minecraft.clanplugin.economy;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.models.ClanMember;
import com.minecraft.clanplugin.models.ClanRole;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages playtime-based rewards for players and clans.
 */
public class PlaytimeRewardManager {
    private final ClanPlugin plugin;
    private final Map<UUID, Long> playerSessionStart;
    private final Map<UUID, Long> lastRewardTime;
    
    private double baseRewardAmount;
    private int rewardIntervalMinutes;
    private double clanBonusPercentage;
    private double leaderBonusPercentage;
    private double officerBonusPercentage;
    
    /**
     * Creates a new playtime reward manager.
     * 
     * @param plugin The clan plugin instance
     */
    public PlaytimeRewardManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.playerSessionStart = new HashMap<>();
        this.lastRewardTime = new HashMap<>();
        
        loadConfig();
    }
    
    /**
     * Loads reward configuration from the config file.
     */
    private void loadConfig() {
        ConfigurationSection rewardSection = plugin.getConfig().getConfigurationSection("economy.playtime_rewards");
        
        if (rewardSection == null) {
            // Use default values if not in config
            baseRewardAmount = 10.0;
            rewardIntervalMinutes = 15;
            clanBonusPercentage = 10.0;
            leaderBonusPercentage = 25.0;
            officerBonusPercentage = 15.0;
            
            // Save default values to config
            plugin.getConfig().set("economy.playtime_rewards.base_amount", baseRewardAmount);
            plugin.getConfig().set("economy.playtime_rewards.interval_minutes", rewardIntervalMinutes);
            plugin.getConfig().set("economy.playtime_rewards.clan_bonus_percentage", clanBonusPercentage);
            plugin.getConfig().set("economy.playtime_rewards.leader_bonus_percentage", leaderBonusPercentage);
            plugin.getConfig().set("economy.playtime_rewards.officer_bonus_percentage", officerBonusPercentage);
            plugin.saveConfig();
        } else {
            // Load from config
            baseRewardAmount = rewardSection.getDouble("base_amount", 10.0);
            rewardIntervalMinutes = rewardSection.getInt("interval_minutes", 15);
            clanBonusPercentage = rewardSection.getDouble("clan_bonus_percentage", 10.0);
            leaderBonusPercentage = rewardSection.getDouble("leader_bonus_percentage", 25.0);
            officerBonusPercentage = rewardSection.getDouble("officer_bonus_percentage", 15.0);
        }
    }
    
    /**
     * Tracks a player's session start time.
     * 
     * @param player The player to track
     */
    public void trackPlayerSession(Player player) {
        UUID playerUuid = player.getUniqueId();
        playerSessionStart.put(playerUuid, System.currentTimeMillis());
        
        // If no reward time is set, initialize it
        if (!lastRewardTime.containsKey(playerUuid)) {
            lastRewardTime.put(playerUuid, System.currentTimeMillis());
        }
    }
    
    /**
     * Ends tracking for a player's session.
     * 
     * @param player The player to stop tracking
     */
    public void endPlayerSession(Player player) {
        UUID playerUuid = player.getUniqueId();
        playerSessionStart.remove(playerUuid);
    }
    
    /**
     * Processes playtime rewards for all online players.
     */
    public void processRewards() {
        long currentTime = System.currentTimeMillis();
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID playerUuid = player.getUniqueId();
            
            // Make sure player is being tracked
            if (!playerSessionStart.containsKey(playerUuid)) {
                trackPlayerSession(player);
                continue;
            }
            
            // Check if it's time for a reward
            long lastReward = lastRewardTime.getOrDefault(playerUuid, 0L);
            long timeSinceLastReward = currentTime - lastReward;
            
            // Convert interval to milliseconds
            long rewardIntervalMs = rewardIntervalMinutes * 60 * 1000;
            
            if (timeSinceLastReward >= rewardIntervalMs) {
                // It's time for a reward
                givePlaytimeReward(player);
                lastRewardTime.put(playerUuid, currentTime);
            }
        }
    }
    
    /**
     * Gives a playtime reward to a player.
     * 
     * @param player The player to reward
     */
    private void givePlaytimeReward(Player player) {
        UUID playerUuid = player.getUniqueId();
        Clan clan = plugin.getStorageManager().getPlayerClan(playerUuid);
        
        // Calculate the reward amount with bonuses
        double rewardAmount = baseRewardAmount;
        
        // Apply clan membership bonus if player is in a clan
        if (clan != null) {
            // Get player's role in clan
            ClanMember member = clan.getMember(playerUuid);
            boolean isLeader = (member != null && member.getRole() == ClanRole.LEADER);
            boolean isOfficer = (member != null && member.getRole() == ClanRole.OFFICER);
            
            // Apply role-specific bonuses
            if (isLeader) {
                rewardAmount += (baseRewardAmount * (leaderBonusPercentage / 100.0));
            } else if (isOfficer) {
                rewardAmount += (baseRewardAmount * (officerBonusPercentage / 100.0));
            } else {
                // Regular clan member bonus
                rewardAmount += (baseRewardAmount * (clanBonusPercentage / 100.0));
            }
            
            // Apply clan level bonus (5% per level)
            int clanLevel = clan.getLevel();
            double levelBonus = baseRewardAmount * (clanLevel * 0.05);
            rewardAmount += levelBonus;
            
            // Also give a small bonus to the clan bank (10% of player reward)
            double clanBankReward = rewardAmount * 0.1;
            plugin.getEconomy().depositToClan(clan.getName(), clanBankReward);
        }
        
        // Give the reward to the player
        if (plugin.getVaultEconomy() != null) {
            plugin.getVaultEconomy().depositPlayer(player, rewardAmount);
            
            // Notify the player
            player.sendMessage(ChatColor.GREEN + "You received " + ChatColor.GOLD + "$" + String.format("%.2f", rewardAmount) + 
                    ChatColor.GREEN + " for playing for " + rewardIntervalMinutes + " minutes!");
                    
            if (clan != null) {
                player.sendMessage(ChatColor.GRAY + "Your clan also received a bonus to its bank!");
            }
        }
    }
    
    /**
     * Gets the playtime for a player's current session.
     * 
     * @param player The player to check
     * @return The playtime in milliseconds
     */
    public long getSessionPlaytime(Player player) {
        UUID playerUuid = player.getUniqueId();
        if (playerSessionStart.containsKey(playerUuid)) {
            long startTime = playerSessionStart.get(playerUuid);
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }
    
    /**
     * Formats playtime to a readable string.
     * 
     * @param timeInMs Time in milliseconds
     * @return Formatted time string
     */
    public String formatPlaytime(long timeInMs) {
        // Convert to minutes for simplicity
        long minutes = timeInMs / (1000 * 60);
        
        if (minutes < 60) {
            return minutes + " minutes";
        } else {
            long hours = minutes / 60;
            minutes = minutes % 60;
            
            if (minutes == 0) {
                return hours + " hours";
            } else {
                return hours + " hours, " + minutes + " minutes";
            }
        }
    }
    
    /**
     * Gets the time until the next reward for a player.
     * 
     * @param player The player to check
     * @return Time in milliseconds until next reward
     */
    public long getTimeUntilNextReward(Player player) {
        UUID playerUuid = player.getUniqueId();
        long lastReward = lastRewardTime.getOrDefault(playerUuid, 0L);
        long currentTime = System.currentTimeMillis();
        long timeSinceLastReward = currentTime - lastReward;
        
        // Convert interval to milliseconds
        long rewardIntervalMs = rewardIntervalMinutes * 60 * 1000;
        
        return Math.max(0, rewardIntervalMs - timeSinceLastReward);
    }
    
    /**
     * Gets the base reward amount.
     * 
     * @return The base reward amount
     */
    public double getBaseRewardAmount() {
        return baseRewardAmount;
    }
    
    /**
     * Gets the reward interval in minutes.
     * 
     * @return The reward interval
     */
    public int getRewardIntervalMinutes() {
        return rewardIntervalMinutes;
    }
}