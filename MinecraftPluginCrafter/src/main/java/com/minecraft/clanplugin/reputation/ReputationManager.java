package com.minecraft.clanplugin.reputation;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages clan reputation and leaderboards.
 */
public class ReputationManager {
    private ClanPlugin plugin;
    private Map<String, Integer> clanReputation;
    private File reputationFile;
    private FileConfiguration reputationConfig;
    
    /**
     * Creates a new reputation manager.
     * 
     * @param plugin The clan plugin instance
     */
    public ReputationManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clanReputation = new HashMap<>();
        
        // Initialize reputation file
        this.reputationFile = new File(plugin.getDataFolder(), "reputation.yml");
        if (!reputationFile.exists()) {
            try {
                reputationFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create reputation.yml!");
                e.printStackTrace();
            }
        }
        
        this.reputationConfig = YamlConfiguration.loadConfiguration(reputationFile);
        
        // Load reputation data
        loadReputation();
    }
    
    /**
     * Loads reputation data from configuration.
     */
    private void loadReputation() {
        if (reputationConfig.contains("clans")) {
            for (String clanName : reputationConfig.getConfigurationSection("clans").getKeys(false)) {
                int reputation = reputationConfig.getInt("clans." + clanName);
                clanReputation.put(clanName, reputation);
            }
        }
    }
    
    /**
     * Saves reputation data to configuration.
     */
    private void saveReputation() {
        for (Map.Entry<String, Integer> entry : clanReputation.entrySet()) {
            reputationConfig.set("clans." + entry.getKey(), entry.getValue());
        }
        
        try {
            reputationConfig.save(reputationFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reputation data!");
            e.printStackTrace();
        }
    }
    
    /**
     * Public method to save all reputation data.
     * This is called when the server is shutting down.
     */
    public void saveReputationData() {
        plugin.getLogger().info("Saving reputation data...");
        saveReputation();
    }
    
    /**
     * Gets a clan's reputation.
     * 
     * @param clan The clan to check
     * @return The clan's reputation
     */
    public int getReputation(Clan clan) {
        return clanReputation.getOrDefault(clan.getName(), 0);
    }
    
    /**
     * Gets a clan's reputation by clan name.
     * 
     * @param clanName The name of the clan to check
     * @return The clan's reputation
     */
    public int getClanReputation(String clanName) {
        return clanReputation.getOrDefault(clanName, 0);
    }
    
    /**
     * Adds reputation to a clan.
     * 
     * @param clan The clan to add reputation to
     * @param amount The amount of reputation to add
     */
    public void addReputation(Clan clan, int amount) {
        if (amount <= 0) {
            return;
        }
        
        int currentRep = getReputation(clan);
        int newRep = currentRep + amount;
        
        clanReputation.put(clan.getName(), newRep);
        saveReputation();
        
        // Notify clan members
        MessageUtils.notifyClan(clan, ChatColor.GREEN + "Your clan gained " + 
                ChatColor.GOLD + amount + ChatColor.GREEN + " reputation!");
    }
    
    /**
     * Removes reputation from a clan.
     * 
     * @param clan The clan to remove reputation from
     * @param amount The amount of reputation to remove
     */
    public void removeReputation(Clan clan, int amount) {
        if (amount <= 0) {
            return;
        }
        
        int currentRep = getReputation(clan);
        int newRep = Math.max(0, currentRep - amount);
        
        clanReputation.put(clan.getName(), newRep);
        saveReputation();
        
        // Notify clan members
        MessageUtils.notifyClan(clan, ChatColor.RED + "Your clan lost " + 
                ChatColor.GOLD + amount + ChatColor.RED + " reputation!");
    }
    
    /**
     * Gets the reputation rank of a clan.
     * 
     * @param clan The clan to check
     * @return The clan's rank (1-based), or -1 if not ranked
     */
    public int getClanRank(Clan clan) {
        if (!clanReputation.containsKey(clan.getName())) {
            return -1;
        }
        
        List<Map.Entry<String, Integer>> sortedClans = getSortedClans();
        
        for (int i = 0; i < sortedClans.size(); i++) {
            if (sortedClans.get(i).getKey().equals(clan.getName())) {
                return i + 1;
            }
        }
        
        return -1;
    }
    
    /**
     * Gets the top ranked clans.
     * 
     * @param limit The maximum number of clans to return
     * @return A list of top ranked clans
     */
    public List<Map.Entry<String, Integer>> getTopClans(int limit) {
        return getSortedClans().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all clans sorted by reputation.
     * 
     * @return A list of clans sorted by reputation
     */
    private List<Map.Entry<String, Integer>> getSortedClans() {
        return clanReputation.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Displays the reputation leaderboard to a player.
     * 
     * @param player The player to display the leaderboard to
     * @param page The page number (1-based)
     * @param entriesPerPage The number of entries per page
     */
    public void displayLeaderboard(Player player, int page, int entriesPerPage) {
        List<Map.Entry<String, Integer>> sortedClans = getSortedClans();
        
        if (sortedClans.isEmpty()) {
            player.sendMessage(ChatColor.RED + "No clans have reputation yet.");
            return;
        }
        
        int totalPages = (int) Math.ceil((double) sortedClans.size() / entriesPerPage);
        page = Math.max(1, Math.min(page, totalPages));
        
        int startIndex = (page - 1) * entriesPerPage;
        int endIndex = Math.min(startIndex + entriesPerPage, sortedClans.size());
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Reputation Leaderboard ===");
        player.sendMessage(ChatColor.YELLOW + "Page " + page + " of " + totalPages);
        
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = sortedClans.get(i);
            int rank = i + 1;
            
            ChatColor rankColor;
            if (rank == 1) {
                rankColor = ChatColor.GOLD;
            } else if (rank == 2) {
                rankColor = ChatColor.GRAY;
            } else if (rank == 3) {
                rankColor = ChatColor.DARK_RED;
            } else {
                rankColor = ChatColor.WHITE;
            }
            
            player.sendMessage(rankColor + "#" + rank + ". " + ChatColor.YELLOW + entry.getKey() + 
                    ChatColor.WHITE + " - " + ChatColor.LIGHT_PURPLE + entry.getValue() + " reputation");
        }
        
        if (page < totalPages) {
            player.sendMessage(ChatColor.GRAY + "Use /clan reputation leaderboard " + (page + 1) + 
                    " to see the next page.");
        }
        
        // Show player's clan position if they're in a clan
        Clan playerClan = plugin.getStorageManager().getPlayerClan(player.getUniqueId());
        if (playerClan != null) {
            int rank = getClanRank(playerClan);
            if (rank > 0) {
                player.sendMessage(ChatColor.YELLOW + "Your clan " + ChatColor.GOLD + playerClan.getName() + 
                        ChatColor.YELLOW + " is ranked " + ChatColor.WHITE + "#" + rank + 
                        ChatColor.YELLOW + " with " + ChatColor.LIGHT_PURPLE + 
                        getReputation(playerClan) + ChatColor.YELLOW + " reputation.");
            }
        }
    }
    
    /**
     * Gets the reputation title for a clan.
     * 
     * @param clan The clan to check
     * @return The clan's reputation title
     */
    public String getReputationTitle(Clan clan) {
        int reputation = getReputation(clan);
        
        if (reputation >= 5000) return ChatColor.LIGHT_PURPLE + "Legendary";
        if (reputation >= 3000) return ChatColor.DARK_PURPLE + "Renowned";
        if (reputation >= 2000) return ChatColor.GOLD + "Distinguished";
        if (reputation >= 1000) return ChatColor.YELLOW + "Respected";
        if (reputation >= 500) return ChatColor.GREEN + "Recognized";
        if (reputation >= 200) return ChatColor.AQUA + "Known";
        if (reputation >= 100) return ChatColor.WHITE + "Emerging";
        return ChatColor.GRAY + "Newcomer";
    }
    
    /**
     * Gets the reputation level name without formatting for a clan.
     * 
     * @param clan The clan to check
     * @return The clan's reputation level name
     */
    public String getReputationLevelName(Clan clan) {
        int reputation = getReputation(clan);
        
        if (reputation >= 5000) return "Legendary";
        if (reputation >= 3000) return "Renowned";
        if (reputation >= 2000) return "Distinguished";
        if (reputation >= 1000) return "Respected";
        if (reputation >= 500) return "Recognized";
        if (reputation >= 200) return "Known";
        if (reputation >= 100) return "Emerging";
        return "Newcomer";
    }
    
    /**
     * Calculates reputation effects for various actions.
     * 
     * @param clan The clan to calculate effects for
     * @return A map of effects and their values
     */
    public Map<String, Integer> getReputationEffects(Clan clan) {
        int reputation = getReputation(clan);
        Map<String, Integer> effects = new HashMap<>();
        
        // Calculate effects based on reputation tier
        if (reputation >= 5000) {
            effects.put("territory_bonus", 5);
            effects.put("war_bonus", 20);
            effects.put("alliance_limit", 5);
            effects.put("tax_reduction", 20);
        } else if (reputation >= 3000) {
            effects.put("territory_bonus", 4);
            effects.put("war_bonus", 15);
            effects.put("alliance_limit", 4);
            effects.put("tax_reduction", 15);
        } else if (reputation >= 2000) {
            effects.put("territory_bonus", 3);
            effects.put("war_bonus", 10);
            effects.put("alliance_limit", 3);
            effects.put("tax_reduction", 10);
        } else if (reputation >= 1000) {
            effects.put("territory_bonus", 2);
            effects.put("war_bonus", 5);
            effects.put("alliance_limit", 2);
            effects.put("tax_reduction", 5);
        } else if (reputation >= 500) {
            effects.put("territory_bonus", 1);
            effects.put("war_bonus", 3);
            effects.put("alliance_limit", 1);
            effects.put("tax_reduction", 3);
        } else {
            effects.put("territory_bonus", 0);
            effects.put("war_bonus", 0);
            effects.put("alliance_limit", 1);
            effects.put("tax_reduction", 0);
        }
        
        return effects;
    }
    
    /**
     * Displays a clan's reputation info to a player.
     * 
     * @param player The player to display the info to
     * @param clan The clan to check
     */
    public void displayReputationInfo(Player player, Clan clan) {
        int reputation = getReputation(clan);
        int rank = getClanRank(clan);
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Reputation: " + clan.getName() + " ===");
        player.sendMessage(ChatColor.YELLOW + "Reputation: " + ChatColor.LIGHT_PURPLE + reputation);
        player.sendMessage(ChatColor.YELLOW + "Rank: " + ChatColor.WHITE + "#" + rank);
        player.sendMessage(ChatColor.YELLOW + "Title: " + getReputationTitle(clan));
        
        // Display reputation effects
        player.sendMessage(ChatColor.GOLD + "=== Reputation Effects ===");
        
        Map<String, Integer> effects = getReputationEffects(clan);
        for (Map.Entry<String, Integer> effect : effects.entrySet()) {
            player.sendMessage(ChatColor.YELLOW + formatEffectName(effect.getKey()) + ": " + 
                    ChatColor.WHITE + formatEffectValue(effect.getKey(), effect.getValue()));
        }
        
        // Show next title
        if (reputation < 5000) {
            String nextTitle;
            int required;
            
            if (reputation < 100) {
                nextTitle = ChatColor.WHITE + "Emerging";
                required = 100;
            } else if (reputation < 200) {
                nextTitle = ChatColor.AQUA + "Known";
                required = 200;
            } else if (reputation < 500) {
                nextTitle = ChatColor.GREEN + "Recognized";
                required = 500;
            } else if (reputation < 1000) {
                nextTitle = ChatColor.YELLOW + "Respected";
                required = 1000;
            } else if (reputation < 2000) {
                nextTitle = ChatColor.GOLD + "Distinguished";
                required = 2000;
            } else if (reputation < 3000) {
                nextTitle = ChatColor.DARK_PURPLE + "Renowned";
                required = 3000;
            } else {
                nextTitle = ChatColor.LIGHT_PURPLE + "Legendary";
                required = 5000;
            }
            
            int needed = required - reputation;
            player.sendMessage(ChatColor.YELLOW + "Next Title: " + nextTitle + 
                    ChatColor.YELLOW + " (" + needed + " more reputation needed)");
        }
    }
    
    /**
     * Formats an effect name for display.
     * 
     * @param effectName The raw effect name
     * @return The formatted effect name
     */
    private String formatEffectName(String effectName) {
        String[] parts = effectName.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            formatted.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1))
                    .append(" ");
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Formats an effect value for display.
     * 
     * @param effectName The effect name
     * @param value The effect value
     * @return The formatted effect value
     */
    private String formatEffectValue(String effectName, int value) {
        switch (effectName) {
            case "territory_bonus":
                return "+" + value + " additional claims";
            case "war_bonus":
                return "+" + value + "% war rewards";
            case "alliance_limit":
                return value + " maximum alliances";
            case "tax_reduction":
                return "-" + value + "% on territory upkeep";
            default:
                return String.valueOf(value);
        }
    }
}