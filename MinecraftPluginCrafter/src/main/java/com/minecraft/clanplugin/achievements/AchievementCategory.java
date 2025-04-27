package com.minecraft.clanplugin.achievements;

import org.bukkit.ChatColor;

/**
 * Categories for clan achievements.
 */
public enum AchievementCategory {
    GENERAL("General", ChatColor.WHITE),
    TERRITORY("Territory", ChatColor.GREEN),
    WAR("War", ChatColor.RED),
    ECONOMY("Economy", ChatColor.GOLD),
    SOCIAL("Social", ChatColor.AQUA),
    PROGRESSION("Progression", ChatColor.LIGHT_PURPLE);
    
    private final String displayName;
    private final ChatColor color;
    
    /**
     * Creates a new achievement category.
     * 
     * @param displayName The display name of the category
     * @param color The color associated with the category
     */
    AchievementCategory(String displayName, ChatColor color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    /**
     * Gets the display name of the category.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the color associated with the category.
     * 
     * @return The category color
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Gets the colored display name of the category.
     * 
     * @return The colored display name
     */
    public String getColoredName() {
        return color + displayName;
    }
}