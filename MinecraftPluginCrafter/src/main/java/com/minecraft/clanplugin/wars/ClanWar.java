package com.minecraft.clanplugin.wars;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a war between two clans.
 */
public class ClanWar {
    
    private final String initiatingClan;
    private final String targetClan;
    private final long startTime;
    private long endTime;
    private WarStatus status;
    private final Map<UUID, Integer> kills;
    private int initiatingClanScore;
    private int targetClanScore;
    
    /**
     * Creates a new clan war
     * 
     * @param initiatingClan The clan that declared war
     * @param targetClan The clan that was declared war on
     */
    public ClanWar(String initiatingClan, String targetClan) {
        this.initiatingClan = initiatingClan;
        this.targetClan = targetClan;
        this.startTime = System.currentTimeMillis();
        this.endTime = startTime + (7 * 24 * 60 * 60 * 1000); // Default: 7 days
        this.status = WarStatus.ACTIVE;
        this.kills = new HashMap<>();
        this.initiatingClanScore = 0;
        this.targetClanScore = 0;
    }
    
    /**
     * Creates a clan war from stored data
     */
    public ClanWar(String initiatingClan, String targetClan, long startTime, long endTime, 
                  WarStatus status, Map<UUID, Integer> kills, int initiatingClanScore, int targetClanScore) {
        this.initiatingClan = initiatingClan;
        this.targetClan = targetClan;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.kills = kills != null ? kills : new HashMap<>();
        this.initiatingClanScore = initiatingClanScore;
        this.targetClanScore = targetClanScore;
    }
    
    /**
     * Gets the clan that initiated the war
     * 
     * @return The initiating clan name
     */
    public String getInitiatingClan() {
        return initiatingClan;
    }
    
    /**
     * Gets the clan that was declared war on
     * 
     * @return The target clan name
     */
    public String getTargetClan() {
        return targetClan;
    }
    
    /**
     * Gets the time when the war started
     * 
     * @return The start time in milliseconds
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the time when the war ends/ended
     * 
     * @return The end time in milliseconds
     */
    public long getEndTime() {
        return endTime;
    }
    
    /**
     * Sets the time when the war ends
     * 
     * @param endTime The end time in milliseconds
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    /**
     * Gets the current status of the war
     * 
     * @return The war status
     */
    public WarStatus getStatus() {
        return status;
    }
    
    /**
     * Sets the status of the war
     * 
     * @param status The new war status
     */
    public void setStatus(WarStatus status) {
        this.status = status;
    }
    
    /**
     * Gets the kill counts for all players
     * 
     * @return A map of player UUIDs to kill counts
     */
    public Map<UUID, Integer> getKills() {
        return new HashMap<>(kills);
    }
    
    /**
     * Gets the kill count for a specific player
     * 
     * @param playerId The UUID of the player
     * @return The player's kill count
     */
    public int getPlayerKills(UUID playerId) {
        return kills.getOrDefault(playerId, 0);
    }
    
    /**
     * Adds a kill for a player
     * 
     * @param playerId The UUID of the player
     * @param clanName The clan the player belongs to
     */
    public void addKill(UUID playerId, String clanName) {
        int currentKills = kills.getOrDefault(playerId, 0);
        kills.put(playerId, currentKills + 1);
        
        // Update clan score
        if (clanName.equalsIgnoreCase(initiatingClan)) {
            initiatingClanScore++;
        } else if (clanName.equalsIgnoreCase(targetClan)) {
            targetClanScore++;
        }
    }
    
    /**
     * Gets the score for the initiating clan
     * 
     * @return The initiating clan's score
     */
    public int getInitiatingClanScore() {
        return initiatingClanScore;
    }
    
    /**
     * Gets the score for the target clan
     * 
     * @return The target clan's score
     */
    public int getTargetClanScore() {
        return targetClanScore;
    }
    
    /**
     * Checks if the war has ended
     * 
     * @return True if the war has ended
     */
    public boolean hasEnded() {
        return status != WarStatus.ACTIVE || System.currentTimeMillis() >= endTime;
    }
    
    /**
     * Gets the currently winning clan
     * 
     * @return The name of the winning clan, or null if tied
     */
    public String getWinningClan() {
        if (initiatingClanScore > targetClanScore) {
            return initiatingClan;
        } else if (targetClanScore > initiatingClanScore) {
            return targetClan;
        }
        return null; // Tied
    }
    
    /**
     * Gets the time remaining in the war
     * 
     * @return The time remaining in milliseconds
     */
    public long getTimeRemaining() {
        if (hasEnded()) {
            return 0;
        }
        
        return Math.max(0, endTime - System.currentTimeMillis());
    }
    
    /**
     * Gets a unique identifier for this war
     * 
     * @return A unique string identifier
     */
    public String getWarId() {
        return initiatingClan + "_vs_" + targetClan + "_" + startTime;
    }
    
    /**
     * Checks if a clan is involved in this war
     * 
     * @param clanName The name of the clan
     * @return True if the clan is involved
     */
    public boolean involves(String clanName) {
        return initiatingClan.equalsIgnoreCase(clanName) || targetClan.equalsIgnoreCase(clanName);
    }
    
    /**
     * Gets the opposing clan name
     * 
     * @param clanName One of the clans in the war
     * @return The opposing clan name, or null if the clan is not in the war
     */
    public String getOpposingClan(String clanName) {
        if (initiatingClan.equalsIgnoreCase(clanName)) {
            return targetClan;
        } else if (targetClan.equalsIgnoreCase(clanName)) {
            return initiatingClan;
        }
        return null;
    }
}