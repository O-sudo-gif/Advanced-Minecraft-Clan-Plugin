package com.minecraft.clanplugin.recruitment;

import java.util.UUID;

/**
 * Represents a recruitment challenge for the clan mini-game.
 */
public class RecruitmentChallenge {
    
    private final String id;
    private final String name;
    private final String description;
    private final int difficulty;
    private final ChallengeType type;
    private final int rewardPoints;
    
    /**
     * The type of recruitment challenge.
     */
    public enum ChallengeType {
        COMBAT,
        GATHERING,
        BUILDING,
        EXPLORATION,
        QUIZ
    }
    
    /**
     * Create a new recruitment challenge.
     * 
     * @param id The challenge ID
     * @param name The challenge name
     * @param description The challenge description
     * @param difficulty The challenge difficulty (1-5)
     * @param type The challenge type
     * @param rewardPoints The points awarded for completing this challenge
     */
    public RecruitmentChallenge(String id, String name, String description, int difficulty, ChallengeType type, int rewardPoints) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.difficulty = difficulty;
        this.type = type;
        this.rewardPoints = rewardPoints;
    }
    
    /**
     * Get the challenge ID.
     * 
     * @return The challenge ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Get the challenge name.
     * 
     * @return The challenge name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Get the challenge description.
     * 
     * @return The challenge description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the challenge difficulty.
     * 
     * @return The challenge difficulty (1-5)
     */
    public int getDifficulty() {
        return difficulty;
    }
    
    /**
     * Get the challenge type.
     * 
     * @return The challenge type
     */
    public ChallengeType getType() {
        return type;
    }
    
    /**
     * Get the points awarded for completing this challenge.
     * 
     * @return The reward points
     */
    public int getRewardPoints() {
        return rewardPoints;
    }
}