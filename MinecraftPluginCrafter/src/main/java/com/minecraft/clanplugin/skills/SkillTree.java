package com.minecraft.clanplugin.skills;

import org.bukkit.ChatColor;

/**
 * Represents a category of skills that clan members can specialize in.
 * Includes both general skill trees and specific role-based specializations.
 */
public enum SkillTree {
    // General skill trees
    COMBAT("Combat", "Combat and PvP focused skills", ChatColor.RED),
    TERRITORY("Territory", "Territory and land management skills", ChatColor.GREEN),
    ECONOMY("Economy", "Economy and resource management skills", ChatColor.GOLD),
    DIPLOMACY("Diplomacy", "Diplomacy and alliance focused skills", ChatColor.AQUA),
    UTILITY("Utility", "Miscellaneous utility skills", ChatColor.LIGHT_PURPLE),
    
    // Role-based skill trees (tied to player activities)
    MINER("Miner", "Mining and ore processing specialization", ChatColor.GRAY),
    FARMER("Farmer", "Farming and food production specialization", ChatColor.DARK_GREEN),
    BUILDER("Builder", "Construction and architecture specialization", ChatColor.YELLOW),
    HUNTER("Hunter", "Hunting and mob combat specialization", ChatColor.DARK_RED);
    
    private final String displayName;
    private final String description;
    private final ChatColor color;
    
    /**
     * Creates a new skill tree.
     * 
     * @param displayName The display name of the tree
     * @param description The description of the tree
     * @param color The color associated with the tree
     */
    SkillTree(String displayName, String description, ChatColor color) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
    }
    
    /**
     * Gets the display name of the tree.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the description of the tree.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the color associated with the tree.
     * 
     * @return The color
     */
    public ChatColor getColor() {
        return color;
    }
    
    /**
     * Gets the colored display name of the tree.
     * 
     * @return The colored display name
     */
    public String getColoredName() {
        return color + displayName;
    }
}