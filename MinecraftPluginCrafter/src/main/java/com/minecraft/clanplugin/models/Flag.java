package com.minecraft.clanplugin.models;

import org.bukkit.Location;

import java.util.UUID;

/**
 * Represents a clan flag that marks and strengthens territory control.
 */
public class Flag {
    private final Location location;
    private final UUID placedBy;
    private final long placedTime;
    private int tier;  // Flag upgrade level
    private int influenceRadius;
    
    /**
     * Creates a new clan flag
     * 
     * @param location The location where the flag is placed
     * @param placedBy The UUID of the player who placed the flag
     */
    public Flag(Location location, UUID placedBy) {
        this.location = location.clone();
        this.placedBy = placedBy;
        this.placedTime = System.currentTimeMillis();
        this.tier = 1;  // Start at tier 1
        this.influenceRadius = 1;  // Default radius of 1 chunk
    }
    
    /**
     * Creates a flag from stored data
     */
    public Flag(Location location, UUID placedBy, long placedTime, int tier, int influenceRadius) {
        this.location = location.clone();
        this.placedBy = placedBy;
        this.placedTime = placedTime;
        this.tier = tier;
        this.influenceRadius = influenceRadius;
    }
    
    /**
     * Gets the location of this flag
     * 
     * @return The flag's location
     */
    public Location getLocation() {
        return location.clone();
    }
    
    /**
     * Gets the UUID of the player who placed this flag
     * 
     * @return The placer's UUID
     */
    public UUID getPlacedBy() {
        return placedBy;
    }
    
    /**
     * Gets the time when this flag was placed
     * 
     * @return The placement time in milliseconds
     */
    public long getPlacedTime() {
        return placedTime;
    }
    
    /**
     * Gets the tier/upgrade level of this flag
     * 
     * @return The flag tier
     */
    public int getTier() {
        return tier;
    }
    
    /**
     * Upgrades the flag to the next tier
     * 
     * @return True if the upgrade was successful
     */
    public boolean upgrade() {
        if (tier < 3) {  // Maximum tier is 3
            tier++;
            updateInfluenceRadius();
            return true;
        }
        return false;
    }
    
    /**
     * Gets the influence radius of this flag in chunks
     * 
     * @return The influence radius
     */
    public int getInfluenceRadius() {
        return influenceRadius;
    }
    
    /**
     * Updates the influence radius based on the current tier
     */
    private void updateInfluenceRadius() {
        // Tier 1: 1 chunk, Tier 2: 2 chunks, Tier 3: 3 chunks
        influenceRadius = tier;
    }
    
    /**
     * Calculates the influence this flag provides at a given distance
     * 
     * @param distance The distance in chunks
     * @return The influence value (0-100)
     */
    public int calculateInfluenceAtDistance(double distance) {
        if (distance <= 0) {
            // Direct flag chunk has maximum influence
            return 40 + (tier * 20);
        } else if (distance <= influenceRadius) {
            // Linearly decrease influence with distance
            double baseInfluence = 40 + (tier * 20);
            double influencePercentage = 1.0 - (distance / influenceRadius);
            return (int)(baseInfluence * influencePercentage);
        } else {
            // No influence beyond radius
            return 0;
        }
    }
    
    /**
     * Calculates the straight-line distance to another location in chunks
     * 
     * @param other The other location
     * @return The distance in chunks, or -1 if in different worlds
     */
    public double distanceToInChunks(Location other) {
        if (!other.getWorld().equals(location.getWorld())) {
            return -1;
        }
        
        // Get the chunk coordinates
        int thisChunkX = location.getBlockX() >> 4;
        int thisChunkZ = location.getBlockZ() >> 4;
        int otherChunkX = other.getBlockX() >> 4;
        int otherChunkZ = other.getBlockZ() >> 4;
        
        // Calculate Euclidean distance in chunks
        int dx = otherChunkX - thisChunkX;
        int dz = otherChunkZ - thisChunkZ;
        
        return Math.sqrt(dx * dx + dz * dz);
    }
}