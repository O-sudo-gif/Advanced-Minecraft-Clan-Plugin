package com.minecraft.clanplugin.progression;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages clan progression, levels, and experience.
 */
public class ProgressionManager {
    private ClanPlugin plugin;
    private Map<Integer, ClanLevel> levels;
    private int maxLevel;
    
    /**
     * Creates a new progression manager.
     * 
     * @param plugin The clan plugin instance
     */
    public ProgressionManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.levels = new HashMap<>();
        loadLevels();
    }
    
    /**
     * Loads level definitions from configuration.
     */
    private void loadLevels() {
        ConfigurationSection levelsSection = plugin.getConfig().getConfigurationSection("progression.levels");
        
        if (levelsSection == null) {
            // Default level setup if not in config
            setupDefaultLevels();
            return;
        }
        
        for (String levelKey : levelsSection.getKeys(false)) {
            int level = Integer.parseInt(levelKey);
            int expRequired = levelsSection.getInt(levelKey + ".exp_required");
            
            ClanLevel clanLevel = new ClanLevel(level, expRequired);
            
            // Load benefits
            ConfigurationSection benefitsSection = levelsSection.getConfigurationSection(levelKey + ".benefits");
            if (benefitsSection != null) {
                for (String benefit : benefitsSection.getKeys(false)) {
                    int value = benefitsSection.getInt(benefit);
                    clanLevel.addBenefit(benefit, value);
                }
            }
            
            levels.put(level, clanLevel);
            
            if (level > maxLevel) {
                maxLevel = level;
            }
        }
    }
    
    /**
     * Sets up default levels if not in configuration.
     */
    private void setupDefaultLevels() {
        // Level 1
        ClanLevel level1 = new ClanLevel(1, 0);
        level1.addBenefit("max_members", 10);
        level1.addBenefit("max_territory", 5);
        levels.put(1, level1);
        
        // Level 2
        ClanLevel level2 = new ClanLevel(2, 1000);
        level2.addBenefit("max_members", 15);
        level2.addBenefit("max_territory", 10);
        level2.addBenefit("income_bonus", 5);
        levels.put(2, level2);
        
        // Level 3
        ClanLevel level3 = new ClanLevel(3, 3000);
        level3.addBenefit("max_members", 20);
        level3.addBenefit("max_territory", 15);
        level3.addBenefit("income_bonus", 10);
        level3.addBenefit("influence_bonus", 5);
        levels.put(3, level3);
        
        // Level 4
        ClanLevel level4 = new ClanLevel(4, 7000);
        level4.addBenefit("max_members", 25);
        level4.addBenefit("max_territory", 20);
        level4.addBenefit("income_bonus", 15);
        level4.addBenefit("influence_bonus", 10);
        level4.addBenefit("armor_bonus", 1);
        levels.put(4, level4);
        
        // Level 5
        ClanLevel level5 = new ClanLevel(5, 15000);
        level5.addBenefit("max_members", 30);
        level5.addBenefit("max_territory", 25);
        level5.addBenefit("income_bonus", 20);
        level5.addBenefit("influence_bonus", 15);
        level5.addBenefit("armor_bonus", 2);
        level5.addBenefit("strength_bonus", 1);
        levels.put(5, level5);
        
        maxLevel = 5;
    }
    
    /**
     * Adds experience to a clan.
     * 
     * @param clan The clan to add experience to
     * @param amount The amount of experience to add
     * @return True if the clan leveled up, false otherwise
     */
    public boolean addExperience(Clan clan, int amount) {
        int currentExp = clan.getExperience();
        int newExp = currentExp + amount;
        clan.setExperience(newExp);
        
        int currentLevel = clan.getLevel();
        int newLevel = calculateLevel(newExp);
        
        if (newLevel > currentLevel) {
            clan.setLevel(newLevel);
            return true;
        }
        
        return false;
    }
    
    /**
     * Calculates the level based on experience.
     * 
     * @param experience The experience to calculate level from
     * @return The level
     */
    public int calculateLevel(int experience) {
        for (int i = maxLevel; i > 0; i--) {
            ClanLevel level = levels.get(i);
            if (experience >= level.getExperienceRequired()) {
                return i;
            }
        }
        
        return 1; // Default to level 1
    }
    
    /**
     * Gets the experience required for the next level.
     * 
     * @param clan The clan to check
     * @return The experience required for the next level, or -1 if at max level
     */
    public int getExperienceForNextLevel(Clan clan) {
        int currentLevel = clan.getLevel();
        
        if (currentLevel >= maxLevel) {
            return -1; // Already at max level
        }
        
        ClanLevel nextLevel = levels.get(currentLevel + 1);
        return nextLevel.getExperienceRequired() - clan.getExperience();
    }
    
    /**
     * Gets the specified benefit value for a clan based on its level.
     * 
     * @param clan The clan to check
     * @param benefitName The name of the benefit
     * @return The benefit value
     */
    public int getClanBenefit(Clan clan, String benefitName) {
        ClanLevel level = levels.get(clan.getLevel());
        return level.getBenefitValue(benefitName);
    }
    
    /**
     * Gets all benefits for a clan based on its level.
     * 
     * @param clan The clan to check
     * @return A map of all benefits and their values
     */
    public Map<String, Integer> getAllClanBenefits(Clan clan) {
        ClanLevel level = levels.get(clan.getLevel());
        return level.getAllBenefits();
    }
    
    /**
     * Gets a list of all level information.
     * 
     * @return A list of all level information
     */
    public List<String> getLevelInformation() {
        List<String> info = new ArrayList<>();
        
        for (int i = 1; i <= maxLevel; i++) {
            ClanLevel level = levels.get(i);
            
            StringBuilder levelInfo = new StringBuilder();
            levelInfo.append(ChatColor.GOLD).append("Level ").append(i).append(": ")
                    .append(ChatColor.YELLOW).append(level.getExperienceRequired()).append(" XP");
            
            info.add(levelInfo.toString());
            
            for (Map.Entry<String, Integer> benefit : level.getAllBenefits().entrySet()) {
                info.add(ChatColor.GRAY + "  - " + formatBenefitName(benefit.getKey()) + ": " + benefit.getValue());
            }
        }
        
        return info;
    }
    
    /**
     * Formats a benefit name for display.
     * 
     * @param name The raw benefit name
     * @return The formatted benefit name
     */
    public String formatBenefitName(String name) {
        String[] parts = name.split("_");
        StringBuilder formatted = new StringBuilder();
        
        for (String part : parts) {
            formatted.append(part.substring(0, 1).toUpperCase())
                    .append(part.substring(1))
                    .append(" ");
        }
        
        return formatted.toString().trim();
    }
    
    /**
     * Gets the required experience for a specific level.
     * 
     * @param level The level to check
     * @return The required experience for the level
     */
    public int getRequiredExperienceForLevel(int level) {
        ClanLevel clanLevel = levels.get(level);
        if (clanLevel != null) {
            return clanLevel.getExperienceRequired();
        }
        return 0;
    }
    
    /**
     * Gets the maximum level available.
     * 
     * @return The maximum level
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Gets the benefits for a specific level.
     * 
     * @param level The level to get benefits for
     * @return Map of benefits for the level
     */
    public Map<String, Integer> getLevelBenefits(int level) {
        ClanLevel clanLevel = levels.get(level);
        if (clanLevel != null) {
            return clanLevel.getAllBenefits();
        }
        return new HashMap<>();
    }
    
    /**
     * Sends level progress information to a player.
     * 
     * @param player The player to send information to
     * @param clan The clan to check
     */
    public void sendProgressInfo(Player player, Clan clan) {
        int currentLevel = clan.getLevel();
        int currentExp = clan.getExperience();
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Level Information ===");
        player.sendMessage(ChatColor.YELLOW + "Current Level: " + ChatColor.WHITE + currentLevel);
        player.sendMessage(ChatColor.YELLOW + "Current Experience: " + ChatColor.WHITE + currentExp);
        
        if (currentLevel < maxLevel) {
            ClanLevel nextLevel = levels.get(currentLevel + 1);
            int expForNextLevel = nextLevel.getExperienceRequired();
            int remaining = expForNextLevel - currentExp;
            
            player.sendMessage(ChatColor.YELLOW + "Experience for Level " + (currentLevel + 1) + ": " 
                    + ChatColor.WHITE + expForNextLevel);
            player.sendMessage(ChatColor.YELLOW + "Remaining: " + ChatColor.WHITE + remaining);
            
            // Show a progress bar
            int progressBarLength = 20;
            int progress = (int) ((double) currentExp / expForNextLevel * progressBarLength);
            
            StringBuilder progressBar = new StringBuilder(ChatColor.YELLOW + "[");
            for (int i = 0; i < progressBarLength; i++) {
                if (i < progress) {
                    progressBar.append(ChatColor.GREEN + "■");
                } else {
                    progressBar.append(ChatColor.GRAY + "■");
                }
            }
            progressBar.append(ChatColor.YELLOW + "]");
            
            player.sendMessage(progressBar.toString());
        } else {
            player.sendMessage(ChatColor.GREEN + "Maximum level reached!");
        }
        
        // Show benefits
        player.sendMessage(ChatColor.GOLD + "=== Current Benefits ===");
        for (Map.Entry<String, Integer> benefit : getAllClanBenefits(clan).entrySet()) {
            player.sendMessage(ChatColor.YELLOW + formatBenefitName(benefit.getKey()) + ": " 
                    + ChatColor.WHITE + benefit.getValue());
        }
    }
}