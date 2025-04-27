package com.minecraft.clanplugin.achievements;

/**
 * Difficulty levels for clan achievements.
 */
public enum AchievementDifficulty {
    EASY(1),
    MEDIUM(2),
    HARD(3),
    LEGENDARY(4);
    
    private final int stars;
    
    /**
     * Creates a new achievement difficulty.
     * 
     * @param stars The number of stars representing the difficulty
     */
    AchievementDifficulty(int stars) {
        this.stars = stars;
    }
    
    /**
     * Gets the number of stars representing the difficulty.
     * 
     * @return The number of stars
     */
    public int getStars() {
        return stars;
    }
}