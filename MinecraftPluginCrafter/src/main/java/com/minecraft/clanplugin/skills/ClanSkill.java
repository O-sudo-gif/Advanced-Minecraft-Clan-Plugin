package com.minecraft.clanplugin.skills;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a skill that clan members can learn and level up.
 */
public class ClanSkill {
    private String id;
    private String name;
    private String description;
    private SkillTree tree;
    private int maxLevel;
    private Map<Integer, Integer> levelCosts;
    private Map<Integer, Map<String, Integer>> levelEffects;
    private List<String> prerequisites;
    
    /**
     * Creates a new clan skill.
     * 
     * @param id The unique identifier for the skill
     * @param name The display name of the skill
     * @param description The description of the skill
     * @param tree The skill tree this skill belongs to
     * @param maxLevel The maximum level of the skill
     */
    public ClanSkill(String id, String name, String description, SkillTree tree, int maxLevel) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tree = tree;
        this.maxLevel = maxLevel;
        this.levelCosts = new HashMap<>();
        this.levelEffects = new HashMap<>();
        this.prerequisites = new ArrayList<>();
    }
    
    /**
     * Gets the unique identifier for the skill.
     * 
     * @return The skill ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name of the skill.
     * 
     * @return The skill name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the description of the skill.
     * 
     * @return The skill description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the skill tree this skill belongs to.
     * 
     * @return The skill tree
     */
    public SkillTree getTree() {
        return tree;
    }
    
    /**
     * Gets the maximum level of the skill.
     * 
     * @return The maximum level
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * Sets the cost to reach a level of the skill.
     * 
     * @param level The level to set the cost for
     * @param cost The cost in skill points
     */
    public void setLevelCost(int level, int cost) {
        if (level <= maxLevel) {
            levelCosts.put(level, cost);
        }
    }
    
    /**
     * Gets the cost to reach a level of the skill.
     * 
     * @param level The level to get the cost for
     * @return The cost in skill points
     */
    public int getLevelCost(int level) {
        return levelCosts.getOrDefault(level, 0);
    }
    
    /**
     * Adds an effect for a level of the skill.
     * 
     * @param level The level to add the effect to
     * @param effectName The name of the effect
     * @param value The value of the effect
     */
    public void addLevelEffect(int level, String effectName, int value) {
        if (level <= maxLevel) {
            Map<String, Integer> effects = levelEffects.getOrDefault(level, new HashMap<>());
            effects.put(effectName, value);
            levelEffects.put(level, effects);
        }
    }
    
    /**
     * Gets all effects for a level of the skill.
     * 
     * @param level The level to get effects for
     * @return A map of effect names to values
     */
    public Map<String, Integer> getLevelEffects(int level) {
        return new HashMap<>(levelEffects.getOrDefault(level, new HashMap<>()));
    }
    
    /**
     * Adds a prerequisite skill.
     * 
     * @param skillId The ID of the prerequisite skill
     */
    public void addPrerequisite(String skillId) {
        prerequisites.add(skillId);
    }
    
    /**
     * Gets all prerequisite skills.
     * 
     * @return A list of prerequisite skill IDs
     */
    public List<String> getPrerequisites() {
        return new ArrayList<>(prerequisites);
    }
    
    /**
     * Gets the colored display string for the skill.
     * 
     * @param currentLevel The current level of the skill
     * @param skillPoints Available skill points
     * @return The formatted display string
     */
    public String getDisplayString(int currentLevel, int skillPoints) {
        ChatColor nameColor = currentLevel > 0 ? ChatColor.GREEN : ChatColor.YELLOW;
        
        StringBuilder display = new StringBuilder();
        display.append(nameColor).append(name).append(" [").append(currentLevel).append("/").append(maxLevel).append("]\n")
               .append(ChatColor.GRAY).append(description).append("\n\n");
        
        if (currentLevel < maxLevel) {
            int nextLevel = currentLevel + 1;
            int cost = getLevelCost(nextLevel);
            
            display.append(ChatColor.YELLOW).append("Next Level Cost: ")
                   .append(skillPoints >= cost ? ChatColor.GREEN : ChatColor.RED)
                   .append(cost).append(" SP\n");
            
            // Show next level effects
            Map<String, Integer> nextEffects = getLevelEffects(nextLevel);
            if (!nextEffects.isEmpty()) {
                display.append(ChatColor.YELLOW).append("Effects at Level ").append(nextLevel).append(":\n");
                
                for (Map.Entry<String, Integer> effect : nextEffects.entrySet()) {
                    display.append(ChatColor.GRAY).append("- ")
                           .append(formatEffectName(effect.getKey())).append(": ")
                           .append(formatEffectValue(effect.getKey(), effect.getValue()))
                           .append("\n");
                }
            }
        } else {
            display.append(ChatColor.GREEN).append("Maximum level reached!\n");
        }
        
        // Show prerequisites
        if (!prerequisites.isEmpty() && currentLevel == 0) {
            display.append(ChatColor.YELLOW).append("Prerequisites:\n");
            
            for (String prereq : prerequisites) {
                display.append(ChatColor.GRAY).append("- ").append(prereq).append("\n");
            }
        }
        
        return display.toString();
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
        if (effectName.endsWith("_percent")) {
            return "+" + value + "%";
        } else if (effectName.contains("reduction")) {
            return "-" + value + "%";
        } else if (value > 0) {
            return "+" + value;
        } else {
            return String.valueOf(value);
        }
    }
}