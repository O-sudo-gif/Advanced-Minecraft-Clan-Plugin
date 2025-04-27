package com.minecraft.clanplugin.bounty;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Bounty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Manages player bounties.
 */
public class BountyManager {
    
    private final ClanPlugin plugin;
    private final Map<UUID, List<Bounty>> activeBounties; // Target UUID -> List of bounties
    private final List<Bounty> recentlyClaimedBounties; // For history
    private final File bountyFile;
    
    // Configurable settings
    private double minimumBountyAmount;
    private int maxActiveBountiesPerTarget;
    private int maxActiveBountiesPerPlacer;
    private int cooldownBetweenClaimsMinutes;
    private int abusePenaltyPercent;
    private long bountyExpiryTimeMillis;
    private boolean enableClanSharing;
    private boolean enableBountyBoard;
    
    /**
     * Creates a new bounty manager.
     * 
     * @param plugin The clan plugin instance
     */
    public BountyManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.activeBounties = new HashMap<>();
        this.recentlyClaimedBounties = new ArrayList<>();
        this.bountyFile = new File(plugin.getDataFolder(), "bounties.yml");
        
        loadConfig();
        loadBounties();
        
        // Schedule regular save task
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveBounties, 6000L, 6000L); // Every 5 minutes
        
        // Schedule bounty expiry check
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiredBounties, 12000L, 12000L); // Every 10 minutes
    }
    
    /**
     * Loads bounty system configuration from config.
     */
    private void loadConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // Set defaults if not present
        if (!config.contains("bounty.minimum_amount")) {
            config.set("bounty.minimum_amount", 100.0);
            config.set("bounty.max_per_target", 5);
            config.set("bounty.max_per_placer", 10);
            config.set("bounty.cooldown_minutes", 30);
            config.set("bounty.abuse_penalty_percent", 25);
            config.set("bounty.expiry_days", 7);
            config.set("bounty.enable_clan_sharing", true);
            config.set("bounty.enable_bounty_board", true);
            plugin.saveConfig();
        }
        
        minimumBountyAmount = config.getDouble("bounty.minimum_amount", 100.0);
        maxActiveBountiesPerTarget = config.getInt("bounty.max_per_target", 5);
        maxActiveBountiesPerPlacer = config.getInt("bounty.max_per_placer", 10);
        cooldownBetweenClaimsMinutes = config.getInt("bounty.cooldown_minutes", 30);
        abusePenaltyPercent = config.getInt("bounty.abuse_penalty_percent", 25);
        bountyExpiryTimeMillis = config.getLong("bounty.expiry_days", 7) * 24 * 60 * 60 * 1000;
        enableClanSharing = config.getBoolean("bounty.enable_clan_sharing", true);
        enableBountyBoard = config.getBoolean("bounty.enable_bounty_board", true);
    }
    
    /**
     * Places a new bounty on a player.
     * 
     * @param targetPlayer The player to place a bounty on
     * @param placerPlayer The player placing the bounty
     * @param amount The bounty amount
     * @return True if the bounty was successfully placed
     */
    public boolean placeBounty(Player targetPlayer, Player placerPlayer, double amount) {
        UUID targetUUID = targetPlayer.getUniqueId();
        UUID placerUUID = placerPlayer.getUniqueId();
        
        // Check if the player is trying to place a bounty on themselves
        if (targetUUID.equals(placerUUID)) {
            placerPlayer.sendMessage(ChatColor.RED + "You cannot place a bounty on yourself!");
            return false;
        }
        
        // Check if the amount is valid
        if (amount < minimumBountyAmount) {
            placerPlayer.sendMessage(ChatColor.RED + "Bounty amount must be at least " + 
                                   ChatColor.GOLD + "$" + minimumBountyAmount);
            return false;
        }
        
        // Check if the player has enough money
        if (plugin.getVaultEconomy() != null) {
            if (plugin.getVaultEconomy().getBalance(placerPlayer) < amount) {
                placerPlayer.sendMessage(ChatColor.RED + "You don't have enough money to place this bounty!");
                return false;
            }
        }
        
        // Check if the target already has too many bounties
        if (getActiveBountiesForTarget(targetUUID).size() >= maxActiveBountiesPerTarget) {
            placerPlayer.sendMessage(ChatColor.RED + "This player already has the maximum number of bounties!");
            return false;
        }
        
        // Check if the placer has already placed too many bounties
        if (getActiveBountiesPlacedBy(placerUUID).size() >= maxActiveBountiesPerPlacer) {
            placerPlayer.sendMessage(ChatColor.RED + "You have already placed the maximum number of bounties!");
            return false;
        }
        
        // Take the money from the placer
        if (plugin.getVaultEconomy() != null) {
            plugin.getVaultEconomy().withdrawPlayer(placerPlayer, amount);
        }
        
        // Create the bounty
        Bounty bounty = new Bounty(targetUUID, placerUUID, amount);
        
        // Add to active bounties
        if (!activeBounties.containsKey(targetUUID)) {
            activeBounties.put(targetUUID, new ArrayList<>());
        }
        activeBounties.get(targetUUID).add(bounty);
        
        // Announce the bounty
        String announcement = ChatColor.GOLD + "=== BOUNTY PLACED ===" + 
                              "\n" + ChatColor.RED + placerPlayer.getName() + ChatColor.YELLOW + " has placed a " + 
                              ChatColor.GOLD + "$" + amount + ChatColor.YELLOW + " bounty on " + 
                              ChatColor.RED + targetPlayer.getName() + ChatColor.YELLOW + "!" +
                              "\n" + ChatColor.GOLD + "===================";
        
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(announcement));
        
        // Save bounties
        saveBounties();
        
        return true;
    }
    
    /**
     * Claims a bounty when a player kills another player.
     * 
     * @param killerUUID The UUID of the killer
     * @param targetUUID The UUID of the killed player
     * @return The total bounty amount claimed, or 0 if no bounty was claimed
     */
    public double claimBounty(UUID killerUUID, UUID targetUUID) {
        // Check if the killer is the same as the target (suicide)
        if (killerUUID.equals(targetUUID)) {
            return 0;
        }
        
        // Check if there are any active bounties on the target
        if (!activeBounties.containsKey(targetUUID) || activeBounties.get(targetUUID).isEmpty()) {
            return 0;
        }
        
        // Get active bounties for the target
        List<Bounty> targetBounties = activeBounties.get(targetUUID);
        
        // Calculate total bounty amount
        double totalAmount = 0;
        for (Bounty bounty : targetBounties) {
            if (bounty.isActive()) {
                bounty.claim(killerUUID);
                totalAmount += bounty.getAmount();
                recentlyClaimedBounties.add(bounty);
            }
        }
        
        // Remove all claimed bounties from active list
        activeBounties.remove(targetUUID);
        
        // If there was a bounty to claim, give the money to the killer
        if (totalAmount > 0) {
            Player killer = Bukkit.getPlayer(killerUUID);
            if (killer != null && plugin.getVaultEconomy() != null) {
                // Give the money to the killer
                plugin.getVaultEconomy().depositPlayer(killer, totalAmount);
                
                // Announce the claim
                String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
                if (targetName == null) targetName = "Unknown";
                
                String announcement = ChatColor.GOLD + "=== BOUNTY CLAIMED ===" + 
                                      "\n" + ChatColor.GREEN + killer.getName() + ChatColor.YELLOW + " has claimed a " + 
                                      ChatColor.GOLD + "$" + String.format("%.2f", totalAmount) + ChatColor.YELLOW + " bounty on " + 
                                      ChatColor.RED + targetName + ChatColor.YELLOW + "!" +
                                      "\n" + ChatColor.GOLD + "=====================";
                
                Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(announcement));
            }
            
            // Save bounties
            saveBounties();
        }
        
        return totalAmount;
    }
    
    /**
     * Gets all active bounties placed on a specific target.
     * 
     * @param targetUUID The UUID of the target
     * @return A list of active bounties for the target
     */
    public List<Bounty> getActiveBountiesForTarget(UUID targetUUID) {
        if (!activeBounties.containsKey(targetUUID)) {
            return new ArrayList<>();
        }
        
        return activeBounties.get(targetUUID).stream()
                .filter(Bounty::isActive)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all active bounties placed by a specific player.
     * 
     * @param placerUUID The UUID of the player who placed the bounties
     * @return A list of active bounties placed by the player
     */
    public List<Bounty> getActiveBountiesPlacedBy(UUID placerUUID) {
        List<Bounty> placedBounties = new ArrayList<>();
        
        for (List<Bounty> bounties : activeBounties.values()) {
            for (Bounty bounty : bounties) {
                if (bounty.isActive() && bounty.isPlacedBy(placerUUID)) {
                    placedBounties.add(bounty);
                }
            }
        }
        
        return placedBounties;
    }
    
    /**
     * Gets the total value of all active bounties on a target.
     * 
     * @param targetUUID The UUID of the target
     * @return The total value of all active bounties on the target
     */
    public double getTotalBountyValue(UUID targetUUID) {
        if (!activeBounties.containsKey(targetUUID)) {
            return 0;
        }
        
        return activeBounties.get(targetUUID).stream()
                .filter(Bounty::isActive)
                .mapToDouble(Bounty::getAmount)
                .sum();
    }
    
    /**
     * Gets all UUIDs of players who have active bounties on them.
     * 
     * @return A set of UUIDs for players who have active bounties
     */
    public Set<UUID> getAllTargetsWithBounties() {
        Set<UUID> targets = new HashSet<>();
        
        for (Map.Entry<UUID, List<Bounty>> entry : activeBounties.entrySet()) {
            // Only include targets that have at least one active bounty
            if (entry.getValue().stream().anyMatch(Bounty::isActive)) {
                targets.add(entry.getKey());
            }
        }
        
        return targets;
    }
    
    /**
     * Checks if clan sharing is enabled for bounties.
     * When clan sharing is enabled, clan members can't claim bounties on their own clan members.
     * 
     * @return True if clan sharing is enabled
     */
    public boolean isClanSharingEnabled() {
        return enableClanSharing;
    }
    
    /**
     * Gets the minimum amount for a bounty.
     * 
     * @return The minimum amount for a bounty
     */
    public double getMinimumBountyAmount() {
        return minimumBountyAmount;
    }
    
    /**
     * Gets the players with the highest total bounty values.
     * 
     * @param limit The maximum number of players to return
     * @return A map of player UUIDs to total bounty values, sorted by value
     */
    public Map<UUID, Double> getTopBounties(int limit) {
        Map<UUID, Double> totalBounties = new HashMap<>();
        
        // Calculate total bounty for each target
        for (Map.Entry<UUID, List<Bounty>> entry : activeBounties.entrySet()) {
            UUID targetUUID = entry.getKey();
            double total = entry.getValue().stream()
                    .filter(Bounty::isActive)
                    .mapToDouble(Bounty::getAmount)
                    .sum();
            
            if (total > 0) {
                totalBounties.put(targetUUID, total);
            }
        }
        
        // Sort by bounty value (descending) and limit the results
        return totalBounties.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
    
    /**
     * Cancels a bounty placed by a player.
     * 
     * @param placerPlayer The player who placed the bounty
     * @param targetUUID The UUID of the target
     * @param index The index of the bounty to cancel (0-based)
     * @return True if the bounty was successfully canceled
     */
    public boolean cancelBounty(Player placerPlayer, UUID targetUUID, int index) {
        UUID placerUUID = placerPlayer.getUniqueId();
        
        // Check if there are any active bounties on the target
        if (!activeBounties.containsKey(targetUUID)) {
            placerPlayer.sendMessage(ChatColor.RED + "No bounties found for that player!");
            return false;
        }
        
        List<Bounty> targetBounties = activeBounties.get(targetUUID);
        List<Bounty> placerBounties = new ArrayList<>();
        
        // Get bounties placed by this player on the target
        for (Bounty bounty : targetBounties) {
            if (bounty.isActive() && bounty.isPlacedBy(placerUUID)) {
                placerBounties.add(bounty);
            }
        }
        
        // Check if the index is valid
        if (index < 0 || index >= placerBounties.size()) {
            placerPlayer.sendMessage(ChatColor.RED + "Invalid bounty index!");
            return false;
        }
        
        Bounty bountyToCancel = placerBounties.get(index);
        
        // Calculate refund amount (apply penalty)
        double refundAmount = bountyToCancel.getAmount() * (1 - (abusePenaltyPercent / 100.0));
        
        // Refund the placer
        if (plugin.getVaultEconomy() != null) {
            plugin.getVaultEconomy().depositPlayer(placerPlayer, refundAmount);
        }
        
        // Remove the bounty
        bountyToCancel.setActive(false);
        targetBounties.remove(bountyToCancel);
        
        if (targetBounties.isEmpty()) {
            activeBounties.remove(targetUUID);
        }
        
        placerPlayer.sendMessage(ChatColor.GREEN + "Bounty canceled! You have been refunded " + 
                               ChatColor.GOLD + "$" + String.format("%.2f", refundAmount) + 
                               ChatColor.GREEN + " (after " + abusePenaltyPercent + "% penalty).");
        
        // Save bounties
        saveBounties();
        
        return true;
    }
    
    /**
     * Checks for expired bounties and removes them.
     */
    private void checkExpiredBounties() {
        boolean hadExpirations = false;
        
        // Get the current time
        long now = System.currentTimeMillis();
        
        // Check all active bounties
        Iterator<Map.Entry<UUID, List<Bounty>>> targetIterator = activeBounties.entrySet().iterator();
        while (targetIterator.hasNext()) {
            Map.Entry<UUID, List<Bounty>> entry = targetIterator.next();
            UUID targetUUID = entry.getKey();
            List<Bounty> bounties = entry.getValue();
            
            Iterator<Bounty> bountyIterator = bounties.iterator();
            while (bountyIterator.hasNext()) {
                Bounty bounty = bountyIterator.next();
                
                // Check if the bounty has expired
                if (bounty.isActive() && (now - bounty.getTimestamp() > bountyExpiryTimeMillis)) {
                    // Refund the placer
                    if (plugin.getVaultEconomy() != null) {
                        OfflinePlayer placer = Bukkit.getOfflinePlayer(bounty.getPlacerUUID());
                        if (placer != null) {
                            plugin.getVaultEconomy().depositPlayer(placer, bounty.getAmount());
                        }
                    }
                    
                    // Remove the bounty
                    bountyIterator.remove();
                    hadExpirations = true;
                }
            }
            
            // If all bounties for this target are gone, remove the target entry
            if (bounties.isEmpty()) {
                targetIterator.remove();
            }
        }
        
        // Save if any bounties were expired
        if (hadExpirations) {
            saveBounties();
        }
    }
    
    /**
     * Saves all bounties to file.
     */
    public void saveBounties() {
        try {
            FileConfiguration config = new YamlConfiguration();
            
            // Save active bounties
            ConfigurationSection activeBountiesSection = config.createSection("active_bounties");
            for (Map.Entry<UUID, List<Bounty>> entry : activeBounties.entrySet()) {
                UUID targetUUID = entry.getKey();
                List<Bounty> bounties = entry.getValue();
                
                ConfigurationSection targetSection = activeBountiesSection.createSection(targetUUID.toString());
                int index = 0;
                
                for (Bounty bounty : bounties) {
                    if (bounty.isActive()) {
                        ConfigurationSection bountySection = targetSection.createSection(String.valueOf(index++));
                        
                        bountySection.set("placer", bounty.getPlacerUUID().toString());
                        bountySection.set("amount", bounty.getAmount());
                        bountySection.set("timestamp", bounty.getTimestamp());
                    }
                }
            }
            
            // Save recently claimed bounties
            ConfigurationSection claimedBountiesSection = config.createSection("claimed_bounties");
            for (int i = 0; i < Math.min(recentlyClaimedBounties.size(), 50); i++) {
                Bounty bounty = recentlyClaimedBounties.get(i);
                
                ConfigurationSection bountySection = claimedBountiesSection.createSection(String.valueOf(i));
                
                bountySection.set("target", bounty.getTargetUUID().toString());
                bountySection.set("placer", bounty.getPlacerUUID().toString());
                bountySection.set("amount", bounty.getAmount());
                bountySection.set("timestamp", bounty.getTimestamp());
                bountySection.set("claimed_by", bounty.getClaimedBy().toString());
                bountySection.set("claimed_timestamp", bounty.getClaimedTimestamp());
            }
            
            // Save to file
            config.save(bountyFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save bounty data", e);
        }
    }
    
    /**
     * Loads bounties from file.
     */
    public void loadBounties() {
        // Clear existing data
        activeBounties.clear();
        recentlyClaimedBounties.clear();
        
        if (!bountyFile.exists()) {
            return; // No data to load
        }
        
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(bountyFile);
            
            // Load active bounties
            ConfigurationSection activeBountiesSection = config.getConfigurationSection("active_bounties");
            if (activeBountiesSection != null) {
                for (String targetUUIDString : activeBountiesSection.getKeys(false)) {
                    try {
                        UUID targetUUID = UUID.fromString(targetUUIDString);
                        ConfigurationSection targetSection = activeBountiesSection.getConfigurationSection(targetUUIDString);
                        
                        if (targetSection != null) {
                            List<Bounty> bounties = new ArrayList<>();
                            
                            for (String indexStr : targetSection.getKeys(false)) {
                                ConfigurationSection bountySection = targetSection.getConfigurationSection(indexStr);
                                
                                if (bountySection != null) {
                                    UUID placerUUID = UUID.fromString(bountySection.getString("placer"));
                                    double amount = bountySection.getDouble("amount");
                                    long timestamp = bountySection.getLong("timestamp");
                                    
                                    Bounty bounty = new Bounty(targetUUID, placerUUID, amount, timestamp, true, null, 0);
                                    bounties.add(bounty);
                                }
                            }
                            
                            if (!bounties.isEmpty()) {
                                activeBounties.put(targetUUID, bounties);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in bounty data: " + targetUUIDString);
                    }
                }
            }
            
            // Load recently claimed bounties
            ConfigurationSection claimedBountiesSection = config.getConfigurationSection("claimed_bounties");
            if (claimedBountiesSection != null) {
                for (String indexStr : claimedBountiesSection.getKeys(false)) {
                    ConfigurationSection bountySection = claimedBountiesSection.getConfigurationSection(indexStr);
                    
                    if (bountySection != null) {
                        try {
                            UUID targetUUID = UUID.fromString(bountySection.getString("target"));
                            UUID placerUUID = UUID.fromString(bountySection.getString("placer"));
                            double amount = bountySection.getDouble("amount");
                            long timestamp = bountySection.getLong("timestamp");
                            UUID claimedByUUID = UUID.fromString(bountySection.getString("claimed_by"));
                            long claimedTimestamp = bountySection.getLong("claimed_timestamp");
                            
                            Bounty bounty = new Bounty(targetUUID, placerUUID, amount, timestamp, false, claimedByUUID, claimedTimestamp);
                            recentlyClaimedBounties.add(bounty);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid UUID in claimed bounty data");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not load bounty data", e);
        }
    }
    
    /**
     * Gets a list of recently claimed bounties.
     * 
     * @param limit The maximum number of bounties to return
     * @return A list of recently claimed bounties
     */
    public List<Bounty> getRecentlyClaimedBounties(int limit) {
        int size = Math.min(limit, recentlyClaimedBounties.size());
        return recentlyClaimedBounties.subList(0, size);
    }
    
    /**
     * Checks if bounty board is enabled.
     * 
     * @return True if bounty board is enabled
     */
    public boolean isBountyBoardEnabled() {
        return enableBountyBoard;
    }
    
    /**
     * Gets the cooldown between bounty claims in minutes.
     * 
     * @return The cooldown between bounty claims in minutes
     */
    public int getCooldownBetweenClaimsMinutes() {
        return cooldownBetweenClaimsMinutes;
    }
}