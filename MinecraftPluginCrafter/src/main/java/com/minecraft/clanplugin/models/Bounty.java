package com.minecraft.clanplugin.models;

import java.util.UUID;

/**
 * Represents a bounty placed on a player.
 */
public class Bounty {
    
    private final UUID targetUUID;
    private final UUID placerUUID;
    private final double amount;
    private final long timestamp;
    private boolean active;
    private UUID claimedBy;
    private long claimedTimestamp;
    
    /**
     * Creates a new bounty.
     * 
     * @param targetUUID The UUID of the target player
     * @param placerUUID The UUID of the player who placed the bounty
     * @param amount The bounty amount
     */
    public Bounty(UUID targetUUID, UUID placerUUID, double amount) {
        this.targetUUID = targetUUID;
        this.placerUUID = placerUUID;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
        this.active = true;
        this.claimedBy = null;
        this.claimedTimestamp = 0;
    }
    
    /**
     * Creates a bounty from saved data.
     * 
     * @param targetUUID The UUID of the target player
     * @param placerUUID The UUID of the player who placed the bounty
     * @param amount The bounty amount
     * @param timestamp The time when the bounty was placed
     * @param active Whether the bounty is active
     * @param claimedBy The UUID of the player who claimed the bounty (or null)
     * @param claimedTimestamp The time when the bounty was claimed
     */
    public Bounty(UUID targetUUID, UUID placerUUID, double amount, long timestamp, 
                 boolean active, UUID claimedBy, long claimedTimestamp) {
        this.targetUUID = targetUUID;
        this.placerUUID = placerUUID;
        this.amount = amount;
        this.timestamp = timestamp;
        this.active = active;
        this.claimedBy = claimedBy;
        this.claimedTimestamp = claimedTimestamp;
    }
    
    /**
     * Gets the target player's UUID.
     * 
     * @return The target player's UUID
     */
    public UUID getTargetUUID() {
        return targetUUID;
    }
    
    /**
     * Gets the placer's UUID.
     * 
     * @return The UUID of the player who placed the bounty
     */
    public UUID getPlacerUUID() {
        return placerUUID;
    }
    
    /**
     * Gets the bounty amount.
     * 
     * @return The bounty amount
     */
    public double getAmount() {
        return amount;
    }
    
    /**
     * Gets the timestamp when the bounty was placed.
     * 
     * @return The timestamp when the bounty was placed
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Checks if the bounty is active.
     * 
     * @return True if the bounty is active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets whether the bounty is active.
     * 
     * @param active Whether the bounty is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }
    
    /**
     * Gets the UUID of the player who claimed the bounty.
     * 
     * @return The UUID of the player who claimed the bounty, or null if not claimed
     */
    public UUID getClaimedBy() {
        return claimedBy;
    }
    
    /**
     * Gets the timestamp when the bounty was claimed.
     * 
     * @return The timestamp when the bounty was claimed, or 0 if not claimed
     */
    public long getClaimedTimestamp() {
        return claimedTimestamp;
    }
    
    /**
     * Marks the bounty as claimed by a player.
     * 
     * @param claimerUUID The UUID of the player who claimed the bounty
     */
    public void claim(UUID claimerUUID) {
        this.active = false;
        this.claimedBy = claimerUUID;
        this.claimedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the age of the bounty in milliseconds.
     * 
     * @return The age of the bounty in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Gets the time since the bounty was claimed in milliseconds.
     * 
     * @return The time since the bounty was claimed in milliseconds, or 0 if not claimed
     */
    public long getTimeSinceClaimed() {
        if (claimedTimestamp == 0) {
            return 0;
        }
        return System.currentTimeMillis() - claimedTimestamp;
    }
    
    /**
     * Checks if the bounty was placed by a specific player.
     * 
     * @param playerUUID The UUID of the player to check
     * @return True if the bounty was placed by the specified player
     */
    public boolean isPlacedBy(UUID playerUUID) {
        return placerUUID.equals(playerUUID);
    }
    
    /**
     * Checks if the bounty is for a specific target.
     * 
     * @param playerUUID The UUID of the player to check
     * @return True if the bounty is for the specified target
     */
    public boolean isTarget(UUID playerUUID) {
        return targetUUID.equals(playerUUID);
    }
}