package com.minecraft.clanplugin.progression;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a clan level with associated benefits.
 */
public class ClanLevel {
    private int level;
    private int experienceRequired;
    private Map<String, Integer> benefits;
    
    /**
     * Creates a new clan level.
     * 
     * @param level The level number
     * @param experienceRequired The experience required to reach this level
     */
    public ClanLevel(int level, int experienceRequired) {
        this.level = level;
        this.experienceRequired = experienceRequired;
        this.benefits = new HashMap<>();
    }
    
    /**
     * Gets the level number.
     * 
     * @return The level number
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Gets the experience required to reach this level.
     * 
     * @return The experience required
     */
    public int getExperienceRequired() {
        return experienceRequired;
    }
    
    /**
     * Adds a benefit to this level.
     * 
     * @param benefitName The name of the benefit
     * @param value The value of the benefit
     */
    public void addBenefit(String benefitName, int value) {
        benefits.put(benefitName, value);
    }
    
    /**
     * Gets the value of a benefit.
     * 
     * @param benefitName The name of the benefit
     * @return The value of the benefit, or 0 if not found
     */
    public int getBenefitValue(String benefitName) {
        return benefits.getOrDefault(benefitName, 0);
    }
    
    /**
     * Gets all benefits for this level.
     * 
     * @return A map of all benefits and their values
     */
    public Map<String, Integer> getAllBenefits() {
        return new HashMap<>(benefits);
    }
}