package com.minecraft.clanplugin.skills;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores skill levels and points for a clan member.
 */
public class MemberSkills {
    private UUID playerUUID;
    private Map<String, Integer> skillLevels;
    private int skillPoints;
    private SkillTree specialization;
    
    /**
     * Creates a new member skills container.
     * 
     * @param playerUUID The UUID of the player
     */
    public MemberSkills(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.skillLevels = new HashMap<>();
        this.skillPoints = 0;
        this.specialization = null;
    }
    
    /**
     * Gets the UUID of the player.
     * 
     * @return The player UUID
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Gets the level of a skill.
     * 
     * @param skillId The ID of the skill
     * @return The skill level
     */
    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }
    
    /**
     * Sets the level of a skill.
     * 
     * @param skillId The ID of the skill
     * @param level The new level
     */
    public void setSkillLevel(String skillId, int level) {
        skillLevels.put(skillId, Math.max(0, level));
    }
    
    /**
     * Increases the level of a skill.
     * 
     * @param skillId The ID of the skill
     * @param amount The amount to increase by
     */
    public void increaseSkillLevel(String skillId, int amount) {
        int currentLevel = getSkillLevel(skillId);
        setSkillLevel(skillId, currentLevel + amount);
    }
    
    /**
     * Checks if a skill can be leveled up.
     * 
     * @param skill The skill to check
     * @param skillManager The skill manager instance
     * @return True if the skill can be leveled up
     */
    public boolean canLevelUp(ClanSkill skill, SkillManager skillManager) {
        int currentLevel = getSkillLevel(skill.getId());
        
        // Check if at max level
        if (currentLevel >= skill.getMaxLevel()) {
            return false;
        }
        
        // Check if enough skill points
        int nextLevel = currentLevel + 1;
        int cost = skill.getLevelCost(nextLevel);
        if (skillPoints < cost) {
            return false;
        }
        
        // Check prerequisites
        for (String prereqId : skill.getPrerequisites()) {
            ClanSkill prereqSkill = skillManager.getSkill(prereqId);
            if (prereqSkill == null) {
                continue;
            }
            
            int prereqLevel = getSkillLevel(prereqId);
            if (prereqLevel == 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Levels up a skill.
     * 
     * @param skill The skill to level up
     * @return True if the skill was successfully leveled up
     */
    public boolean levelUpSkill(ClanSkill skill) {
        int currentLevel = getSkillLevel(skill.getId());
        
        // Check if at max level
        if (currentLevel >= skill.getMaxLevel()) {
            return false;
        }
        
        // Check if enough skill points
        int nextLevel = currentLevel + 1;
        int cost = skill.getLevelCost(nextLevel);
        if (skillPoints < cost) {
            return false;
        }
        
        // Spend skill points and level up
        skillPoints -= cost;
        increaseSkillLevel(skill.getId(), 1);
        
        return true;
    }
    
    /**
     * Gets the available skill points.
     * 
     * @return The number of skill points
     */
    public int getSkillPoints() {
        return skillPoints;
    }
    
    /**
     * Sets the available skill points.
     * 
     * @param points The new number of skill points
     */
    public void setSkillPoints(int points) {
        this.skillPoints = Math.max(0, points);
    }
    
    /**
     * Adds skill points.
     * 
     * @param points The number of points to add
     */
    public void addSkillPoints(int points) {
        if (points > 0) {
            this.skillPoints += points;
        }
    }
    
    /**
     * Gets the specialization tree.
     * 
     * @return The specialization tree, or null if not specialized
     */
    public SkillTree getSpecialization() {
        return specialization;
    }
    
    /**
     * Sets the specialization tree.
     * 
     * @param specialization The new specialization tree
     */
    public void setSpecialization(SkillTree specialization) {
        this.specialization = specialization;
    }
    
    /**
     * Gets the number of skills with at least one level.
     * 
     * @return The number of learned skills
     */
    public int getLearnedSkillCount() {
        int count = 0;
        for (int level : skillLevels.values()) {
            if (level > 0) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Gets the total levels invested across all skills.
     * 
     * @return The total skill levels
     */
    public int getTotalSkillLevels() {
        int total = 0;
        for (int level : skillLevels.values()) {
            total += level;
        }
        return total;
    }
    
    /**
     * Gets the skill levels in a particular tree.
     * 
     * @param tree The skill tree to check
     * @param skillManager The skill manager instance
     * @return The total skill levels in the tree
     */
    public int getTreeSkillLevels(SkillTree tree, SkillManager skillManager) {
        int total = 0;
        
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            ClanSkill skill = skillManager.getSkill(entry.getKey());
            
            if (skill != null && skill.getTree() == tree) {
                total += entry.getValue();
            }
        }
        
        return total;
    }
    
    /**
     * Calculates if specialized in a tree based on skill distribution.
     * 
     * @param skillManager The skill manager instance
     * @return The calculated specialization, or null if not specialized
     */
    public SkillTree calculateSpecialization(SkillManager skillManager) {
        int totalLevels = getTotalSkillLevels();
        
        if (totalLevels < 5) {
            // Not enough total levels to specialize
            return null;
        }
        
        SkillTree dominantTree = null;
        int highestLevels = 0;
        
        for (SkillTree tree : SkillTree.values()) {
            int treeLevels = getTreeSkillLevels(tree, skillManager);
            
            if (treeLevels > highestLevels) {
                highestLevels = treeLevels;
                dominantTree = tree;
            }
        }
        
        // Check if dominant tree has at least 50% of all levels
        if (dominantTree != null && highestLevels >= totalLevels * 0.5) {
            return dominantTree;
        }
        
        return null;
    }
    
    /**
     * Gets all skill levels.
     * 
     * @return A map of skill IDs to levels
     */
    public Map<String, Integer> getSkillLevels() {
        return new HashMap<>(skillLevels);
    }
    
    /**
     * Gets the effect value for a specific skill effect across all skills.
     * 
     * @param effectName The name of the effect
     * @param skillManager The skill manager instance
     * @return The total effect value
     */
    public int getTotalEffectValue(String effectName, SkillManager skillManager) {
        int total = 0;
        
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            int level = entry.getValue();
            
            if (level > 0) {
                ClanSkill skill = skillManager.getSkill(entry.getKey());
                
                if (skill != null) {
                    Map<String, Integer> effects = skill.getLevelEffects(level);
                    
                    if (effects.containsKey(effectName)) {
                        total += effects.get(effectName);
                    }
                }
            }
        }
        
        // Apply specialization bonus if applicable
        if (specialization != null) {
            switch (effectName) {
                // General Skill Trees
                case "damage_bonus":
                case "crit_chance":
                    if (specialization == SkillTree.COMBAT || specialization == SkillTree.HUNTER) {
                        total += (int) (total * 0.2); // 20% bonus for combat/hunter specialization
                    }
                    break;
                case "territory_influence":
                case "flag_strength":
                    if (specialization == SkillTree.TERRITORY || specialization == SkillTree.BUILDER) {
                        total += (int) (total * 0.2); // 20% bonus for territory/builder specialization
                    }
                    break;
                case "income_bonus":
                case "cost_reduction":
                    if (specialization == SkillTree.ECONOMY || 
                        specialization == SkillTree.MINER || 
                        specialization == SkillTree.FARMER) {
                        total += (int) (total * 0.2); // 20% bonus for economy/miner/farmer specialization
                    }
                    break;
                case "alliance_bonus":
                case "reputation_gain":
                    if (specialization == SkillTree.DIPLOMACY) {
                        total += (int) (total * 0.2); // 20% bonus for diplomacy specialization
                    }
                    break;
                    
                // Role-based specializations
                case "mining_speed":
                case "ore_yield":
                    if (specialization == SkillTree.MINER) {
                        total += (int) (total * 0.3); // 30% bonus for miner specialization
                    }
                    break;
                case "crop_growth":
                case "food_yield":
                    if (specialization == SkillTree.FARMER) {
                        total += (int) (total * 0.3); // 30% bonus for farmer specialization
                    }
                    break;
                case "build_speed":
                case "resource_conservation":
                    if (specialization == SkillTree.BUILDER) {
                        total += (int) (total * 0.3); // 30% bonus for builder specialization
                    }
                    break;
                case "bow_damage":
                case "mob_loot":
                    if (specialization == SkillTree.HUNTER) {
                        total += (int) (total * 0.3); // 30% bonus for hunter specialization
                    }
                    break;
            }
        }
        
        return total;
    }
}