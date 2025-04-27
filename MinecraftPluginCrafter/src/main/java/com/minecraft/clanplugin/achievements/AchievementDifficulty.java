package com.minecraft.clanplugin.achievements;

import org.bukkit.ChatColor;

/**
 * Difficulty levels for clan achievements.
 */
public enum AchievementDifficulty {
    EASY(1, ChatColor.GREEN, 1),
    MEDIUM(2, ChatColor.YELLOW, 2),
    HARD(3, ChatColor.RED, 3),
    EPIC(4, ChatColor.LIGHT_PURPLE, 4),
    LEGENDARY(5, ChatColor.GOLD, 5);
    
    private final int stars;
    private final ChatColor color;
    private final int points;
    
    /**
     * Creates a new achievement difficulty.
     * 
     * @param stars The number of stars representing the difficulty
     * @param color The color associated with this difficulty
     * @param points The point value for firework effects and other visuals
     */
    AchievementDifficulty(int stars, ChatColor color, int points) {
        this.stars = stars;
        this.color = color;
        this.points = points;
    }
    
    /**
     * Gets the number of stars representing the difficulty.
     * 
     * @return The number of stars
     */
    public int getStars() {
        return stars;
    }
    
    /**
     * Gets the color associated with this difficulty.
     * 
     * @return The ChatColor for this difficulty
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Gets the point value for this difficulty.
     * Used for determining visual effects intensity.
     * 
     * @return The point value
     */
    public int getPoints() {
        return points;
    }
}