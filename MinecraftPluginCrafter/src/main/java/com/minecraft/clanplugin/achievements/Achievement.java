package com.minecraft.clanplugin.achievements;

import org.bukkit.ChatColor;

/**
 * Represents a clan achievement that can be earned.
 */
public class Achievement {
    private String id;
    private String name;
    private String description;
    private AchievementCategory category;
    private AchievementDifficulty difficulty;
    private int experienceReward;
    private int reputationReward;
    
    /**
     * Creates a new achievement.
     * 
     * @param id The unique identifier for the achievement
     * @param name The display name of the achievement
     * @param description The description of how to earn the achievement
     * @param category The category of achievement
     * @param difficulty The difficulty level of the achievement
     * @param experienceReward The experience reward for earning the achievement
     * @param reputationReward The reputation reward for earning the achievement
     */
    public Achievement(String id, String name, String description, AchievementCategory category, 
                      AchievementDifficulty difficulty, int experienceReward, int reputationReward) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.difficulty = difficulty;
        this.experienceReward = experienceReward;
        this.reputationReward = reputationReward;
    }
    
    /**
     * Gets the unique identifier for the achievement.
     * 
     * @return The achievement ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of the achievement.
     * 
     * @return The achievement name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of how to earn the achievement.
     * 
     * @return The achievement description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the category of achievement.
     * 
     * @return The achievement category
     */
    public AchievementCategory getCategory() {
        return category;
    }
    
    /**
     * Gets the difficulty level of the achievement.
     * 
     * @return The achievement difficulty
     */
    public AchievementDifficulty getDifficulty() {
        return difficulty;
    }
    
    /**
     * Gets the experience reward for earning the achievement.
     * 
     * @return The experience reward
     */
    public int getExperienceReward() {
        return experienceReward;
    }
    
    /**
     * Gets the reputation reward for earning the achievement.
     * 
     * @return The reputation reward
     */
    public int getReputationReward() {
        return reputationReward;
    }
    
    /**
     * Gets the colored display string for the achievement.
     * 
     * @param unlocked Whether the achievement has been unlocked
     * @return The formatted display string
     */
    public String getDisplayString(boolean unlocked) {
        ChatColor nameColor = unlocked ? ChatColor.GREEN : ChatColor.GRAY;
        ChatColor descColor = unlocked ? ChatColor.YELLOW : ChatColor.DARK_GRAY;
        
        StringBuilder display = new StringBuilder();
        display.append(nameColor).append(name).append(" ").append(getDifficultyStars()).append("\n")
               .append(descColor).append(description).append("\n");
        
        if (unlocked) {
            display.append(ChatColor.GOLD).append("Rewards: ")
                   .append(ChatColor.AQUA).append(experienceReward).append(" XP, ")
                   .append(ChatColor.LIGHT_PURPLE).append(reputationReward).append(" Reputation");
        } else {
            display.append(ChatColor.GRAY).append("Locked");
        }
        
        return display.toString();
    }
    
    /**
     * Gets a star representation of the difficulty.
     * 
     * @return A string of stars representing difficulty
     */
    private String getDifficultyStars() {
        ChatColor starColor;
        
        switch (difficulty) {
            case EASY:
                starColor = ChatColor.GREEN;
                break;
            case MEDIUM:
                starColor = ChatColor.YELLOW;
                break;
            case HARD:
                starColor = ChatColor.RED;
                break;
            case LEGENDARY:
                starColor = ChatColor.LIGHT_PURPLE;
                break;
            default:
                starColor = ChatColor.GRAY;
                break;
        }
        
        int stars = difficulty.getStars();
        return starColor + "★".repeat(stars);
    }
}