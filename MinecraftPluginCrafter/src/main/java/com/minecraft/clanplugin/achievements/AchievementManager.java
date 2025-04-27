package com.minecraft.clanplugin.achievements;

import com.minecraft.clanplugin.ClanPlugin;
import com.minecraft.clanplugin.models.Clan;
import com.minecraft.clanplugin.utils.MessageUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages clan achievements, tracking, and rewards.
 */
public class AchievementManager {
    private ClanPlugin plugin;
    private Map<String, Achievement> achievements;
    private File achievementsFile;
    private FileConfiguration achievementsConfig;
    
    /**
     * Creates a new achievement manager.
     * 
     * @param plugin The clan plugin instance
     */
    public AchievementManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.achievements = new HashMap<>();
        
        // Initialize achievements file
        this.achievementsFile = new File(plugin.getDataFolder(), "achievements.yml");
        if (!achievementsFile.exists()) {
            plugin.saveResource("achievements.yml", false);
        }
        
        this.achievementsConfig = YamlConfiguration.loadConfiguration(achievementsFile);
        
        // Load achievements
        loadAchievements();
    }
    
    /**
     * Loads achievements from configuration file.
     */
    private void loadAchievements() {
        ConfigurationSection achievementsSection = plugin.getConfig().getConfigurationSection("achievements");
        
        if (achievementsSection == null) {
            // Create default achievements if not configured
            setupDefaultAchievements();
            return;
        }
        
        for (String id : achievementsSection.getKeys(false)) {
            ConfigurationSection section = achievementsSection.getConfigurationSection(id);
            
            if (section != null) {
                String name = section.getString("name");
                String description = section.getString("description");
                String categoryStr = section.getString("category", "GENERAL");
                String difficultyStr = section.getString("difficulty", "EASY");
                int expReward = section.getInt("exp_reward", 100);
                int repReward = section.getInt("rep_reward", 10);
                
                AchievementCategory category;
                try {
                    category = AchievementCategory.valueOf(categoryStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    category = AchievementCategory.GENERAL;
                }
                
                AchievementDifficulty difficulty;
                try {
                    difficulty = AchievementDifficulty.valueOf(difficultyStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    difficulty = AchievementDifficulty.EASY;
                }
                
                Achievement achievement = new Achievement(id, name, description, category, difficulty, expReward, repReward);
                achievements.put(id, achievement);
            }
        }
    }
    
    /**
     * Sets up default achievements if not configured.
     */
    private void setupDefaultAchievements() {
        // General achievements
        addAchievement("first_clan", "Founding Father", "Create your first clan", 
                AchievementCategory.GENERAL, AchievementDifficulty.EASY, 100, 10);
        
        addAchievement("recruit_members", "Recruitment Drive", "Have at least 5 members in your clan", 
                AchievementCategory.GENERAL, AchievementDifficulty.EASY, 200, 20);
                
        addAchievement("clan_master", "Clan Master", "Reach level 5 with your clan", 
                AchievementCategory.PROGRESSION, AchievementDifficulty.HARD, 1000, 100);
        
        // Territory achievements
        addAchievement("first_claim", "Stake Your Claim", "Claim your first territory", 
                AchievementCategory.TERRITORY, AchievementDifficulty.EASY, 100, 10);
                
        addAchievement("territory_empire", "Territorial Empire", "Own 20 territory chunks", 
                AchievementCategory.TERRITORY, AchievementDifficulty.HARD, 800, 80);
                
        addAchievement("fortified", "Fortified", "Place 3 level 3 flags in your territory", 
                AchievementCategory.TERRITORY, AchievementDifficulty.MEDIUM, 500, 50);
        
        // War achievements
        addAchievement("first_blood", "First Blood", "Win your first clan war", 
                AchievementCategory.WAR, AchievementDifficulty.MEDIUM, 300, 30);
                
        addAchievement("war_machine", "War Machine", "Win 5 clan wars", 
                AchievementCategory.WAR, AchievementDifficulty.HARD, 1000, 100);
                
        addAchievement("legendary_warrior", "Legendary Warrior", "Get 50 kills in clan wars", 
                AchievementCategory.WAR, AchievementDifficulty.LEGENDARY, 2000, 200);
        
        // Economy achievements
        addAchievement("first_deposit", "Investor", "Make your first deposit to clan bank", 
                AchievementCategory.ECONOMY, AchievementDifficulty.EASY, 100, 10);
                
        addAchievement("wealthy_clan", "Wealthy Clan", "Accumulate 10,000 in clan bank", 
                AchievementCategory.ECONOMY, AchievementDifficulty.HARD, 800, 80);
                
        addAchievement("tax_collector", "Tax Collector", "Collect taxes from members 10 times", 
                AchievementCategory.ECONOMY, AchievementDifficulty.MEDIUM, 400, 40);
        
        // Social achievements
        addAchievement("alliance_builder", "Alliance Builder", "Form your first alliance", 
                AchievementCategory.SOCIAL, AchievementDifficulty.EASY, 150, 15);
                
        addAchievement("diplomatic_master", "Diplomatic Master", "Have 3 alliances at once", 
                AchievementCategory.SOCIAL, AchievementDifficulty.MEDIUM, 500, 50);
                
        addAchievement("community_leader", "Community Leader", "Maintain a clan for 30 days", 
                AchievementCategory.SOCIAL, AchievementDifficulty.LEGENDARY, 1500, 150);
    }
    
    /**
     * Adds a new achievement to the manager.
     */
    private void addAchievement(String id, String name, String description, AchievementCategory category,
                               AchievementDifficulty difficulty, int expReward, int repReward) {
        Achievement achievement = new Achievement(id, name, description, category, difficulty, expReward, repReward);
        achievements.put(id, achievement);
    }
    
    /**
     * Unlocks an achievement for a clan if not already unlocked.
     * 
     * @param clan The clan that earned the achievement
     * @param achievementId The ID of the achievement to unlock
     * @return True if the achievement was newly unlocked, false if already unlocked
     */
    public boolean unlockAchievement(Clan clan, String achievementId) {
        Achievement achievement = achievements.get(achievementId);
        
        if (achievement == null) {
            return false;
        }
        
        if (hasAchievement(clan, achievementId)) {
            return false;
        }
        
        // Save the achievement
        String path = "clans." + clan.getName() + ".achievements." + achievementId;
        achievementsConfig.set(path, System.currentTimeMillis());
        saveAchievements();
        
        // Award rewards
        plugin.getProgressionManager().addExperience(clan, achievement.getExperienceReward());
        plugin.getReputationManager().addReputation(clan, achievement.getReputationReward());
        
        // Notify clan members
        String message = ChatColor.GOLD + "Achievement Unlocked: " + achievement.getName() + "\n" +
                ChatColor.YELLOW + achievement.getDescription() + "\n" +
                ChatColor.AQUA + "Rewards: " + achievement.getExperienceReward() + " XP, " +
                achievement.getReputationReward() + " Reputation";
        
        MessageUtils.notifyClan(clan, message);
        
        return true;
    }
    
    /**
     * Checks if a clan has unlocked an achievement.
     * 
     * @param clan The clan to check
     * @param achievementId The ID of the achievement
     * @return True if the clan has unlocked the achievement
     */
    public boolean hasAchievement(Clan clan, String achievementId) {
        String path = "clans." + clan.getName() + ".achievements." + achievementId;
        return achievementsConfig.contains(path);
    }
    
    /**
     * Gets the time when a clan unlocked an achievement.
     * 
     * @param clan The clan to check
     * @param achievementId The ID of the achievement
     * @return The time the achievement was unlocked, or 0 if not unlocked
     */
    public long getAchievementUnlockTime(Clan clan, String achievementId) {
        String path = "clans." + clan.getName() + ".achievements." + achievementId;
        return achievementsConfig.getLong(path, 0);
    }
    
    /**
     * Gets all achievements a clan has unlocked.
     * 
     * @param clan The clan to check
     * @return A set of achievement IDs the clan has unlocked
     */
    public Set<String> getUnlockedAchievements(Clan clan) {
        Set<String> unlocked = new HashSet<>();
        
        String path = "clans." + clan.getName() + ".achievements";
        ConfigurationSection section = achievementsConfig.getConfigurationSection(path);
        
        if (section != null) {
            unlocked.addAll(section.getKeys(false));
        }
        
        return unlocked;
    }
    
    /**
     * Gets all achievements in a specific category.
     * 
     * @param category The category to filter by
     * @return A list of achievements in the category
     */
    public List<Achievement> getAchievementsByCategory(AchievementCategory category) {
        List<Achievement> filtered = new ArrayList<>();
        
        for (Achievement achievement : achievements.values()) {
            if (achievement.getCategory() == category) {
                filtered.add(achievement);
            }
        }
        
        return filtered;
    }
    
    /**
     * Gets an achievement by its ID.
     * 
     * @param id The achievement ID
     * @return The achievement, or null if not found
     */
    public Achievement getAchievement(String id) {
        return achievements.get(id);
    }
    
    /**
     * Gets all available achievements.
     * 
     * @return A collection of all achievements
     */
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }
    
    /**
     * Gets the number of achievements a clan has unlocked.
     * 
     * @param clan The clan to check
     * @return The number of unlocked achievements
     */
    public int getUnlockedCount(Clan clan) {
        return getUnlockedAchievements(clan).size();
    }
    
    /**
     * Gets the completion percentage for achievements.
     * 
     * @param clan The clan to check
     * @return The percentage of achievements completed
     */
    public double getCompletionPercentage(Clan clan) {
        int unlocked = getUnlockedCount(clan);
        int total = achievements.size();
        
        return (double) unlocked / total * 100;
    }
    
    /**
     * Displays achievement progress to a player.
     * 
     * @param player The player to display progress to
     * @param clan The clan to check
     */
    public void displayAchievementProgress(Player player, Clan clan) {
        int unlocked = getUnlockedCount(clan);
        int total = achievements.size();
        double percentage = getCompletionPercentage(clan);
        
        player.sendMessage(ChatColor.GOLD + "=== Clan Achievements ===");
        player.sendMessage(ChatColor.YELLOW + "Progress: " + ChatColor.WHITE + 
                unlocked + "/" + total + " (" + String.format("%.1f", percentage) + "%)");
        
        // Display progress bar
        int barLength = 20;
        int progress = (int) (percentage / 100 * barLength);
        
        StringBuilder progressBar = new StringBuilder(ChatColor.YELLOW + "[");
        for (int i = 0; i < barLength; i++) {
            if (i < progress) {
                progressBar.append(ChatColor.GREEN + "■");
            } else {
                progressBar.append(ChatColor.GRAY + "■");
            }
        }
        progressBar.append(ChatColor.YELLOW + "]");
        
        player.sendMessage(progressBar.toString());
        
        // Display category summary
        player.sendMessage(ChatColor.GOLD + "Categories:");
        
        for (AchievementCategory category : AchievementCategory.values()) {
            List<Achievement> categoryAchievements = getAchievementsByCategory(category);
            int categoryTotal = categoryAchievements.size();
            
            if (categoryTotal == 0) {
                continue;
            }
            
            int categoryUnlocked = 0;
            for (Achievement achievement : categoryAchievements) {
                if (hasAchievement(clan, achievement.getId())) {
                    categoryUnlocked++;
                }
            }
            
            double categoryPercentage = (double) categoryUnlocked / categoryTotal * 100;
            
            player.sendMessage(category.getColoredName() + ChatColor.WHITE + ": " + 
                    categoryUnlocked + "/" + categoryTotal + " (" + 
                    String.format("%.1f", categoryPercentage) + "%)");
        }
    }
    
    /**
     * Saves the achievements configuration.
     */
    private void saveAchievements() {
        try {
            achievementsConfig.save(achievementsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save achievements data!");
            e.printStackTrace();
        }
    }
}